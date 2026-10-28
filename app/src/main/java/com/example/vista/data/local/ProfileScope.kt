package com.example.vista.data.local

import androidx.room.withTransaction
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileScope @Inject constructor(private val auth: FirebaseAuth, private val database: AppDatabase) {
    fun current() = auth.currentUser?.uid ?: ""
    val owners = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(current()) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }.distinctUntilChanged()
    /** Legacy rows remain intact on upgrade and are claimed once by the active profile. */
    suspend fun claimLegacy(owner: String = current()): String {
        if (owner.isBlank()) return owner
        database.withTransaction {
            database.openHelper.writableDatabase.execSQL("UPDATE watchlist SET ownerId = ? WHERE ownerId = ''", arrayOf(owner))
            database.openHelper.writableDatabase.execSQL("UPDATE playback_history SET ownerId = ? WHERE ownerId = ''", arrayOf(owner))
        }
        return owner
    }
}
