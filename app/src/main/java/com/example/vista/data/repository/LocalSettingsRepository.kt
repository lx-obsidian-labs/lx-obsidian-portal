package com.example.vista.data.repository

import com.example.vista.data.local.PreferencesManager
import com.example.vista.data.local.dao.PlaybackHistoryDao
import com.example.vista.data.local.dao.RecentSearchDao
import com.example.vista.domain.model.AppSettings
import com.example.vista.domain.model.AppTheme
import com.example.vista.domain.model.CellularStreamingSetting
import com.example.vista.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton
import coil.ImageLoader

@Singleton
class LocalSettingsRepository @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val historyDao: PlaybackHistoryDao,
    private val recentSearchDao: RecentSearchDao,
    private val imageLoader: ImageLoader,
    private val cacheManager: com.example.vista.data.cache.CacheManager,
    private val profile: com.example.vista.data.local.ProfileScope,
    private val session: com.example.vista.data.session.SessionManager
) : SettingsRepository {

    override fun getSettings(): Flow<AppSettings> = preferencesManager.settings

    override suspend fun updateSettings(settings: AppSettings) {
        preferencesManager.updateSettings(settings)
    }

    override suspend fun updateAutoplayNext(enabled: Boolean) {
        preferencesManager.updateAutoplayNext(enabled)
    }

    override suspend fun updatePreferredQuality(quality: String) {
        preferencesManager.updatePreferredQuality(quality)
    }

    override suspend fun updateSubtitleLanguage(language: String) {
        preferencesManager.updateSubtitleLanguage(language)
    }

    override suspend fun updateSubtitleEnabled(enabled: Boolean) {
        preferencesManager.updateSubtitleEnabled(enabled)
    }

    override suspend fun updateSubtitleSize(size: Float) {
        preferencesManager.updateSubtitleSize(size)
    }

    override suspend fun updateSubtitleColor(color: String) {
        preferencesManager.updateSubtitleColor(color)
    }

    override suspend fun updatePlaybackSpeed(speed: Float) {
        preferencesManager.updatePlaybackSpeed(speed)
    }

    override suspend fun updateCellularStreaming(setting: CellularStreamingSetting) {
        preferencesManager.updateCellularStreaming(setting)
    }

    override suspend fun updateAppTheme(theme: AppTheme) {
        preferencesManager.updateAppTheme(theme)
    }

    override suspend fun updateDataSaverMode(enabled: Boolean) {
        preferencesManager.updateDataSaverMode(enabled)
    }

    override suspend fun updateAutoResume(enabled: Boolean) {
        preferencesManager.updateAutoResume(enabled)
    }

    override suspend fun updateNotificationsEnabled(enabled: Boolean) {
        preferencesManager.updateNotificationsEnabled(enabled)
    }

    override suspend fun updateNotificationNewEpisodes(enabled: Boolean) {
        preferencesManager.updateNotificationNewEpisodes(enabled)
    }

    override suspend fun updateNotificationNewSeasons(enabled: Boolean) {
        preferencesManager.updateNotificationNewSeasons(enabled)
    }

    override suspend fun updateNotificationReleases(enabled: Boolean) {
        preferencesManager.updateNotificationReleases(enabled)
    }

    override suspend fun updateNotificationTvGuideReminders(enabled: Boolean) {
        preferencesManager.updateNotificationTvGuideReminders(enabled)
    }

    override suspend fun updateShareDiagnostics(enabled: Boolean) {
        preferencesManager.updateShareDiagnostics(enabled)
    }

    override suspend fun updateAudioLanguage(language: String) {
        preferencesManager.updateAudioLanguage(language)
    }

    override suspend fun updateDoubleTapToSeek(seconds: Int) {
        preferencesManager.updateDoubleTapToSeek(seconds)
    }

    override suspend fun updateShowAdultContent(show: Boolean) {
        preferencesManager.updateShowAdultContent(show)
    }

    override suspend fun updateCacheLimitMb(limitMb: Int) {
        preferencesManager.updateCacheLimitMb(limitMb)
    }

    override suspend fun clearHistory() {
        historyDao.deleteAll(profile.claimLegacy())
    }

    override suspend fun clearSearchHistory() {
        // Search history is stored in Room; the DataStore copy is cleared too so entries written by
        // older builds are removed as well.
        recentSearchDao.deleteAll()
        preferencesManager.clearRecentSearches()
    }

    @OptIn(coil.annotation.ExperimentalCoilApi::class)
    override suspend fun clearCache() {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { cacheManager.clearAllCache() }
    }

    override suspend fun repairPlaybackCache() {
        session.refreshSession().getOrThrow()
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { cacheManager.repairPlaybackCache() }
    }
}
