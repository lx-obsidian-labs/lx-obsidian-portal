package com.example.vista.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vista.domain.repository.CatalogRepository
import com.example.vista.domain.repository.ScheduleItem
import com.example.vista.domain.repository.ScheduleRepository
import com.example.vista.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ScheduleUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val schedule: List<ScheduleItem> = emptyList(),
    /** ISO yyyy-MM-dd. A plain string keeps this API-24 safe and sorts correctly. */
    val selectedDate: String = DateUtils.todayIso(),
    val error: String? = null
)

data class DaySchedule(
    val date: String,
    val dayName: String,
    val dateLabel: String,
    val episodes: List<ScheduleItem>
)

@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val scheduleRepository: ScheduleRepository,
    private val catalogRepository: CatalogRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScheduleUiState())
    val uiState: StateFlow<ScheduleUiState> = _uiState.asStateFlow()

    private val _lookupMessage = MutableStateFlow<String?>(null)
    val lookupMessage: StateFlow<String?> = _lookupMessage.asStateFlow()

    /**
     * TV Maze ids are a third namespace again — they are neither TMDB movie nor TMDB TV ids, so
     * passing one to the details screen resolved to whatever unrelated title happened to share the
     * number. Resolve the show to its TMDB id by name before navigating instead.
     */
    fun resolveSeriesId(showName: String, onResolved: (String?) -> Unit) {
        if (showName.isBlank()) {
            onResolved(null)
            return
        }
        viewModelScope.launch {
            val results = try {
                catalogRepository.searchSeries(showName)
            } catch (e: Exception) {
                emptyList()
            }
            val match = results.firstOrNull { it.title.equals(showName, ignoreCase = true) }
                ?: results.firstOrNull()
            if (match == null) {
                _lookupMessage.value = "\"$showName\" isn't in the catalogue yet."
            }
            onResolved(match?.id)
        }
    }

    fun clearLookupMessage() {
        _lookupMessage.value = null
    }

    // Weekly schedule grouped by day
    private val _weeklySchedule = MutableStateFlow<List<DaySchedule>>(emptyList())
    val weeklySchedule: StateFlow<List<DaySchedule>> = _weeklySchedule.asStateFlow()

    init {
        loadSchedule()
        loadWeeklySchedule()
    }

    private fun loadSchedule() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val schedule = scheduleRepository.getSchedule(
                    country = "US",
                    date = _uiState.value.selectedDate
                )
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        schedule = schedule
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load schedule"
                    )
                }
            }
        }
    }

    private fun loadWeeklySchedule() {
        viewModelScope.launch {
            try {
                val weeklySchedule = scheduleRepository.getWeekSchedule(country = "US")
                val today = DateUtils.todayIso()

                // Group episodes by their air date, normalised so a date-time cannot land in a
                // different bucket than a plain date.
                val groupedByDay = weeklySchedule.groupBy { episode ->
                    DateUtils.toIsoOrNull(episode.airDate) ?: today
                }

                // Create DaySchedule objects for each day of the week
                val daySchedules = (0..6).map { daysFromNow ->
                    val date = DateUtils.isoDaysFromToday(daysFromNow)
                    val dayName = when (daysFromNow) {
                        0 -> "Today"
                        1 -> "Tomorrow"
                        else -> DateUtils.formatIso(date, "EEE")
                    }
                    val dateLabel = DateUtils.formatIso(date, "MMM d")

                    DaySchedule(
                        date = date,
                        dayName = dayName,
                        dateLabel = dateLabel,
                        episodes = groupedByDay[date] ?: emptyList()
                    )
                }

                _weeklySchedule.value = daySchedules
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun selectDate(date: String) {
        _uiState.update { it.copy(selectedDate = date) }
        loadSchedule()
    }

    fun selectToday() {
        selectDate(DateUtils.todayIso())
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            try {
                val schedule = scheduleRepository.getSchedule(
                    country = "US",
                    date = _uiState.value.selectedDate
                )
                _uiState.update {
                    it.copy(
                        isRefreshing = false,
                        schedule = schedule
                    )
                }
                // Also refresh weekly schedule
                loadWeeklySchedule()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isRefreshing = false,
                        error = e.message ?: "Failed to refresh schedule"
                    )
                }
            }
        }
    }
}
