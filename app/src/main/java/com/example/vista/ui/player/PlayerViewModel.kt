package com.example.vista.ui.player

import android.os.SystemClock
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vista.domain.model.*
import com.example.vista.domain.repository.HistoryRepository
import com.example.vista.domain.repository.PlaybackRepository
import com.example.vista.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlayerUiState(
    val isLoading: Boolean = true,
    val mediaId: String = "",
    val title: String = "",
    val streamSources: List<StreamSource> = emptyList(),
    val selectedSource: StreamSource? = null,
    val subtitles: List<SubtitleTrack> = emptyList(),
    val audioTracks: List<AudioTrack> = emptyList(),
    // Playback starts automatically once the player is prepared.
    val isPlaying: Boolean = true,
    val playbackSpeed: Float = 1f,
    val availableQualities: List<String> = listOf("Auto", "1080p", "720p", "480p", "360p"),
    val selectedQuality: String = "Auto",
    val isFullscreen: Boolean = false,
    val showControls: Boolean = true,
    val error: String? = null,
    val isRecovering: Boolean = false,
    val canRepairManually: Boolean = true,
    val retryAttemptCount: Int = 0,
    val sourceGeneration: Int = 0,
    val isSeries: Boolean = false,
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
    val nextEpisode: Episode? = null,
    // Guest limits
    val isGuest: Boolean = false,
    val guestLimitReached: Boolean = false,
    val guestMinutesRemaining: Int = 0,
    // Playback settings, honored while playing
    val autoplayNext: Boolean = true,
    val autoResume: Boolean = true,
    val doubleTapToSeekSeconds: Int = 10
)

/**
 * The 1-second playback tick, kept out of [PlayerUiState] on purpose.
 *
 * `currentPosition`/`duration` change once per second of playback. On `PlayerUiState` that made the
 * whole player screen recompose every second; as their own flow, only the scrubber/time-text reads
 * those values and recompose, leaving the player, overlay and settings sheet untouched.
 */
data class PlayerProgress(
    val currentPosition: Long = 0L,
    val duration: Long = 0L
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val playbackRepository: PlaybackRepository,
    private val historyRepository: HistoryRepository,
    private val catalogRepository: com.example.vista.domain.repository.CatalogRepository,
    private val settingsRepository: SettingsRepository,
    private val preferencesManager: com.example.vista.data.local.PreferencesManager,
    private val guestSession: com.example.vista.util.GuestSessionManager,
    private val vistaAnalytics: com.example.vista.data.analytics.VistaAnalytics,
    private val cacheManager: com.example.vista.data.cache.CacheManager,
    private val playbackRecoveryManager: com.example.vista.data.playback.PlaybackRecoveryManager,
    private val playerHealthMonitor: com.example.vista.data.playback.PlayerHealthMonitor,
    private val playbackAccess: com.example.vista.data.playback.PlaybackAccess,
    private val sessionManager: com.example.vista.data.session.SessionManager
) : ViewModel() {

    private val playbackSessionId = "${savedStateHandle.get<String>("mediaId") ?: "session"}_${System.currentTimeMillis()}"

    private val mediaId: String = savedStateHandle["mediaId"] ?: ""
    private val title: String = savedStateHandle["title"] ?: ""
    private val season: String? = savedStateHandle["season"]
    private val episode: String? = savedStateHandle["episode"]
    private val poster: String? = savedStateHandle["poster"]

    /**
     * The media type comes from the route. It must never be derived from whether a season number
     * is present: a series opened without an episode was previously played as a movie, and TMDB's
     * movie ids overlap TV ids, so that resolved to an unrelated film.
     */
    private val mediaType: MediaType = when (savedStateHandle.get<String>("type")?.lowercase()) {
        "series", "tv", "show" -> MediaType.SERIES
        "movie", "film" -> MediaType.MOVIE
        // Legacy routes that predate the explicit type argument.
        else -> if (season != null) MediaType.SERIES else MediaType.MOVIE
    }

    private val _uiState = MutableStateFlow(PlayerUiState(
        mediaId = mediaId, 
        title = title,
        seasonNumber = season?.toIntOrNull(),
        episodeNumber = episode?.toIntOrNull(),
        isSeries = mediaType == MediaType.SERIES
    ))
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private val _progress = MutableStateFlow(PlayerProgress())
    val progress: StateFlow<PlayerProgress> = _progress.asStateFlow()

    /** True while the user's finger is on the scrubber, so the 1 s position poll can't fight the drag. */
    private var isScrubbing = false

    private var progressSaveJob: kotlinx.coroutines.Job? = null
    private val recoveryBudget = com.example.vista.data.playback.RecoveryBudget()
    private var recoveryJob: kotlinx.coroutines.Job? = null

    // Owned by the ViewModel: onCleared() must still persist playback, but by then viewModelScope
    // is already cancelled, so the exit save launches on this scope instead.
    private val exitScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Guards against two save leaks: a scrub jumping straight to the end is not the same as
    // watching to the end, and rapid ±10 s taps must not produce one write per tap.
    private var suppressCompletionUntil = 0L
    private var lastSeekSaveAt = 0L

    /** True once a completion is reported to analytics, so repeated 5 s saves never double-count. */
    private var completionReported = false

    private companion object {
        const val COMPLETION_GRACE_MS = 2_000L
        const val MIN_SEEK_SAVE_INTERVAL_MS = 2_000L
    }

    init {
        cacheManager.registerActiveSession(playbackSessionId)
        loadPlaybackData()
        observeSettings()
        observeGuestLimits()
    }

    private fun observeGuestLimits() {
        viewModelScope.launch {
            preferencesManager.isGuest.collect { isGuest ->
                _uiState.update { state -> state.copy(isGuest = isGuest) }
            }
        }
        viewModelScope.launch {
            guestSession.limitReached.collect { reached ->
                _uiState.update { it.copy(guestLimitReached = reached) }
            }
        }
        viewModelScope.launch {
            guestSession.watchedSeconds.collect {
                _uiState.update { state -> state.copy(guestMinutesRemaining = guestSession.remainingMinutes) }
            }
        }
    }

    /**
     * Called once per second of real playback. Feeds the guest session cap; a signed-in viewer is
     * never limited.
     */
    fun onPlaybackTick() {
        val state = _uiState.value
        if (!state.isGuest || !state.isPlaying) return
        if (guestSession.recordPlayback(1)) {
            // Hard stop: the clock on the session ran out, playback does not continue behind the
            // sign-up prompt.
            _uiState.update { it.copy(isPlaying = false, guestLimitReached = true) }
        }
    }

    private fun observeSettings() {
        viewModelScope.launch {
            settingsRepository.getSettings().collect { settings ->
                _uiState.update { 
                    it.copy(
                        playbackSpeed = settings.playbackSpeed,
                        selectedQuality = if (settings.dataSaverMode) "480p" else settings.preferredQuality,
                        autoplayNext = settings.autoplayNext,
                        autoResume = settings.autoResume,
                        doubleTapToSeekSeconds = settings.doubleTapToSeekSeconds
                    )
                }
            }
        }
    }

    private fun loadPlaybackData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                playbackAccess.requireAccess()
                // A series with no episode defaults to the season premiere rather than falling back
                // to a movie lookup.
                val sNum = season?.toIntOrNull() ?: if (mediaType == MediaType.SERIES) 1 else null
                val eNum = episode?.toIntOrNull() ?: if (mediaType == MediaType.SERIES) 1 else null

                val streams = playbackRepository.getStreamSources(
                    mediaId, mediaType, sNum, eNum, titleHint = title
                )

                // No provider could serve this title (unknown id, or none that supports it). Say so
                // instead of dropping the user onto a blank player.
                if (streams.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "No playable source was found for this title."
                        )
                    }
                    return@launch
                }

                val subtitles = playbackRepository.getSubtitles(mediaId)
                val audioTracks = playbackRepository.getAudioTracks(mediaId)
                val progress = historyRepository.getProgress(mediaId, mediaType)
                // Read the resume/autoplay prefs at load time; observeSettings() may not have
                // delivered them yet, and the resume decision is made right here.
                val settings = settingsRepository.getSettings().first()

                // Fetch next episode info if it's a series
                var nextEp: Episode? = null
                if (mediaType == MediaType.SERIES && sNum != null && eNum != null) {
                    try {
                        val episodes = catalogRepository.getEpisodes(mediaId, sNum)
                        nextEp = episodes.find { it.episodeNumber == eNum + 1 }
                        // If not found in current season, try next season (optional, but good)
                    } catch (e: Exception) { /* Ignore */ }
                }

_uiState.update {
                    it.copy(
                        isLoading = false,
                        streamSources = streams,
                        selectedSource = streams.firstOrNull(),
                        subtitles = subtitles,
                        audioTracks = audioTracks,
                        // "Auto-resume" only restores the saved position if the setting is on.
                        isSeries = mediaType == MediaType.SERIES,
                        seasonNumber = sNum,
                        episodeNumber = eNum,
                        autoplayNext = settings.autoplayNext,
                        autoResume = settings.autoResume
                    )
                }
                _progress.value = PlayerProgress(
                    currentPosition = if (settings.autoResume) progress?.currentPosition ?: 0L else 0L,
                    duration = 0L
                )
                vistaAnalytics.track(
                    "playback_started",
                    properties = mapOf("mediaType" to if (mediaType == MediaType.SERIES) "series" else "movie"),
                    isGuest = _uiState.value.isGuest
                )
            } catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load playback data"
                    )
                }
            }
        }
    }

    fun onPlayPauseToggle() {
        _uiState.update { it.copy(isPlaying = !it.isPlaying) }
    }

    /**
     * Updates the scrubber while the user drags. While scrubbing the 1 s position poll only
     * refreshes the duration so it cannot fight the drag preview.
     */
    fun onSeekPreview(position: Long) {
        isScrubbing = true
        _progress.update { it.copy(currentPosition = position.coerceAtLeast(0L)) }
    }

    /**
     * Commits a seek that has already been applied to the player.
     *
     * Writes are throttled (one per [MIN_SEEK_SAVE_INTERVAL_MS]) and never used to record
     * completion: scrubbing to the end is not the same as watching to the end, so a recent seek
     * suppresses the 95 % mark for a short grace period.
     */
    fun onSeekTo(position: Long) {
        isScrubbing = false
        _progress.update { it.copy(currentPosition = position.coerceAtLeast(0L)) }
        suppressCompletionUntil = SystemClock.elapsedRealtime() + COMPLETION_GRACE_MS
        val now = SystemClock.elapsedRealtime()
        if (now - lastSeekSaveAt >= MIN_SEEK_SAVE_INTERVAL_MS) {
            lastSeekSaveAt = now
            saveProgress()
        }
    }

    /**
 * Updates the position and duration from the real player. During a scrub the preview dominates
 * so only the duration is refreshed to avoid fighting the drag.
 */
    fun onProgressUpdate(position: Long, duration: Long) {
        // During a scrub the preview wins; only refresh the duration so the progress view updates.
        _progress.update {
            if (isScrubbing) it.copy(duration = duration) else it.copy(currentPosition = position, duration = duration)
        }
        // Save progress periodically
        if (progressSaveJob?.isActive != true) {
            progressSaveJob = viewModelScope.launch {
                kotlinx.coroutines.delay(5000) // Save every 5 seconds
                saveProgress()
            }
        }
    }

    fun onPlaybackSpeedChange(speed: Float) {
        _uiState.update { it.copy(playbackSpeed = speed) }
    }

    fun onSourceChange(source: StreamSource) {
        // The player swap happens as soon as the composable rebuilds; there is no artificial
        // delay to wait out before the controls come back.
        recoveryJob?.cancel()
        _uiState.update { it.copy(selectedSource = source, isLoading = false, isRecovering = false, error = null, sourceGeneration = it.sourceGeneration + 1) }
    }

    fun onQualityChange(quality: String) {
        _uiState.update { it.copy(selectedQuality = quality) }
        // In a real app, this would trigger a player reload with different quality
    }

    fun onFullscreenToggle() {
        _uiState.update { it.copy(isFullscreen = !it.isFullscreen) }
    }

    fun onControlsVisibilityToggle() {
        _uiState.update { it.copy(showControls = !it.showControls) }
    }

    fun hideControls() {
        _uiState.update { it.copy(showControls = false) }
    }

    fun onSubtitleSelected(subtitle: SubtitleTrack?) {
        _uiState.update { it.copy(subtitles = _uiState.value.subtitles.map { s ->
            s.copy(/* Update selected state */)
        }) }
    }

    fun onAudioTrackSelected(track: AudioTrack) {
        // Update audio track
    }

    private fun saveProgress() {
        val snapshot = _uiState.value
        viewModelScope.launch { persist(snapshot) }
    }

    /**
     * Persists one frozen snapshot of playback state.
     *
     * Shared by the periodic/seek saves (launched on [viewModelScope]) and the exit save (launched
     * on [exitScope] because `onCleared` runs after the view model scope is already cancelled).
     */
    private suspend fun persist(state: PlayerUiState) {
        val p = _progress.value
        // Embed/WebView sources never report a duration; writing them would only litter Continue
        // Watching with 0-second rows that can never be resumed.
        if (p.duration <= 0) return

        val previous = historyRepository.getProgress(state.mediaId, mediaType)
        // Restarting well before the end clears a completed flag; otherwise preserve it so the
        // periodic 5 s save can't undo markAsCompleted.
        val completed = if (p.currentPosition < p.duration * 0.02) {
            false
        } else {
            previous?.isCompleted == true
        }

        val progress = PlaybackProgress(
            mediaId = state.mediaId,
            mediaType = mediaType,
            title = state.title,
            posterUrl = poster,
            currentPosition = p.currentPosition,
            duration = p.duration,
            lastPlayedTimestamp = System.currentTimeMillis(),
            seasonNumber = state.seasonNumber,
            episodeNumber = state.episodeNumber,
            isCompleted = completed
        )
        historyRepository.saveProgress(progress)

        // ~95 % is "finished": it drops the title from Continue Watching and flips the completed
        // state in Watch History. Only natural progression counts, never a freshly-scrubbed
        // position.
        val reachedEnd = p.currentPosition >= p.duration * 0.95 &&
            SystemClock.elapsedRealtime() >= suppressCompletionUntil
        if (reachedEnd) {
            historyRepository.markAsCompleted(state.mediaId, mediaType)
            if (!completionReported) {
                completionReported = true
                vistaAnalytics.track(
                    "playback_completed",
                    properties = mapOf("mediaType" to if (mediaType == MediaType.SERIES) "series" else "movie"),
                    isGuest = state.isGuest
                )
            }
        } else if (p.currentPosition < p.duration * 0.02) {
            // Restarting well before the end means the next watch-through is a fresh completion.
            completionReported = false
        }
    }

    fun onPlayerError(error: String) {
        _uiState.update { it.copy(error = error) }
    }

    /**
     * Automatic verification recovery handler for ExoPlayer / stream exceptions.
     */
    fun onPlayerException(error: Throwable) {
        val currentState = _uiState.value
        if (currentState.isRecovering) return

        _uiState.update { it.copy(isRecovering = true, error = "Verifying playback & recovering stream...") }

        recoveryJob = viewModelScope.launch {
            val result = playbackRecoveryManager.attemptRecovery(
                mediaId = mediaId,
                currentPosition = _progress.value.currentPosition,
                error = error,
                currentSource = currentState.selectedSource,
                availableSources = currentState.streamSources,
                retryAttemptCount = currentState.retryAttemptCount,
                budget = recoveryBudget
            )

            when (result) {
                is com.example.vista.data.playback.RecoveryResult.RetrySameSource -> {
                    val refreshed = try {
                        kotlinx.coroutines.withTimeout(20_000) {
                            playbackRepository.getStreamSources(mediaId, mediaType, currentState.seasonNumber, currentState.episodeNumber, titleHint = title)
                        }.firstOrNull { it.name == currentState.selectedSource?.name }
                    } catch (_: kotlinx.coroutines.TimeoutCancellationException) { null }
                    catch (e: kotlinx.coroutines.CancellationException) { throw e }
                    catch (_: Exception) { null }
                    if (refreshed == null) {
                        _uiState.update { it.copy(isRecovering = false, error = "The provider could not refresh this stream. Try again later.", retryAttemptCount = it.retryAttemptCount + 1) }
                        return@launch
                    }
                    _uiState.update {
                        it.copy(
                            isRecovering = false,
                            selectedSource = refreshed,
                            sourceGeneration = it.sourceGeneration + 1,
                            error = null,
                            retryAttemptCount = it.retryAttemptCount + 1,
                            isPlaying = true
                        )
                    }
                }
                is com.example.vista.data.playback.RecoveryResult.SwitchToAlternativeSource -> {
                    _uiState.update {
                        it.copy(
                            isRecovering = false,
                            selectedSource = result.newSource,
                            error = null,
                            retryAttemptCount = it.retryAttemptCount + 1,
                            isPlaying = true
                        )
                    }
                }
                is com.example.vista.data.playback.RecoveryResult.RequiresNetwork -> {
                    _uiState.update {
                        it.copy(
                            isRecovering = false,
                            error = "Network connection lost. Please check connection.",
                            canRepairManually = true
                        )
                    }
                }
                is com.example.vista.data.playback.RecoveryResult.Failed -> {
                    _uiState.update {
                        it.copy(
                            isRecovering = false,
                            error = result.userMessage,
                            canRepairManually = result.canRepairManually
                        )
                    }
                }
            }
        }
    }

    /**
     * Executes manual "Repair Playback": invalidates local temporary player cache and flushes HTTP response cache,
     * then resets retry count and retries loading.
     */
    fun repairPlaybackAndRetry() {
        if (_uiState.value.isRecovering) return
        val source = _uiState.value.selectedSource
        val position = _progress.value
        _uiState.update { it.copy(isRecovering = true, error = null, isPlaying = false) }
        recoveryJob = viewModelScope.launch {
            try {
                playbackAccess.requireAccess()
                sessionManager.refreshSession().getOrThrow()
                source?.url?.let { cacheManager.invalidatePlaybackResponse(it) }
                val refreshed = kotlinx.coroutines.withTimeout(20_000) {
                    playbackRepository.getStreamSources(mediaId, mediaType, _uiState.value.seasonNumber, _uiState.value.episodeNumber, titleHint = title)
                }.firstOrNull { source == null || it.name == source.name } ?: error("The provider could not refresh this stream.")
                _progress.value = position
                recoveryBudget.reset()
                _uiState.update { it.copy(selectedSource = refreshed, sourceGeneration = it.sourceGeneration + 1, retryAttemptCount = 0, isRecovering = false, isPlaying = true) }
            } catch (_: kotlinx.coroutines.TimeoutCancellationException) {
                _uiState.update { it.copy(isRecovering = false, error = "The provider did not respond in time.") }
            } catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (e: Exception) { _uiState.update { it.copy(isRecovering = false, error = e.message ?: "Playback repair failed.") } }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    override fun onCleared() {
        super.onCleared()
        cacheManager.unregisterActiveSession(playbackSessionId)
        // viewModelScope is already cancelled by the time onCleared runs, so a plain launch would
        // never execute. Persist the final position from the scope we own.
        val snapshot = _uiState.value
        exitScope.launch { persist(snapshot) }
    }
}
