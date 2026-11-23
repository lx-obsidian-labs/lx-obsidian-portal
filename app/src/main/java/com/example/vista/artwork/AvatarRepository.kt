package com.example.vista.artwork

import com.example.vista.data.local.PreferencesManager
import com.example.vista.data.local.ProfileScope
import com.example.vista.data.remote.firebase.FirebaseManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

/** Account-scoped stable IDs, immediate local persistence, and best-effort existing profile sync. */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@Singleton
class AvatarRepository @Inject constructor(
    private val preferences: PreferencesManager,
    private val profile: ProfileScope,
    private val firebase: FirebaseManager
) {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val saveMutex = Mutex()
    private val syncMutex = Mutex()
    val selectedId: Flow<String> = profile.owners.flatMapLatest { owner ->
        preferences.avatarId(owner).map { VistaArtworkRegistry.avatar(it.orEmpty()).id }.onStart { emit("default") }
    }.catch { emit("default") }.distinctUntilChanged()
        .stateIn(applicationScope, SharingStarted.WhileSubscribed(5_000), "default")

    init {
        applicationScope.launch {
            profile.owners.collectLatest { owner ->
                if (owner.isBlank()) return@collectLatest
                try {
                    if (preferences.avatarSyncPending(owner)) sync(owner)
                    else if (preferences.avatarId(owner).first() == null) {
                        val remote = withTimeoutOrNull(5_000) { firebase.getViewerProfile() }
                        if (profile.current() == owner && remote?.uid == owner) {
                            preferences.restoreAvatarIfUnset(owner, VistaArtworkRegistry.avatar(remote.avatarId).id)
                        }
                    }
                } catch (e: CancellationException) { throw e }
                catch (_: Exception) { /* The local/default avatar never depends on network access. */ }
            }
        }
    }

    suspend fun save(id: String): String = saveMutex.withLock {
        require(VistaArtworkRegistry.selectable(id)) { "Choose an available user avatar." }
        val owner = profile.current()
        preferences.setAvatarId(owner, id, syncPending = owner.isNotBlank())
        when {
            owner.isBlank() -> "Avatar saved on this device."
            sync(owner) -> "Avatar saved to your profile."
            else -> "Saved on this device. Account sync will retry when you reopen the avatar editor."
        }
    }

    suspend fun retrySync() {
        try { val owner = profile.current(); if (owner.isNotBlank()) sync(owner) }
        catch (e: CancellationException) { throw e }
        catch (_: Exception) { }
    }

    private suspend fun sync(owner: String): Boolean = syncMutex.withLock {
        if (profile.current() != owner) return@withLock false
        val id = preferences.avatarId(owner).first() ?: return@withLock true
        if (!VistaArtworkRegistry.selectable(id)) return@withLock false
        try {
            val saved = withTimeoutOrNull(8_000) { firebase.updateAvatar(owner, id); true } ?: false
            if (saved) preferences.acknowledgeAvatarSync(owner, id)
            saved
        } catch (e: CancellationException) { throw e }
        catch (_: Exception) { false }
    }
}
