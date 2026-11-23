package com.example.vista.data.remote.firebase

import android.os.Bundle
import com.example.vista.domain.model.Viewer
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseManager @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val analytics: FirebaseAnalytics,
    private val crashlytics: FirebaseCrashlytics
) {
    val currentUser get() = auth.currentUser

    fun isUserLoggedIn(): Boolean = auth.currentUser != null

    // ==================== Email/Password Auth ====================

    suspend fun signInWithEmail(email: String, password: String): Result<Unit> {
        return try {
            auth.signInWithEmailAndPassword(email, password).await()
            logInAnalytics("email_sign_in")
            Result.success(Unit)
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) {
            crashlytics.recordException(e)
            Result.failure(e)
        }
    }

    suspend fun createUserWithEmail(email: String, password: String): Result<Unit> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            result.user?.let { user ->
                logEvent(
                    "account_created",
                    bundleOf(
                        "method" to "email",
                        "user_id" to user.uid
                    )
                )
            }
            logInAnalytics("email_sign_up")
            Result.success(Unit)
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) {
            crashlytics.recordException(e)
            Result.failure(e)
        }
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            logEvent("password_reset_requested", bundleOf("email" to email))
            Result.success(Unit)
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) {
            crashlytics.recordException(e)
            Result.failure(e)
        }
    }

    suspend fun updateEmail(newEmail: String): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(
            IllegalStateException("No current user")
        )
        return try {
            user.verifyBeforeUpdateEmail(newEmail).await()
            logEvent("email_updated")
            Result.success(Unit)
        } catch (e: Exception) {
            crashlytics.recordException(e)
            Result.failure(e)
        }
    }

    // ==================== Sign Out ====================

    // Phone-number sign-in was removed: it required an SMS-retriever activity, a per-region
    // quota and a Firebase console configuration we could not verify, and it could leave a viewer
    // stuck on a verification screen with no fallback. Email/password and guest mode cover it.

    fun signOut() {
        auth.signOut()
        analytics.setUserId(null)
    }

    // ==================== Profile Management ====================

    suspend fun getViewerProfile(): Viewer? {
        return try {
            getViewerProfileOrThrow()
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) {
            crashlytics.recordException(e)
            null
        }
    }

    suspend fun getViewerProfileOrThrow(): Viewer? {
        val uid = auth.currentUser?.uid ?: return null
        val snapshot = firestore.collection("viewers").document(uid).get().await()
        check(auth.currentUser?.uid == uid) { "Account changed." }
        return if (snapshot.exists()) {
            // Preserve last-seen updates without making an auxiliary write gate a valid sign-in.
            firestore.collection("viewers").document(uid).update("lastSeenAt", System.currentTimeMillis())
            snapshot.toObject(Viewer::class.java)
        } else null
    }

    suspend fun saveViewerProfile(viewer: Viewer) {
        val uid = auth.currentUser?.uid ?: return
        check(viewer.uid == uid) { "Account changed." }
        try {
            firestore.collection("viewers").document(uid)
                .set(viewer, com.google.firebase.firestore.SetOptions.merge()).await()
            analytics.setUserId(uid)
            logEvent("profile_updated", null)
        } catch (e: Exception) {
            crashlytics.recordException(e)
            throw e
        }
    }

    suspend fun updateAvatar(owner: String, avatarId: String) {
        check(auth.currentUser?.uid == owner) { "Account changed." }
        firestore.collection("viewers").document(owner)
            .set(mapOf("avatarId" to avatarId), com.google.firebase.firestore.SetOptions.merge()).await()
    }

    suspend fun updateProfileAlias(owner: String, alias: String) {
        check(auth.currentUser?.uid == owner) { "Account changed." }
        firestore.collection("viewers").document(owner)
            .set(mapOf("alias" to alias), com.google.firebase.firestore.SetOptions.merge()).await()
    }

    // ==================== Utility ====================

    fun logEvent(event: String, params: Bundle? = null) {
        analytics.logEvent(event, params)
    }

    private fun logInAnalytics(event: String) {
        val user = auth.currentUser
        val params = user?.let { bundleOf("user_id" to it.uid) }
        logEvent(event, params)
    }

    private fun bundleOf(vararg pairs: Pair<String, Any?>): Bundle {
        return Bundle().apply {
            pairs.forEach { (key, value) ->
                when (value) {
                    is String -> putString(key, value)
                    is Int -> putInt(key, value)
                    is Long -> putLong(key, value)
                    is Boolean -> putBoolean(key, value)
                    is Float -> putFloat(key, value)
                }
            }
        }
    }
}
