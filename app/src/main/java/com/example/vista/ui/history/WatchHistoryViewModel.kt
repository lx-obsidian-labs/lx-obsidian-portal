package com.example.vista.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vista.domain.model.MediaType
import com.example.vista.domain.model.PlaybackProgress
import com.example.vista.domain.repository.HistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WatchHistoryUiState(
    val isLoading: Boolean = true,
    val entries: List<PlaybackProgress> = emptyList(),
    val query: String = ""
) {
    /** In-memory title filter; keeps the DB flow untouched and the UI instantly responsive. */
    val filteredEntries: List<PlaybackProgress>
        get() = if (query.isBlank()) {
            entries
        } else {
            val needle = query.trim()
            entries.filter { it.title.contains(needle, ignoreCase = true) }
        }

    /** Whether the *filtered* list is empty (the underlying history may still have rows). */
    val isEmpty: Boolean get() = !isLoading && filteredEntries.isEmpty()
    val isFilteringWithoutResults: Boolean get() = query.isNotBlank() && !isLoading && filteredEntries.isEmpty() && entries.isNotEmpty()
}

/**
 * Backs the watch-history screen from the same `playback_history` table that Continue Watching
 * reads, so the two can never disagree. Entries carry the season/episode they were last played at,
 * which is what makes resuming a series land on the right episode.
 */
@HiltViewModel
class WatchHistoryViewModel @Inject constructor(
    private val historyRepository: HistoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WatchHistoryUiState())
    val uiState: StateFlow<WatchHistoryUiState> = _uiState.asStateFlow()

    init {
        observeHistory()
    }

    private fun observeHistory() {
        viewModelScope.launch {
            historyRepository.getWatchHistory().collect { entries ->
                _uiState.update { it.copy(isLoading = false, entries = entries) }
            }
        }
    }

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
    }

    fun removeEntry(entry: PlaybackProgress) {
        viewModelScope.launch {
            historyRepository.removeProgress(entry.mediaId, entry.mediaType)
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            historyRepository.clearHistory()
        }
    }

    /** Resumes an entry, defaulting a series to the episode it left off on. */
    fun resumeTarget(entry: PlaybackProgress): ResumeTarget = ResumeTarget(
        mediaId = entry.mediaId,
        title = entry.title,
        mediaType = entry.mediaType,
        season = if (entry.mediaType == MediaType.SERIES) entry.seasonNumber ?: 1 else null,
        episode = if (entry.mediaType == MediaType.SERIES) entry.episodeNumber ?: 1 else null,
        posterUrl = entry.posterUrl
    )

    data class ResumeTarget(
        val mediaId: String,
        val title: String,
        val mediaType: MediaType,
        val season: Int?,
        val episode: Int?,
        val posterUrl: String?
    )
}
