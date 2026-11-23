package com.example.vista.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.vista.domain.model.AgeBand
import com.example.vista.domain.model.AppSettings
import com.example.vista.domain.model.AppTheme
import com.example.vista.domain.model.CellularStreamingSetting
import com.example.vista.util.CountryResolver
import java.security.MessageDigest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val AUTOPLAY_NEXT = booleanPreferencesKey("autoplay_next")
        val PREFERRED_QUALITY = stringPreferencesKey("preferred_quality")
        val SUBTITLE_LANGUAGE = stringPreferencesKey("subtitle_language")
        val SUBTITLE_ENABLED = booleanPreferencesKey("subtitle_enabled")
        val SUBTITLE_SIZE = floatPreferencesKey("subtitle_size")
        val SUBTITLE_COLOR = stringPreferencesKey("subtitle_color")
        val PLAYBACK_SPEED = floatPreferencesKey("playback_speed")
        val CELLULAR_STREAMING = stringPreferencesKey("cellular_streaming")
        val APP_THEME = stringPreferencesKey("app_theme")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val NOTIF_NEW_EPISODES = booleanPreferencesKey("notif_new_episodes")
        val NOTIF_NEW_SEASONS = booleanPreferencesKey("notif_new_seasons")
        val NOTIF_RELEASES = booleanPreferencesKey("notif_releases")
        val NOTIF_TV_GUIDE = booleanPreferencesKey("notif_tv_guide")
        val SHARE_DIAGNOSTICS = booleanPreferencesKey("share_diagnostics")
        val AUDIO_LANGUAGE = stringPreferencesKey("audio_language")
        val DOUBLE_TAP_TO_SEEK = intPreferencesKey("double_tap_to_seek")
        val SHOW_ADULT_CONTENT = booleanPreferencesKey("show_adult_content")
        val AGE_BAND = stringPreferencesKey("age_band")
        val KIDS_MODE = booleanPreferencesKey("kids_mode")
        val PARENTAL_PIN_HASH = stringPreferencesKey("parental_pin_hash")
        val DATA_SAVER_MODE = booleanPreferencesKey("data_saver_mode")
        val AUTO_RESUME = booleanPreferencesKey("auto_resume")
        val CACHE_LIMIT_MB = intPreferencesKey("cache_limit_mb")
        val DOWNLOAD_MAX_HEIGHT = intPreferencesKey("download_max_height")
        val RECENT_SEARCHES = stringSetPreferencesKey("recent_searches")
        val TRENDING_SEARCHES = stringSetPreferencesKey("trending_searches")
        val GUEST_MODE = booleanPreferencesKey("guest_mode")
        val GUEST_TRIAL_STARTED_AT = longPreferencesKey("guest_trial_started_at")
        val ONBOARDING_SEEN_AT = longPreferencesKey("onboarding_seen_at")
        val DISPLAY_NAME = stringPreferencesKey("display_name")
        val PREMIUM_ACKED = booleanPreferencesKey("premium_acked")
        val BELL_LAST_SEEN_AT = longPreferencesKey("bell_last_seen_at")
        val NOTIF_ASKED = booleanPreferencesKey("notif_permission_asked")
        val NOTIF_FCM_TOKEN = stringPreferencesKey("notif_fcm_token")
        val COUNTRY_CODE = stringPreferencesKey("country_code")
        val MOTD_ENABLED = booleanPreferencesKey("motd_enabled")
        val HIDDEN_FESTIVE_SEASON = intPreferencesKey("hidden_festive_season")
        val MOTD_LAST_CARD_DAY = longPreferencesKey("motd_last_card_day")
        val MOTD_NOT_INTERESTED = booleanPreferencesKey("motd_not_interested")
        val MOTD_LAST_DISMISSED_DAY = longPreferencesKey("motd_last_dismissed_day")

        // Milestone sharing. LAUNCH_COUNT is the segmentation input: share prompts only go to
        // viewers who have opened the app repeatedly, so casual and brand-new installs are excluded.
        val LAUNCH_COUNT = intPreferencesKey("launch_count")
        val SHARE_PROMPTS_ENABLED = booleanPreferencesKey("share_prompts_enabled")
        val SHARE_CELEBRATED = stringSetPreferencesKey("share_celebrated")
        val SHARE_PROMPT_MONTH = stringPreferencesKey("share_prompt_month")
        val SHARE_PROMPT_COUNT = intPreferencesKey("share_prompt_count")
        val SHARE_LAST_PROMPT_AT = longPreferencesKey("share_last_prompt_at")

        // AI Features
        val AI_FEATURES_ENABLED = booleanPreferencesKey("ai_features_enabled")
        val AI_CHAT_ENABLED = booleanPreferencesKey("ai_chat_enabled")
        val AI_RECOMMENDATIONS_ENABLED = booleanPreferencesKey("ai_recommendations_enabled")
        val AI_PERSONALIZATION_ENABLED = booleanPreferencesKey("ai_personalization_enabled")
        val FAVORITE_GENRES = stringSetPreferencesKey("favorite_genres")
        val FAVORITE_ACTORS = stringSetPreferencesKey("favorite_actors")
        val USER_ID = stringPreferencesKey("user_id")
    }

    companion object {
        /** Non-renewable guest free-trial length. */
        const val GUEST_TRIAL_DAYS = 3L

        /** How long before a returning viewer is shown the onboarding "welcome back" sequence. */
        const val WELCOME_BACK_AFTER_DAYS = 30L
    }

    /**
     * Whether the current viewer is browsing as a guest.
     *
     * This is a local-only flag deliberately: a guest session needs no Firebase account, so guest
     * mode works without enabling the Anonymous sign-in provider in the console and cannot lock a
     * viewer out if that provider is unavailable.
     */
    val isGuest: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.GUEST_MODE] ?: false
    }

    suspend fun setGuestMode(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[Keys.GUEST_MODE] = enabled }
    }

    /** When the current viewer's free guest trial started (0 = no trial started yet). */
    val guestTrialStartedAt: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[Keys.GUEST_TRIAL_STARTED_AT] ?: 0L
    }

    /**
     * Starts the non-renewable trial on first guest use. Returns false when the trial has already
     * been used up (a previous guest ran past [GUEST_TRIAL_DAYS]) so the caller can block access.
     */
    suspend fun startOrCheckGuestTrial(): Boolean {
        val startedAt = guestTrialStartedAtValue()
        if (startedAt == 0L) {
            context.dataStore.edit { prefs -> prefs[Keys.GUEST_TRIAL_STARTED_AT] = System.currentTimeMillis() }
            return true
        }
        return !guestTrialExpired(startedAt)
    }

    fun guestTrialExpired(startedAt: Long): Boolean {
        val trialMs = GUEST_TRIAL_DAYS * 24 * 60 * 60 * 1000L
        return System.currentTimeMillis() - startedAt > trialMs
    }

    suspend fun guestTrialStartedAtValue(): Long =
        context.dataStore.data.map { it[Keys.GUEST_TRIAL_STARTED_AT] ?: 0L }.first()

    /** When onboarding was last completed (0 = first-time viewer). */
    val onboardingSeenAt: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[Keys.ONBOARDING_SEEN_AT] ?: 0L
    }

    suspend fun markOnboardingSeen() {
        context.dataStore.edit { prefs -> prefs[Keys.ONBOARDING_SEEN_AT] = System.currentTimeMillis() }
    }

    /**
     * How many times this install has been cold started.
     *
     * Incremented by the caller on each process start, and used only as a maturity signal for share
     * prompts. It is deliberately a count of *process* starts rather than resumes, so backgrounding
     * and returning to the app cannot slowly tip someone over the "power user" threshold.
     */
    suspend fun recordAppLaunch(): Int {
        var updated = 0
        context.dataStore.edit { prefs ->
            updated = (prefs[Keys.LAUNCH_COUNT] ?: 0) + 1
            prefs[Keys.LAUNCH_COUNT] = updated
        }
        return updated
    }

    suspend fun launchCount(): Int = context.dataStore.data.map { it[Keys.LAUNCH_COUNT] ?: 0 }.first()

    /**
     * Whether milestone share prompts are enabled. Defaults on; the opt-out must be easy to find.
     *
     * A Flow rather than a one-shot read so the Settings switch reflects the stored value, and so
     * the coordinator can take `.first()` at process start without a race against the settings flow.
     */
    val sharePromptsEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.SHARE_PROMPTS_ENABLED] ?: true
    }

    suspend fun setSharePromptsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SHARE_PROMPTS_ENABLED] = enabled }
    }

    /** Milestone ids already offered, so the same win is never celebrated twice. */
    suspend fun celebratedMilestones(): Set<String> =
        context.dataStore.data.map { it[Keys.SHARE_CELEBRATED] ?: emptySet() }.first()

    /**
     * Records a milestone as celebrated and spends one slot of this month's allowance.
     *
     * Idempotent by milestone id: a milestone delivered as a notification and then opened, which
     * surfaces the same id twice, still counts only once. Without this a single prompt would burn
     * two of the two monthly slots and silently halve the cap.
     *
     * The month key is rolled forward whenever it does not match the current month, which is what
     * resets the monthly cap without needing a scheduled job to run at midnight on the first.
     *
     * @return true if this call was the one that consumed a slot.
     */
    suspend fun recordSharePromptShown(milestoneId: String, nowMillis: Long = System.currentTimeMillis()): Boolean {
        val currentMonth = com.example.vista.domain.model.SharePromptPolicy.currentMonthKey(nowMillis)
        var consumedSlot = false
        context.dataStore.edit { prefs ->
            val celebrated = prefs[Keys.SHARE_CELEBRATED] ?: emptySet()
            if (milestoneId !in celebrated) {
                consumedSlot = true
                val storedMonth = prefs[Keys.SHARE_PROMPT_MONTH]
                val count = if (storedMonth == currentMonth) (prefs[Keys.SHARE_PROMPT_COUNT] ?: 0) else 0
                prefs[Keys.SHARE_PROMPT_COUNT] = count + 1
                prefs[Keys.SHARE_PROMPT_MONTH] = currentMonth
                prefs[Keys.SHARE_LAST_PROMPT_AT] = nowMillis
            }
            prefs[Keys.SHARE_CELEBRATED] = celebrated + milestoneId
        }
        return consumedSlot
    }

    suspend fun sharePromptHistory(): Triple<Int, String, Long> {
        var month = ""
        var count = 0
        var last = 0L
        context.dataStore.data.map { prefs ->
            month = prefs[Keys.SHARE_PROMPT_MONTH] ?: ""
            count = prefs[Keys.SHARE_PROMPT_COUNT] ?: 0
            last = prefs[Keys.SHARE_LAST_PROMPT_AT] ?: 0L
            prefs
        }.first()
        return Triple(count, month, last)
    }

    /** Name used for the home greeting, saved by onboarding / profile. */
    val displayName: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.DISPLAY_NAME] ?: ""
    }

    suspend fun setDisplayName(name: String) {
        context.dataStore.edit { prefs -> prefs[Keys.DISPLAY_NAME] = name.trim() }
    }

    private fun avatarKey(owner: String) = stringPreferencesKey("avatar_id:${if (owner.isBlank()) "guest" else "user:$owner"}")
    private fun avatarPendingKey(owner: String) = booleanPreferencesKey("avatar_sync_pending:${if (owner.isBlank()) "guest" else "user:$owner"}")
    fun avatarId(owner: String): Flow<String?> = context.dataStore.data.map { it[avatarKey(owner)] }
    suspend fun setAvatarId(owner: String, id: String, syncPending: Boolean = false) {
        context.dataStore.edit { prefs ->
            prefs[avatarKey(owner)] = id
            prefs[avatarPendingKey(owner)] = syncPending
        }
    }
    suspend fun restoreAvatarIfUnset(owner: String, id: String) {
        context.dataStore.edit { prefs -> if (prefs[avatarKey(owner)] == null) prefs[avatarKey(owner)] = id }
    }
    suspend fun avatarSyncPending(owner: String): Boolean = context.dataStore.data.map { it[avatarPendingKey(owner)] ?: false }.first()
    suspend fun acknowledgeAvatarSync(owner: String, id: String) {
        context.dataStore.edit { prefs -> if (prefs[avatarKey(owner)] == id) prefs[avatarPendingKey(owner)] = false }
    }

    /** Whether the R20/month premium announcement has been acknowledged. */
    val premiumAcked: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.PREMIUM_ACKED] ?: false
    }

    suspend fun markPremiumAcked() {
        context.dataStore.edit { prefs -> prefs[Keys.PREMIUM_ACKED] = true }
    }

    /** Last time the notification bell was opened (drives the unread badge). */
    val bellLastSeenAt: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[Keys.BELL_LAST_SEEN_AT] ?: 0L
    }

    suspend fun markBellSeen() {
        context.dataStore.edit { prefs -> prefs[Keys.BELL_LAST_SEEN_AT] = System.currentTimeMillis() }
    }

    /** Whether the POST_NOTIFICATIONS runtime permission was already asked (ask once, never again). */
    suspend fun hasAskedForNotificationPermission(): Boolean =
        context.dataStore.data.map { it[Keys.NOTIF_ASKED] ?: false }.first()

    suspend fun markNotificationPermissionAsked() {
        context.dataStore.edit { prefs -> prefs[Keys.NOTIF_ASKED] = true }
    }

    /** Latest FCM registration token, kept for diagnostics (Settings > About). */
    val fcmToken: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.NOTIF_FCM_TOKEN] ?: ""
    }

    suspend fun setFcmToken(token: String) {
        context.dataStore.edit { prefs -> prefs[Keys.NOTIF_FCM_TOKEN] = token }
    }

    /**
     * The viewer's ISO 3166-1 alpha-2 country ("" when undetermined). Drives announcement targeting
     * and, later, the referral programme's per-market rules.
     */
    val countryCode: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.COUNTRY_CODE] ?: ""
    }

    /**
     * Captures the viewer's country on first launch and returns whatever is now on record.
     *
     * Resolved once and then left alone, deliberately. What matters is the market the viewer was
     * acquired in, so a handset that later switches region - or picks up a travel SIM - must not
     * re-bucket somebody out of the list they were counted in.
     */
    suspend fun ensureCountryCode(): String {
        val recorded = countryCode.first()
        if (recorded.isNotEmpty()) return recorded
        val resolved = CountryResolver.resolve(context)
        context.dataStore.edit { prefs -> prefs[Keys.COUNTRY_CODE] = resolved }
        return resolved
    }

    val ageBand: Flow<AgeBand> = context.dataStore.data.map { prefs ->
        try { AgeBand.valueOf(prefs[Keys.AGE_BAND] ?: AgeBand.ADULT.name) } catch (_: Exception) { AgeBand.ADULT }
    }

    val kidsMode: Flow<Boolean> = context.dataStore.data.map { prefs -> prefs[Keys.KIDS_MODE] ?: false }
    val hasParentalPin: Flow<Boolean> = context.dataStore.data.map { prefs -> !prefs[Keys.PARENTAL_PIN_HASH].isNullOrBlank() }

    suspend fun setAgeBand(band: AgeBand) {
        context.dataStore.edit { prefs ->
            prefs[Keys.AGE_BAND] = band.name
            if (band != AgeBand.ADULT) prefs[Keys.SHOW_ADULT_CONTENT] = false
        }
    }

    suspend fun setKidsMode(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.KIDS_MODE] = enabled
            if (enabled) prefs[Keys.SHOW_ADULT_CONTENT] = false
        }
    }

    suspend fun setParentalPin(pin: String) {
        context.dataStore.edit { prefs ->
            if (pin.isBlank()) prefs.remove(Keys.PARENTAL_PIN_HASH)
            else prefs[Keys.PARENTAL_PIN_HASH] = hashPin(pin)
        }
    }

    suspend fun verifyParentalPin(pin: String): Boolean =
        context.dataStore.data.map { prefs ->
            val stored = prefs[Keys.PARENTAL_PIN_HASH]
            stored.isNullOrBlank() || MessageDigest.isEqual(stored.toByteArray(), hashPin(pin).toByteArray())
        }.first()

    private fun hashPin(pin: String): String =
        MessageDigest.getInstance("SHA-256").digest(pin.trim().toByteArray()).joinToString("") { "%02x".format(it) }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        val band = try { AgeBand.valueOf(prefs[Keys.AGE_BAND] ?: AgeBand.ADULT.name) } catch (_: Exception) { AgeBand.ADULT }
        val kids = prefs[Keys.KIDS_MODE] ?: false
        val adultAllowed = band == AgeBand.ADULT && !kids
        AppSettings(
            autoplayNext = prefs[Keys.AUTOPLAY_NEXT] ?: true,
            preferredQuality = prefs[Keys.PREFERRED_QUALITY] ?: "Auto",
            subtitleLanguage = prefs[Keys.SUBTITLE_LANGUAGE] ?: "Off",
            subtitleEnabled = prefs[Keys.SUBTITLE_ENABLED] ?: false,
            subtitleSize = prefs[Keys.SUBTITLE_SIZE] ?: 16f,
            subtitleColor = prefs[Keys.SUBTITLE_COLOR] ?: "White",
            playbackSpeed = prefs[Keys.PLAYBACK_SPEED] ?: 1f,
            cellularStreaming = try {
                CellularStreamingSetting.valueOf(prefs[Keys.CELLULAR_STREAMING] ?: "ALWAYS_ASK")
            } catch (_: Exception) {
                CellularStreamingSetting.ALWAYS_ASK
            },
            appTheme = try {
                AppTheme.valueOf(prefs[Keys.APP_THEME] ?: "DARK")
            } catch (_: Exception) {
                AppTheme.DARK
            },
            notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: true,
            notificationNewEpisodes = prefs[Keys.NOTIF_NEW_EPISODES] ?: true,
            notificationNewSeasons = prefs[Keys.NOTIF_NEW_SEASONS] ?: true,
            notificationReleases = prefs[Keys.NOTIF_RELEASES] ?: true,
            notificationTvGuideReminders = prefs[Keys.NOTIF_TV_GUIDE] ?: true,
            shareDiagnostics = prefs[Keys.SHARE_DIAGNOSTICS] ?: true,
            audioLanguage = prefs[Keys.AUDIO_LANGUAGE] ?: "Original",
            doubleTapToSeekSeconds = prefs[Keys.DOUBLE_TAP_TO_SEEK] ?: 10,
            showAdultContent = adultAllowed && (prefs[Keys.SHOW_ADULT_CONTENT] ?: false),
            dataSaverMode = prefs[Keys.DATA_SAVER_MODE] ?: false,
            autoResume = prefs[Keys.AUTO_RESUME] ?: true,
            cacheLimitMb = prefs[Keys.CACHE_LIMIT_MB] ?: 500,
            ageBand = band,
            kidsMode = kids
        )
    }

    suspend fun updateSettings(settings: AppSettings) {
        context.dataStore.edit { prefs ->
            prefs[Keys.AUTOPLAY_NEXT] = settings.autoplayNext
            prefs[Keys.PREFERRED_QUALITY] = settings.preferredQuality
            prefs[Keys.SUBTITLE_LANGUAGE] = settings.subtitleLanguage
            prefs[Keys.SUBTITLE_ENABLED] = settings.subtitleEnabled
            prefs[Keys.SUBTITLE_SIZE] = settings.subtitleSize
            prefs[Keys.SUBTITLE_COLOR] = settings.subtitleColor
            prefs[Keys.PLAYBACK_SPEED] = settings.playbackSpeed
            prefs[Keys.CELLULAR_STREAMING] = settings.cellularStreaming.name
            prefs[Keys.APP_THEME] = settings.appTheme.name
            prefs[Keys.NOTIFICATIONS_ENABLED] = settings.notificationsEnabled
            prefs[Keys.NOTIF_NEW_EPISODES] = settings.notificationNewEpisodes
            prefs[Keys.NOTIF_NEW_SEASONS] = settings.notificationNewSeasons
            prefs[Keys.NOTIF_RELEASES] = settings.notificationReleases
            prefs[Keys.NOTIF_TV_GUIDE] = settings.notificationTvGuideReminders
            prefs[Keys.SHARE_DIAGNOSTICS] = settings.shareDiagnostics
            prefs[Keys.AUDIO_LANGUAGE] = settings.audioLanguage
            prefs[Keys.DOUBLE_TAP_TO_SEEK] = settings.doubleTapToSeekSeconds
            prefs[Keys.SHOW_ADULT_CONTENT] = settings.ageBand == AgeBand.ADULT && !settings.kidsMode && settings.showAdultContent
            prefs[Keys.AGE_BAND] = settings.ageBand.name
            prefs[Keys.KIDS_MODE] = settings.kidsMode
            prefs[Keys.DATA_SAVER_MODE] = settings.dataSaverMode
            prefs[Keys.AUTO_RESUME] = settings.autoResume
            prefs[Keys.CACHE_LIMIT_MB] = settings.cacheLimitMb
        }
    }

    suspend fun updateCacheLimitMb(limitMb: Int) {
        context.dataStore.edit { it[Keys.CACHE_LIMIT_MB] = limitMb }
    }

    suspend fun updateNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    }

    /** Latest known FCM registration; read by the messaging service for diagnostics. */
    suspend fun notificationsEnabled(): Boolean =
        context.dataStore.data.map { it[Keys.NOTIFICATIONS_ENABLED] ?: true }.first()

    suspend fun updateNotificationNewEpisodes(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOTIF_NEW_EPISODES] = enabled }
    }

    suspend fun updateNotificationNewSeasons(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOTIF_NEW_SEASONS] = enabled }
    }

    suspend fun updateNotificationReleases(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOTIF_RELEASES] = enabled }
    }

    suspend fun updateNotificationTvGuideReminders(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOTIF_TV_GUIDE] = enabled }
    }

    suspend fun updateShareDiagnostics(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SHARE_DIAGNOSTICS] = enabled }
    }

    suspend fun updateAudioLanguage(language: String) {
        context.dataStore.edit { it[Keys.AUDIO_LANGUAGE] = language }
    }

    suspend fun updateDoubleTapToSeek(seconds: Int) {
        context.dataStore.edit { it[Keys.DOUBLE_TAP_TO_SEEK] = seconds }
    }

    suspend fun updateShowAdultContent(show: Boolean) {
        val allowed = settings.first().let { it.ageBand == AgeBand.ADULT && !it.kidsMode }
        context.dataStore.edit { it[Keys.SHOW_ADULT_CONTENT] = show && allowed }
    }

    suspend fun updateDataSaverMode(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DATA_SAVER_MODE] = enabled }
    }

    suspend fun updateAutoplayNext(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTOPLAY_NEXT] = enabled }
    }

    suspend fun updatePreferredQuality(quality: String) {
        context.dataStore.edit { it[Keys.PREFERRED_QUALITY] = quality }
    }

    suspend fun updateSubtitleLanguage(language: String) {
        context.dataStore.edit { it[Keys.SUBTITLE_LANGUAGE] = language }
    }

    suspend fun updateSubtitleEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SUBTITLE_ENABLED] = enabled }
    }

    suspend fun updateSubtitleSize(size: Float) {
        context.dataStore.edit { it[Keys.SUBTITLE_SIZE] = size }
    }

    suspend fun updateSubtitleColor(color: String) {
        context.dataStore.edit { it[Keys.SUBTITLE_COLOR] = color }
    }

    suspend fun updatePlaybackSpeed(speed: Float) {
        context.dataStore.edit { it[Keys.PLAYBACK_SPEED] = speed }
    }

    suspend fun updateCellularStreaming(setting: CellularStreamingSetting) {
        context.dataStore.edit { it[Keys.CELLULAR_STREAMING] = setting.name }
    }

    suspend fun updateAppTheme(theme: AppTheme) {
        context.dataStore.edit { it[Keys.APP_THEME] = theme.name }
    }

    suspend fun updateAutoResume(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_RESUME] = enabled }
    }
    
    // Recent and Trending Searches
    val recentSearches: Flow<List<String>> = context.dataStore.data.map { prefs ->
        prefs[Keys.RECENT_SEARCHES]?.toList() ?: emptyList()
    }
    
    val trendingSearches: Flow<List<String>> = context.dataStore.data.map { prefs ->
        prefs[Keys.TRENDING_SEARCHES]?.toList() ?: listOf(
            "Dune", "Breaking Bad", "Inception", "Stranger Things", "The Matrix"
        )
    }
    
    suspend fun addRecentSearch(query: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.RECENT_SEARCHES]?.toMutableList() ?: mutableListOf()
            current.remove(query) // Remove if exists to avoid duplicates
            current.add(0, query) // Add to front
            // Keep only last 10 searches
            val trimmed = current.take(10)
            prefs[Keys.RECENT_SEARCHES] = trimmed.toSet()
        }
    }
    
    suspend fun clearRecentSearches() {
        context.dataStore.edit { prefs ->
            prefs.remove(Keys.RECENT_SEARCHES)
        }
    }

    // ── Movie of the Day ──

    /**
     * Whether the daily suggestion is shown at all.
     *
     * Defaults to on. A suggestion is the app's main reason to be opened on an ordinary day, and
     * an opt-in nobody finds would leave the feature unused while still costing a work schedule.
     */
    val movieOfTheDayEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.MOTD_ENABLED] ?: true
    }

    val hiddenFestiveSeason: Flow<Int> = context.dataStore.data.map { it[Keys.HIDDEN_FESTIVE_SEASON] ?: 0 }
    val downloadMaxHeight: Flow<Int> = context.dataStore.data.map { it[Keys.DOWNLOAD_MAX_HEIGHT] ?: 720 }
    suspend fun setDownloadMaxHeight(value: Int) {
        require(value in setOf(480, 720, Int.MAX_VALUE))
        context.dataStore.edit { it[Keys.DOWNLOAD_MAX_HEIGHT] = value }
    }
    suspend fun hideFestiveSeason(year: Int) { context.dataStore.edit { it[Keys.HIDDEN_FESTIVE_SEASON] = year } }

    suspend fun setMovieOfTheDayEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[Keys.MOTD_ENABLED] = enabled }
    }

    /**
     * The last day the in-app card was shown, as an epoch day.
     *
     * Stored rather than a boolean so an app that is opened twice in one evening shows it once, and a
     * card that was dismissed is not brought back the same day.
     */
    val movieOfTheDayLastCardDay: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[Keys.MOTD_LAST_CARD_DAY] ?: 0L
    }

    suspend fun markMovieOfTheDayCardShown(epochDay: Long) {
        context.dataStore.edit { prefs -> prefs[Keys.MOTD_LAST_CARD_DAY] = epochDay }
    }

    /** Whether viewer marked Movie of the Day as "Not interested". */
    val movieOfTheDayNotInterested: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.MOTD_NOT_INTERESTED] ?: false
    }

    suspend fun setMovieOfTheDayNotInterested(value: Boolean) {
        context.dataStore.edit { prefs -> prefs[Keys.MOTD_NOT_INTERESTED] = value }
    }

    val movieOfTheDayLastDismissedDay: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[Keys.MOTD_LAST_DISMISSED_DAY] ?: 0L
    }

    suspend fun markMovieOfTheDayDismissedDay(epochDay: Long) {
        context.dataStore.edit { prefs -> prefs[Keys.MOTD_LAST_DISMISSED_DAY] = epochDay }
    }

    // ── AI Features ──
    
    val aiFeaturesEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.AI_FEATURES_ENABLED] ?: true
    }

    suspend fun setAiFeaturesEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[Keys.AI_FEATURES_ENABLED] = enabled }
    }

    val aiChatEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.AI_CHAT_ENABLED] ?: true
    }

    suspend fun setAiChatEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[Keys.AI_CHAT_ENABLED] = enabled }
    }

    val aiRecommendationsEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.AI_RECOMMENDATIONS_ENABLED] ?: true
    }

    suspend fun setAiRecommendationsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[Keys.AI_RECOMMENDATIONS_ENABLED] = enabled }
    }

    val aiPersonalizationEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.AI_PERSONALIZATION_ENABLED] ?: true
    }

    suspend fun setAiPersonalizationEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[Keys.AI_PERSONALIZATION_ENABLED] = enabled }
    }

    suspend fun getFavoriteGenres(): List<String> =
        context.dataStore.data.map { it[Keys.FAVORITE_GENRES]?.toList() ?: emptyList() }.first()

    suspend fun setFavoriteGenres(genres: List<String>) {
        context.dataStore.edit { prefs -> prefs[Keys.FAVORITE_GENRES] = genres.toSet() }
    }

    suspend fun getFavoriteActors(): List<String> =
        context.dataStore.data.map { it[Keys.FAVORITE_ACTORS]?.toList() ?: emptyList() }.first()

    suspend fun setFavoriteActors(actors: List<String>) {
        context.dataStore.edit { prefs -> prefs[Keys.FAVORITE_ACTORS] = actors.toSet() }
    }

    suspend fun getUserId(): String =
        context.dataStore.data.map { it[Keys.USER_ID] ?: "" }.first()

    suspend fun setUserId(userId: String) {
        context.dataStore.edit { prefs -> prefs[Keys.USER_ID] = userId }
    }
}
