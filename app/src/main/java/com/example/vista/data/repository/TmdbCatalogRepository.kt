package com.example.vista.data.repository

import com.example.vista.data.remote.shoofaflam.ShoofaflamRepository
import com.example.vista.data.remote.tmdb.TmdbApiService
import com.example.vista.data.remote.tmdb.TmdbImageUtils
import com.example.vista.domain.model.*
import com.example.vista.domain.repository.CatalogRepository
import com.example.vista.util.dedupeByTitle
import android.util.Log
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TmdbCatalogRepository @Inject constructor(
    private val apiService: TmdbApiService,
    private val shoofaflamRepository: ShoofaflamRepository,
    private val tvmazeRepository: TvmazeCatalogRepository,
    private val tvmazeApiService: com.example.vista.data.remote.tvmaze.TvmazeApiService,
    private val imdbApiService: com.example.vista.data.remote.imdb.ImdbApiService
) : CatalogRepository {

    private val tmdbIdCache = java.util.concurrent.ConcurrentHashMap<String, Int>()

    // @JvmName keeps the three erased generic overloads distinct on the JVM. The matching rules
    // themselves live in util/TitleMatching.kt so search and the catalogue cannot diverge.
    @JvmName("dedupeMovieTitles")
    private fun List<Movie>.dedupeTitles(): List<Movie> =
        dedupeByTitle({ it.title }, { it.releaseYear })

    @JvmName("dedupeSeriesTitles")
    private fun List<Series>.dedupeTitles(): List<Series> =
        dedupeByTitle({ it.title }, { it.firstAirYear })

    @JvmName("dedupeItemTitles")
    private fun List<MediaItem>.dedupeTitles(): List<MediaItem> = dedupeByTitle(
        title = { it.title },
        year = { item ->
            when (item) {
                is MediaItem.MovieItem -> item.year
                is MediaItem.SeriesItem -> item.year
                else -> null
            }
        }
    )

    private suspend fun resolveToTmdbId(id: String, isMovie: Boolean = true): Int? {
        val intId = id.toIntOrNull()
        if (intId != null) return intId

        if (id.startsWith("tt")) {
            // The same IMDb id is resolved from Details, Search and Watch History surfaces; cache
            // the result so each one does not repeat the findByExternalId round-trip. 0 marks a
            // failed lookup (ConcurrentHashMap forbids null values).
            val key = "${if (isMovie) "movie:" else "series:"}$id"
            tmdbIdCache[key]?.let { if (it != 0) return it }
            val resolved = try {
                val findResponse = apiService.findByExternalId(id)
                if (isMovie) {
                    findResponse.movieResults.firstOrNull()?.id
                } else {
                    findResponse.tvResults.firstOrNull()?.id
                }
            } catch (e: Exception) {
                null
            }
            tmdbIdCache[key] = resolved ?: 0
            return resolved
        }
        return null
    }

    /**
     * One page of any declared [CatalogSection]. Each section's content is defined exactly once
     * here, so the home row, its "See All" page and its load-more requests always agree.
     */
    override suspend fun getSection(section: CatalogSection, page: Int): List<MediaItem> {
        return try {
            val items: List<MediaItem> = when (section) {
                CatalogSection.TRENDING_MOVIES ->
                    apiService.getTrendingMovies(page).results?.map { it.toMediaItem() }.orEmpty()

                CatalogSection.TRENDING_SERIES ->
                    apiService.getTrendingTvShows(page).results?.map { it.toMediaItem() }.orEmpty()

                CatalogSection.TVMAZE_TRENDING -> if (page == 1) {
                    tvmazeRepository.searchSeries("trending").map { it.toMediaItem() }
                } else emptyList()

                CatalogSection.NEW_RELEASES ->
                    apiService.getUpcomingMovies(page).results?.map { it.toMediaItem() }.orEmpty()

                // Blend TVMaze hits into the popular series row
                CatalogSection.POPULAR_SERIES -> {
                    var tmdbFailure: Exception? = null
                    val tmdb = try {
                        apiService.getPopularTvShows(page).results?.map { it.toMediaItem() }.orEmpty()
                    } catch (e: CancellationException) { throw e }
                    catch (e: Exception) { tmdbFailure = e; emptyList() }
                    val tvmaze = if (page == 1) {
                        tvmazeRepository.searchSeries("trending").map { it.toMediaItem() }
                    } else emptyList()
                    if (tmdb.isEmpty() && tvmaze.isEmpty()) tmdbFailure?.let { throw it }
                    (tmdb + tvmaze).dedupeTitles()
                }

                CatalogSection.POPULAR_MOVIES -> if (page == 1) {
                    getPopularMovies(1).map { it.toMediaItem() }
                } else {
                    apiService.getPopularMovies(page).results?.map { it.toMediaItem() }.orEmpty()
                }

                CatalogSection.TOP_RATED_MOVIES ->
                    apiService.getTopRatedMovies(page).results?.map { it.toMediaItem() }.orEmpty()

                // Genre ids come from TMDB: 28 action, 35 comedy, 27 horror, 16 animation, 18 drama.
                // Movie and series genres are requested from their own endpoints so a row never
                // silently mixes both types under one label.
                CatalogSection.ACTION ->
                    apiService.discoverMovies("28", page).results?.map { it.toMediaItem() }.orEmpty()

                CatalogSection.COMEDY ->
                    apiService.discoverMovies("35", page).results?.map { it.toMediaItem() }.orEmpty()

                CatalogSection.HORROR ->
                    apiService.discoverMovies("27", page).results?.map { it.toMediaItem() }.orEmpty()

                CatalogSection.ANIMATION ->
                    apiService.discoverMovies("16", page).results?.map { it.toMediaItem() }.orEmpty()

                CatalogSection.DRAMA_SERIES ->
                    apiService.discoverTvShows("18", page).results?.map { it.toMediaItem() }.orEmpty()
            }
            items.dedupeTitles().take(20)
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) {
            Log.e("TmdbCatalog", "Section ${section.id} failed: ${e.javaClass.simpleName}" +
                if (e is retrofit2.HttpException) " HTTP ${e.code()}" else "")
            throw CatalogLoadException.from(e)
        }
    }

    override suspend fun getHomeSections(): List<HomeSection> {
        return supervisorScope {
            // Every declared row is fetched concurrently and independently: one failing endpoint
            // drops that row only instead of emptying the whole home screen.
            val results = CatalogSection.entries
                .map { section -> async {
                    val result = try { Result.success(getSection(section)) }
                    catch (e: CancellationException) { throw e }
                    catch (e: Exception) { Result.failure<List<MediaItem>>(e) }
                    section to result
                } }
                .associate { it.await() }
            val loaded = results.mapValues { it.value.getOrDefault(emptyList()) }

            val sections = mutableListOf<HomeSection>()

            // Hero carousel is curated from the trending movies row rather than fetched again.
            val trendingMovies = loaded[CatalogSection.TRENDING_MOVIES].orEmpty()
            if (trendingMovies.isNotEmpty()) {
                sections.add(
                    HomeSection(
                        CatalogSection.FEATURED_ID,
                        "Featured Today",
                        HomeSectionType.FEATURED_CAROUSEL,
                        trendingMovies.take(5)
                    )
                )
            }

            // Then the declared rows, in declaration order.
            for (section in CatalogSection.entries) {
                val items = loaded[section].orEmpty()
                if (items.isNotEmpty()) {
                    sections.add(HomeSection(section.id, section.title, section.type, items))
                }
            }

            if (sections.isEmpty()) {
                results.values.firstNotNullOfOrNull { it.exceptionOrNull() }?.let { throw it }
            }
            sections
        }
    }

    override suspend fun getFeaturedMovies(): List<Movie> {
        return try {
            apiService.getTrendingMovies().results?.take(5)?.map { it.toDomainMovie() } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getTrendingMovies(page: Int): List<Movie> {
        return try {
            apiService.getTrendingMovies(page).results?.map { it.toDomainMovie() } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getNewReleases(page: Int): List<Movie> {
        return try {
            apiService.getUpcomingMovies(page).results?.map { it.toDomainMovie() } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getPopularMovies(page: Int): List<Movie> {
        var tmdbFailure: Exception? = null
        val tmdbMovies = try {
            apiService.getPopularMovies(page).results?.map { it.toDomainMovie() } ?: emptyList()
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) {
            tmdbFailure = e
            emptyList()
        }

        val shoofaflamMovies = try {
            shoofaflamRepository.getPopularMovies(20)
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) {
            Log.e("TmdbCatalog", "Error fetching popular Shoofaflam movies: ${e.message}")
            emptyList()
        }

        // TMDB first, then local extras, deduplicated by title (the two sources share no id space).
        val movies = (tmdbMovies.dedupeTitles() + shoofaflamMovies).dedupeTitles()
        if (movies.isEmpty()) tmdbFailure?.let { throw CatalogLoadException.from(it) }
        return movies
    }

    override suspend fun getPopularSeries(page: Int): List<Series> {
        val tmdbSeries = try {
            apiService.getPopularTvShows(page).results?.map { it.toDomainSeries() } ?: emptyList()
        } catch (e: Exception) {
            Log.e("TmdbCatalog", "Error fetching popular TMDB series: ${e.message}")
            emptyList()
        }

        val shoofaflamSeries = try {
            shoofaflamRepository.getPopularSeries(20)
        } catch (e: Exception) {
            Log.e("TmdbCatalog", "Error fetching popular Shoofaflam series: ${e.message}")
            emptyList()
        }

        // TMDB first, then local extras, deduplicated by title (the two sources share no id space).
        return (tmdbSeries.dedupeTitles() + shoofaflamSeries).dedupeTitles()
    }

    override suspend fun getMovieDetails(id: String): Movie? {
        return try {
            val movieId = resolveToTmdbId(id, isMovie = true)
            if (movieId != null) {
                val movie = apiService.getMovieDetails(movieId)
                val credits = try { apiService.getMovieCredits(movieId) } catch (_: Exception) { null }

                movie.toDomainMovie().copy(
                    id = id,
                    cast = credits?.cast?.map { it.toDomainPerson() } ?: emptyList(),
                    director = credits?.crew?.filter { it.job == "Director" }?.mapNotNull { it.name }?.distinct()?.joinToString()?.takeIf { it.isNotBlank() }
                )
            } else {
                // Try IMDB search as fallback
                try {
                    val imdb = imdbApiService.search(id).description?.firstOrNull { it.title.equals(id, ignoreCase = true) }
                    
                    if (imdb?.imdbId != null) {
                        val tmdbId = resolveToTmdbId(imdb.imdbId, isMovie = true)
                        if (tmdbId != null) return getMovieDetails(tmdbId.toString())
                    }
                } catch (_: Exception) {}

                shoofaflamRepository.getTitleBySlug(id)?.toDomainMovie()
            }
        } catch (e: Exception) {
            shoofaflamRepository.getTitleBySlug(id)?.toDomainMovie()
        }
    }

    override suspend fun getSeriesDetails(id: String): Series? {
        // Handle TVMaze specific IDs
        if (id.startsWith("tvmaze_")) {
            val tvmazeId = id.removePrefix("tvmaze_").toIntOrNull()
            if (tvmazeId != null) return tvmazeRepository.getSeriesDetails(tvmazeId)
        }

        return try {
            val seriesId = resolveToTmdbId(id, isMovie = false)
            if (seriesId != null) {
                val series = apiService.getTvShowDetails(seriesId)
                val credits = try { apiService.getTvShowCredits(seriesId) } catch (_: Exception) { null }

                // Same as movies: the requested id stays the canonical id everywhere.
                series.toDomainSeries().copy(
                    id = id,
                    cast = credits?.cast?.map { it.toDomainPerson() } ?: emptyList()
                )
            } else {
                // Try TVMaze search by name as a fallback for missing TMDB IDs (e.g. slugs)
                val searchName = id.replace("-", " ")
                val fallback = tvmazeRepository.searchSeries(searchName).firstOrNull { 
                    it.title.equals(searchName, ignoreCase = true) || it.id.contains(id) 
                }
                fallback ?: shoofaflamRepository.getTitleBySlug(id)?.toDomainSeries()
            }
        } catch (e: Exception) {
            shoofaflamRepository.getTitleBySlug(id)?.toDomainSeries()
        }
    }

    override suspend fun getSeasonDetails(seriesId: String, seasonNumber: Int): Season? {
        return try {
            val id = resolveToTmdbId(seriesId, isMovie = false) ?: return null
            val season = apiService.getSeasonDetails(id, seasonNumber)
            season.toDomainSeason(seriesId)
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun getEpisodes(seriesId: String, seasonNumber: Int): List<Episode> {
        // 1. Try TMDB
        val tmdbEpisodes = try {
            val id = resolveToTmdbId(seriesId, isMovie = false)
            if (id != null) {
                val response = apiService.getEpisodes(id, seasonNumber)
                response.episodes?.map { it.toDomainEpisode(seriesId) }
            } else null
        } catch (e: Exception) {
            null
        }

        if (!tmdbEpisodes.isNullOrEmpty()) return tmdbEpisodes

        // 2. Try TVMaze fallback
        return try {
            if (seriesId.startsWith("tvmaze_")) {
                val tvmazeId = seriesId.removePrefix("tvmaze_").toIntOrNull()
                if (tvmazeId != null) {
                    tvmazeApiService.getEpisodes(tvmazeId).filter { it.season == seasonNumber }.map { it.toDomainEpisode(seriesId) }
                } else emptyList()
            } else {
                // If it's a name, search TVMaze
                val show = tvmazeRepository.searchSeries(seriesId).firstOrNull()
                if (show != null) {
                    val tvmazeId = show.id.removePrefix("tvmaze_").toIntOrNull()
                    if (tvmazeId != null) {
                        tvmazeApiService.getEpisodes(tvmazeId).filter { it.season == seasonNumber }.map { it.toDomainEpisode(seriesId) }
                    } else emptyList()
                } else emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getSimilarMovies(id: String): List<Movie> {
        return try {
            val movieId = resolveToTmdbId(id, isMovie = true) ?: return emptyList()
            apiService.getSimilarMovies(movieId).results?.map { it.toDomainMovie() } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getSimilarSeries(id: String): List<Series> {
        return try {
            val seriesId = resolveToTmdbId(id, isMovie = false) ?: return emptyList()
            apiService.getSimilarTvShows(seriesId).results?.map { it.toDomainSeries() } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getMoviesByGenre(genreId: String, page: Int): List<Movie> {
        return try {
            apiService.discoverMovies(genreId, page).results?.map { it.toDomainMovie() } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getSeriesByGenre(genreId: String, page: Int): List<Series> {
        return try {
            apiService.discoverTvShows(genreId, page).results?.map { it.toDomainSeries() } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun searchMovies(query: String, page: Int): List<Movie> {
        return try {
            apiService.searchMovies(query, page).results?.map { it.toDomainMovie() } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun searchSeries(query: String, page: Int): List<Series> {
        return try {
            val tmdbSeries = apiService.searchTvShows(query, page).results?.map { it.toDomainSeries() } ?: emptyList()
            
            // TVMaze as an additional source for series
            val tvmazeSeries = if (page == 1) {
                tvmazeRepository.searchSeries(query)
            } else {
                emptyList()
            }
            
            (tmdbSeries + tvmazeSeries).dedupeTitles()
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getGenres(): List<Genre> {
        return try {
            val movieGenres = apiService.getMovieGenres().genres
            val tvGenres = apiService.getTvGenres().genres
            (movieGenres + tvGenres).distinctBy { it.id }.map { it.toDomainGenre() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getCast(mediaId: String, mediaType: MediaType): List<Person> {
        return try {
            val isMovie = mediaType == MediaType.MOVIE
            val id = resolveToTmdbId(mediaId, isMovie = isMovie) ?: return emptyList()
            // Never fall back to the other namespace: a TV id would silently return a movie's cast.
            if (isMovie) {
                apiService.getMovieCredits(id).cast?.map { it.toDomainPerson() } ?: emptyList()
            } else {
                apiService.getTvShowCredits(id).cast?.map { it.toDomainPerson() } ?: emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getRecommendedMovies(movieId: String): List<Movie> {
        return try {
            val id = resolveToTmdbId(movieId, isMovie = true) ?: return emptyList()
            apiService.getRecommendedMovies(id).results?.map { it.toDomainMovie() } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getRecommendedSeries(seriesId: String): List<Series> {
        return try {
            val id = resolveToTmdbId(seriesId, isMovie = false) ?: return emptyList()
            apiService.getRecommendedTvShows(id).results?.map { it.toDomainSeries() } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getMovieVideos(movieId: String): List<Video> {
        return try {
            val id = resolveToTmdbId(movieId, isMovie = true) ?: return emptyList()
            apiService.getMovieVideos(id).results?.map { video ->
                Video(
                    id = video.id,
                    key = video.key,
                    name = video.name,
                    site = video.site,
                    type = video.type,
                    official = video.official
                )
            } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getTvShowVideos(tvShowId: String): List<Video> {
        return try {
            val id = resolveToTmdbId(tvShowId, isMovie = false) ?: return emptyList()
            apiService.getTvShowVideos(id).results?.map { video ->
                Video(
                    id = video.id,
                    key = video.key,
                    name = video.name,
                    site = video.site,
                    type = video.type,
                    official = video.official
                )
            } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getPersonDetails(personId: String): PersonDetails? {
        return try {
            val id = personId.toIntOrNull() ?: return null
            val person = apiService.getPersonDetails(id)
            val movieCredits = try { apiService.getPersonMovieCredits(id) } catch (_: Exception) { null }
            val tvCredits = try { apiService.getPersonTvCredits(id) } catch (_: Exception) { null }
            
            person.toDomainPersonDetails().copy(
                movieCredits = (movieCredits?.cast ?: emptyList()).map { it.toDomainMovieCredit() },
                tvCredits = (tvCredits?.cast ?: emptyList()).map { it.toDomainTvCredit() }
            )
        } catch (e: Exception) {
            null
        }
    }
}
