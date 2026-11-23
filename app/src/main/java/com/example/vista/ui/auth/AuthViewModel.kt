package com.example.vista.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vista.data.local.PreferencesManager
import com.example.vista.data.remote.firebase.FirebaseManager
import com.example.vista.domain.model.AgeBand
import com.example.vista.domain.model.Viewer
import com.example.vista.util.AdManager
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

sealed class AuthMode {
    data object SignIn : AuthMode()
    data object SignUp : AuthMode()
    data object ForgotPassword : AuthMode()
}

data class AuthUiState(
    val isLoading: Boolean = false, val isCheckingStatus: Boolean = true,
    val isUserLoggedIn: Boolean = false, val needsProfileSetup: Boolean = false,
    val error: String? = null, val authMode: AuthMode = AuthMode.SignIn,
    val isGuest: Boolean = false, val info: String? = null,
    val problem: AuthProblem? = null, val failedOperation: AuthOperation? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val firebaseManager: FirebaseManager, private val auth: FirebaseAuth,
    private val preferencesManager: PreferencesManager
) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState = _uiState.asStateFlow()
    // Passwords stay only in this ViewModel's memory, never SavedStateHandle/DataStore/logging.
    private val _draft = MutableStateFlow(AuthFormDraft())
    val draft = _draft.asStateFlow()
    private var operation: Job? = null
    private var pendingProfile: Viewer? = null
    private var pendingAgeBand: AgeBand = AgeBand.ADULT
    init { checkUserStatus() }

    suspend fun postAuthRoute(): String = if (preferencesManager.onboardingSeenAt.first() == 0L)
        com.example.vista.navigation.Screen.Onboarding.route else com.example.vista.navigation.Screen.Home.route
    fun updateEmail(value: String) { if (!_uiState.value.isLoading) _draft.update { it.copy(email = value.take(254)) } }
    fun updatePassword(value: String) { if (!_uiState.value.isLoading) _draft.update { it.copy(password = value) } }
    fun updateConfirmation(value: String) { if (!_uiState.value.isLoading) _draft.update { it.copy(confirmation = value) } }

    private fun fail(error: Throwable, step: AuthOperation) {
        val problem = AuthFeedback.from(error, step)
        _uiState.update { it.copy(isLoading = false, isCheckingStatus = false, error = problem.message,
            problem = problem, failedOperation = step) }
    }
    private fun validation(message: String) {
        val problem = AuthProblem("Check your details", message)
        _uiState.update { it.copy(error = message, problem = problem, isLoading = false) }
    }
    private suspend fun completeProfileLookup() {
        check(firebaseManager.isUserLoggedIn()) { "Sign-in session unavailable." }
        val profile = withTimeout(12_000) { firebaseManager.getViewerProfileOrThrow() }
        preferencesManager.setGuestMode(false)
        AdManager.setGuestMode(false)
        _draft.update { it.copy(password = "", confirmation = "") }
        _uiState.update { it.copy(isLoading = false, isCheckingStatus = false, isUserLoggedIn = true,
            isGuest = false, needsProfileSetup = profile == null, error = null, problem = null) }
    }
    private fun checkUserStatus() {
        if (operation?.isActive == true) return
        operation = viewModelScope.launch {
            if (!firebaseManager.isUserLoggedIn()) {
                _uiState.update { it.copy(isCheckingStatus = false, isUserLoggedIn = false) }
            } else {
                _uiState.update { it.copy(isLoading = true, error = null, problem = null) }
                try { completeProfileLookup() }
                catch (e: TimeoutCancellationException) { fail(e, AuthOperation.PROFILE_LOOKUP) }
                catch (e: CancellationException) { throw e }
                catch (e: Exception) { fail(e, AuthOperation.PROFILE_LOOKUP) }
            }
        }
    }
    fun onAuthSuccess() = checkUserStatus()
    fun continueAsGuest() {
        if (operation?.isActive == true) return
        _uiState.update { it.copy(isLoading = true, error = null, problem = null) }
        operation = viewModelScope.launch {
            try {
                if (preferencesManager.startOrCheckGuestTrial()) {
                    preferencesManager.setGuestMode(true); AdManager.setGuestMode(true)
                    _uiState.update { it.copy(isGuest = true, isLoading = false, error = null) }
                } else validation("Your free guest trial has ended. Create an account or sign in to keep watching.")
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { fail(e, AuthOperation.SIGN_IN) }
        }
    }
    fun setError(message: String?) { if (message == null) clearError() else validation(message) }
    fun setLoading(loading: Boolean) { _uiState.update { it.copy(isLoading = loading) } }
    private fun mode(mode: AuthMode) {
        if (operation?.isActive == true) return
        _uiState.update { it.copy(authMode = mode, error = null, problem = null, info = null) }
    }
    fun switchToSignUp() = mode(AuthMode.SignUp)
    fun switchToSignIn() = mode(AuthMode.SignIn)
    fun switchToForgotPassword() = mode(AuthMode.ForgotPassword)
    fun backFromForgotPassword() = switchToSignIn()
    fun clearError() { _uiState.update { it.copy(error = null, problem = null) } }
    fun clearInfo() { _uiState.update { it.copy(info = null) } }

    fun signUpWithEmail(email: String, password: String) = submit(AuthOperation.SIGN_UP, email, password)
    fun signInWithEmail(email: String, password: String) = submit(AuthOperation.SIGN_IN, email, password)
    fun sendPasswordReset(email: String) = submit(AuthOperation.RESET_PASSWORD, email, "")

    private fun submit(step: AuthOperation, email: String, password: String) {
        if (operation?.isActive == true) return
        if (!AuthInputPolicy.validEmail(email)) { validation("Enter a complete email address, such as name@example.com."); return }
        if (step == AuthOperation.SIGN_UP) AuthInputPolicy.signupPasswordError(password)?.let { validation(it); return }
        if (step == AuthOperation.SIGN_IN && password.isEmpty()) { validation("Enter your password to sign in."); return }
        _draft.update { it.copy(email = AuthInputPolicy.email(email), password = if (step == AuthOperation.RESET_PASSWORD) it.password else password) }
        _uiState.update { it.copy(isLoading = true, error = null, problem = null, info = null, failedOperation = null) }
        operation = viewModelScope.launch {
            var activeStep = step
            try {
                withTimeout(20_000) {
                    when (step) {
                        AuthOperation.SIGN_UP -> firebaseManager.createUserWithEmail(AuthInputPolicy.email(email), password)
                        AuthOperation.SIGN_IN -> firebaseManager.signInWithEmail(AuthInputPolicy.email(email), password)
                        else -> firebaseManager.sendPasswordReset(AuthInputPolicy.email(email))
                    }.getOrThrow()
                }
                if (step == AuthOperation.RESET_PASSWORD) {
                    _uiState.update { it.copy(isLoading = false, authMode = AuthMode.SignIn,
                        info = "If an account uses this email address, a password reset link has been requested. Check your inbox and spam folder.") }
                } else {
                    activeStep = AuthOperation.PROFILE_LOOKUP
                    completeProfileLookup()
                }
            } catch (e: TimeoutCancellationException) { fail(e, activeStep) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { fail(e, activeStep) }
        }
    }

    fun retryLast() {
        when (_uiState.value.failedOperation) {
            AuthOperation.PROFILE_LOOKUP -> checkUserStatus()
            AuthOperation.PROFILE_WRITE -> pendingProfile?.let { writeProfile(it, pendingAgeBand) }
            AuthOperation.SIGN_UP -> signUpWithEmail(_draft.value.email, _draft.value.password)
            AuthOperation.SIGN_IN -> signInWithEmail(_draft.value.email, _draft.value.password)
            AuthOperation.RESET_PASSWORD -> sendPasswordReset(_draft.value.email)
            else -> clearError()
        }
    }
    fun recover() {
        when (_uiState.value.problem?.recovery) {
            AuthRecovery.RETRY -> retryLast()
            AuthRecovery.SIGN_IN -> switchToSignIn()
            AuthRecovery.RESET_PASSWORD -> switchToForgotPassword()
            else -> clearError()
        }
    }
    fun saveProfile(alias: String, avatarId: String, phoneNumber: String? = null, email: String? = null, ageBand: AgeBand = AgeBand.ADULT) {
        val user = firebaseManager.currentUser ?: run { validation("Please sign in again to finish your profile."); return }
        if (alias.trim().isEmpty()) { validation("Choose a display name for your profile."); return }
        val viewer = Viewer(uid = user.uid, alias = alias.trim(),
            avatarId = com.example.vista.artwork.VistaArtworkRegistry.avatar(avatarId).id,
            email = email ?: user.email, phoneNumber = phoneNumber ?: user.phoneNumber,
            createdAt = System.currentTimeMillis(), lastSeenAt = System.currentTimeMillis())
        pendingProfile = viewer
        pendingAgeBand = ageBand
        writeProfile(viewer, ageBand)
    }
    private fun writeProfile(viewer: Viewer, ageBand: AgeBand) {
        if (operation?.isActive == true) return
        _uiState.update { it.copy(isLoading = true, error = null, problem = null) }
        operation = viewModelScope.launch {
            try {
                withTimeout(20_000) { firebaseManager.saveViewerProfile(viewer) }
                preferencesManager.setAvatarId(viewer.uid, viewer.avatarId)
                preferencesManager.setDisplayName(viewer.alias)
                preferencesManager.setAgeBand(ageBand)
                preferencesManager.setGuestMode(false); AdManager.setGuestMode(false)
                _draft.update { it.copy(password = "", confirmation = "") }
                _uiState.update { it.copy(isLoading = false, isCheckingStatus = false, isUserLoggedIn = true,
                    needsProfileSetup = false, isGuest = false) }
            } catch (e: TimeoutCancellationException) { fail(e, AuthOperation.PROFILE_WRITE) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { fail(e, AuthOperation.PROFILE_WRITE) }
        }
    }
    fun signOut() {
        operation?.cancel(); firebaseManager.signOut(); _draft.value = AuthFormDraft()
        _uiState.value = AuthUiState(isCheckingStatus = false)
    }
    override fun onCleared() { _draft.value = AuthFormDraft(); super.onCleared() }
}
