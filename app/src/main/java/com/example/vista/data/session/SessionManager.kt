package com.example.vista.data.session

import com.example.vista.data.local.PreferencesManager
import com.example.vista.data.remote.firebase.FirebaseManager
import com.example.vista.util.GuestSessionManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Session Manager for Vista Watch.
 *
 * Handles playback authentication session validation and refreshing expired sessions
 * (e.g. Firebase token renewal or guest session validation) during playback verification errors.
 */
@Singleton
class SessionManager @Inject constructor(
    private val firebaseManager: FirebaseManager,
    private val guestSessionManager: GuestSessionManager,
    private val preferencesManager: PreferencesManager
) {

    /** Checks whether the active user or guest session is valid for playback. */
    suspend fun isSessionValid(): Boolean {
        val isGuest = preferencesManager.isGuest.first()
        if (isGuest) {
            val startedAt = preferencesManager.guestTrialStartedAtValue()
            if (startedAt > 0L && preferencesManager.guestTrialExpired(startedAt)) {
                return false
            }
            return !guestSessionManager.limitReached.value
        }
        return firebaseManager.isUserLoggedIn()
    }

    /**
     * Refreshes the current playback session (Firebase auth token or guest verification).
     */
    suspend fun refreshSession(): Result<Unit> {
        return try {
            val isGuest = preferencesManager.isGuest.first()
            if (isGuest) {
                val valid = preferencesManager.startOrCheckGuestTrial()
                if (valid && !guestSessionManager.limitReached.value) {
                    Result.success(Unit)
                } else {
                    Result.failure(IllegalStateException("Guest session trial or playback limit reached"))
                }
            } else {
                val user = firebaseManager.currentUser
                if (user != null) {
                    // Force refresh Firebase ID token
                    user.getIdToken(true).await()
                    Result.success(Unit)
                } else {
                    Result.failure(IllegalStateException("Sign in again to verify your session."))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
