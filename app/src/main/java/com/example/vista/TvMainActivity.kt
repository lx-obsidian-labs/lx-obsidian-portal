package com.example.vista

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Text
import com.example.vista.data.local.PreferencesManager
import com.example.vista.domain.model.MediaItem
import com.example.vista.navigation.Screen
import com.example.vista.ui.components.ProvideShimmerAnimation
import com.example.vista.ui.player.PlayerScreen
import com.example.vista.ui.theme.Accent
import com.example.vista.ui.theme.CardSurface
import com.example.vista.ui.theme.VistaTheme
import com.example.vista.ui.tv.TvDetailsScreen
import com.example.vista.ui.tv.TvHomeScreen
import com.example.vista.ui.tv.TvSearchScreen
import com.example.vista.util.InAppUpdateManager
import com.example.vista.util.UpdateState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class TvMainActivity : ComponentActivity() {

    @Inject
    lateinit var preferencesManager: PreferencesManager

    @OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        InAppUpdateManager.checkForUpdates(this)

        setContent {
            val settings by preferencesManager.settings.collectAsStateWithLifecycle(
                initialValue = com.example.vista.domain.model.AppSettings()
            )

            VistaTheme(appTheme = settings.appTheme) {
                com.example.vista.util.ProvideDeviceConfiguration(deviceTypeOverride = com.example.vista.util.DeviceType.TV) {
                    ProvideShimmerAnimation {
                        TvUpdateHost(preferencesManager = preferencesManager)
                    }
                }
            }
        }
    }
}

@Composable
fun TvAppEntryPoint() {
    val navController = rememberNavController()

    BackHandler(enabled = navController.currentBackStackEntryAsState().value?.destination?.route != "home") {
        navController.popBackStack()
    }

    com.example.vista.ui.tv.TvFocusScrolling {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            TvHomeScreen(
                onMediaClick = { item ->
                    navigateToDetails(navController, item)
                },
                onSearchClick = {
                    navController.navigate(Screen.TvSearch.route)
                },
                onSupportClick = { navController.navigate(Screen.Support.route) }
            )
        }

        composable(Screen.Support.route) {
            com.example.vista.support.SupportScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.TvSearch.route) {
            TvSearchScreen(
                onBackClick = { navController.popBackStack() },
                onMediaClick = { item ->
                    navigateToDetails(navController, item)
                }
            )
        }

        composable(
            route = Screen.MovieDetails.route,
            arguments = listOf(navArgument("movieId") { type = NavType.StringType })
        ) { entry ->
            TvDetailsScreen(
                onBackClick = { navController.popBackStack() },
                onWatchClick = { id, title, mediaType, season, episode, poster ->
                    navController.navigate(
                        Screen.Player.createRoute(
                            mediaId = id,
                            title = title,
                            mediaType = mediaType,
                            season = season,
                            episode = episode,
                            poster = poster
                        )
                    ) {
                        popUpTo(Screen.Home.route)
                    }
                }
            )
        }

        composable(
            route = Screen.SeriesDetails.route,
            arguments = listOf(navArgument("seriesId") { type = NavType.StringType })
        ) { entry ->
            TvDetailsScreen(
                onBackClick = { navController.popBackStack() },
                onWatchClick = { id, title, mediaType, season, episode, poster ->
                    navController.navigate(
                        Screen.Player.createRoute(
                            mediaId = id,
                            title = title,
                            mediaType = mediaType,
                            season = season,
                            episode = episode,
                            poster = poster
                        )
                    ) {
                        popUpTo(Screen.Home.route)
                    }
                }
            )
        }

        composable(
            route = Screen.Player.route,
            arguments = listOf(
                navArgument("mediaId") { type = NavType.StringType },
                navArgument("title") { type = NavType.StringType },
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
    }
    }
}

private fun navigateToDetails(
    navController: androidx.navigation.NavHostController,
    item: MediaItem
) {
    // Route by the item's real type. Every item used to be sent to the movie route, so series
    // resolved against TMDB's movie namespace and opened an unrelated film.
    val route = when (item) {
        is MediaItem.SeriesItem -> Screen.SeriesDetails.createRoute(item.id)
        else -> Screen.MovieDetails.createRoute(item.id)
    }
    navController.navigate(route)
}

/** Minimal, TV-friendly update dialog driven by InAppUpdateManager state. */
@OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)
@Composable
private fun TvUpdateHost(preferencesManager: PreferencesManager) {
    val updateState by InAppUpdateManager.updateState.collectAsStateWithLifecycle()
    var dismissed by remember { mutableStateOf(false) }

    val available = updateState as? UpdateState.UpdateAvailable
    val downloading = updateState as? UpdateState.Downloading
    val complete = updateState as? UpdateState.DownloadComplete
    val failed = updateState as? UpdateState.Failed

    val context = androidx.compose.ui.platform.LocalContext.current

    // Update available
    if (available != null && !dismissed) {
        // A mandatory release must not be dismissible — the previous handler let the BACK key
        // close it, which left the install stranded on an old version indefinitely.
        Dialog(onDismissRequest = { if (available.mandatory != true) dismissed = true }) {
            TvUpdateDialogSurface {
                Text(
                    "Update Available",
                    style = MaterialTheme.typography.headlineMedium,
                    color = com.example.vista.ui.theme.PrimaryText
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "A new version of Vista Watch (v${available.latestVersion}) is available.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = com.example.vista.ui.theme.MutedText
                )
                if (available.changelog.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        available.changelog,
                        style = MaterialTheme.typography.bodyMedium,
                        color = com.example.vista.ui.theme.PrimaryText
                    )
                }
                Spacer(Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(
                        onClick = {
                            if (available.mandatory) dismissed = true
                            InAppUpdateManager.downloadUpdate(context, available.downloadUrl)
                        }
                    ) {
                        Text("Update")
                    }
                    if (!available.mandatory) {
                        OutlinedButton(onClick = { dismissed = true }) {
                            Text("Later")
                        }
                    }
                }
            }
        }
    }

    // Downloading
    if (downloading != null) {
        Dialog(onDismissRequest = {}) {
            TvUpdateDialogSurface {
                Text(
                    "Downloading…",
                    style = MaterialTheme.typography.headlineMedium,
                    color = com.example.vista.ui.theme.PrimaryText
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    if (downloading.progress > 0) "${downloading.progress}% downloaded"
                    else "Downloading the latest version…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = com.example.vista.ui.theme.MutedText
                )
            }
        }
    }

    // Install ready
    if (complete != null) {
        Dialog(onDismissRequest = { InAppUpdateManager.dismissUpdate(context) }) {
            TvUpdateDialogSurface {
                Text(
                    "Update Ready",
                    style = MaterialTheme.typography.headlineMedium,
                    color = com.example.vista.ui.theme.PrimaryText
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "The update has been downloaded. Install it to apply the new version.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = com.example.vista.ui.theme.MutedText
                )
                Spacer(Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(
                        onClick = {
                            InAppUpdateManager.installUpdate(context, complete.file)
                        }
                    ) {
                        Text("Install")
                    }
                    OutlinedButton(onClick = { InAppUpdateManager.dismissUpdate(context) }) {
                        Text("Later")
                    }
                }
            }
        }
    }

    // Failed
    if (failed != null) {
        Dialog(onDismissRequest = { InAppUpdateManager.dismissUpdate(context) }) {
            TvUpdateDialogSurface {
                Text(
                    "Update Failed",
                    style = MaterialTheme.typography.headlineMedium,
                    color = com.example.vista.ui.theme.PrimaryText
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    failed.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = com.example.vista.ui.theme.MutedText
                )
                Spacer(Modifier.height(24.dp))
                Button(onClick = { InAppUpdateManager.dismissUpdate(context) }) {
                    Text("OK")
                }
            }
        }
    }

    // Premium announcement (TV variant): shown once, never on top of an update dialog. TV has no
    // onboarding flow, so unlike the phone host this is gated purely on the ack flag.
    val premiumAcked by preferencesManager.premiumAcked.collectAsStateWithLifecycle(initialValue = false)
    var showPremiumDialog by remember { mutableStateOf(false) }
    val updateBlocking = available != null || complete != null || failed != null
    LaunchedEffect(premiumAcked, updateBlocking) {
        if (!premiumAcked && !updateBlocking) {
            kotlinx.coroutines.delay(3000)
            if (!premiumAcked && !updateBlocking) showPremiumDialog = true
        }
    }
    if (showPremiumDialog) {
        val premiumScope = rememberCoroutineScope()
        Dialog(onDismissRequest = { showPremiumDialog = false }) {
            TvUpdateDialogSurface {
                Text(
                    "Vista Watch Premium is coming",
                    style = MaterialTheme.typography.headlineMedium,
                    color = com.example.vista.ui.theme.PrimaryText
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "Early 2027, R20/month. Be among the first to know when it launches.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = com.example.vista.ui.theme.MutedText
                )
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = {
                        premiumScope.launch { preferencesManager.markPremiumAcked() }
                        showPremiumDialog = false
                    }
                ) {
                    Text("Got it")
                }
            }
        }
    }

    TvAppEntryPoint()
}

@OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)
@Composable
private fun TvUpdateDialogSurface(content: @Composable ColumnScope.() -> Unit) {
    androidx.compose.material3.Surface(
        modifier = Modifier.width(560.dp),
        color = CardSurface,
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            verticalArrangement = Arrangement.Center
        ) {
            IconHeader()
            Spacer(Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
private fun IconHeader() {
    Box(
        modifier = Modifier
            .size(48.dp)
            .background(Accent.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.Icon(
            Icons.Outlined.SystemUpdate,
            contentDescription = null,
            tint = Accent,
            modifier = Modifier.size(28.dp)
        )
    }
}
