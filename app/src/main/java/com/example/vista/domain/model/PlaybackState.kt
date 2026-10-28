package com.example.vista.domain.model

// MARK: - Playback Models

data class PlaybackState(
    val mediaId: String,
    val mediaType: MediaType,
    val currentPosition: Long,
    val duration: Long,
    val isPlaying: Boolean,
    val playbackSpeed: Float = 1f,
    val isFullscreen: Boolean = false,
    val selectedSubtitle: SubtitleTrack? = null,
    val selectedAudio: AudioTrack? = null,
    val selectedQuality: String? = null,
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null
)

enum class MediaType {
    MOVIE, SERIES
}

data class PlaybackProgress(
    val mediaId: String,
    val mediaType: MediaType,
    val title: String,
    val posterUrl: String? = null,
    val rating: Double? = null,
    val currentPosition: Long,
    val duration: Long,
    val lastPlayedTimestamp: Long,
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
    val isCompleted: Boolean = false
) {
    val progress: Float
        get() = if (duration > 0) (currentPosition.toFloat() / duration).coerceIn(0f, 1f) else 0f

    val isAlmostComplete: Boolean
        get() = progress > 0.9f

    val timeRemaining: Long
        get() = (duration - currentPosition).coerceAtLeast(0)
}

// MARK: - UI State Models

sealed class UiState<out T> {
    data object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String, val cause: Throwable? = null) : UiState<Nothing>()
    data object Empty : UiState<Nothing>()
}

data class PaginationState(
    val currentPage: Int = 1,
    val isLoading: Boolean = false,
    val hasMore: Boolean = true,
    val error: String? = null
)
