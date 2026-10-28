package com.example.vista.data.remote.tmdb

import com.example.vista.data.local.dao.RecentSearchDao
import com.example.vista.data.local.entity.RecentSearchEntity
import com.example.vista.data.remote.shoofaflam.ShoofaflamRepository
import com.example.vista.domain.model.*
import com.example.vista.domain.repository.SearchRepository
import com.example.vista.util.sameTitle
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TmdbSearchRepository @Inject constructor(
    private val apiService: TmdbApiService,
    private val shoofaflamRepository: ShoofaflamRepository,
    private val recentSearchDao: RecentSearchDao
) : SearchRepository {

    /**
     * Searches movies, series and people.
     *
     * Movies and TV are queried through their own typed endpoints (`/search/movie`, `/search/tv`),
     * never a blended endpoint, so a result can never be ambiguous about which namespace its id
     * belongs to — that ambiguity is what made series open unrelated films. `/search/person` is
     * used for people.
     *
     * Note: TMDB's search payloads carry no genre data, so [SearchFilters.genres] cannot be applied
     * here without a per-title details call; year, rating and sort filters are applied locally.
     */
    override suspend fun search(query: String, filters: SearchFilters): SearchResult = coroutineScope {
        val page = filters.page ?: 1
        
        // Use multi-search for "All" tab to reduce network overhead and improve speed.
        if (filters.type == SearchType.ALL) {
            try {
                val multiResults = async { runCatching { apiService.multiSearch(query, page) }.getOrNull() }
                val localMovies = async { runCatching { shoofaflamRepository.searchTitles(query, 5) }.getOrDefault(emptyList()) }
                
                val results = multiResults.await()?.results.orEmpty()
                val moviesList = mutableListOf<Movie>()
                val seriesList = mutableListOf<Series>()
                val peopleList = mutableListOf<Person>()
                
                results.forEach { res ->
                    when (res.mediaType) {
                        "movie" -> res.title?.let { moviesList.add(res.toDomainMovie()) }
                        "tv" -> res.name?.let { seriesList.add(res.toDomainSeries()) }
                        "person" -> res.name?.let { peopleList.add(res.toDomainPerson()) }
                    }
                }
                
                val localOnly = localMovies.await().filterNot { local ->
                    moviesList.any { sameTitle(it.title, it.releaseYear, local.title, local.releaseYear) }
                }
                
                val filteredMovies = applyFilters(moviesList + localOnly, filters, { it.title }, { it.releaseYear }, { it.rating })
                val filteredSeries = applyFilters(seriesList, filters, { it.title }, { it.firstAirYear }, { it.rating })
                
                return@coroutineScope SearchResult(
                    movies = filteredMovies.distinctBy { it.id },
                    series = filteredSeries.distinctBy { it.id },
                    people = peopleList.distinctBy { it.id },
                    totalCount = filteredMovies.size + filteredSeries.size + peopleList.size
                )
            } catch (e: Exception) {
                // Fallback to individual searches if multi-search fails
            }
        }

        val wantsMovies = filters.type == SearchType.MOVIES || filters.type == SearchType.ALL
        val wantsSeries = filters.type == SearchType.SERIES || filters.type == SearchType.ALL
        val wantsPeople = filters.type == SearchType.PEOPLE || filters.type == SearchType.ALL

        try {
            val movieResults = async {
                if (wantsMovies) runCatching { apiService.searchMovies(query, page) }.getOrNull() else null
            }
            val tvResults = async {
                if (wantsSeries) runCatching { apiService.searchTvShows(query, page) }.getOrNull() else null
            }
            val peopleResults = async {
                if (wantsPeople) runCatching { apiService.searchPeople(query, page) }.getOrNull() else null
            }
            val localMovies = async {
                if (wantsMovies) {
                    runCatching { shoofaflamRepository.searchTitles(query, 10) }.getOrDefault(emptyList())
                } else {
                    emptyList()
                }
            }

            val tmdbMovies = movieResults.await()?.results?.map { it.toDomainMovie() }.orEmpty()
            val tmdbSeries = tvResults.await()?.results?.map { it.toDomainSeries() }.orEmpty()
            val people = peopleResults.await()?.results?.map { it.toDomainPerson() }.orEmpty()

            // The local catalogue is a fallback: a title TMDB already found is dropped from it, and
            // the two are matched on title because they share no id space.
            val localOnly = localMovies.await().filterNot { local ->
                tmdbMovies.any { sameTitle(it.title, it.releaseYear, local.title, local.releaseYear) }
            }

            val movies = applyFilters(tmdbMovies + localOnly, filters, { it.title }, { it.releaseYear }, { it.rating })
            val series = applyFilters(tmdbSeries, filters, { it.title }, { it.firstAirYear }, { it.rating })

            SearchResult(
                movies = movies.distinctBy { it.id },
                series = series.distinctBy { it.id },
                people = people.distinctBy { it.id },
                totalCount = movies.size + series.size + people.size
            )
        } catch (e: Exception) {
            SearchResult(emptyList(), emptyList(), emptyList(), 0)
        }
    }

    private fun <T> applyFilters(
        items: List<T>,
        filters: SearchFilters,
        title: (T) -> String,
        year: (T) -> Int?,
        rating: (T) -> Double?
    ): List<T> {
        val filtered = items.filter { item ->
            val itemYear = year(item)
            val itemRating = rating(item) ?: 0.0
            (filters.yearFrom == null || (itemYear != null && itemYear >= filters.yearFrom)) &&
                (filters.yearTo == null || (itemYear != null && itemYear <= filters.yearTo)) &&
                (filters.ratingFrom == null || itemRating >= filters.ratingFrom)
        }
        // TMDB already returns results in relevance order, so only re-sort when asked to.
        return when (filters.sortBy) {
            SortBy.POPULARITY -> filtered
            SortBy.RATING -> filtered.sortedByDescending { rating(it) ?: 0.0 }
            SortBy.YEAR -> filtered.sortedByDescending { year(it) ?: 0 }
            SortBy.TITLE -> filtered.sortedBy { title(it).lowercase() }
        }
    }

    // Recent searches live in Room: the ordered query returns them newest-first and the primary key
    // on `query` de-duplicates. The previous DataStore backing stored them as an unordered Set, so
    // "most recent first" was not guaranteed and single entries could not be removed.

    override fun getRecentSearches(): Flow<List<String>> =
        recentSearchDao.getRecentSearches().map { entities -> entities.map { it.query } }

    override suspend fun addRecentSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.length < 2) return
        recentSearchDao.insert(RecentSearchEntity(query = trimmed, timestamp = System.currentTimeMillis()))
    }

    override suspend fun removeRecentSearch(query: String) {
        recentSearchDao.delete(query)
    }

    override suspend fun clearRecentSearches() {
        recentSearchDao.deleteAll()
    }

    override fun getTrendingSearches(): Flow<List<String>> {
        return flow {
            try {
                val trending = apiService.getTrendingMovies(1).results?.take(5)?.mapNotNull { it.title }
                    ?: emptyList()
                emit(trending)
            } catch (e: Exception) {
                // Fallback to static defaults when the network is unavailable.
                emit(listOf("Spider-Man", "Avengers", "Batman", "Inception", "Interstellar"))
            }
        }
    }
}
