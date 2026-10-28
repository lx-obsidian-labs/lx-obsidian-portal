package com.example.vista.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vista.domain.model.PlaybackProgress
import com.example.vista.domain.model.WatchStats
import com.example.vista.domain.model.WatchStatsCalculator
import com.example.vista.domain.repository.HistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class StatsUiState(
    val isLoading: Boolean = true,
    val stats: WatchStats = WatchStats(),
    /** Shown as a name only; null when nothing has been played. */
    val mostRecentTitle: String? = null
) {
    val isEmpty: Boolean get() = !isLoading && !stats.hasAnything
}

/**
 * Aggregate view of everything played, derived live from the history table.
 *
 * `stateIn` rather than a hand-rolled collect: the whole screen is a pure function of the history
 * flow, so there is no state to sequence and nothing to clean up in onCleared.
 */
@HiltViewModel
class StatsViewModel @Inject constructor(
    historyRepository: HistoryRepository
) : ViewModel() {

    val uiState: StateFlow<StatsUiState> = historyRepository.getWatchHistory()
        .map { history: List<PlaybackProgress> ->
            StatsUiState(
                isLoading = false,
                stats = WatchStatsCalculator.compute(history),
                mostRecentTitle = history.maxByOrNull { it.lastPlayedTimestamp }?.title
            )
        }
        .stateIn(
            scope = viewModelScope,
            // Survives a rotation without recomputing, but drops the subscription if the screen is
            // backgrounded for good rather than holding the database awake indefinitely.
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = StatsUiState()
        )
}