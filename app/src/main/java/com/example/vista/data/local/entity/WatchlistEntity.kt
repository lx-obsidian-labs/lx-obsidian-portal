package com.example.vista.data.local.entity

import androidx.room.Entity

/**
 * A saved title. The primary key is the media id *plus* the media type, because provider id
 * namespaces (TMDB, Shoofaflam, ...) are not interchangeable: without the type, a movie and a
 * series that share a numeric id would silently overwrite each other in the saved list.
 */
@Entity(tableName = "watchlist", primaryKeys = ["id", "type", "ownerId"])
data class WatchlistEntity(
    val id: String,
    val title: String,
    val posterUrl: String?,
    val backdropUrl: String?,
    val rating: Double?,
    val year: Int?,
    val type: String, // "movie" or "series"
    val addedAt: Long = System.currentTimeMillis(),
    val ownerId: String = ""
)
