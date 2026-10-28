package com.example.vista.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vista.data.local.PreferencesManager
import com.example.vista.data.remote.firebase.FirebaseManager
import com.example.vista.domain.model.Viewer
import com.example.vista.util.AdManager
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AuthMode {
    data object SignIn : AuthMode()
    data object SignUp : AuthMode()
    data object ForgotPassword : AuthMode()
}

data class AuthUiState(
    val isLoading: Boolean = false,
    val isCheckingStatus: Boolean = true,
    val isUserLoggedIn: Boolean = false,
    val needsProfileSetup: Boolean = false,
    val error: String? = null,
    val authMode: AuthMode = AuthMode.SignIn,
    /** Set once the viewer chooses to continue without an account. */
    val isGuest: Boolean = false,
    /** Shown after a successful password reset request. */
    val info: String? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val firebaseManager: FirebaseManager,
    private val auth: FirebaseAuth,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        checkUserStatus()
    }

    private fun checkUserStatus() {
        viewModelScope.launch {
            if (firebaseManager.isUserLoggedIn()) {
                val profile = firebaseManager.getViewerProfile()
                _uiState.update { it.copy(
                    isUserLoggedIn = true,
                    needsProfileSetup = profile == null,
                    isCheckingStatus = false
                ) }
            } else {
                _uiState.update { it.copy(
                    isUserLoggedIn = false,
                    isCheckingStatus = false
                ) }
            }
        }
    }

    fun onAuthSuccess() {
        // Signing in clears any guest flag and its session limits.
        viewModelScope.launch {
            preferencesManager.setGuestMode(false)
            AdManager.setGuestMode(false)
        }
        checkUserStatus()
    }

    /**
     * Continues without an account. Guest viewing is local-only — no Firebase account is created —
     * is restricted by [com.example.vista.util.GuestSessionManager], and is capped at a single
     * non-renewable trial window recorded at the moment it first starts.
     */
    fun continueAsGuest() {
        viewModelScope.launch {
            val trialActive = preferencesManager.startOrCheckGuestTrial()
            if (trialActive) {
                preferencesManager.setGuestMode(true)
                AdManager.setGuestMode(true)
                _uiState.update { it.copy(isGuest = true, error = null) }
            } else {
                _uiState.update { it.copy(
                    isGuest = false,
                    error = "Your free guest trial has ended. Create an account to keep watching."
                ) }
            }
        }
    }

    fun setError(message: String?) {
        _uiState.update { it.copy(error = message, isLoading = false) }
    }

    fun setLoading(loading: Boolean) {
        _uiState.update { it.copy(isLoading = loading) }
    }

    fun switchToSignUp() {
        _uiState.update { it.copy(authMode = AuthMode.SignUp, error = null) }
    }

    fun switchToSignIn() {
        _uiState.update { it.copy(authMode = AuthMode.SignIn, error = null) }
    }

    fun switchToForgotPassword() {
        _uiState.update { it.copy(authMode = AuthMode.ForgotPassword, error = null) }
    }

    fun backFromForgotPassword() {
        _uiState.update { it.copy(authMode = AuthMode.SignIn, error = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearInfo() {
        _uiState.update { it.copy(info = null) }
    }

    fun signUpWithEmail(email: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = firebaseManager.createUserWithEmail(email, password)
            result.onSuccess {
                _uiState.update { it.copy(isLoading = false) }
                // Check if profile already exists, otherwise redirect to setup
                val profile = firebaseManager.getViewerProfile()
                _uiState.update { state ->
                    state.copy(
                        isUserLoggedIn = true,
                        needsProfileSetup = profile == null,
                        authMode = AuthMode.SignIn
                    )
                }
            }.onFailure { e ->
                _uiState.update { 
                    it.copy(isLoading = false, error = e.message ?: "Failed to create account") 
                }
            }
        }
    }

    fun signInWithEmail(email: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = firebaseManager.signInWithEmail(email, password)
            result.onSuccess {
                _uiState.update { it.copy(isLoading = false) }
                checkUserStatus()
            }.onFailure { e ->
                _uiState.update { 
                    it.copy(isLoading = false, error = e.message ?: "Failed to sign in") 
                }
            }
        }
    }

    fun sendPasswordReset(email: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = firebaseManager.sendPasswordReset(email)
            result.onSuccess {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = null,
                        authMode = AuthMode.SignIn,
                        info = "Password reset link sent to $email."
                    )
                }
            }.onFailure { e ->
                _uiState.update { 
                    it.copy(isLoading = false, error = e.message ?: "Failed to send reset email") 
                }
            }
        }
    }

    fun saveProfile(alias: String, avatarId: String, phoneNumber: String? = null, email: String? = null) {
        val currentUser = firebaseManager.currentUser ?: return
        val viewer = Viewer(
            uid = currentUser.uid,
            alias = alias,
            avatarId = avatarId,
            email = email ?: currentUser.email,
            phoneNumber = phoneNumber ?: currentUser.phoneNumber,
            createdAt = System.currentTimeMillis(),
            lastSeenAt = System.currentTimeMillis(),
            appVersion = ""
        )
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                firebaseManager.saveViewerProfile(viewer)
                _uiState.update { 
                    it.copy(isLoading = false, needsProfileSetup = false) 
                }
                onAuthSuccess()
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(isLoading = false, error = e.message ?: "Failed to save profile") 
                }
            }
        }
    }

    fun signOut() {
        firebaseManager.signOut()
        _uiState.update {
            AuthUiState(authMode = AuthMode.SignIn)
        }
    }
}
