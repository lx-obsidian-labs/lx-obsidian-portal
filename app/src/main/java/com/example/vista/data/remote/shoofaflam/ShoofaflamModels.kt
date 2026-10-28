package com.example.vista.data.remote.shoofaflam

import com.google.gson.annotations.SerializedName

data class ShoofaflamTitle(
    val slug: String,
    @SerializedName("title_ar") val titleAr: String?,
    @SerializedName("title_en") val titleEn: String?,
    val year: Any?, // Flexible to handle String or Int
    val type: String?,
    val rating: Double?,
    val genres: List<String>?,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("platforms") val platforms: List<Any>? = null
) {
    private fun getParsedYear(): Int? {
        return when (year) {
            is Double -> year.toInt()
            is Float -> year.toInt()
            is Int -> year
            is String -> year.toIntOrNull()
            else -> null
        }
    }

    fun toDomainMovie(): com.example.vista.domain.model.Movie {
        val parsedYear = getParsedYear()
        val posterUrl = posterPath?.let {
            if (it.startsWith("http")) it
            else "https://image.tmdb.org/t/p/w500$it"
        }
        return com.example.vista.domain.model.Movie(
            id = slug,
            title = titleEn ?: titleAr ?: "Unknown",
            overview = "${genres?.joinToString(", ") ?: "Movie"} (${parsedYear ?: "N/A"})",
            posterUrl = posterUrl,
            backdropUrl = posterUrl,
            releaseYear = parsedYear,
            runtimeMinutes = null,
            rating = rating,
            voteCount = null,
            genres = genres?.map { com.example.vista.domain.model.Genre(it.hashCode().toString(), it) } ?: emptyList(),
            cast = emptyList(),
            director = null,
            ageRating = null
        )
    }

    fun toDomainSeries(): com.example.vista.domain.model.Series {
        val parsedYear = getParsedYear()
        val posterUrl = posterPath?.let {
            if (it.startsWith("http")) it
            else "https://image.tmdb.org/t/p/w500$it"
        }
        return com.example.vista.domain.model.Series(
            id = slug,
            title = titleEn ?: titleAr ?: "Unknown",
            overview = "${genres?.joinToString(", ") ?: "Series"} (${parsedYear ?: "N/A"})",
            posterUrl = posterUrl,
            backdropUrl = posterUrl,
            firstAirYear = parsedYear,
            lastAirYear = null,
            rating = rating,
            voteCount = null,
            genres = genres?.map { com.example.vista.domain.model.Genre(it.hashCode().toString(), it) } ?: emptyList(),
            cast = emptyList(),
            creators = emptyList(),
            totalSeasons = 1,
            totalEpisodes = 0,
            status = null,
            ageRating = null
        )
    }

    fun toMediaItem(): com.example.vista.domain.model.MediaItem.MovieItem {
        val parsedYear = getParsedYear()
        val posterUrl = posterPath?.let {
            if (it.startsWith("http")) it
            else "https://image.tmdb.org/t/p/w500$it"
        }
        return com.example.vista.domain.model.MediaItem.MovieItem(
            id = slug,
            title = titleEn ?: titleAr ?: "Unknown",
            posterUrl = posterUrl,
            rating = rating,
            year = parsedYear,
            duration = null,
            backdropUrl = posterUrl
        )
    }

    fun toSeriesMediaItem(): com.example.vista.domain.model.MediaItem.SeriesItem {
        val parsedYear = getParsedYear()
        val posterUrl = posterPath?.let {
            if (it.startsWith("http")) it
            else "https://image.tmdb.org/t/p/w500$it"
        }
        return com.example.vista.domain.model.MediaItem.SeriesItem(
            id = slug,
            title = titleEn ?: titleAr ?: "Unknown",
            posterUrl = posterUrl,
            rating = rating,
            year = parsedYear,
            totalSeasons = null,
            backdropUrl = posterUrl
        )
    }
}

data class ShoofaflamPlatform(
    val id: Int,
    val nameAr: String?,
    @SerializedName("name_en") val nameEn: String?,
    val slug: String?,
    val color: String?,
    val url: String?,
    val countries: List<String>?
)

data class ShoofaflamStats(
    @SerializedName("total_titles") val totalTitles: Int,
    @SerializedName("total_movies") val totalMovies: Int,
    @SerializedName("total_series") val totalSeries: Int,
    @SerializedName("total_platforms") val totalPlatforms: Int,
    @SerializedName("titles_with_platforms") val titlesWithPlatforms: Int,
    @SerializedName("last_updated") val lastUpdated: String?
)

data class ShoofaflamTitlesResponse(
    val titles: List<ShoofaflamTitle>
)

data class ShoofaflamPlatformsResponse(
    val platforms: List<ShoofaflamPlatform>
)
