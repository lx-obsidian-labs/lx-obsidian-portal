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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.vista.domain.model.AgeBand
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
    "DATA_STORAGE", "DATA_USAGE" -> SettingsPage.DATA_STORAGE
    "DOWNLOADS", "DOWNLOAD_SETTINGS" -> SettingsPage.DOWNLOADS
    "AI_VOICE", "AI", "VOICE" -> SettingsPage.AI_VOICE
    "FAMILY", "KIDS", "AGE", "AGE_RESTRICTION" -> SettingsPage.FAMILY
    "APPEARANCE" -> SettingsPage.APPEARANCE
    "PRIVACY_HISTORY", "PRIVACY" -> SettingsPage.PRIVACY_HISTORY
    "ABOUT" -> SettingsPage.ABOUT
    "SUPPORT", "HELP", "HELP_SUPPORT" -> SettingsPage.SUPPORT
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
        SettingsPage.FAMILY -> "Family & Age"
        SettingsPage.APPEARANCE -> "Appearance"
        SettingsPage.PRIVACY_HISTORY -> "Privacy & History"
        SettingsPage.ABOUT -> "About Vista Watch"
        SettingsPage.DOWNLOADS -> "Downloads"
        SettingsPage.AI_VOICE -> "AI & Voice"
        SettingsPage.SUPPORT -> "Help & Support"
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
                    SettingsPage.MAIN -> MainSettingsPage(viewModel, uiState, navController)
                    SettingsPage.DOWNLOADS -> DownloadSettingsPage(viewModel, uiState, navController)
                    SettingsPage.AI_VOICE -> AdvancedAiPage(viewModel, uiState)
                    SettingsPage.ACCOUNT -> AccountSettingsPage(viewModel, uiState, navController)
                    SettingsPage.PLAYBACK -> PlaybackSettingsPage(viewModel, uiState)
                    SettingsPage.AUDIO_SUBTITLES -> AudioSubtitlesPage(viewModel, uiState)
                    SettingsPage.NOTIFICATIONS -> NotificationsPage(viewModel, uiState)
                    SettingsPage.DATA_STORAGE -> DataStoragePage(viewModel, uiState, navController)
                    SettingsPage.FAMILY -> FamilySettingsPage(viewModel, uiState, navController)
                    SettingsPage.APPEARANCE -> AppearancePage(viewModel, uiState)
                    SettingsPage.PRIVACY_HISTORY -> PrivacyHistoryPage(viewModel, uiState)
                    SettingsPage.ABOUT -> AboutPage(viewModel, uiState, navController)
                    SettingsPage.SUPPORT -> com.example.vista.support.SupportContent()
                }
            }
        }
    }
}

// ── Main ──

@Composable
private fun MainSettingsPage(viewModel: SettingsViewModel, uiState: SettingsUiState, navController: NavController) {
    val context = LocalContext.current
    val messenger = rememberVistaMessenger()
    val scope = rememberCoroutineScope()
    var query by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("") }
    OutlinedTextField(value = query, onValueChange = { query = it.take(100) }, singleLine = true,
        label = { Text("Search settings") }, placeholder = { Text("Downloads, AI, subtitles, privacy…") },
        leadingIcon = { Icon(Icons.Outlined.Search, null) },
        trailingIcon = { if (query.isNotBlank()) IconButton(onClick = { query = "" }) { Icon(Icons.Outlined.Close, "Clear settings search") } },
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), shape = RoundedCornerShape(16.dp))
    if (query.isNotBlank()) {
        val found = SettingsDirectory.search(query)
        SettingsHeader("${found.size} MATCHING ${if (found.size == 1) "SECTION" else "SECTIONS"}")
        SettingsGroup {
            found.forEach { item -> SettingsNavRow(Icons.Outlined.Settings, Accent, item.title, item.description) {
                query = ""; viewModel.navigateTo(item.page)
            } }
        }
        if (found.isEmpty()) Text("No settings match. Try ‘offline’, ‘voice’ or ‘quality’.", color = SecondaryText, modifier = Modifier.padding(24.dp))
        return
    }
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
                com.example.vista.artwork.CurrentUserAvatar(size = 56.dp)
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

    com.example.vista.feedback.FeedbackPollCard(alwaysShow = true, onSignIn = { navController.navigate(Screen.Auth.route) })

    SettingsHeader("QUICK OVERVIEW")
    SettingsGroup {
        SettingsTextRow("Streaming quality", if (uiState.dataSaverMode) "Data Saver · 480p preference" else uiState.preferredQuality)
        SettingsTextRow("Mobile data", uiState.cellularStreaming.displayName)
        SettingsTextRow("Free device storage", uiState.freeStorage)
    }
    SettingsHeader("AI & VOICE")
    SettingsGroup {
        SettingsNavRow(Icons.Outlined.AutoAwesome, Accent, "Assistant control centre", "Taste, tool actions, voice and personalization") {
            viewModel.navigateTo(SettingsPage.AI_VOICE)
        }
        SettingsSwitchRow(
            title = "Vista AI",
            subtitle = "Cloud-powered movie conversations and discovery",
            checked = uiState.aiEnabled,
            onCheckedChange = viewModel::updateAiEnabled
        )
        SettingsSwitchRow(
            title = "AI chat",
            subtitle = "Send questions and recent conversation to our AI service. Sign-in required.",
            checked = uiState.aiChatEnabled,
            onCheckedChange = viewModel::updateAiChatEnabled
        )
        SettingsNavRow(
            icon = Icons.Outlined.RecordVoiceOver,
            iconBackground = Accent,
            title = "Voice quality & language",
            subtitle = "Choose or download a natural voice in Android. Tap the chat mic for hands-free turns.",
            onClick = {
                try { context.startActivity(Intent("com.android.settings.TTS_SETTINGS")) }
                catch (_: Exception) { scope.launch { messenger?.show("Open Android Settings → Text-to-speech to manage voices.", VistaMessageKind.INFO) } }
            }
        )
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
        SettingsNavRow(Icons.Outlined.FamilyRestroom, Accent, "Family & Age", "${uiState.ageBand.displayName} · ${if (uiState.kidsMode) "Kids mode on" else "Kids mode off"}") {
            viewModel.navigateTo(SettingsPage.FAMILY)
        }
        Divider(color = Color.White.copy(alpha = 0.06f), modifier = Modifier.padding(start = 56.dp))
        SettingsNavRow(Icons.Outlined.DownloadForOffline, Accent, "Downloads", "${uiState.readyDownloads} ready · ${uiState.downloadedSize} on this profile") {
            viewModel.navigateTo(SettingsPage.DOWNLOADS)
        }
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
        SettingsNavRow(Icons.Outlined.HelpOutline, Accent, "Help & Support", "Report a problem, account help and official contacts") {
            viewModel.navigateTo(SettingsPage.SUPPORT)
        }
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
            title = "About Vista Watch",
            subtitle = "Version ${com.example.vista.BuildConfig.VERSION_NAME}",
            onClick = { viewModel.navigateTo(SettingsPage.ABOUT) }
        )
    }
}

// ── Family & Age ──

@Composable
private fun FamilySettingsPage(viewModel: SettingsViewModel, uiState: SettingsUiState, navController: NavController) {
    var pin by remember { mutableStateOf("") }
    SettingsHeader("AGE RATING")
    SettingsGroup {
        SettingsSelectRow("Profile age band", uiState.ageBand.displayName, AgeBand.values().map { it.displayName }) { selected ->
            viewModel.requestAgeBand(AgeBand.values().first { it.displayName == selected })
        }
        SettingsTextRow("Applies to", uiState.ageBand.description)
        SettingsSwitchRow("Mature titles", "Allow 18+ and adult catalog entries", uiState.showAdultContent,
            enabled = uiState.ageBand == AgeBand.ADULT && !uiState.kidsMode,
            onCheckedChange = viewModel::requestShowAdultContent)
    }
    SettingsHeader("KIDS MODE")
    SettingsGroup {
        SettingsSwitchRow("Kids mode", "Simple home with family-friendly titles only", uiState.kidsMode,
            onCheckedChange = viewModel::requestKidsMode)
        SettingsActionRow("Open Kids mode") { navController.navigate(com.example.vista.navigation.Screen.Kids.route) }
        SettingsTextRow("Switching back", if (uiState.hasParentalPin) "Requires your parental PIN" else "No PIN set — anyone can switch back")
    }
    SettingsHeader("PARENTAL PIN")
    SettingsGroup {
        OutlinedTextField(
            value = pin,
            onValueChange = { pin = it.filter(Char::isDigit).take(6) },
            label = { Text("4–6 digit PIN") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(14.dp)
        )
        SettingsActionRow(if (uiState.hasParentalPin) "Change PIN" else "Set PIN") {
            if (pin.length in 4..6) { viewModel.requestParentalPin(pin); pin = "" }
        }
        if (uiState.hasParentalPin) SettingsActionRow("Remove PIN") { viewModel.requestParentalPin("") }
    }
    Text("Age limits filter catalog metadata, AI suggestions and Kids mode. Playback providers are third-party and can change; supervise younger viewers.",
        color = SecondaryText, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(24.dp))

    val pinTitle = when (uiState.familyPinAction) {
        FamilyPinAction.DISABLE_KIDS -> "Leave Kids mode?"
        FamilyPinAction.LOOSEN_AGE -> "Change age band?"
        FamilyPinAction.ENABLE_ADULT_CONTENT -> "Allow mature titles?"
        FamilyPinAction.CHANGE_PIN -> "Change parental PIN?"
        null -> null
    }
    if (pinTitle != null) {
        AlertDialog(
            onDismissRequest = viewModel::cancelFamilyPin,
            title = { Text(pinTitle) },
            text = {
                Column {
                    Text("Enter your parental PIN to confirm this change.", color = SecondaryText)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = uiState.familyPinInput,
                        onValueChange = viewModel::updateFamilyPinInput,
                        label = { Text("Parental PIN") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        isError = uiState.familyPinError != null,
                        shape = RoundedCornerShape(14.dp)
                    )
                    uiState.familyPinError?.let { Text(it, color = ErrorAccent, style = MaterialTheme.typography.bodySmall) }
                }
            },
            confirmButton = { TextButton(onClick = viewModel::confirmFamilyPin) { Text("Confirm", color = Accent) } },
            dismissButton = { TextButton(onClick = viewModel::cancelFamilyPin) { Text("Cancel") } },
            containerColor = RaisedSurface
        )
    }
}

// ── Account ──

@Composable
private fun DownloadSettingsPage(viewModel: SettingsViewModel, uiState: SettingsUiState, navController: NavController) {
    LaunchedEffect(Unit) { viewModel.refreshSizes() }
    SettingsHeader("OFFLINE LIBRARY")
    SettingsGroup {
        SettingsTextRow("Ready on this profile", uiState.readyDownloads.toString())
        SettingsTextRow("Downloaded media", uiState.downloadedSize)
        SettingsTextRow("Free device storage", uiState.freeStorage)
        SettingsNavRow(Icons.Outlined.DownloadForOffline, Accent, "Manage downloads", "Progress, pause, resume, retry, delete and play offline") {
            navController.navigate(Screen.Downloads.route)
        }
    }
    SettingsHeader("DOWNLOAD DEFAULTS")
    SettingsGroup {
        val labels = mapOf(480 to "Saver · prefer up to 480p", 720 to "Balanced · prefer up to 720p", Int.MAX_VALUE to "Best compatible quality")
        SettingsSelectRow("Default quality", labels[uiState.downloadMaxHeight] ?: labels.getValue(720), labels.values.toList()) { selected ->
            viewModel.updateDownloadQuality(labels.entries.first { it.value == selected }.key)
        }
        SettingsTextRow("Connection policy", "Unmetered only")
        SettingsTextRow("Roaming", "Paused")
        SettingsTextRow("Simultaneous downloads", "1")
    }
    Text("Quality is selected from the source's actual compatible tracks. Sizes can be unknown for adaptive streams. Downloads and their index are separate from temporary cache; use Manage downloads to remove them.",
        color = SecondaryText, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(24.dp))
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun AdvancedAiPage(viewModel: SettingsViewModel, uiState: SettingsUiState) {
    val context = LocalContext.current
    val messenger = rememberVistaMessenger()
    val scope = rememberCoroutineScope()
    var reset by remember { mutableStateOf(false) }
    SettingsHeader("ASSISTANT")
    SettingsGroup {
        SettingsSwitchRow("Vista AI", "Enable assistant and AI discovery features", uiState.aiEnabled, onCheckedChange = viewModel::updateAiEnabled)
        SettingsSwitchRow("Conversation & actions", "Search, compare, library, resume and download-review actions", uiState.aiChatEnabled,
            enabled = uiState.aiEnabled, onCheckedChange = viewModel::updateAiChatEnabled)
        SettingsTextRow("Cloud setup", if (com.example.vista.BuildConfig.VISTA_AI_BASE_URL.isBlank()) "Local catalog mode" else "Backend configured · sign-in required")
    }
    SettingsHeader("YOUR TASTE")
    SettingsGroup {
        SettingsSwitchRow("Personalized suggestions", "Use explicit genres and meaningful viewing on this profile", uiState.taste.enabled,
            onCheckedChange = viewModel::enablePersonalization)
        FlowRow(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Action", "Comedy", "Drama", "Science Fiction", "Romance", "Horror", "Animation").forEach { genre ->
                FilterChip(selected = genre in uiState.taste.genres, onClick = { viewModel.toggleTasteGenre(genre) },
                    label = { Text(genre) }, enabled = uiState.taste.enabled)
            }
        }
        SettingsTextRow("Titles marked Not for me", uiState.taste.disliked.size.toString())
        SettingsActionRow("Reset taste & feedback") { reset = true }
    }
    SettingsHeader("VOICE & DEVICE")
    SettingsGroup {
        SettingsActionRow("Voice engine, quality & language") {
            try { context.startActivity(Intent("com.android.settings.TTS_SETTINGS")) }
            catch (_: Exception) { scope.launch { messenger?.show("Open Android Settings → Text-to-speech to manage installed voices.") } }
        }
        SettingsActionRow("Microphone & app permissions") {
            context.startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
        }
    }
    Text("Actions use catalog-backed titles, not arbitrary commands. Save/remove actions need confirmation; downloads open source and quality review. Spoiler and read-aloud controls are available in chat. Device voices vary by installed engine.",
        color = SecondaryText, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(24.dp))
    if (reset) AlertDialog(onDismissRequest = { reset = false }, title = { Text("Reset your taste?") },
        text = { Text("Clear selected genres, recommendation feedback and inferred-taste history cutoff. Your watch list, downloads and viewing history are kept.") },
        confirmButton = { TextButton(onClick = { viewModel.resetPersonalization(); reset = false }) { Text("Reset taste") } },
        dismissButton = { TextButton(onClick = { reset = false }) { Text("Cancel") } })
}

@Composable
private fun AccountSettingsPage(
    viewModel: SettingsViewModel,
    uiState: SettingsUiState,
    navController: NavController
) {
    var showSignOutDialog by remember { mutableStateOf(false) }
    var showAvatarPicker by remember { mutableStateOf(false) }

    Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        com.example.vista.artwork.CurrentUserAvatar(size = 60.dp)
        OutlinedButton(onClick = { showAvatarPicker = true }) { Text("Edit Avatar", color = PrimaryText) }
    }
    if (showAvatarPicker) com.example.vista.artwork.VistaAvatarPicker(onDismiss = { showAvatarPicker = false })

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
            options = listOf("Auto", "1080p", "720p", "480p", "360p"),
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
    Text("These preferences apply to compatible native streams. Embedded providers control their own audio and subtitles; a missing language cannot be created by changing a preference.",
        color = SecondaryText, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(16.dp))
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
    val context = LocalContext.current
    SettingsHeader("ANDROID DELIVERY")
    SettingsGroup {
        SettingsTextRow("App permission", if (androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()) "Allowed in Android" else "Blocked in Android")
        SettingsActionRow("Manage Android notification channels") {
            context.startActivity(Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName))
        }
    }
    SettingsHeader("MASTER CONTROL")
    SettingsGroup {
        SettingsSwitchRow(
            title = "Recommendations & announcements",
            subtitle = "Allow nonessential push messages and reminder delivery",
            checked = uiState.notificationsEnabled,
            onCheckedChange = { viewModel.updateNotificationsEnabled(it) }
        )
    }

    SettingsHeader("ANNOUNCEMENTS")
    SettingsGroup {
        SettingsSwitchRow(
            title = "Feature news & festive greetings",
            subtitle = "Optional announcements about Vista and seasonal events",
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
            onCheckedChange = { viewModel.updateSharePromptsEnabled(it) }
        )
    }

    SettingsHeader("MOVIE OF THE DAY")
    SettingsGroup {
        SettingsSwitchRow(
            title = "Daily film suggestion",
            subtitle = "Home suggestion; reminders follow your notification preferences",
            checked = uiState.movieOfTheDayEnabled,
            onCheckedChange = { viewModel.updateMovieOfTheDay(it) }
        )
    }

    SettingsHeader("WHAT YOU WATCH")
    SettingsGroup {
        SettingsSwitchRow(
            title = "New episodes",
            subtitle = "Automatic scheduling is not available yet",
            checked = uiState.notificationsEnabled && uiState.notificationNewEpisodes,
            enabled = false,
            onCheckedChange = { viewModel.updateNotificationNewEpisodes(it) }
        )
        Divider(color = Color.White.copy(alpha = 0.06f), modifier = Modifier.padding(start = 16.dp))
        SettingsSwitchRow(
            title = "New seasons",
            subtitle = "Automatic scheduling is not available yet",
            checked = uiState.notificationsEnabled && uiState.notificationNewSeasons,
            enabled = false,
            onCheckedChange = { viewModel.updateNotificationNewSeasons(it) }
        )
        Divider(color = Color.White.copy(alpha = 0.06f), modifier = Modifier.padding(start = 16.dp))
        SettingsSwitchRow(
            title = "TV guide reminders",
            subtitle = "Automatic scheduling is not available yet",
            checked = uiState.notificationsEnabled && uiState.notificationTvGuideReminders,
            enabled = false,
            onCheckedChange = { viewModel.updateNotificationTvGuideReminders(it) }
        )
    }

    Text(
        "Essential release/security messages use a separate Android channel. Android permission and channel settings still control delivery. Automatic episode and premiere reminder scheduling is not available yet.",
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
        SettingsTextRow(title = "Total app storage", value = uiState.storageUsed)
        SettingsTextRow(title = "Cache size", value = uiState.cacheSize)
        SettingsTextRow(title = "Downloads on this profile", value = uiState.downloadedSize)
        SettingsTextRow(title = "Free device storage", value = uiState.freeStorage)
        SettingsActionRow("Refresh storage figures") { viewModel.refreshSizes() }
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
    SettingsHeader("SEASONAL DISCOVERY")
    SettingsGroup {
        val date = java.time.LocalDate.now()
        val year = com.example.vista.motd.FestiveSeason.moment(date)?.seasonYear ?: date.year
        SettingsSwitchRow("Festive collections", "Curated Home picks and Christmas countdown, November–early January",
            uiState.hiddenFestiveSeason != year, onCheckedChange = viewModel::setFestiveEnabled)
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

    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        com.example.vista.support.PrivacyLegalContent()
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
    SettingsGroup {
        SettingsNavRow(Icons.Outlined.Slideshow, Accent, "Replay introduction", "Review what Vista can do and how offline features work") {
            navController.navigate(Screen.OnboardingReplay.route)
        }
    }
    val buildCode = com.example.vista.BuildConfig.VERSION_CODE
    val updateState by InAppUpdateManager.updateState.collectAsStateWithLifecycle()
    var lastSeenChecking by remember { mutableStateOf(false) }

    LaunchedEffect(updateState) {
        val checkFinished = lastSeenChecking && updateState is UpdateState.Idle
        lastSeenChecking = updateState is UpdateState.Checking
        if (checkFinished) {
            scope.launch { messenger?.show("Vista Watch is up to date", VistaMessageKind.SUCCESS) }
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
            text = "Vista Watch",
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

    Spacer(modifier = Modifier.height(24.dp))
    com.example.vista.support.AboutContactContent(onOpenSupport = { viewModel.navigateTo(SettingsPage.SUPPORT) })
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
