package com.example.vista.data.remote.tvmaze

import com.google.gson.annotations.SerializedName

// TVmaze Show model
data class TvmazeShow(
    val id: Int,
    val name: String,
    val url: String?,
    val type: String?,
    val language: String?,
    val genres: List<String>,
    val status: String?,
    val runtime: Int?,
    @SerializedName("average_runtime") val averageRuntime: Int?,
    val premiered: String?,
    val ended: String?,
    val rating: TvmazeRating?,
    val network: TvmazeNetwork?,
    @SerializedName("web_channel") val webChannel: TvmazeNetwork?,
    val image: TvmazeImage?,
    val summary: String?,
    val weight: Int?,
    val externals: TvmazeExternals?
) {
    fun toMediaItem(): com.example.vista.domain.model.MediaItem.SeriesItem {
        return com.example.vista.domain.model.MediaItem.SeriesItem(
            id = "tvmaze_$id",
            title = name,
            posterUrl = image?.medium,
            rating = rating?.average,
            year = premiered?.takeIf { it.isNotEmpty() }?.let {
                try { it.substring(0, 4).toIntOrNull() } catch (_: Exception) { null }
            },
            totalSeasons = null,
            backdropUrl = image?.original
        )
    }
}

data class TvmazeRating(
    val average: Double?
)

data class TvmazeNetwork(
    val id: Int,
    val name: String?,
    val country: TvmazeCountry?
)

data class TvmazeCountry(
    val name: String?,
    val code: String?,
    val timezone: String?
)

data class TvmazeImage(
    val medium: String?,
    val original: String?
)

data class TvmazeExternals(
    val tvrage: Int?,
    val thetvdb: Int?,
    val imdb: String?
)

// Schedule Episode model
data class TvmazeScheduleEpisode(
    val id: Int,
    val url: String?,
    val name: String?,
    val season: Int?,
    val number: Int?,
    @SerializedName("airdate") val airDate: String?,
    @SerializedName("airtime") val airTime: String?,
    @SerializedName("airstamp") val airStamp: String?,
    val runtime: Int?,
    val rating: TvmazeRating?,
    val image: TvmazeImage?,
    val summary: String?,
    val _links: TvmazeLinks?
) {
    fun toDomainEpisode(): com.example.vista.domain.model.Episode {
        return com.example.vista.domain.model.Episode(
            seriesId = _links?.show?.href?.substringAfterLast("/") ?: "",
            seasonNumber = season ?: 0,
            episodeNumber = number ?: 0,
            title = name ?: "",
            overview = summary?.replace(Regex("<[^>]*>"), ""), // Strip HTML tags
            thumbnailUrl = image?.medium,
            runtimeMinutes = runtime,
            rating = rating?.average,
            airDate = airDate,
            stillUrl = image?.original
        )
    }
}

data class TvmazeLinks(
    val show: TvmazeLink?,
    val self: TvmazeLink?
)

data class TvmazeLink(
    val href: String?
)

// Search result
data class TvmazeSearchResult(
    val score: Double?,
    val show: TvmazeShow?
)

// Episode model
data class TvmazeEpisode(
    val id: Int,
    val url: String?,
    val name: String?,
    val season: Int?,
    val number: Int?,
    val type: String?,
    @SerializedName("airdate") val airDate: String?,
    @SerializedName("airtime") val airTime: String?,
    @SerializedName("airstamp") val airStamp: String?,
    val runtime: Int?,
    val rating: TvmazeRating?,
    val image: TvmazeImage?,
    val summary: String?
) {
    fun toDomainEpisode(seriesId: String): com.example.vista.domain.model.Episode {
        return com.example.vista.domain.model.Episode(
            seriesId = seriesId,
            seasonNumber = season ?: 0,
            episodeNumber = number ?: 0,
            title = name ?: "",
            overview = summary?.replace(Regex("<[^>]*>"), ""),
            thumbnailUrl = image?.medium,
            runtimeMinutes = runtime,
            rating = rating?.average,
            airDate = airDate,
            stillUrl = image?.original
        )
    }
}
