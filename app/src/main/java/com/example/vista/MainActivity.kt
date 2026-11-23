package com.example.vista

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.sp
import com.example.vista.ui.theme.SecondaryText
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.vista.data.local.PreferencesManager
import com.example.vista.data.analytics.VistaAnalytics
import com.example.vista.data.remote.firebase.FirebaseManager
import com.example.vista.domain.model.AppTheme
import com.example.vista.navigation.BottomNavItem
import com.example.vista.navigation.Screen
import com.example.vista.navigation.*
import com.example.vista.ai.AiChatScreen
import com.example.vista.ui.auth.AuthScreen
import com.example.vista.ui.auth.SetupProfileScreen
import com.example.vista.ui.details.DetailsScreen
import com.example.vista.ui.discover.DiscoverScreen
import com.example.vista.ui.history.WatchHistoryScreen
import com.example.vista.ui.library.LibraryScreen
import com.example.vista.ui.home.HomeScreen
import com.example.vista.ui.player.PlayerScreen
import com.example.vista.ui.player.TrailerPlayerScreen
import com.example.vista.ui.profile.ProfileScreen
import com.example.vista.ui.schedule.ScheduleScreen
import com.example.vista.ui.search.SearchScreen
import com.example.vista.ui.settings.SettingsScreen
import com.example.vista.ui.sharing.ShareMilestoneScreen
import com.example.vista.ui.stats.StatsScreen
import com.example.vista.ui.cast.CastDetailsScreen
import com.example.vista.ui.seeall.SeeAllScreen
import com.example.vista.ui.splash.SplashScreen
import com.example.vista.ui.theme.Accent
import com.example.vista.ui.theme.Background
import com.example.vista.ui.theme.CardSurface
import com.example.vista.ui.theme.MutedText
import com.example.vista.ui.theme.PrimaryText
import com.example.vista.ui.theme.VistaTheme
import com.example.vista.util.AdManager
import com.example.vista.ui.components.LocalVistaMessenger
import com.example.vista.ui.components.ProvideShimmerAnimation
import com.example.vista.ui.components.VistaMessenger
import com.example.vista.ui.components.VistaSnackbarHost
import com.example.vista.util.InAppUpdateManager
import com.example.vista.util.UpdateState
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** How often a running install re-checks the version manifest. */
private const val UPDATE_RECHECK_INTERVAL_MS = 2 * 60 * 60 * 1000L // 2 hours

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var preferencesManager: PreferencesManager

    @Inject
    lateinit var firebaseManager: FirebaseManager

    /** Hosted once in the root Scaffold; any screen can raise a message through it. */
    @Inject
    lateinit var vistaMessenger: VistaMessenger

    @Inject
    lateinit var vistaAnalytics: VistaAnalytics

    /**
     * Raised by a Movie of the Day tap, consumed by [VistaWatchApp].
     *
     * A flow rather than a direct navigation call because the intent can arrive in `onCreate`,
     * before the navigation graph exists. The Compose layer waits for the graph and clears this.
     */
    val openHomeRequest = MutableStateFlow(false)

    /**
     * Milestone id a share notification asked us to open, consumed by [VistaWatchApp].
     *
     * A flow rather than a direct navigation call for the same reason as [openHomeRequest]: the
     * intent can arrive in `onCreate` before the navigation graph exists.
     */
    val openShareRequest = MutableStateFlow<String?>(null)

    /** Cold starts, used to decide whether a milestone becomes a notification or an immediate open. */
    private var isColdStart = true

    @Inject
    lateinit var sharingMilestoneCoordinator: com.example.vista.sharing.SharingMilestoneCoordinator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // A notification tap ("check update" / announcement / daily pick) must trigger a forced
        // check even when the activity is recreated, not only when it is re-delivered via
        // onNewIntent.
        handleIncomingIntent(intent)

        // First-party telemetry, best effort. Country is added server-side from the request edge.
        lifecycleScope.launch {
            vistaAnalytics.track(
                "app_open",
                isGuest = preferencesManager.isGuest.first()
            )
        }

        // Counts process starts only. This is the maturity signal that keeps share prompts away
        // from new and casual viewers, so it must not tick on every resume.
        lifecycleScope.launch {
            preferencesManager.recordAppLaunch()
            evaluateShareMilestone()
        }

        // Check for updates on startup
        com.example.vista.util.InAppUpdateManager.checkForUpdates(this)

        setContent {
            val settings by preferencesManager.settings.collectAsStateWithLifecycle(
                initialValue = com.example.vista.domain.model.AppSettings()
            )
            val isGuest by preferencesManager.isGuest.collectAsStateWithLifecycle(
                initialValue = false
            )

            // Guests see interstitials more often, so the ad policy follows the viewer's mode.
            LaunchedEffect(isGuest) {
                AdManager.setGuestMode(isGuest)
            }

            VistaTheme(appTheme = settings.appTheme) {
                com.example.vista.util.ProvideDeviceConfiguration {
                    ProvideShimmerAnimation {
                        CompositionLocalProvider(LocalVistaMessenger provides vistaMessenger) {
                            VistaWatchApp(firebaseManager, this@MainActivity, vistaMessenger, isGuest)
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: android.content.Intent?) {
        if (intent == null) return

        // Ordered before the Home check below, and both extras are independent.
        intent.getStringExtra(EXTRA_OPEN_SHARE_MILESTONE)?.takeIf { it.isNotBlank() }?.let { id ->
            openShareRequest.value = id
            intent.removeExtra(EXTRA_OPEN_SHARE_MILESTONE)
        }

        if (intent.getBooleanExtra(EXTRA_OPEN_HOME, false)) {
            openHomeRequest.value = true
            // Consumed here so a configuration change or a later redelivery of the same Intent
            // does not bounce the viewer back to Home a second time.
            intent.removeExtra(EXTRA_OPEN_HOME)
        }

        val checkUpdate = intent.getBooleanExtra(
            com.example.vista.data.remote.firebase.VistaMessagingService.EXTRA_CHECK_UPDATE,
            false
        )
        if (checkUpdate) {
            // Bypass the 30s dedupe window: the viewer explicitly tapped an announcement.
            InAppUpdateManager.checkForUpdates(this, force = true)
        }
    }

    companion object {
        /** Set by [MovieOfTheDayWorker] so the daily pick lands on the Home card. */
        const val EXTRA_OPEN_HOME = "vista.open_home"

    /** Milestone id for [SharingMilestoneNotifier] to open the dedicated share screen. */
        const val EXTRA_OPEN_SHARE_MILESTONE = "vista.open_share_milestone"
    }

    /**
     * Offers a share milestone, if the viewer is eligible for one.
     *
     * Cold start means the win was reached while the app was closed, so it arrives as a
     * notification the viewer chooses to open. On a warm start they are already here, so the screen
     * opens directly - being notified about something they just did in person is noise.
     */
    private suspend fun evaluateShareMilestone() {
        // Captured before the reset below: the flag decides both how the coordinator delivers the
        // prompt and whether this caller should also open the screen.
        val wasColdStart = isColdStart
        val milestone = try {
            sharingMilestoneCoordinator.evaluate(notifyInsteadOfOpening = wasColdStart)
        } catch (_: Exception) {
            // Never let a share prompt be the reason the app fails to start.
            null
        }
        isColdStart = false

        // A cold start already delivered a notification, so opening the screen as well would be
        // the same win shown twice.
        if (milestone != null && !wasColdStart) {
            openShareRequest.value = milestone.id
        }
    }
}

/**
 * Whether a navigation destination (or one of its parents in the hierarchy) is a bottom tab.
 *
 * Matched on the route with any query arguments stripped, because several tabs are registered as
 * patterns rather than bare routes - Discover as `discover?tab={tab}` and Settings as
 * `settings?page={page}`. Without this, the tab bar would fail to highlight Settings while the user
 * sat on Settings' root, because the destination's route is the pattern and never equals the bare
 * "settings" a tab would be compared against.
 *
 * Stripping the query also means every Settings *sub-page* keeps the Settings tab highlighted,
 * which is the behaviour users expect from a tab.
 */
private fun androidx.navigation.NavDestination?.isBottomTab(tabRoute: String): Boolean {
    val tabBase = tabRoute.substringBefore('?')
    return this?.hierarchy?.any { it.route?.substringBefore('?') == tabBase } == true
}

@Composable
fun VistaWatchApp(
    firebaseManager: FirebaseManager,
    activity: Activity,
    vistaMessenger: VistaMessenger,
    isGuest: Boolean = false
) {
    val navController = rememberNavController()
    val splashScope = rememberCoroutineScope()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Onboarding / premium gating state
    val mainActivity = activity as? MainActivity
        ?: error("VistaWatchApp requires a MainActivity")
    val preferencesManager = mainActivity.preferencesManager
    val onboardingSeenAt by preferencesManager.onboardingSeenAt.collectAsStateWithLifecycle(
        initialValue = 0L
    )
    val premiumAcked by preferencesManager.premiumAcked.collectAsStateWithLifecycle(
        initialValue = false
    )

    // Ask for notification permission once (API 33+). Release announcements are the app's only
    // way to reach a running install, so the request is surfaced early rather than hidden.
    val notifPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { }
    LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            val asked = preferencesManager.hasAskedForNotificationPermission()
            if (!asked) {
                preferencesManager.markNotificationPermissionAsked()
                notifPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val showBottomBar = BottomNavItem.items.any { item ->
        currentDestination.isBottomTab(item.screen.route)
    }

    // A Movie of the Day tap should put the viewer in front of the card, not back where they
    // happened to be when the reminder arrived.
    val openHomeRequest by mainActivity.openHomeRequest.collectAsStateWithLifecycle()
    LaunchedEffect(openHomeRequest) {
        if (!openHomeRequest) return@LaunchedEffect
        mainActivity.openHomeRequest.value = false

        // A signed-out viewer has nothing to go back to, and the notification should not become a
        // way around the sign-in wall.
        if (!firebaseManager.isUserLoggedIn() && !isGuest) return@LaunchedEffect

        // On a cold start the graph is still on Splash, which navigates to Home by itself a moment
        // later. Moving the graph now would just be undone by that navigation.
        if (navController.currentBackStackEntry?.destination?.route == Screen.Splash.route) {
            return@LaunchedEffect
        }

        navController.navigate(Screen.Home.route) {
            popUpTo(navController.graph.findStartDestination().id)
            launchSingleTop = true
        }
    }

    // A milestone notification tap opens the dedicated share screen directly. Deliberately not routed
    // through Home first: the whole point of the notification is to save the viewer hunting for a
    // share button.
    val openShareRequest by mainActivity.openShareRequest.collectAsStateWithLifecycle()
    LaunchedEffect(openShareRequest) {
        val milestoneId = openShareRequest ?: return@LaunchedEffect
        mainActivity.openShareRequest.value = null

        if (!firebaseManager.isUserLoggedIn() && !isGuest) return@LaunchedEffect

        // On a cold start the graph is still on Splash and moves to Home on its own; navigating now
        // would just be undone by that.
        if (navController.currentBackStackEntry?.destination?.route == Screen.Splash.route) {
            return@LaunchedEffect
        }

        navController.navigate(Screen.ShareMilestone.createRoute(milestoneId)) {
            launchSingleTop = true
        }
    }

    // App Open Ad state
    var showAppOpenAd by remember { mutableStateOf(false) }
    var hasShownSplash by remember { mutableStateOf(false) }

    // Check for a new release as soon as the app starts, not only once the home screen loads.
    // Previously an install only learned about an update after reaching Home, so anyone sitting on
    // the sign-in screen (or with a home screen that failed to load) was never prompted.
    LaunchedEffect(Unit) {
        InAppUpdateManager.checkForUpdates(activity)
        while (true) {
            kotlinx.coroutines.delay(UPDATE_RECHECK_INTERVAL_MS)
            InAppUpdateManager.checkForUpdates(activity)
        }
    }

    // Re-check whenever the app returns to the foreground, so a long-lived install is prompted.
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                InAppUpdateManager.checkForUpdates(activity)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Update state
    val updateState by InAppUpdateManager.updateState.collectAsStateWithLifecycle()
    var showUpdateDialog by remember { mutableStateOf(false) }
    var showDownloadingDialog by remember { mutableStateOf(false) }
    var showInstallDialog by remember { mutableStateOf(false) }
    var failedMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(updateState) {
        when (updateState) {
            is UpdateState.UpdateAvailable -> showUpdateDialog = true
            is UpdateState.Downloading -> {
                showUpdateDialog = false
                showDownloadingDialog = true
            }
            is UpdateState.DownloadComplete -> {
                showDownloadingDialog = false
                showInstallDialog = true
            }
            is UpdateState.Failed -> {
                showDownloadingDialog = false
                if (showUpdateDialog || showInstallDialog) {
                    failedMessage = (updateState as UpdateState.Failed).message
                }
            }
            else -> {}
        }
    }

    // Premium announcement host. Shown exactly once per viewer (until acked), after onboarding and
    // never on top of a mandatory update prompt. The "announce it" is the whole point: a launch
    // date countdown is only useful if the viewer has actually landed on a screen.
    var showPremiumDialog by remember { mutableStateOf(false) }
    val premiumGateReady = !premiumAcked && onboardingSeenAt > 0
    LaunchedEffect(premiumGateReady, showUpdateDialog, showInstallDialog, hasShownSplash) {
        if (premiumGateReady && !showUpdateDialog && !showInstallDialog && hasShownSplash) {
            kotlinx.coroutines.delay(2500)
            if (premiumGateReady && !showUpdateDialog && !showInstallDialog) {
                showPremiumDialog = true
            }
        }
    }
    if (showPremiumDialog) {
        val premiumScope = rememberCoroutineScope()
        com.example.vista.ui.components.PremiumPromptDialog(
            onAcknowledge = {
                premiumScope.launch { preferencesManager.markPremiumAcked() }
                showPremiumDialog = false
            },
            onDismiss = { showPremiumDialog = false }
        )
    }

    // Show app open ad once after splash completes
    if (showAppOpenAd) {
        LaunchedEffect(Unit) {
            AdManager.showAppOpenAd(activity) {
                showAppOpenAd = false
            }
        }
    }

    // Update Available dialog
    if (showUpdateDialog) {
        val update = updateState as? UpdateState.UpdateAvailable
        AlertDialog(
            onDismissRequest = {
                if (update?.mandatory != true) {
                    showUpdateDialog = false
                    InAppUpdateManager.dismissUpdate(activity)
                }
            },
            containerColor = CardSurface,
            shape = RoundedCornerShape(24.dp),
            icon = {
                Surface(
                    color = Accent.copy(alpha = 0.1f),
                    shape = CircleShape,
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Outlined.SystemUpdate,
                            contentDescription = null,
                            tint = Accent,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            },
            title = {
                Text(
                    "New version is here!",
                    color = PrimaryText,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Vista Watch v${update?.latestVersion ?: ""} is ready for you.",
                        color = SecondaryText,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (!update?.changelog.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Surface(
                            color = Background.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                update?.changelog ?: "",
                                color = MutedText,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(12.dp),
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showUpdateDialog = false
                        update?.let { InAppUpdateManager.downloadUpdate(activity, it.downloadUrl) }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Accent),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text("Install Update Now", color = PrimaryText, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                if (update?.mandatory != true) {
                    TextButton(
                        onClick = {
                            showUpdateDialog = false
                            InAppUpdateManager.dismissUpdate(activity)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Maybe later", color = MutedText)
                    }
                }
            }
        )
    }

    // Downloading progress dialog
    if (showDownloadingDialog) {
        val progress = (updateState as? UpdateState.Downloading)?.progress ?: 0
        AlertDialog(
            onDismissRequest = {},
            containerColor = CardSurface,
            shape = RoundedCornerShape(20.dp),
            icon = {
                Icon(
                    Icons.Outlined.SystemUpdate,
                    contentDescription = null,
                    tint = Accent,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    "Downloading Update",
                    color = PrimaryText,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CircularProgressIndicator(
                        color = Accent,
                        progress = { if (progress > 0) progress / 100f else 0f }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        if (progress > 0) "$progress% downloaded… keep this screen open."
                        else "Your update is being downloaded. Keep this screen open.",
                        color = MutedText,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {},
            dismissButton = {}
        )
    }

    // Install ready dialog
    if (showInstallDialog) {
        val completedFile = (updateState as? UpdateState.DownloadComplete)?.file
        val canInstall = remember { InAppUpdateManager.hasInstallPermission(activity) }
        AlertDialog(
            onDismissRequest = { showInstallDialog = false },
            containerColor = CardSurface,
            shape = RoundedCornerShape(20.dp),
            icon = {
                Icon(
                    Icons.Outlined.SystemUpdate,
                    contentDescription = null,
                    tint = Accent,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    if (canInstall) "Update Ready" else "Permission Needed",
                    color = PrimaryText,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Text(
                    if (canInstall) {
                        "The new version of Vista Watch has been downloaded. Tap Install to update the app."
                    } else {
                        "To install updates, allow Vista Watch to install apps from this source."
                    },
                    color = MutedText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showInstallDialog = false
                        if (completedFile != null) {
                            if (canInstall) {
                                InAppUpdateManager.installUpdate(activity, completedFile)
                            } else {
                                InAppUpdateManager.openInstallSettings(activity)
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Accent),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (canInstall) "Install" else "Allow Installs",
                        color = PrimaryText,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showInstallDialog = false }) {
                    Text("Later", color = MutedText)
                }
            }
        )
    }

    // Update failed dialog
    if (failedMessage != null) {
        AlertDialog(
            onDismissRequest = { failedMessage = null },
            containerColor = CardSurface,
            shape = RoundedCornerShape(20.dp),
            icon = {
                Icon(
                    Icons.Outlined.SystemUpdate,
                    contentDescription = null,
                    tint = Accent,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    "Update Failed",
                    color = PrimaryText,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Text(
                    failedMessage ?: "Something went wrong while updating. Please try again.",
                    color = MutedText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = { failedMessage = null },
                    colors = ButtonDefaults.buttonColors(containerColor = Accent),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("OK", color = PrimaryText, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Background,
        snackbarHost = { VistaSnackbarHost(messenger = vistaMessenger) },
        bottomBar = {            if (showBottomBar) {
                Surface(
                    color = Color.Black.copy(alpha = 0.9f),
                    modifier = Modifier.fillMaxWidth(),
                    tonalElevation = 8.dp
                ) {
                    NavigationBar(
                        containerColor = Color.Transparent,
                        contentColor = MutedText,
                        modifier = Modifier.height(64.dp),
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        BottomNavItem.items.forEach { item ->
                            val selected = currentDestination.isBottomTab(item.screen.route)

                            NavigationBarItem(
                                icon = {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = item.icon,
                                            contentDescription = item.title,
                                            tint = if (selected) Accent else MutedText,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        if (selected) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Box(
                                                modifier = Modifier
                                                    .size(width = 14.dp, height = 3.dp)
                                                    .clip(CircleShape)
                                                    .background(Accent)
                                            )
                                        }
                                    }
                                },
                                selected = selected,
                                onClick = {
                                    if (item.screen == Screen.Discover &&
                                        currentDestination.isBottomTab(Screen.Discover.route)
                                    ) {
                                        navController.navigate(Screen.Discover.route) {
                                            popUpTo(Screen.Discover.ROUTE_WITH_TAB) { inclusive = true }
                                            launchSingleTop = true
                                            restoreState = false
                                        }
                                    } else {
                                        navController.navigate(item.rootRoute) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Accent,
                                    unselectedIconColor = MutedText,
                                    indicatorColor = Color.Transparent
                                )
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Splash.route) {
                SplashScreen(onSplashComplete = {
                    splashScope.launch {
                        // Expired guest trial → back to auth (the 3-day free trial does not renew).
                        val isGuestNow = isGuest
                        if (isGuestNow) {
                            val trialActive = runCatching { preferencesManager.startOrCheckGuestTrial() }.getOrDefault(true)
                            if (!trialActive) {
                                runCatching { preferencesManager.setGuestMode(false) }
                                navController.navigate(Screen.Auth.route) {
                                    popUpTo(Screen.Splash.route) { inclusive = true }
                                }
                                return@launch
                            }
                        }

                        // A returning guest skips the auth wall; guest mode is a persisted choice.
                        val seenAt = runCatching { preferencesManager.onboardingSeenAt.first() }.getOrDefault(0L)
                        if (firebaseManager.isUserLoggedIn() || isGuestNow) {
                            if (seenAt == 0L ||
                                com.example.vista.util.DateUtils.daysSince(seenAt) >=
                                com.example.vista.data.local.PreferencesManager.WELCOME_BACK_AFTER_DAYS
                            ) {
                                // First-time viewer → full tour. Been away → welcome-back screen.
                                navController.navigate(Screen.Onboarding.route) {
                                    popUpTo(Screen.Splash.route) { inclusive = true }
                                }
                            } else {
                                navController.navigate(Screen.Home.route) {
                                    popUpTo(Screen.Splash.route) { inclusive = true }
                                }
                            }
                        } else {
                            navController.navigate(Screen.Auth.route) {
                                popUpTo(Screen.Splash.route) { inclusive = true }
                            }
                        }
                        if (!hasShownSplash) {
                            hasShownSplash = true
                            showAppOpenAd = true
                        }
                    }
                })
            }

            composable(Screen.Auth.route) {
                AuthScreen(navController = navController)
            }
            composable(Screen.SetupProfile.route) {
                SetupProfileScreen(navController = navController)
            }
            composable(Screen.Onboarding.route) {
                com.example.vista.ui.onboarding.OnboardingScreen(navController = navController)
            }
            composable(Screen.OnboardingReplay.route) {
                com.example.vista.ui.onboarding.OnboardingScreen(navController = navController, forceTour = true)
            }

            composableWithFadeTransition(Screen.Home.route) {
                HomeScreen(navController = navController)
            }
            composable(
                Screen.AiAssistant.PATTERN,
                arguments = listOf(
                    navArgument("mediaId") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("mediaType") { type = NavType.StringType; nullable = true; defaultValue = null }
                )
            ) { AiChatScreen(navController = navController) }
            composableWithFadeTransition(
                route = Screen.Discover.ROUTE_WITH_TAB,
                arguments = listOf(navArgument("tab") { type = NavType.StringType; defaultValue = "" })
            ) {
                DiscoverScreen(navController = navController)
            }
            composableWithFadeTransition(Screen.Schedule.route) {
                ScheduleScreen(navController = navController)
            }
            composableWithFadeTransition(Screen.Library.route) {
                LibraryScreen(navController = navController)
            }
            composable(Screen.Downloads.PATTERN, arguments = listOf(
                navArgument("downloadMediaId") { type = NavType.StringType; defaultValue = "" },
                navArgument("downloadTitle") { type = NavType.StringType; defaultValue = "" },
                navArgument("downloadType") { type = NavType.StringType; defaultValue = "movie" },
                navArgument("downloadSeason") { type = NavType.StringType; defaultValue = "0" },
                navArgument("downloadEpisode") { type = NavType.StringType; defaultValue = "0" }
            )) { com.example.vista.downloads.DownloadsScreen(navController) }
            composable(Screen.OfflinePlayer.route, arguments = listOf(navArgument("downloadId") { type = NavType.StringType; defaultValue = "" })) {
                com.example.vista.downloads.OfflinePlayerScreen(navController)
            }
            composableWithFadeTransition(Screen.Profile.route) {
                ProfileScreen(navController = navController)
            }
            composableWithFadeTransition(Screen.Kids.route) {
                com.example.vista.ui.kids.KidsModeScreen(navController = navController)
            }

            composableWithSearchTransition(Screen.Search.PATTERN,
                arguments = listOf(navArgument("query") { type = NavType.StringType; defaultValue = "" })
            ) { entry ->
                SearchScreen(navController = navController, initialQuery = entry.arguments?.getString("query").orEmpty())
            }

            composableWithSlideTransition(
                route = Screen.MovieDetails.route,
                arguments = listOf(navArgument("movieId") { type = NavType.StringType; defaultValue = "" })
            ) {
                DetailsScreen(navController = navController)
            }
            composableWithSlideTransition(
                route = Screen.SeriesDetails.route,
                arguments = listOf(navArgument("seriesId") { type = NavType.StringType; defaultValue = "" })
            ) {
                DetailsScreen(navController = navController)
            }

            composableWithFadeOnlyTransition(
                route = Screen.Player.route,
                arguments = listOf(
                    navArgument("mediaId") { type = NavType.StringType; defaultValue = "" },
                    navArgument("title") { type = NavType.StringType; defaultValue = "" },
                    navArgument("type") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument("season") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument("episode") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument("poster") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) {
                PlayerScreen(navController = navController)
            }

            composableWithFadeOnlyTransition(
                route = Screen.Trailer.route,
                arguments = listOf(
                    navArgument("videoKey") { type = NavType.StringType; defaultValue = "" },
                    navArgument("title") { type = NavType.StringType; defaultValue = "" }
                )
            ) { backStackEntry ->
                val videoKey = backStackEntry.arguments?.getString("videoKey") ?: ""
                val title = backStackEntry.arguments?.getString("title") ?: ""
                TrailerPlayerScreen(
                    navController = navController,
                    videoKey = videoKey,
                    title = title
                )
            }

            composableWithSlideTransition(
                route = Screen.CastDetails.route,
                arguments = listOf(navArgument("personId") { type = NavType.StringType })
            ) {
                CastDetailsScreen(navController = navController)
            }

            composableWithSlideUpTransition(
                route = Screen.Settings.ROUTE_WITH_PAGE,
                arguments = listOf(
                    navArgument("page") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val page = backStackEntry.arguments?.getString("page")
                SettingsScreen(navController = navController, page = page)
            }

            composableWithSlideTransition(Screen.Support.route) {
                com.example.vista.support.SupportScreen(onBack = { navController.popBackStack() })
            }

            composableWithSlideTransition(Screen.WatchHistory.route) {
                WatchHistoryScreen(navController = navController)
            }

            composableWithSlideTransition(Screen.Stats.route) {
                StatsScreen(navController = navController)
            }

            composableWithSlideTransition(
                route = Screen.ShareMilestone.route,
                arguments = listOf(navArgument("milestoneId") { type = NavType.StringType })
            ) {
                ShareMilestoneScreen(
                    milestoneId = it.arguments?.getString("milestoneId").orEmpty(),
                    navController = navController
                )
            }

            composableWithSlideTransition(
                route = Screen.SeeAll.route,
                arguments = listOf(
                    navArgument("sectionId") { type = NavType.StringType },
                    navArgument("title") { type = NavType.StringType }
                )
            ) {
                SeeAllScreen(navController = navController)
            }
        }
    }
}
