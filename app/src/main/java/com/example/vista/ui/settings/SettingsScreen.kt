package com.example.vista.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.vista.domain.model.AppTheme
import com.example.vista.domain.model.CellularStreamingSetting
import com.example.vista.navigation.Screen
import com.example.vista.ui.components.BrandAttribution
import com.example.vista.ui.components.TmdbAttribution
import com.example.vista.ui.components.VistaMessageKind
import com.example.vista.ui.components.rememberVistaMessenger
import kotlinx.coroutines.launch
import com.example.vista.ui.theme.*
import com.example.vista.util.InAppUpdateManager
import com.example.vista.util.UpdateState

/** Maps the deep-link `page` query value back to a settings page. */
internal fun settingsPageFromRoute(page: String?): SettingsPage = when (page?.uppercase()) {
    "ACCOUNT" -> SettingsPage.ACCOUNT
    "PLAYBACK" -> SettingsPage.PLAYBACK
    "AUDIO_SUBTITLES", "SUBTITLES" -> SettingsPage.AUDIO_SUBTITLES
    "NOTIFICATIONS" -> SettingsPage.NOTIFICATIONS
    "DATA_STORAGE", "DOWNLOADS", "DATA_USAGE" -> SettingsPage.DATA_STORAGE
    "APPEARANCE" -> SettingsPage.APPEARANCE
    "PRIVACY_HISTORY", "PRIVACY" -> SettingsPage.PRIVACY_HISTORY
    "ABOUT" -> SettingsPage.ABOUT
    else -> SettingsPage.MAIN
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    page: String? = null,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Honor a deep link that arrives after the screen is already composed.
    LaunchedEffect(page) {
        val target = settingsPageFromRoute(page)
        if (target != SettingsPage.MAIN && uiState.currentPage == SettingsPage.MAIN) {
            viewModel.navigateTo(target)
        }
    }

    BackHandler(enabled = uiState.currentPage != SettingsPage.MAIN) {
        viewModel.goBack()
    }

    val pageTitle = when (uiState.currentPage) {
        SettingsPage.MAIN -> "Settings"
        SettingsPage.ACCOUNT -> "Account"
        SettingsPage.PLAYBACK -> "Playback"
        SettingsPage.AUDIO_SUBTITLES -> "Audio & Subtitles"
        SettingsPage.NOTIFICATIONS -> "Notifications"
        SettingsPage.DATA_STORAGE -> "Data & Storage"
        SettingsPage.APPEARANCE -> "Appearance"
        SettingsPage.PRIVACY_HISTORY -> "Privacy & History"
        SettingsPage.ABOUT -> "About Vista Cinema"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // No arrow on MAIN: it is a bottom tab root, so there is nothing behind it to go back
            // to, and an arrow that pops off a root tab leaves the user with no way back. System
            // back still works, so a Settings screen pushed from Profile is still escapable.
            if (uiState.currentPage != SettingsPage.MAIN) {
                IconButton(onClick = { viewModel.goBack() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back",
                        tint = PrimaryText
                    )
                }
            }
            Column(modifier = Modifier.padding(start = 4.dp)) {
                Text(
                    text = pageTitle,
                    style = MaterialTheme.typography.headlineSmall,
                    color = PrimaryText,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = uiState.viewer?.alias?.let { "Signed in as $it" } ?: "Guest session",
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedText
                )
            }
        }

        AnimatedContent(
            targetState = uiState.currentPage,
            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
            label = "settings_page"
        ) { page ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 32.dp)
            ) {
                when (page) {
                    SettingsPage.MAIN -> MainSettingsPage(viewModel, uiState)
                    SettingsPage.ACCOUNT -> AccountSettingsPage(viewModel, uiState, navController)
                    SettingsPage.PLAYBACK -> PlaybackSettingsPage(viewModel, uiState)
                    SettingsPage.AUDIO_SUBTITLES -> AudioSubtitlesPage(viewModel, uiState)
                    SettingsPage.NOTIFICATIONS -> NotificationsPage(viewModel, uiState)
                    SettingsPage.DATA_STORAGE -> DataStoragePage(viewModel, uiState, navController)
                    SettingsPage.APPEARANCE -> AppearancePage(viewModel, uiState)
                    SettingsPage.PRIVACY_HISTORY -> PrivacyHistoryPage(viewModel, uiState)
                    SettingsPage.ABOUT -> AboutPage(viewModel, uiState, navController)
                }
            }
        }
    }
}

// ── Main ──

@Composable
private fun MainSettingsPage(viewModel: SettingsViewModel, uiState: SettingsUiState) {
    // Profile banner
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        color = RaisedSurface,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { viewModel.navigateTo(SettingsPage.ACCOUNT) }
                .background(Brush.horizontalGradient(listOf(Accent.copy(alpha = 0.15f), RaisedSurface)))
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(Brush.linearGradient(listOf(Accent, AccentDark)), CircleShape)
                    .border(2.dp, Color.White.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = uiState.viewer?.alias?.ifBlank { null } ?: "Guest User",
                    style = MaterialTheme.typography.titleMedium,
                    color = PrimaryText,
                    fontWeight = FontWeight.Black
                )
                Spacer(modifier = Modifier.height(2.dp))
                Surface(
                    color = Accent.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = if (uiState.viewer != null) "VISTA MEMBER" else "3-DAY GUEST TRIAL",
                        color = Accent,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
            Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = MutedText)
        }
    }

    SettingsHeader("PLAYBACK")
    SettingsGroup {
        SettingsNavRow(
            icon = Icons.Outlined.PlayArrow,
            iconBackground = Accent,
            title = "Playback",
            subtitle = "${uiState.preferredQuality} • Autoplay ${if (uiState.autoplayNext) "on" else "off"} • ${uiState.doubleTapToSeekSeconds}s seek",
            onClick = { viewModel.navigateTo(SettingsPage.PLAYBACK) }
        )
        Divider(color = Color.White.copy(alpha = 0.06f), modifier = Modifier.padding(start = 56.dp))
        SettingsNavRow(
            icon = Icons.Outlined.ClosedCaption,
            iconBackground = Accent,
            title = "Audio & Subtitles",
            subtitle = if (uiState.subtitleEnabled) {
                "${uiState.audioLanguage} • Subtitles ${uiState.subtitleLanguage.lowercase()}"
            } else {
                "${uiState.audioLanguage} • Subtitles off"
            },
            onClick = { viewModel.navigateTo(SettingsPage.AUDIO_SUBTITLES) }
        )
    }

    SettingsHeader("CONNECTIVITY & CONTENT")
    SettingsGroup {
        SettingsNavRow(
            icon = Icons.Outlined.Notifications,
            iconBackground = Accent,
            title = "Notifications",
            subtitle = if (uiState.notificationsEnabled) "On" else "Paused",
            onClick = { viewModel.navigateTo(SettingsPage.NOTIFICATIONS) }
        )
        Divider(color = Color.White.copy(alpha = 0.06f), modifier = Modifier.padding(start = 56.dp))
        SettingsNavRow(
            icon = Icons.Outlined.Storage,
            iconBackground = Accent,
            title = "Data & Storage",
            subtitle = "${uiState.cacheSize} cached",
            onClick = { viewModel.navigateTo(SettingsPage.DATA_STORAGE) }
        )
    }

    SettingsHeader("APP")
    SettingsGroup {
        SettingsNavRow(
            icon = Icons.Outlined.DarkMode,
            iconBackground = Accent,
            title = "Appearance",
            subtitle = uiState.appTheme.displayName,
            onClick = { viewModel.navigateTo(SettingsPage.APPEARANCE) }
        )
        Divider(color = Color.White.copy(alpha = 0.06f), modifier = Modifier.padding(start = 56.dp))
        SettingsNavRow(
            icon = Icons.Outlined.History,
            iconBackground = Accent,
            title = "Privacy & History",
            subtitle = if (uiState.shareDiagnostics) "Diagnostics shared" else "Diagnostics off",
            onClick = { viewModel.navigateTo(SettingsPage.PRIVACY_HISTORY) }
        )
        Divider(color = Color.White.copy(alpha = 0.06f), modifier = Modifier.padding(start = 56.dp))
        SettingsNavRow(
            icon = Icons.Outlined.Info,
            iconBackground = AmbientPurple,
            title = "About Vista Cinema",
            subtitle = "Version ${com.example.vista.BuildConfig.VERSION_NAME}",
            onClick = { viewModel.navigateTo(SettingsPage.ABOUT) }
        )
    }
}

// ── Account ──

@Composable
private fun AccountSettingsPage(
    viewModel: SettingsViewModel,
    uiState: SettingsUiState,
    navController: NavController
) {
    var showSignOutDialog by remember { mutableStateOf(false) }

    if (uiState.viewer?.uid != null) {
        SettingsHeader("PROFILE")
        SettingsGroup {
            SettingsTextRow(title = "Name", value = uiState.viewer?.alias ?: "")
            SettingsTextRow(title = "Email", value = uiState.viewer?.email ?: "")
            SettingsTextRow(title = "Member since", value = com.example.vista.util.DateUtils.formatDate(uiState.viewer?.createdAt ?: 0L))
            SettingsActionRow(title = "Sign Out", color = Accent) { showSignOutDialog = true }
        }
    } else {
        SettingsHeader("GUEST SESSION")
        SettingsGroup {
            SettingsTextRow(title = "Status", value = "Guest")
            SettingsTextRow(title = "Trial", value = "Free 3 days")
            SettingsActionRow(
                title = "Sign In or Create Account",
                color = Accent,
                onClick = {
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text("Sign Out?") },
            text = { Text("You will be signed out and returned to the login screen.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.signOut()
                    showSignOutDialog = false
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }) {
                    Text("Sign Out", color = Accent)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text("Cancel", color = PrimaryText)
                }
            },
            containerColor = RaisedSurface
        )
    }
}

// ── Playback ──

@Composable
private fun PlaybackSettingsPage(viewModel: SettingsViewModel, uiState: SettingsUiState) {
    SettingsHeader("RECOVERY")
    SettingsGroup {
        SettingsActionRow(title = "Repair playback", onClick = { viewModel.repairPlaybackCache() })
    }
    Text("Close the player first. Repair refreshes your app session and cleans inactive temporary files. It does not delete history or grant provider access.", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodySmall)
    SettingsHeader("EPISODES")
    SettingsGroup {
        SettingsSwitchRow(
            title = "Autoplay next episode",
            subtitle = "Start the next episode automatically",
            checked = uiState.autoplayNext,
            onCheckedChange = { viewModel.updateAutoplayNext(it) }
        )
        Divider(color = Color.White.copy(alpha = 0.06f), modifier = Modifier.padding(start = 16.dp))
        SettingsSwitchRow(
            title = "Auto-resume playback",
            subtitle = "Start where you left off",
            checked = uiState.autoResume,
            onCheckedChange = { viewModel.updateAutoResume(it) }
        )
    }

    SettingsHeader("QUALITY")
    SettingsGroup {
        SettingsSelectRow(
            title = "Preferred video quality",
            value = uiState.preferredQuality,
            options = listOf("Auto", "Best Available", "High (1080p)", "Standard (720p)", "Data Saver (480p)"),
            onSelect = { viewModel.updatePreferredQuality(it) }
        )
        Divider(color = Color.White.copy(alpha = 0.06f), modifier = Modifier.padding(start = 16.dp))
        SettingsSelectRow(
            title = "Default playback speed",
            value = "${uiState.playbackSpeed}x",
            options = listOf("0.5x", "0.75x", "1x", "1.25x", "1.5x", "2x"),
            onSelect = { viewModel.updatePlaybackSpeed(it.replace("x", "").toFloat()) }
        )
    }

    SettingsHeader("SEEK CONTROLS")
    SettingsGroup {
        SettingsChipRow(
            title = "Double-tap to skip",
            subtitle = "Seconds skipped when you double-tap the video",
            value = "${uiState.doubleTapToSeekSeconds}s",
            options = listOf("10s", "15s", "30s"),
            onSelect = { option -> viewModel.updateDoubleTapToSeek(option.replace("s", "").toInt()) }
        )
    }
}

// ── Audio & Subtitles ──

private val SubtitleColors = listOf(
    "White",
    "Yellow",
    "Cyan",
    "Green",
    "Pink",
    "Red"
)

@Composable
private fun AudioSubtitlesPage(viewModel: SettingsViewModel, uiState: SettingsUiState) {
    SettingsHeader("AUDIO")
    SettingsGroup {
        SettingsSelectRow(
            title = "Preferred audio language",
            value = uiState.audioLanguage,
            options = listOf("Original", "English", "French", "Spanish", "German", "Portuguese", "Japanese"),
            onSelect = { viewModel.updateAudioLanguage(it) }
        )
    }

    SettingsHeader("SUBTITLES")
    SettingsGroup {
        SettingsSwitchRow(
            title = "Show subtitles",
            subtitle = "Display subtitles while watching",
            checked = uiState.subtitleEnabled,
            onCheckedChange = { viewModel.updateSubtitleEnabled(it) }
        )
        Divider(color = Color.White.copy(alpha = 0.06f), modifier = Modifier.padding(start = 16.dp))
        SettingsSelectRow(
            title = "Subtitle language",
            value = uiState.subtitleLanguage,
            options = listOf("Off", "English", "French", "Spanish", "German", "Portuguese", "Japanese"),
            onSelect = { viewModel.updateSubtitleLanguage(it) }
        )
        Divider(color = Color.White.copy(alpha = 0.06f), modifier = Modifier.padding(start = 16.dp))
        SubtitleSizeRow(
            size = uiState.subtitleSize,
            onSizeChange = { viewModel.updateSubtitleSize(it) }
        )
        Divider(color = Color.White.copy(alpha = 0.06f), modifier = Modifier.padding(start = 16.dp))
        SubtitleColorRow(
            selected = uiState.subtitleColor,
            onSelect = { viewModel.updateSubtitleColor(it) }
        )
    }

    SubtitlePreview(uiState)
}

@Composable
private fun SubtitleSizeRow(size: Float, onSizeChange: (Float) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Subtitle size",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                color = PrimaryText
            )
            Text(
                "${size.toInt().coerceAtLeast(12)} pt",
                style = MaterialTheme.typography.bodyMedium,
                color = Accent,
                fontWeight = FontWeight.Bold
            )
        }
        Slider(
            value = size,
            onValueChange = onSizeChange,
            valueRange = 12f..28f,
            steps = 15,
            colors = SliderDefaults.colors(
                thumbColor = Accent,
                activeTrackColor = Accent,
                inactiveTrackColor = Color.White.copy(alpha = 0.15f)
            )
        )
    }
}

@Composable
private fun SubtitleColorRow(selected: String, onSelect: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .selectableGroup()
            .padding(16.dp)
    ) {
        Text("Subtitle color", style = MaterialTheme.typography.bodyLarge, color = PrimaryText)
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SubtitleColors.forEach { name ->
                val swatch = when (name.lowercase()) {
                    "yellow" -> Color(0xFFFFE082)
                    "cyan" -> Color(0xFF80DEEA)
                    "green" -> Color(0xFFA5D6A7)
                    "pink" -> Color(0xFFF48FB1)
                    "red" -> Color(0xFFE57373)
                    else -> Color(0xFFFAFAFA)
                }
                val isSelected = selected.equals(name, ignoreCase = true)
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .selectable(
                            selected = isSelected,
                            role = androidx.compose.ui.semantics.Role.RadioButton,
                            onClick = { onSelect(name) }
                        )
                        .background(swatch, CircleShape)
                        .then(
                            if (isSelected) Modifier.border(3.dp, Accent, CircleShape) else Modifier.border(3.dp, Color.Transparent, CircleShape)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(Icons.Outlined.Check, contentDescription = "Selected", tint = Color.Black, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SubtitlePreview(uiState: SettingsUiState) {
    val displayColor = when (uiState.subtitleColor.lowercase()) {
        "yellow" -> Color(0xFFFFE082)
        "cyan" -> Color(0xFF80DEEA)
        "green" -> Color(0xFFA5D6A7)
        "pink" -> Color(0xFFF48FB1)
        "red" -> Color(0xFFE57373)
        else -> Color(0xFFFAFAFA)
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        color = Color.Black,
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(Brush.radialGradient(listOf(Color(0xFF1A1A2E), Color.Black))),
            contentAlignment = Alignment.BottomCenter
        ) {
            Text(
                text = if (uiState.subtitleEnabled) "The best of cinema, beautifully presented." else "Subtitles are off",
                color = displayColor,
                fontSize = uiState.subtitleSize.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp)
            )
        }
    }
}

// ── Notifications ──

@Composable
private fun NotificationsPage(viewModel: SettingsViewModel, uiState: SettingsUiState) {
    SettingsHeader("MASTER CONTROL")
    SettingsGroup {
        SettingsSwitchRow(
            title = "Enable notifications",
            subtitle = "Master switch for all in-app alerts",
            checked = uiState.notificationsEnabled,
            onCheckedChange = { viewModel.updateNotificationsEnabled(it) }
        )
    }

    SettingsHeader("ANNOUNCEMENTS")
    SettingsGroup {
        SettingsSwitchRow(
            title = "Updates & releases",
            subtitle = "New Vista Cinema releases and special announcements",
            checked = uiState.notificationsEnabled && uiState.notificationReleases,
            enabled = uiState.notificationsEnabled,
            onCheckedChange = { viewModel.updateNotificationReleases(it) }
        )
    }

    // Separate from the announcement switches above: this one controls the milestone share prompt,
    // which is a different kind of message (an invitation, not news) and has its own channel, so
    // silencing announcements must not silently silence it either.
    SettingsHeader("SHARING")
    SettingsGroup {
        SettingsSwitchRow(
            title = "Share your wins",
            subtitle = "Occasionally invite a friend after a real achievement",
            checked = uiState.sharePromptsEnabled,
            enabled = uiState.notificationsEnabled,
            onCheckedChange = { viewModel.updateSharePromptsEnabled(it) }
        )
    }

    SettingsHeader("MOVIE OF THE DAY")
    SettingsGroup {
        SettingsSwitchRow(
            title = "Daily film suggestion",
            subtitle = "A free film picked for you each day, plus a reminder",
            checked = uiState.movieOfTheDayEnabled && uiState.notificationsEnabled,
            enabled = uiState.notificationsEnabled,
            onCheckedChange = { viewModel.updateMovieOfTheDay(it) }
        )
    }

    SettingsHeader("WHAT YOU WATCH")
    SettingsGroup {
        SettingsSwitchRow(
            title = "New episodes",
            subtitle = "New episodes of shows in your list",
            checked = uiState.notificationsEnabled && uiState.notificationNewEpisodes,
            enabled = uiState.notificationsEnabled,
            onCheckedChange = { viewModel.updateNotificationNewEpisodes(it) }
        )
        Divider(color = Color.White.copy(alpha = 0.06f), modifier = Modifier.padding(start = 16.dp))
        SettingsSwitchRow(
            title = "New seasons",
            subtitle = "When a new season of a show you follow arrives",
            checked = uiState.notificationsEnabled && uiState.notificationNewSeasons,
            enabled = uiState.notificationsEnabled,
            onCheckedChange = { viewModel.updateNotificationNewSeasons(it) }
        )
        Divider(color = Color.White.copy(alpha = 0.06f), modifier = Modifier.padding(start = 16.dp))
        SettingsSwitchRow(
            title = "TV guide reminders",
            subtitle = "Reminders for scheduled premieres",
            checked = uiState.notificationsEnabled && uiState.notificationTvGuideReminders,
            enabled = uiState.notificationsEnabled,
            onCheckedChange = { viewModel.updateNotificationTvGuideReminders(it) }
        )
    }

    Text(
        "Release announcements always reach you even when notifications are paused — they are how the app tells you about new versions.",
        style = MaterialTheme.typography.labelSmall,
        color = MutedText,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
    )
}

// ── Data & Storage ──

@Composable
private fun DataStoragePage(viewModel: SettingsViewModel, uiState: SettingsUiState, navController: NavController) {
    val messenger = rememberVistaMessenger()
    val scope = rememberCoroutineScope()

    SettingsHeader("DATA USAGE")
    SettingsGroup {
        SettingsSwitchRow(
            title = "Data Saver mode",
            subtitle = "Reduce quality to save data",
            checked = uiState.dataSaverMode,
            onCheckedChange = { viewModel.updateDataSaverMode(it) }
        )
        Divider(color = Color.White.copy(alpha = 0.06f), modifier = Modifier.padding(start = 16.dp))
        SettingsSelectRow(
            title = "Cellular streaming",
            value = uiState.cellularStreaming.displayName,
            options = CellularStreamingSetting.entries.map { it.displayName },
            onSelect = { setting ->
                CellularStreamingSetting.entries.find { it.displayName == setting }?.let {
                    viewModel.updateCellularStreaming(it)
                }
            }
        )
        }

    SettingsHeader("STORAGE & CACHE")
    SettingsGroup {
        SettingsTextRow(title = "App & watch history", value = uiState.storageUsed)
        SettingsTextRow(title = "Cache size", value = uiState.cacheSize)
        Divider(color = Color.White.copy(alpha = 0.06f), modifier = Modifier.padding(start = 16.dp))
        SettingsSelectRow(
            title = "Cache size limit",
            value = "${uiState.cacheLimitMb} MB",
            options = listOf("250 MB", "500 MB", "1000 MB", "2000 MB"),
            onSelect = { selected ->
                val limit = selected.replace(" MB", "").toIntOrNull() ?: 500
                viewModel.updateCacheLimitMb(limit)
                scope.launch { messenger?.show("Cache limit set to $selected", VistaMessageKind.SUCCESS) }
            }
        )
        Divider(color = Color.White.copy(alpha = 0.06f), modifier = Modifier.padding(start = 16.dp))
        SettingsActionRow(title = "Repair playback", onClick = {
            viewModel.repairPlaybackCache()
        })
        SettingsActionRow(title = "Clear image & app cache", onClick = {
            viewModel.clearCache()
            scope.launch { messenger?.show("All cache cleared", VistaMessageKind.SUCCESS) }
        })
        SettingsActionRow(title = "Watch history", onClick = {
            navController.navigate(Screen.WatchHistory.route)
        })
    }

    Text(
        "Cache holds temporary video player segments, posters and artwork. Clearing or repairing it removes stale data without affecting your active playback or saved watchlist.",
        style = MaterialTheme.typography.labelSmall,
        color = MutedText,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
    )
}

// ── Appearance ──

@Composable
private fun AppearancePage(viewModel: SettingsViewModel, uiState: SettingsUiState) {
    val themes = AppTheme.entries
    SettingsHeader("THEME")
    SettingsGroup {
        themes.forEachIndexed { index, theme ->
            SettingsRadioRow(
                title = theme.displayName,
                selected = uiState.appTheme == theme,
                onClick = { viewModel.updateAppTheme(theme) }
            )
            if (index != themes.lastIndex) {
                Divider(color = Color.White.copy(alpha = 0.06f), modifier = Modifier.padding(start = 16.dp))
            }
        }
    }
    Spacer(modifier = Modifier.height(24.dp))
}

// ── Privacy & History ──

@Composable
private fun PrivacyHistoryPage(viewModel: SettingsViewModel, uiState: SettingsUiState) {
    val messenger = rememberVistaMessenger()
    val scope = rememberCoroutineScope()
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var showClearSearchDialog by remember { mutableStateOf(false) }

    SettingsHeader("HISTORY")
    SettingsGroup {
        SettingsActionRow(title = "Clear watch history") { showClearHistoryDialog = true }
        Divider(color = Color.White.copy(alpha = 0.06f), modifier = Modifier.padding(start = 16.dp))
        SettingsActionRow(title = "Clear search history") { showClearSearchDialog = true }
    }

    SettingsHeader("DIAGNOSTICS")
    SettingsGroup {
        SettingsSwitchRow(
            title = "Share diagnostics & usage",
            subtitle = "Anonymous crash reports help us fix issues faster",
            checked = uiState.shareDiagnostics,
            onCheckedChange = { viewModel.updateShareDiagnostics(it) }
        )
    }

    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = { Text("Clear watch history?") },
            text = { Text("This will remove all playback history and Continue Watching progress.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearHistory()
                    showClearHistoryDialog = false
                    scope.launch { messenger?.show("Watch history cleared", VistaMessageKind.SUCCESS) }
                }) {
                    Text("Clear History", color = Accent)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text("Cancel", color = PrimaryText)
                }
            },
            containerColor = RaisedSurface
        )
    }

    if (showClearSearchDialog) {
        AlertDialog(
            onDismissRequest = { showClearSearchDialog = false },
            title = { Text("Clear search history?") },
            text = { Text("This will remove all your recent search queries.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearSearchHistory()
                    showClearSearchDialog = false
                    scope.launch { messenger?.show("Search history cleared", VistaMessageKind.SUCCESS) }
                }) {
                    Text("Clear Searches", color = Accent)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearSearchDialog = false }) {
                    Text("Cancel", color = PrimaryText)
                }
            },
            containerColor = RaisedSurface
        )
    }
}

// ── About ──

@Composable
private fun AboutPage(
    viewModel: SettingsViewModel,
    uiState: SettingsUiState,
    navController: NavController
) {
    val context = LocalContext.current
    val messenger = rememberVistaMessenger()
    val scope = rememberCoroutineScope()
    val appVersion = com.example.vista.BuildConfig.VERSION_NAME
    val buildCode = com.example.vista.BuildConfig.VERSION_CODE
    val updateState by InAppUpdateManager.updateState.collectAsStateWithLifecycle()
    var lastSeenChecking by remember { mutableStateOf(false) }

    LaunchedEffect(updateState) {
        val checkFinished = lastSeenChecking && updateState is UpdateState.Idle
        lastSeenChecking = updateState is UpdateState.Checking
        if (checkFinished) {
            scope.launch { messenger?.show("Vista Cinema is up to date", VistaMessageKind.SUCCESS) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(Brush.linearGradient(listOf(Accent, AccentDark)), RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.Tv,
                contentDescription = null,
                tint = PrimaryText,
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Vista Cinema",
            style = MaterialTheme.typography.headlineSmall,
            color = PrimaryText,
            fontWeight = FontWeight.Black
        )
        Spacer(modifier = Modifier.height(4.dp))
        Surface(
            color = Accent.copy(alpha = 0.15f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "v$appVersion (Build $buildCode)",
                color = Accent,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
    }

    SettingsHeader("UPDATES & BUILD")
    SettingsGroup {
        SettingsActionRow(
            title = if (updateState is UpdateState.Checking) "Checking for updates..." else "Check for updates"
        ) {
            scope.launch { messenger?.show("Checking for updates…") }
            InAppUpdateManager.checkForUpdates(context, force = true)
        }
        Divider(color = Color.White.copy(alpha = 0.06f), modifier = Modifier.padding(start = 16.dp))
        SettingsTextRow(title = "App version", value = "v$appVersion")
        SettingsTextRow(title = "Build code", value = buildCode.toString())
        SettingsTextRow(title = "Developer", value = "LX Obsidian Labs")
    }

    SettingsHeader("LINKS")
    SettingsGroup {
        SettingsNavRow(title = "Official Site", onClick = {
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.lxobsidianportal.co.za")))
            } catch (_: Exception) {
                scope.launch { messenger?.show("Unable to open link", VistaMessageKind.ERROR) }
            }
        })
        Divider(color = Color.White.copy(alpha = 0.06f), modifier = Modifier.padding(start = 56.dp))
        SettingsNavRow(title = "Privacy Policy", onClick = {
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.lxobsidianportal.co.za/privacy-policy.html")))
            } catch (_: Exception) {
                scope.launch { messenger?.show("Unable to open link", VistaMessageKind.ERROR) }
            }
        })
        Divider(color = Color.White.copy(alpha = 0.06f), modifier = Modifier.padding(start = 56.dp))
        SettingsNavRow(title = "Contact Support", onClick = {
            try {
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:")
                    putExtra(Intent.EXTRA_SUBJECT, "Vista Cinema Support")
                }
                context.startActivity(intent)
            } catch (_: Exception) {
                scope.launch { messenger?.show("No email app found", VistaMessageKind.ERROR) }
            }
        })
    }

    Spacer(modifier = Modifier.height(24.dp))
    SettingsHeader("DATA SOURCES")
    TmdbAttribution(modifier = Modifier.padding(16.dp))
    BrandAttribution()
}

// ── Visual Components ──

@Composable
private fun SettingsHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = Accent,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(start = 24.dp, top = 24.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        color = RaisedSurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.04f))
    ) {
        Column(content = content)
    }
}

@Composable
private fun SettingsNavRow(
    icon: ImageVector? = null,
    iconBackground: Color = Accent,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                onClick()
            }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(iconBackground.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconBackground, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = PrimaryText, fontWeight = FontWeight.SemiBold)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MutedText)
            }
        }
        Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = MutedText)
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) PrimaryText else MutedText
            )
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MutedText)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedTrackColor = Accent,
                disabledCheckedTrackColor = Accent.copy(alpha = 0.4f)
            )
        )
    }
}

@Composable
private fun SettingsSelectRow(
    title: String,
    value: String,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showDialog = true }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = PrimaryText)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = Accent, fontWeight = FontWeight.Bold)
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(title) },
            text = {
                Column {
                    options.forEach { option ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelect(option)
                                    showDialog = false
                                }
                                .padding(vertical = 12.dp)
                        ) {
                            RadioButton(selected = (option == value || value.startsWith(option)), onClick = null)
                            Spacer(Modifier.width(16.dp))
                            Text(option)
                        }
                    }
                }
            },
            confirmButton = {},
            containerColor = RaisedSurface
        )
    }
}

@Composable
private fun SettingsChipRow(
    title: String,
    subtitle: String? = null,
    value: String,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, color = PrimaryText)
        if (subtitle != null) {
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MutedText)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                val selected = value == option
                FilterChip(
                    selected = selected,
                    onClick = { onSelect(option) },
                    label = { Text(option, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = CardSurface,
                        selectedContainerColor = Accent,
                        labelColor = SecondaryText,
                        selectedLabelColor = PrimaryText
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }
    }
}

@Composable
private fun SettingsRadioRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = PrimaryText)
        RadioButton(selected = selected, onClick = null, colors = RadioButtonDefaults.colors(selectedColor = Accent))
    }
}

@Composable
private fun SettingsTextRow(title: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = PrimaryText)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MutedText)
    }
}

@Composable
private fun SettingsActionRow(title: String, color: Color = PrimaryText, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, color = color)
    }
}
