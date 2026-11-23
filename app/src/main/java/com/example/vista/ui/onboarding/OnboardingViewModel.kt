package com.example.vista.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vista.data.local.PreferencesManager
import com.example.vista.data.remote.firebase.FirebaseManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingUiState(
    val isWelcomeBack: Boolean = false,
    val suggestedName: String = "",
    val daysAway: Int = 0,
    val isLoading: Boolean = true,
    val isFinishing: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val firebaseManager: FirebaseManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val seenAt = preferencesManager.onboardingSeenAt.first()
            val displayName = preferencesManager.displayName.first()

            // Signed-in viewers get their profile alias as the suggested name.
            val suggested = if (displayName.isNotBlank()) {
                displayName
            } else {
                try { kotlinx.coroutines.withTimeoutOrNull(2_000) { firebaseManager.getViewerProfile()?.alias } ?: "" }
                catch (e: kotlinx.coroutines.CancellationException) { throw e }
                catch (_: Exception) { "" }
            }

            _uiState.update {
                it.copy(
                    isWelcomeBack = seenAt > 0L,
                    suggestedName = suggested,
                    daysAway = com.example.vista.util.DateUtils.daysSince(seenAt),
                    isLoading = false
                )
            }
        }
    }

    /** Saves the chosen name and stamps onboarding as complete. */
    fun finish(name: String, onFinished: () -> Unit) {
        if (_uiState.value.isLoading || _uiState.value.isFinishing) return
        _uiState.update { it.copy(isFinishing = true, error = null) }
        viewModelScope.launch {
            try {
                val finalName = name.trim().take(32)
                if (finalName.isNotBlank()) {
                    preferencesManager.setDisplayName(finalName)
                } else if (_uiState.value.suggestedName.isNotBlank()) {
                    preferencesManager.setDisplayName(_uiState.value.suggestedName)
                }
                preferencesManager.markOnboardingSeen()
                onFinished()
            } catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (_: Exception) { _uiState.update { it.copy(error = "Couldn't save your welcome preferences. Please try again.") } }
            finally { _uiState.update { it.copy(isFinishing = false) } }
        }
    }
}
