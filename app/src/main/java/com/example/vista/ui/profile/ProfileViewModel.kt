package com.example.vista.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vista.data.remote.firebase.FirebaseManager
import com.example.vista.domain.model.AppSettings
import com.example.vista.domain.model.Viewer
import com.example.vista.domain.repository.HistoryRepository
import com.example.vista.domain.repository.SettingsRepository
import com.example.vista.domain.repository.WatchlistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val isLoading: Boolean = true,
    val settings: AppSettings = AppSettings(),
    val viewer: Viewer? = null,
    val watchlistCount: Int = 0,
    val historyCount: Int = 0,
    val error: String? = null,
    val isSaving: Boolean = false
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val watchlistRepository: WatchlistRepository,
    private val historyRepository: HistoryRepository,
    private val firebaseManager: FirebaseManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _profileSaved = MutableSharedFlow<Boolean>()
    val profileSaved: SharedFlow<Boolean> = _profileSaved.asSharedFlow()

    private val _passwordResetResult = MutableSharedFlow<Boolean>()
    val passwordResetResult: SharedFlow<Boolean> = _passwordResetResult.asSharedFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val profile = firebaseManager.getViewerProfile()
            combine(
                settingsRepository.getSettings(),
                watchlistRepository.getWatchlist(),
                historyRepository.getContinueWatching()
            ) { settings, watchlist, history ->
                ProfileUiState(
                    isLoading = false,
                    settings = settings,
                    viewer = profile,
                    watchlistCount = watchlist.size,
                    historyCount = history.size
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    fun updateProfileAlias(alias: String) {
        val currentViewer = _uiState.value.viewer ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                val updated = currentViewer.copy(alias = alias)
                firebaseManager.saveViewerProfile(updated)
                _uiState.update { it.copy(viewer = updated, isSaving = false) }
                _profileSaved.emit(true)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isSaving = false) }
                _profileSaved.emit(false)
            }
        }
    }

    fun sendPasswordReset(email: String) {
        viewModelScope.launch {
            val result = firebaseManager.sendPasswordReset(email)
            _passwordResetResult.emit(result.isSuccess)
        }
    }

    fun signOut() {
        firebaseManager.signOut()
    }

    fun refreshProfile() {
        viewModelScope.launch {
            val profile = firebaseManager.getViewerProfile()
            _uiState.update { it.copy(viewer = profile) }
        }
    }
}
