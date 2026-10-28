package com.example.vista.data.local.entity

import androidx.room.Entity

/**
 * Resume state for a played title. Keyed by media id plus type so a movie and a series that share
 * a provider id cannot overwrite each other's playback progress.
 */
@Entity(tableName = "playback_history", primaryKeys = ["mediaId", "mediaType", "ownerId"])
data class PlaybackHistoryEntity(
    val mediaId: String,
    val mediaType: String,
    val title: String,
    val posterUrl: String? = null,
    val rating: Double? = null,
    val currentPosition: Long,
    val duration: Long,
    val lastPlayedTimestamp: Long,
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
    val isCompleted: Boolean = false,
    val ownerId: String = ""
) {
    val progress: Float
        get() = if (duration > 0) (currentPosition.toFloat() / duration).coerceIn(0f, 1f) else 0f
}
