package com.example.vista.data.remote.tmdb

import com.google.gson.annotations.SerializedName

// TMDB Image URL builders
object TmdbImageUtils {
    private const val IMAGE_BASE_URL = "https://image.tmdb.org/t/p"
    
    fun posterUrl(path: String?, size: String = "w500"): String? {
        return path?.let { "$IMAGE_BASE_URL/$size$it" }
    }
    
    fun backdropUrl(path: String?, size: String = "w1280"): String? {
        return path?.let { "$IMAGE_BASE_URL/$size$it" }
    }
    
    fun profileUrl(path: String?, size: String = "w185"): String? {
        return path?.let { "$IMAGE_BASE_URL/$size$it" }
    }
    
    fun stillUrl(path: String?, size: String = "w300"): String? {
        return path?.let { "$IMAGE_BASE_URL/$size$it" }
    }
}

// Response wrapper
data class TmdbResponse<T>(
    val page: Int,
    val results: List<T>? = null,
    @SerializedName("total_pages") val totalPages: Int? = 0,
    @SerializedName("total_results") val totalResults: Int? = 0
)

// Movie models
data class TmdbMovie(
    val id: Int,
    val title: String? = null,
    val overview: String? = null,
    @SerializedName("poster_path") val posterPath: String? = null,
    @SerializedName("backdrop_path") val backdropPath: String? = null,
    @SerializedName("release_date") val releaseDate: String? = null,
    @SerializedName("vote_average") val voteAverage: Double? = 0.0,
    @SerializedName("vote_count") val voteCount: Int? = 0,
    @SerializedName("genre_ids") val genreIds: List<Int> = emptyList()
) {
    fun toDomainMovie(): com.example.vista.domain.model.Movie {
        return com.example.vista.domain.model.Movie(
            id = id.toString(),
            title = title ?: "Unknown",
            overview = overview ?: "",
            posterUrl = TmdbImageUtils.posterUrl(posterPath),
            backdropUrl = TmdbImageUtils.backdropUrl(backdropPath),
            releaseYear = releaseDate?.takeIf { it.isNotEmpty() && it.length >= 4 }?.let {
                try { it.substring(0, 4).toIntOrNull() } catch (_: Exception) { null }
            },
            runtimeMinutes = null,
            rating = voteAverage ?: 0.0,
            voteCount = voteCount ?: 0,
            genres = emptyList(), // Will be populated in detail endpoint
            cast = emptyList(), // Will be populated in detail endpoint
            director = null,
            ageRating = null
        )
    }
    
    fun toMediaItem(): com.example.vista.domain.model.MediaItem.MovieItem {
        return com.example.vista.domain.model.MediaItem.MovieItem(
            id = id.toString(),
            title = title ?: "Unknown",
            posterUrl = TmdbImageUtils.posterUrl(posterPath),
            rating = voteAverage ?: 0.0,
            year = releaseDate?.takeIf { it.isNotEmpty() && it.length >= 4 }?.let {
                try { it.substring(0, 4).toIntOrNull() } catch (_: Exception) { null }
            },
            duration = null,
            backdropUrl = TmdbImageUtils.backdropUrl(backdropPath)
        )
    }
}

// TV Show models
data class TmdbTvShow(
    val id: Int,
    val name: String? = null,
    val overview: String? = null,
    @SerializedName("poster_path") val posterPath: String? = null,
    @SerializedName("backdrop_path") val backdropPath: String? = null,
    @SerializedName("first_air_date") val firstAirDate: String? = null,
    @SerializedName("vote_average") val voteAverage: Double? = 0.0,
    @SerializedName("vote_count") val voteCount: Int? = 0,
    @SerializedName("genre_ids") val genreIds: List<Int> = emptyList(),
    @SerializedName("number_of_seasons") val numberOfSeasons: Int? = null,
    @SerializedName("number_of_episodes") val numberOfEpisodes: Int? = null
) {
    fun toDomainSeries(): com.example.vista.domain.model.Series {
        return com.example.vista.domain.model.Series(
            id = id.toString(),
            title = name ?: "Unknown",
            overview = overview ?: "",
            posterUrl = TmdbImageUtils.posterUrl(posterPath),
            backdropUrl = TmdbImageUtils.backdropUrl(backdropPath),
            firstAirYear = firstAirDate?.takeIf { it.isNotEmpty() && it.length >= 4 }?.let {
                try { it.substring(0, 4).toIntOrNull() } catch (_: Exception) { null }
            },
            lastAirYear = null,
            rating = voteAverage ?: 0.0,
            voteCount = voteCount ?: 0,
            genres = emptyList(),
            cast = emptyList(),
            creators = emptyList(),
            totalSeasons = numberOfSeasons ?: 1,
            totalEpisodes = numberOfEpisodes ?: 0,
            status = null,
            ageRating = null
        )
    }
    
    fun toMediaItem(): com.example.vista.domain.model.MediaItem.SeriesItem {
        return com.example.vista.domain.model.MediaItem.SeriesItem(
            id = id.toString(),
            title = name ?: "Unknown",
            posterUrl = TmdbImageUtils.posterUrl(posterPath),
            rating = voteAverage ?: 0.0,
            year = firstAirDate?.takeIf { it.isNotEmpty() && it.length >= 4 }?.let {
                try { it.substring(0, 4).toIntOrNull() } catch (_: Exception) { null }
            },
            totalSeasons = numberOfSeasons,
            backdropUrl = TmdbImageUtils.backdropUrl(backdropPath)
        )
    }
}

// Movie Detail Response
data class TmdbMovieDetail(
    val id: Int,
    val title: String? = null,
    val overview: String? = null,
    @SerializedName("poster_path") val posterPath: String? = null,
    @SerializedName("backdrop_path") val backdropPath: String? = null,
    @SerializedName("release_date") val releaseDate: String? = null,
    val runtime: Int? = null,
    @SerializedName("vote_average") val voteAverage: Double? = 0.0,
    @SerializedName("vote_count") val voteCount: Int? = 0,
    val genres: List<TmdbGenre>? = null,
    val status: String? = null,
    @SerializedName("imdb_id") val imdbId: String? = null,
    @SerializedName("production_companies") val productionCompanies: List<TmdbCompany>? = null
) {
    fun toDomainMovie(): com.example.vista.domain.model.Movie {
        return com.example.vista.domain.model.Movie(
            id = imdbId ?: id.toString(),
            title = title ?: "Unknown",
            overview = overview ?: "",
            posterUrl = TmdbImageUtils.posterUrl(posterPath),
            backdropUrl = TmdbImageUtils.backdropUrl(backdropPath),
            releaseYear = releaseDate?.takeIf { it.isNotEmpty() && it.length >= 4 }?.let {
                try { it.substring(0, 4).toIntOrNull() } catch (_: Exception) { null }
            },
            runtimeMinutes = runtime,
            rating = voteAverage ?: 0.0,
            voteCount = voteCount ?: 0,
            genres = genres?.map { it.toDomainGenre() } ?: emptyList(),
            cast = emptyList(), // Will be populated separately
            director = productionCompanies?.firstOrNull()?.name,
            ageRating = status
        )
    }
}

// TV Show Detail Response
data class TmdbTvShowDetail(
    val id: Int,
    val name: String? = null,
    val overview: String? = null,
    @SerializedName("poster_path") val posterPath: String? = null,
    @SerializedName("backdrop_path") val backdropPath: String? = null,
    @SerializedName("first_air_date") val firstAirDate: String? = null,
    @SerializedName("last_air_date") val lastAirDate: String? = null,
    @SerializedName("number_of_seasons") val numberOfSeasons: Int? = 0,
    @SerializedName("number_of_episodes") val numberOfEpisodes: Int? = 0,
    @SerializedName("vote_average") val voteAverage: Double? = 0.0,
    @SerializedName("vote_count") val voteCount: Int? = 0,
    val genres: List<TmdbGenre>? = null,
    val status: String? = null,
    val creators: List<TmdbCreator>? = null,
    @SerializedName("external_ids") val externalIds: TmdbExternalIds? = null
) {
    fun toDomainSeries(): com.example.vista.domain.model.Series {
        return com.example.vista.domain.model.Series(
            id = externalIds?.imdbId ?: id.toString(),
            title = name ?: "Unknown",
            overview = overview ?: "",
            posterUrl = TmdbImageUtils.posterUrl(posterPath),
            backdropUrl = TmdbImageUtils.backdropUrl(backdropPath),
            firstAirYear = firstAirDate?.takeIf { it.isNotEmpty() && it.length >= 4 }?.let {
                try { it.substring(0, 4).toIntOrNull() } catch (_: Exception) { null }
            },
            lastAirYear = lastAirDate?.takeIf { it.isNotEmpty() && it.length >= 4 }?.let {
                try { it.substring(0, 4).toIntOrNull() } catch (_: Exception) { null }
            },
            rating = voteAverage ?: 0.0,
            voteCount = voteCount ?: 0,
            genres = genres?.map { it.toDomainGenre() } ?: emptyList(),
            cast = emptyList(),
            creators = creators?.mapNotNull { it.name } ?: emptyList(),
            totalSeasons = numberOfSeasons ?: 1,
            totalEpisodes = numberOfEpisodes ?: 0,
            status = status,
            ageRating = null
        )
    }
}

data class TmdbExternalIds(
    @SerializedName("imdb_id") val imdbId: String? = null
)

// Genre
data class TmdbGenre(
    val id: Int,
    val name: String
) {
    fun toDomainGenre(): com.example.vista.domain.model.Genre {
        return com.example.vista.domain.model.Genre(id.toString(), name)
    }
}

// Company
data class TmdbCompany(
    val id: Int,
    val name: String
)

// Creator
data class TmdbCreator(
    val id: Int,
    val name: String?
)

// Cast/Credits
data class TmdbCastResponse(
    val id: Int,
    val cast: List<TmdbCastMember>? = null
)

data class TmdbCastMember(
    val id: Int,
    val name: String,
    @SerializedName("profile_path") val profilePath: String?,
    val character: String?,
    @SerializedName("known_for_department") val department: String?
) {
    fun toDomainPerson(): com.example.vista.domain.model.Person {
        return com.example.vista.domain.model.Person(
            id = id.toString(),
            name = name,
            profileUrl = TmdbImageUtils.profileUrl(profilePath),
            character = character,
            department = department
        )
    }
}

// Season/Episode models
data class TmdbSeasonResponse(
    val id: Int,
    val name: String? = null,
    val overview: String? = null,
    @SerializedName("poster_path") val posterPath: String? = null,
    @SerializedName("episode_count") val episodeCount: Int? = 0,
    @SerializedName("season_number") val seasonNumber: Int,
    @SerializedName("air_date") val airDate: String? = null
) {
    fun toDomainSeason(seriesId: String): com.example.vista.domain.model.Season {
        return com.example.vista.domain.model.Season(
            seriesId = seriesId,
            seasonNumber = seasonNumber,
            title = name ?: "Season $seasonNumber",
            overview = overview,
            posterUrl = TmdbImageUtils.posterUrl(posterPath),
            episodeCount = episodeCount ?: 0,
            airYear = airDate?.takeIf { it.isNotEmpty() && it.length >= 4 }?.let {
                try { it.substring(0, 4).toIntOrNull() } catch (_: Exception) { null }
            }
        )
    }
}

data class TmdbEpisodeResponse(
    val episodes: List<TmdbEpisode>? = null
)

data class TmdbEpisode(
    val id: Int,
    @SerializedName("episode_number") val episodeNumber: Int,
    @SerializedName("season_number") val seasonNumber: Int,
    val name: String? = null,
    val overview: String? = null,
    @SerializedName("still_path") val stillPath: String? = null,
    @SerializedName("runtime") val runtime: Int? = null,
    @SerializedName("vote_average") val voteAverage: Double? = null,
    @SerializedName("air_date") val airDate: String? = null
) {
    fun toDomainEpisode(seriesId: String): com.example.vista.domain.model.Episode {
        return com.example.vista.domain.model.Episode(
            seriesId = seriesId,
            seasonNumber = seasonNumber,
            episodeNumber = episodeNumber,
            title = name ?: "Episode $episodeNumber",
            overview = overview,
            thumbnailUrl = TmdbImageUtils.stillUrl(stillPath, "w300"),
            runtimeMinutes = runtime,
            rating = voteAverage,
            airDate = airDate,
            stillUrl = TmdbImageUtils.stillUrl(stillPath, "w780")
        )
    }
}

// Video/Trailer models
data class TmdbVideo(
    val id: String,
    val key: String,
    val name: String,
    val site: String,
    val type: String,
    val official: Boolean,
    @SerializedName("published_at") val publishedAt: String?,
    @SerializedName("iso_639_1") val iso6391: String?,
    @SerializedName("iso_3166_1") val iso31661: String?,
    @SerializedName("movie_id") val movieId: Int?,
    @SerializedName("tv_id") val tvId: Int?
) {
    fun isYouTubeTrailer(): Boolean {
        return site == "YouTube" && (type == "Trailer" || type == "Teaser")
    }
    
    fun getYouTubeUrl(): String? {
        return if (site == "YouTube") "https://www.youtube.com/watch?v=$key" else null
    }
    
    fun getYouTubeThumbnailUrl(): String? {
        return if (site == "YouTube") "https://img.youtube.com/vi/$key/mqdefault.jpg" else null
    }
    
    fun getYouTubeEmbedUrl(): String? {
        return if (site == "YouTube") "https://www.youtube.com/embed/$key" else null
    }
}

// Person models
data class TmdbPersonDetail(
    val id: Int,
    val name: String,
    @SerializedName("profile_path") val profilePath: String?,
    val biography: String?,
    val birthday: String?,
    val deathday: String?,
    @SerializedName("place_of_birth") val placeOfBirth: String?,
    val popularity: Double?,
    @SerializedName("known_for_department") val knownForDepartment: String?,
    @SerializedName("also_known_as") val alsoKnownAs: List<String> = emptyList()
) {
    fun toDomainPersonDetails(): com.example.vista.domain.model.PersonDetails {
        return com.example.vista.domain.model.PersonDetails(
            id = id.toString(),
            name = name,
            profileUrl = TmdbImageUtils.profileUrl(profilePath, "w500"),
            biography = biography,
            birthday = birthday,
            deathday = deathday,
            placeOfBirth = placeOfBirth,
            popularity = popularity,
            knownForDepartment = knownForDepartment,
            alsoKnownAs = alsoKnownAs,
            movieCredits = emptyList(), // Will be populated separately
            tvCredits = emptyList() // Will be populated separately
        )
    }
}

data class TmdbPersonMovieCreditsResponse(
    val cast: List<TmdbPersonMovieCredit>? = null,
    val crew: List<TmdbPersonMovieCredit>? = null
)

data class TmdbPersonMovieCredit(
    val id: Int,
    val title: String?,
    val character: String?,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("release_date") val releaseDate: String?,
    @SerializedName("vote_average") val voteAverage: Double?
) {
    fun toDomainMovieCredit(): com.example.vista.domain.model.PersonMovieCredit {
        return com.example.vista.domain.model.PersonMovieCredit(
            id = id.toString(),
            title = title ?: "Unknown",
            character = character,
            posterUrl = TmdbImageUtils.posterUrl(posterPath),
            releaseYear = releaseDate?.takeIf { it.isNotEmpty() }?.let {
                try { it.substring(0, 4).toIntOrNull() } catch (_: Exception) { null }
            },
            rating = voteAverage
        )
    }
}

data class TmdbPersonTvCreditsResponse(
    val cast: List<TmdbPersonTvCredit>? = null,
    val crew: List<TmdbPersonTvCredit>? = null
)

data class TmdbPersonTvCredit(
    val id: Int,
    val name: String?,
    val character: String?,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("first_air_date") val firstAirDate: String?,
    @SerializedName("vote_average") val voteAverage: Double?
) {
    fun toDomainTvCredit(): com.example.vista.domain.model.PersonTvCredit {
        return com.example.vista.domain.model.PersonTvCredit(
            id = id.toString(),
            name = name ?: "Unknown",
            character = character,
            posterUrl = TmdbImageUtils.posterUrl(posterPath),
            firstAirYear = firstAirDate?.takeIf { it.isNotEmpty() }?.let {
                try { it.substring(0, 4).toIntOrNull() } catch (_: Exception) { null }
            },
            rating = voteAverage
        )
    }
}

// Multi-search response
data class TmdbMultiSearchResult(
    val id: Int,
    @SerializedName("media_type") val mediaType: String,
    val title: String? = null,
    val name: String? = null,
    val overview: String? = null,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("backdrop_path") val backdropPath: String?,
    @SerializedName("release_date") val releaseDate: String?,
    @SerializedName("first_air_date") val firstAirDate: String?,
    @SerializedName("profile_path") val profilePath: String? = null,
    @SerializedName("vote_average") val voteAverage: Double,
    @SerializedName("vote_count") val voteCount: Int
) {
    fun toDomainMovie(): com.example.vista.domain.model.Movie {
        return com.example.vista.domain.model.Movie(
            id = id.toString(),
            title = title ?: "Unknown",
            overview = overview ?: "",
            posterUrl = TmdbImageUtils.posterUrl(posterPath),
            backdropUrl = TmdbImageUtils.backdropUrl(backdropPath),
            releaseYear = releaseDate?.takeIf { it.isNotEmpty() && it.length >= 4 }?.let {
                try { it.substring(0, 4).toIntOrNull() } catch (_: Exception) { null }
            },
            runtimeMinutes = null,
            rating = voteAverage,
            voteCount = voteCount,
            genres = emptyList(),
            cast = emptyList(),
            director = null,
            ageRating = null
        )
    }

    fun toDomainSeries(): com.example.vista.domain.model.Series {
        return com.example.vista.domain.model.Series(
            id = id.toString(),
            title = name ?: "Unknown",
            overview = overview ?: "",
            posterUrl = TmdbImageUtils.posterUrl(posterPath),
            backdropUrl = TmdbImageUtils.backdropUrl(backdropPath),
            firstAirYear = firstAirDate?.takeIf { it.isNotEmpty() && it.length >= 4 }?.let {
                try { it.substring(0, 4).toIntOrNull() } catch (_: Exception) { null }
            },
            lastAirYear = null,
            rating = voteAverage,
            voteCount = voteCount,
            genres = emptyList(),
            cast = emptyList(),
            creators = emptyList(),
            totalSeasons = 1,
            totalEpisodes = 0,
            status = null,
            ageRating = null
        )
    }

    fun toDomainPerson(): com.example.vista.domain.model.Person {
        return com.example.vista.domain.model.Person(
            id = id.toString(),
            name = name ?: "Unknown",
            profileUrl = TmdbImageUtils.profileUrl(profilePath ?: posterPath),
            character = null,
            department = null
        )
    }

    fun toMediaItem(): com.example.vista.domain.model.MediaItem? {
        return when (mediaType) {
            "movie" -> {
                val movieTitle = title ?: return null
                com.example.vista.domain.model.MediaItem.MovieItem(
                    id = id.toString(),
                    title = movieTitle,
                    posterUrl = TmdbImageUtils.posterUrl(posterPath),
                    rating = voteAverage,
                    year = releaseDate?.takeIf { it.isNotEmpty() }?.let {
                        try { it.substring(0, 4).toIntOrNull() } catch (_: Exception) { null }
                    },
                    duration = null,
                    backdropUrl = TmdbImageUtils.backdropUrl(backdropPath)
                )
            }
            "tv" -> {
                val tvName = name ?: return null
                com.example.vista.domain.model.MediaItem.SeriesItem(
                    id = id.toString(),
                    title = tvName,
                    posterUrl = TmdbImageUtils.posterUrl(posterPath),
                    rating = voteAverage,
                    year = firstAirDate?.takeIf { it.isNotEmpty() }?.let {
                        try { it.substring(0, 4).toIntOrNull() } catch (_: Exception) { null }
                    },
                    totalSeasons = null,
                    backdropUrl = TmdbImageUtils.backdropUrl(backdropPath)
                )
            }
            else -> null
        }
    }
}
