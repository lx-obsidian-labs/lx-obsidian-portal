package com.example.vista.ui.sharing

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vista.domain.model.MilestoneEngine
import com.example.vista.domain.model.SharingMilestone
import com.example.vista.domain.model.WatchStats
import com.example.vista.domain.model.WatchStatsCalculator
import com.example.vista.domain.repository.HistoryRepository
import com.example.vista.sharing.SharingMilestoneCoordinator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ShareMilestoneUiState(
    val isLoading: Boolean = true,
    /** Null when the milestone no longer holds, e.g. a streak that has since lapsed. */
    val milestone: SharingMilestone? = null
)

/**
 * Resolves the tapped milestone against current data, so the numbers on screen are the numbers that
 * are true now rather than whatever was true when the notification was posted.
 */
@HiltViewModel
class ShareMilestoneViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val historyRepository: HistoryRepository,
    private val coordinator: SharingMilestoneCoordinator
) : ViewModel() {

    private val milestoneId: String = savedStateHandle.get<String>(ARG_MILESTONE_ID).orEmpty()

    private val _uiState = MutableStateFlow(ShareMilestoneUiState())
    val uiState: StateFlow<ShareMilestoneUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                val stats: WatchStats = WatchStatsCalculator.compute(historyRepository.getWatchHistory().first())
                val milestone = MilestoneEngine.resolve(milestoneId, stats)
                _uiState.value = ShareMilestoneUiState(isLoading = false, milestone = milestone)
                // Consume the allowance slot only when the milestone resolved successfully.
                runCatching { coordinator.markShown(milestoneId) }
            } catch (_: Exception) {
                _uiState.value = ShareMilestoneUiState(isLoading = false, milestone = null)
            }
        }
    }

    fun onShared() {
        viewModelScope.launch { coordinator.markShown(milestoneId) }
    }

    fun onDeclined() {
        viewModelScope.launch { coordinator.markShown(milestoneId) }
    }

    companion object {
        const val ARG_MILESTONE_ID = "milestoneId"
    }
}