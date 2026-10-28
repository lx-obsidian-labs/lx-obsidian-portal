package com.example.vista.data.remote.watchmode

import com.google.gson.annotations.SerializedName

data class WatchmodeSource(
    @SerializedName("source_id") val sourceId: Int,
    val name: String,
    val type: String,
    val region: String,
    val url: String?,
    @SerializedName("format") val format: String?,
    @SerializedName("price") val price: Double?,
    @SerializedName("seasons") val seasons: Int?,
    @SerializedName("episodes") val episodes: Int?
)

data class WatchmodeDetails(
    val id: Int,
    val title: String,
    val type: String,
    val year: Int?,
    @SerializedName("tmdb_id") val tmdbId: Int?,
    @SerializedName("tmdb_type") val tmdbType: String?
)
