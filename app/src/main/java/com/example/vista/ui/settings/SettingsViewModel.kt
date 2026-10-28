package com.example.vista.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vista.data.remote.firebase.FirebaseManager
import com.example.vista.domain.model.AppSettings
import com.example.vista.domain.model.AppTheme
import com.example.vista.domain.model.CellularStreamingSetting
import com.example.vista.domain.model.Viewer
import com.example.vista.domain.repository.SettingsRepository
import com.example.vista.motd.MovieOfTheDayScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

enum class SettingsPage {
    MAIN,
    ACCOUNT,
    PLAYBACK,
    AUDIO_SUBTITLES,
    NOTIFICATIONS,
    DATA_STORAGE,
    APPEARANCE,
    PRIVACY_HISTORY,
    ABOUT
}

data class SettingsUiState(
    val currentPage: SettingsPage = SettingsPage.MAIN,
    val autoplayNext: Boolean = true,
    val preferredQuality: String = "Auto",
    val subtitleLanguage: String = "Off",
    val subtitleEnabled: Boolean = false,
    val subtitleSize: Float = 16f,
    val subtitleColor: String = "White",
    val playbackSpeed: Float = 1f,
    val cellularStreaming: CellularStreamingSetting = CellularStreamingSetting.ALWAYS_ASK,
    val appTheme: AppTheme = AppTheme.DARK,
    val dataSaverMode: Boolean = false,
    val autoResume: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val notificationNewEpisodes: Boolean = true,
    val notificationNewSeasons: Boolean = true,
    val notificationReleases: Boolean = true,
    val notificationTvGuideReminders: Boolean = true,
    val movieOfTheDayEnabled: Boolean = true,
    /** Opt-out for milestone share prompts. Independent of [notificationsEnabled] on purpose. */
    val sharePromptsEnabled: Boolean = true,
    val shareDiagnostics: Boolean = true,
    val audioLanguage: String = "Original",
    val doubleTapToSeekSeconds: Int = 10,
    val showAdultContent: Boolean = false,
    val cacheLimitMb: Int = 500,
    val storageUsed: String = "0 B",
    val cacheSize: String = "0 B",
    val viewer: Viewer? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val firebaseManager: FirebaseManager,
    private val preferencesManager: com.example.vista.data.local.PreferencesManager,
    private val messenger: com.example.vista.ui.components.VistaMessenger,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
        loadMovieOfTheDayPreference()
        loadSharePromptsPreference()
        loadViewerProfile()
        loadDeviceInfo()
    }

    /**
     * Read separately from [loadSettings] because the flag lives in the local preferences store
     * rather than in [com.example.vista.domain.model.AppSettings]; it drives a local work schedule
     * and has no synced counterpart.
     */
    private fun loadMovieOfTheDayPreference() {
        viewModelScope.launch {
            preferencesManager.movieOfTheDayEnabled.collect { enabled ->
                _uiState.update { it.copy(movieOfTheDayEnabled = enabled) }
            }
        }
    }

    /** Same reasoning as [loadMovieOfTheDayPreference]: a local flag with no synced counterpart. */
    private fun loadSharePromptsPreference() {
        viewModelScope.launch {
            preferencesManager.sharePromptsEnabled.collect { enabled ->
                _uiState.update { it.copy(sharePromptsEnabled = enabled) }
            }
        }
    }

    private fun loadSettings() {
        viewModelScope.launch {
            settingsRepository.getSettings().collect { settings ->
                _uiState.update {
                    it.copy(
                        autoplayNext = settings.autoplayNext,
                        preferredQuality = settings.preferredQuality,
                        subtitleLanguage = settings.subtitleLanguage,
                        subtitleEnabled = settings.subtitleEnabled,
                        subtitleSize = settings.subtitleSize,
                        subtitleColor = settings.subtitleColor,
                        playbackSpeed = settings.playbackSpeed,
                        cellularStreaming = settings.cellularStreaming,
                        appTheme = settings.appTheme,
                        dataSaverMode = settings.dataSaverMode,
                        autoResume = settings.autoResume,
                        notificationsEnabled = settings.notificationsEnabled,
                        notificationNewEpisodes = settings.notificationNewEpisodes,
                        notificationNewSeasons = settings.notificationNewSeasons,
                        notificationReleases = settings.notificationReleases,
                        notificationTvGuideReminders = settings.notificationTvGuideReminders,
                        shareDiagnostics = settings.shareDiagnostics,
                        audioLanguage = settings.audioLanguage,
                        doubleTapToSeekSeconds = settings.doubleTapToSeekSeconds,
                        showAdultContent = settings.showAdultContent,
                        cacheLimitMb = settings.cacheLimitMb
                    )
                }
            }
        }
    }

    private fun loadViewerProfile() {
        viewModelScope.launch {
            val viewer = firebaseManager.getViewerProfile()
            _uiState.update { it.copy(viewer = viewer) }
        }
    }

    private fun loadDeviceInfo() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    storageUsed = formatSize(directorySize(context.filesDir) + directorySize(context.dataDir)),
                    cacheSize = formatSize(cacheBytes())
                )
            }
        }
    }

    fun refreshSizes() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    storageUsed = formatSize(directorySize(context.filesDir) + directorySize(context.dataDir)),
                    cacheSize = formatSize(cacheBytes())
                )
            }
        }
    }

    private fun cacheBytes(): Long {
        var total = directorySize(context.cacheDir)
        context.externalCacheDir?.let { total += directorySize(it) }
        return total
    }

    private fun directorySize(dir: File?): Long {
        if (dir == null || !dir.exists()) return 0L
        var size = 0L
        dir.listFiles()?.forEach { file ->
            size += if (file.isDirectory) directorySize(file) else runCatching { file.length() }.getOrDefault(0L)
        }
        return size
    }

    private fun formatSize(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return String.format(java.util.Locale.US, "%.0f KB", kb)
        val mb = kb / 1024.0
        if (mb < 1024) return String.format(java.util.Locale.US, "%.1f MB", mb)
        val gb = mb / 1024.0
        return String.format(java.util.Locale.US, "%.2f GB", gb)
    }

    fun navigateTo(page: SettingsPage) {
        _uiState.update { it.copy(currentPage = page) }
    }

    fun goBack(): Boolean {
        return if (_uiState.value.currentPage != SettingsPage.MAIN) {
            _uiState.update { it.copy(currentPage = SettingsPage.MAIN) }
            true
        } else {
            false
        }
    }

    fun updateAutoplayNext(autoplay: Boolean) {
        viewModelScope.launch { settingsRepository.updateAutoplayNext(autoplay) }
    }

    fun updatePreferredQuality(quality: String) {
        viewModelScope.launch { settingsRepository.updatePreferredQuality(quality) }
    }

    fun updateSubtitleLanguage(language: String) {
        viewModelScope.launch { settingsRepository.updateSubtitleLanguage(language) }
    }

    fun updateSubtitleEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateSubtitleEnabled(enabled) }
    }

    fun updateSubtitleSize(size: Float) {
        _uiState.update { it.copy(subtitleSize = size) }
        viewModelScope.launch { settingsRepository.updateSubtitleSize(size) }
    }

    fun updateSubtitleColor(color: String) {
        viewModelScope.launch { settingsRepository.updateSubtitleColor(color) }
    }

    fun updatePlaybackSpeed(speed: Float) {
        viewModelScope.launch { settingsRepository.updatePlaybackSpeed(speed) }
    }

    fun updateCellularStreaming(setting: CellularStreamingSetting) {
        viewModelScope.launch { settingsRepository.updateCellularStreaming(setting) }
    }

    fun updateAppTheme(theme: AppTheme) {
        viewModelScope.launch { settingsRepository.updateAppTheme(theme) }
    }

    fun updateDataSaverMode(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateDataSaverMode(enabled) }
    }

    fun updateAutoResume(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateAutoResume(enabled) }
    }

    fun updateAudioLanguage(language: String) {
        viewModelScope.launch { settingsRepository.updateAudioLanguage(language) }
    }

    fun updateDoubleTapToSeek(seconds: Int) {
        _uiState.update { it.copy(doubleTapToSeekSeconds = seconds) }
        viewModelScope.launch { settingsRepository.updateDoubleTapToSeek(seconds) }
    }

    fun updateShowAdultContent(show: Boolean) {
        viewModelScope.launch { settingsRepository.updateShowAdultContent(show) }
    }

    fun updateNotificationsEnabled(enabled: Boolean) {
        _uiState.update { it.copy(notificationsEnabled = enabled) }
        viewModelScope.launch { settingsRepository.updateNotificationsEnabled(enabled) }
    }

    /**
     * Opts out of milestone share prompts.
     *
     * Written straight to PreferencesManager rather than through SettingsRepository because this is
     * a prompt about the viewer's own behaviour, not a playback or content preference, and it has to
     * be readable synchronously at process start - before the settings flow has emitted - so the
     * prompt can be suppressed without a race.
     */
    fun updateSharePromptsEnabled(enabled: Boolean) {
        _uiState.update { it.copy(sharePromptsEnabled = enabled) }
        viewModelScope.launch { preferencesManager.setSharePromptsEnabled(enabled) }
    }

    fun updateNotificationNewEpisodes(enabled: Boolean) {
        _uiState.update { it.copy(notificationNewEpisodes = enabled) }
        viewModelScope.launch { settingsRepository.updateNotificationNewEpisodes(enabled) }
    }

    fun updateNotificationNewSeasons(enabled: Boolean) {
        _uiState.update { it.copy(notificationNewSeasons = enabled) }
        viewModelScope.launch { settingsRepository.updateNotificationNewSeasons(enabled) }
    }

    fun updateNotificationReleases(enabled: Boolean) {
        _uiState.update { it.copy(notificationReleases = enabled) }
        viewModelScope.launch { settingsRepository.updateNotificationReleases(enabled) }
    }

    fun updateNotificationTvGuideReminders(enabled: Boolean) {
        _uiState.update { it.copy(notificationTvGuideReminders = enabled) }
        viewModelScope.launch { settingsRepository.updateNotificationTvGuideReminders(enabled) }
    }

    /**
     * Turns the daily suggestion on or off.
     *
     * Also (re)registers or cancels the worker here rather than leaving it to app start, so the
     * change is immediate instead of appearing after the next cold launch.
     */
    fun updateMovieOfTheDay(enabled: Boolean) {
        _uiState.update { it.copy(movieOfTheDayEnabled = enabled) }
        viewModelScope.launch {
            // Persist before scheduling. Without this the switch only lived in _uiState, so the
            // opt-out silently reverted to the default (on) the next time a ViewModel was built -
            // and the card came back for someone who had deliberately turned it off.
            runCatching { preferencesManager.setMovieOfTheDayEnabled(enabled) }
            runCatching { MovieOfTheDayScheduler.schedule(context, enabled) }
        }
    }

    fun updateShareDiagnostics(enabled: Boolean) {
        _uiState.update { it.copy(shareDiagnostics = enabled) }
        viewModelScope.launch { settingsRepository.updateShareDiagnostics(enabled) }
    }

    fun clearHistory() {
        viewModelScope.launch { settingsRepository.clearHistory() }
    }

    fun clearSearchHistory() {
        viewModelScope.launch { settingsRepository.clearSearchHistory() }
    }

    fun updateCacheLimitMb(limitMb: Int) {
        _uiState.update { it.copy(cacheLimitMb = limitMb) }
        viewModelScope.launch { settingsRepository.updateCacheLimitMb(limitMb) }
    }

    fun clearCache() {
        viewModelScope.launch {
            try {
                settingsRepository.clearCache()
            } catch (_: Exception) {
            }
            refreshSizes()
        }
    }

    fun repairPlaybackCache() {
        viewModelScope.launch {
            try {
                settingsRepository.repairPlaybackCache()
                messenger.show("Session refreshed. Inactive temporary player files cleaned. Your saved data is preserved.")
            } catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (e: Exception) {
                messenger.show(e.message ?: "Playback repair could not finish.", com.example.vista.ui.components.VistaMessageKind.ERROR)
            }
            refreshSizes()
        }
    }

    fun signOut() {
        firebaseManager.signOut()
    }
}
