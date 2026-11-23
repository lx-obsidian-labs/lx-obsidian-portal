@file:OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.example.vista.ui.player

import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.compose.foundation.BorderStroke
import com.example.vista.data.remote.playback.EmbedOrigins
import com.example.vista.data.remote.playback.EmbedAdPolicy
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import com.example.vista.navigation.Screen
import com.example.vista.domain.model.StreamType
import com.example.vista.ui.components.CoilImage
import com.example.vista.ui.theme.*
import com.example.vista.util.ViewUtils
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * Best-effort read of the current screen brightness as a 0f..1f fraction, used to seed the
 * brightness slider. Falls back to the device setting, then to 50%.
 */
private fun currentScreenBrightness(context: android.content.Context): Float {
    val windowOverride = (context as? android.app.Activity)?.window?.attributes?.screenBrightness ?: -1f
    if (windowOverride >= 0f) return windowOverride
    return try {
        val raw = android.provider.Settings.System.getInt(
            context.contentResolver,
            android.provider.Settings.System.SCREEN_BRIGHTNESS
        )
        (raw / 255f).coerceIn(0.05f, 1f)
    } catch (_: Exception) {
        0.5f
    }
}

/**
 * Shown when a guest exhausts their session allowance. There is deliberately no "keep watching"
 * escape hatch — the cap only means anything if it is enforced.
 */
@Composable
private fun GuestLimitDialog(onSignIn: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardSurface,
        icon = {
            Icon(Icons.Outlined.Lock, contentDescription = null, tint = Accent, modifier = Modifier.size(40.dp))
        },
        title = { Text("Guest session ended", color = PrimaryText, fontWeight = FontWeight.Bold) },
        text = {
            Text(
                "You've reached the guest viewing limit for this session. " +
                    "Create a free account to keep watching without limits.",
                color = SecondaryText
            )
        },
        confirmButton = {
            Button(
                onClick = onSignIn,
                colors = ButtonDefaults.buttonColors(containerColor = Accent),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Create Account", color = PrimaryText, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Not now", color = MutedText) }
        }
    )
}

@Composable
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
fun PlayerScreen(
    navController: NavController,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val observedProgress by viewModel.progress.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as? android.app.Activity
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    val lifecycleState by lifecycleOwner.lifecycle.currentStateFlow.collectAsStateWithLifecycle()
    val isTv = com.example.vista.util.LocalTvConfiguration.current.isTv

    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }
    var nativePlayerView by remember { mutableStateOf<PlayerView?>(null) }
    var embeddedPlayerView by remember { mutableStateOf<WebView?>(null) }
    var showPlaybackSettings by remember { mutableStateOf(false) }
    var autoplayCancelled by remember(uiState.mediaId, uiState.seasonNumber, uiState.episodeNumber) { mutableStateOf(false) }
    var autoplayRemaining by remember { mutableStateOf<Int?>(null) }
    var showTvHint by remember(uiState.selectedSource?.url, uiState.sourceGeneration) { mutableStateOf(true) }
    var playbackEnded by remember(uiState.selectedSource?.url, uiState.sourceGeneration) { mutableStateOf(false) }
    var isPipMode by remember { mutableStateOf(false) }

    fun playNextEpisode() {
        val next = uiState.nextEpisode ?: return
        if (uiState.guestLimitReached || uiState.needsDataConsent) return
        val currentPlayer = navController.currentDestination?.id ?: return
        autoplayCancelled = true
        navController.navigate(Screen.Player.createRoute(
            mediaId = uiState.mediaId, title = uiState.title,
            mediaType = com.example.vista.domain.model.MediaType.SERIES,
            season = next.seasonNumber, episode = next.episodeNumber, poster = next.thumbnailUrl
        )) { popUpTo(currentPlayer) { inclusive = true }; launchSingleTop = true }
    }

    // Autoplay belongs to the playback lifecycle, not the visibility of the controls overlay.
    LaunchedEffect(playbackEnded, uiState.autoplayNext, uiState.nextEpisode, lifecycleState, autoplayCancelled,
        uiState.guestLimitReached, uiState.needsDataConsent, uiState.error, uiState.isRecovering) {
        autoplayRemaining = null
        if (!autoplayCancelled && com.example.vista.data.playback.PlaybackUiPolicy.canAutoplay(
                playbackEnded, uiState.autoplayNext, uiState.nextEpisode != null,
                lifecycleState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED),
                uiState.guestLimitReached, uiState.needsDataConsent, uiState.error != null, uiState.isRecovering)) {
            for (seconds in 3 downTo 1) { autoplayRemaining = seconds; delay(1_000) }
            autoplayRemaining = null
            playNextEpisode()
        }
    }

    LaunchedEffect(showTvHint) { if (showTvHint) { delay(4_000); showTvHint = false } }
    LaunchedEffect(uiState.showControls, uiState.isPlaying, uiState.selectedSource, uiState.sourceGeneration) {
        if (uiState.showControls && uiState.isPlaying && (!isTv || uiState.selectedSource?.type == StreamType.OTHER)) {
            delay(3_000)
            viewModel.hideControls()
        }
    }

    // ---- Playback brightness (accessibility) ------------------------------------------------
    // The window brightness override is per-activity, so it is applied here and reset on exit
    // rather than mutating the device-wide setting (which would need WRITE_SETTINGS).
    val systemBrightness = remember { currentScreenBrightness(context) }
    var brightness by remember { mutableFloatStateOf(systemBrightness) }
    var followsSystemBrightness by remember { mutableStateOf(true) }
    var showBrightnessPanel by remember { mutableStateOf(false) }

    LaunchedEffect(brightness, followsSystemBrightness) {
        activity?.window?.let { window ->
            val attrs = window.attributes
            attrs.screenBrightness = if (followsSystemBrightness) {
                android.view.WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            } else {
                // Never fully dark: a 0 override makes the screen unreadable.
                brightness.coerceIn(0.05f, 1f)
            }
            window.attributes = attrs
        }
    }

    fun formatTime(ms: Long): String {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%02d:%02d".format(minutes, seconds)
    }

    // Handle orientation and system UI
    LaunchedEffect(Unit) {
        activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        // Long-form video: the screen should not sleep mid-playback.
        activity?.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    DisposableEffect(Unit) {
        onDispose {
            activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            // Hand the brightness back to the system on the way out.
            activity?.window?.let { window ->
                val attrs = window.attributes
                attrs.screenBrightness = android.view.WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                window.attributes = attrs
            }
            exoPlayer?.release()
        }
    }

    // Lifecycle observer for pause/play and PiP
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> {
                    if (activity?.isInPictureInPictureMode == true) {
                        isPipMode = true
                    } else {
                        exoPlayer?.pause()
                    }
                }
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> {
                    isPipMode = false
                    if (uiState.isPlaying) exoPlayer?.play()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    androidx.activity.compose.BackHandler {
        when {
            showPlaybackSettings -> { showPlaybackSettings = false; embeddedPlayerView?.requestFocus() }
            autoplayRemaining != null -> { autoplayCancelled = true; autoplayRemaining = null }
            isTv && uiState.showControls -> { nativePlayerView?.hideController(); viewModel.hideControls(); embeddedPlayerView?.requestFocus() }
            else -> navController.popBackStack()
        }
    }

    if (uiState.guestLimitReached) {
        GuestLimitDialog(
            onSignIn = {
                navController.navigate(Screen.Auth.route) {
                    popUpTo(Screen.Home.route) { inclusive = false }
                }
            },
            onDismiss = { navController.popBackStack() }
        )
    }

    if (uiState.needsDataConsent) {
        AlertDialog(
            onDismissRequest = { navController.popBackStack() },
            title = { Text("Use mobile or metered data?") },
            text = { Text("Streaming video can use a lot of data. Allow it for this viewing session? You can reduce quality or enable Data Saver in Settings.") },
            confirmButton = { TextButton(onClick = viewModel::allowMeteredPlayback) { Text("Use data") } },
            dismissButton = { TextButton(onClick = { navController.popBackStack() }) { Text("Go back") } }
        )
    }

    if (isTv && showPlaybackSettings) TvPlaybackOptions(
        sources = uiState.streamSources, selected = uiState.selectedSource, autoplay = uiState.autoplayNext,
        hasNextEpisode = uiState.nextEpisode != null,
        onSelect = { viewModel.onSourceChange(it); showPlaybackSettings = false },
        onAutoplayChange = viewModel::setAutoplayNext,
        onNextEpisode = { showPlaybackSettings = false; playNextEpisode() },
        onHelp = { showPlaybackSettings = false; exoPlayer?.pause(); navController.navigate(Screen.Support.route) },
        onDismiss = { showPlaybackSettings = false; embeddedPlayerView?.requestFocus() }
    )

    // Keep the real player in step with the transport controls: the play/pause toggle and speed
    // changes previously updated the UI state only, leaving the player untouched.
    LaunchedEffect(uiState.isPlaying, exoPlayer) {
        val player = exoPlayer ?: return@LaunchedEffect
        if (uiState.isPlaying) player.play() else player.pause()
    }

    LaunchedEffect(uiState.playbackSpeed, exoPlayer) {
        exoPlayer?.setPlaybackSpeed(uiState.playbackSpeed)
    }

    LaunchedEffect(uiState.selectedQuality, uiState.subtitleEnabled, uiState.subtitleLanguage, uiState.audioLanguage, exoPlayer) {
        val player = exoPlayer ?: return@LaunchedEffect
        val maxHeight = com.example.vista.domain.model.PlaybackPreferencesPolicy.maxHeight(uiState.selectedQuality)
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .setMaxVideoSize(Int.MAX_VALUE, maxHeight)
            .setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_TEXT, !uiState.subtitleEnabled)
            .setPreferredTextLanguage(com.example.vista.domain.model.PlaybackPreferencesPolicy.language(uiState.subtitleLanguage))
            .setSelectUndeterminedTextLanguage(uiState.subtitleEnabled)
            .setPreferredAudioLanguage(com.example.vista.domain.model.PlaybackPreferencesPolicy.language(uiState.audioLanguage))
            .build()
    }

    /**
     * Drives playback state from the actual player.
     *
     * Nothing used to report position back to the view model, so `currentPosition` stayed at zero:
     * the scrubber never moved, the elapsed time never advanced, and every saved progress row held
     * 0 — which is why Continue Watching and Watch History never had anything to resume.
     */
    LaunchedEffect(exoPlayer) {
        val player = exoPlayer ?: return@LaunchedEffect
        while (true) {
            if (player.isPlaying) {
                viewModel.onProgressUpdate(
                    position = player.currentPosition.coerceAtLeast(0L),
                    duration = player.duration.coerceAtLeast(0L)
                )
                viewModel.onPlaybackTick()
            }
            kotlinx.coroutines.delay(1000)
        }
    }

    // Pause immediately when a guest runs out of session time.
    LaunchedEffect(uiState.guestLimitReached) {
        if (uiState.guestLimitReached) exoPlayer?.pause()
    }

    fun seekBy(deltaMs: Long) {
        val player = exoPlayer ?: return
        val max = if (player.duration > 0) player.duration else Long.MAX_VALUE
        val target = (player.currentPosition + deltaMs).coerceIn(0L, max)
        player.seekTo(target)
        viewModel.onSeekTo(target)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onPreviewKeyEvent { event ->
                val key = event.nativeKeyEvent
                if (isTv && com.example.vista.data.playback.PlaybackUiPolicy.opensTvOptions(
                        key.keyCode, key.action == android.view.KeyEvent.ACTION_DOWN, key.isLongPress)) {
                    showPlaybackSettings = true
                    true
                } else false
            }
    ) {
        // Brightness panel: an explicit, labelled control rather than a hidden gesture, so it is
        // reachable by screen readers as well as by touch.
        if (showBrightnessPanel) {
            Surface(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 24.dp)
                    .width(220.dp),
                color = Color.Black.copy(alpha = 0.75f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.LightMode, contentDescription = null, tint = Accent, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (followsSystemBrightness) "Brightness (System)" else "Brightness",
                            color = PrimaryText,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${(brightness * 100).roundToInt()}%",
                            color = SecondaryText,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    Slider(
                        value = brightness,
                        onValueChange = {
                            followsSystemBrightness = false
                            brightness = it
                        },
                        valueRange = 0.05f..1f,
                        colors = SliderDefaults.colors(
                            thumbColor = Accent,
                            activeTrackColor = Accent,
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                        )
                    )
                    TextButton(
                        onClick = {
                            followsSystemBrightness = true
                            brightness = systemBrightness
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Follow system", color = Accent, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        if (uiState.isLoading || uiState.isRecovering) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(color = Accent)
                Spacer(modifier = Modifier.height(16.dp))
                Text(if (uiState.isRecovering) "Getting you back to the story" else "Finding your stream",
                    color = PrimaryText, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text(if (uiState.isRecovering) "Checking this source and restoring playback…" else "Checking available playback sources…",
                    color = SecondaryText, style = MaterialTheme.typography.bodySmall)
            }
        } else if (uiState.error != null) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .widthIn(max = 560.dp)
                    .padding(24.dp)
                    .background(CardSurface, RoundedCornerShape(16.dp))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Outlined.Warning,
                    contentDescription = null,
                    tint = Accent,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text("Let's get playback working", color = PrimaryText,
                    style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = ViewUtils.formatError(uiState.error),
                    color = PrimaryText,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (uiState.canRepairManually) Button(
                        onClick = {
                            viewModel.clearError()
                            viewModel.repairPlaybackAndRetry()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Accent),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Repair Playback", color = PrimaryText, fontWeight = FontWeight.Bold)
                    }
                    if (uiState.streamSources.size > 1) {
                        OutlinedButton(
                            onClick = {
                                viewModel.clearError()
                                val alt = uiState.streamSources.firstOrNull { it != uiState.selectedSource }
                                if (alt != null) viewModel.onSourceChange(alt)
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Switch Server", color = PrimaryText)
                        }
                    } else if (uiState.streamSources.isEmpty()) {
                        OutlinedButton(
                            onClick = {
                                viewModel.clearError()
                                navController.navigate(Screen.Search.createRoute(uiState.title)) {
                                    popUpTo(Screen.Home.route)
                                }
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Find Alternatives", color = PrimaryText)
                        }
                    }
                    TextButton(onClick = {
                        viewModel.clearError()
                        navController.popBackStack()
                    }) {
                        Text("Go Back", color = MutedText)
                    }
                }
            }
        } else {
            // Video player
            uiState.selectedSource?.let { source ->
                key(source.url, uiState.sourceGeneration) {
                // The type alone decides this. The previous test also matched on the URL containing "vidsrc" or
                // "embed", which would have sent a resolved manifest from one of those providers into
                // the WebView instead of ExoPlayer - breaking playback and resume for exactly the
                // titles the resolver was added to fix.
                if (source.type == StreamType.OTHER) {
                    // Use WebView for embed sources
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                embeddedPlayerView = this
                                isFocusable = true
                                isFocusableInTouchMode = true
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                webViewClient = object : WebViewClient() {
                                    override fun onPageCommitVisible(view: WebView?, url: String?) {
                                        super.onPageCommitVisible(view, url)
                                        view?.evaluateJavascript(EmbedAdPolicy.cleanupScript, null)
                                    }

                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        view?.evaluateJavascript(EmbedAdPolicy.cleanupScript, null)
                                        if (isTv) { viewModel.setControlsVisible(false, uiState.sourceGeneration); view?.requestFocus() }
                                    }

                                    override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                        if (url != null && url != "about:blank" && !EmbedOrigins.allowsTopLevel(url)) {
                                            view?.stopLoading()
                                            return
                                        }
                                        super.onPageStarted(view, url, favicon)
                                    }

                                    /**
                                     * True when a main-frame navigation stays on the provider host that
                                     * the app itself asked for. See [EmbedOrigins] for why only the
                                     * main frame is restricted.
                                     */
                                    @Suppress("DEPRECATION")
                                    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean =
                                        com.example.vista.data.remote.playback.EmbedNavigationPolicy.blocks(url.orEmpty(), mainFrame = true)

                                    override fun shouldOverrideUrlLoading(
                                        view: WebView?,
                                        request: android.webkit.WebResourceRequest?
                                    ): Boolean {
                                        val url = request?.url?.toString().orEmpty()
                                        return com.example.vista.data.remote.playback.EmbedNavigationPolicy.blocks(url,
                                            mainFrame = request?.isForMainFrame != false)
                                    }

                                    override fun shouldInterceptRequest(
                                        view: WebView?,
                                        request: android.webkit.WebResourceRequest?
                                    ): android.webkit.WebResourceResponse? {
                                        val url = request?.url?.toString().orEmpty()
                                        if (com.example.vista.data.remote.playback.EmbedNavigationPolicy.blocks(url, request?.isForMainFrame == true)) {
                                            return android.webkit.WebResourceResponse("text/plain", "UTF-8", null)
                                        }
                                        // Belt and braces alongside the network security config: refuse a
                                        // cleartext sub-resource even if some path reaches this WebView
                                        // without inheriting the app-wide policy.
                                        if (EmbedOrigins.isCleartext(url)) {
                                            return android.webkit.WebResourceResponse("text/plain", "UTF-8", null)
                                        }
                                        if (request != null) viewModel.onEmbeddedMedia(source.url, uiState.sourceGeneration, url, request.requestHeaders)
                                        return super.shouldInterceptRequest(view, request)
                                    }

                                    override fun onReceivedError(
                                        view: WebView?,
                                        request: android.webkit.WebResourceRequest?,
                                        error: android.webkit.WebResourceError?
                                    ) {
                                        // Surface real failures (typically a dead provider) so the user
                                        // can switch source instead of staring at a blank frame.
                                        if (request?.isForMainFrame == true) {
                                            viewModel.onPlayerError(
                                                "This source could not be loaded. Try another server."
                                            )
                                        }
                                    }
                                }
                                webChromeClient = object : WebChromeClient() {
                                    /** Pop-ups are the main ad vector on these embeds — refuse them. */
                                    override fun onCreateWindow(
                                        view: WebView?,
                                        isDialog: Boolean,
                                        isUserGesture: Boolean,
                                        resultMsg: android.os.Message?
                                    ): Boolean {
                                        // Returning false without supplying a WebView keeps the pop-up dead.
                                        return false
                                    }
                                }
                                settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    loadWithOverviewMode = true
                                    useWideViewPort = true
                                    mediaPlaybackRequiresUserGesture = false
                                    // Block window.open() pop-ups without disabling the player scripts.
                                    javaScriptCanOpenWindowsAutomatically = false
                                    setSupportMultipleWindows(true)
                                    // NEVER_ALLOW, not ALWAYS_ALLOW. ALWAYS_ALLOW let an http sub-resource
                                    // load inside an https provider page, so anyone on the network could
                                    // rewrite the page and have their JavaScript execute in this app.
                                    mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                                    // The page only needs http/https. Refuse local filesystem and content
                                    // provider reads so a provider page cannot probe the device.
                                    allowFileAccess = false
                                    allowContentAccess = false
                                    @Suppress("DEPRECATION")
                                    databaseEnabled = false
                                    setGeolocationEnabled(false)
                                    userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Safari/537.36"
                                }
                                // Provider download/app-install ads never launch an external downloader/browser.
                                setDownloadListener { _, _, _, _, _ -> }
                                setOnTouchListener { _, event ->
                                    if (!isTv && event.action == android.view.MotionEvent.ACTION_DOWN) viewModel.setControlsVisible(true)
                                    false
                                }
                                setOnKeyListener { _, code, event ->
                                    if (isTv && com.example.vista.data.playback.PlaybackUiPolicy.opensTvOptions(
                                            code, event.action == android.view.KeyEvent.ACTION_DOWN, event.isLongPress)) {
                                        showPlaybackSettings = true
                                        true
                                    } else false
                                }
                                post { if (isTv) requestFocus() }
                                // Runs before provider scripts, including in cross-origin player
                                // frames. Older WebViews retain the page-callback fallback above.
                                if (androidx.webkit.WebViewFeature.isFeatureSupported(
                                        androidx.webkit.WebViewFeature.DOCUMENT_START_SCRIPT
                                    )) {
                                    androidx.webkit.WebViewCompat.addDocumentStartJavaScript(
                                        this, EmbedAdPolicy.cleanupScript, setOf("*")
                                    )
                                }
                                loadUrl(source.url)
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                         onRelease = { webView ->
                            if (embeddedPlayerView == webView) embeddedPlayerView = null
                            webView.webViewClient = WebViewClient()
                            webView.webChromeClient = null
                            webView.stopLoading()
                            webView.loadUrl("about:blank")
                            webView.clearHistory()
                            webView.removeAllViews()
                            webView.destroy()
                        }
                    )
                } else {
                    // Use Media3 ExoPlayer for direct sources
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                layoutParams = FrameLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                nativePlayerView = this
                                useController = isTv
                                controllerShowTimeoutMs = 3000
                                controllerAutoShow = true
                                controllerHideOnTouch = true
                                if (isTv) {
                                    setControllerVisibilityListener(PlayerView.ControllerVisibilityListener { visibility ->
                                        viewModel.setControlsVisible(visibility == android.view.View.VISIBLE, uiState.sourceGeneration)
                                    })
                                    requestFocus()
                                } else setOnTouchListener { _, event ->
                                    if (event.action == android.view.MotionEvent.ACTION_UP) viewModel.setControlsVisible(true)
                                    true
                                }

                                val http = androidx.media3.datasource.DefaultHttpDataSource.Factory()
                                    .setDefaultRequestProperties(source.headers)
                                    .setConnectTimeoutMs(10_000)
                                    .setReadTimeoutMs(20_000)
                                val player = ExoPlayer.Builder(ctx)
                                    .setMediaSourceFactory(androidx.media3.exoplayer.source.DefaultMediaSourceFactory(http))
                                    .build()
                                player.addListener(object : androidx.media3.common.Player.Listener {
                                    override fun onPlaybackStateChanged(playbackState: Int) {
                                        playbackEnded = playbackState == androidx.media3.common.Player.STATE_ENDED
                                    }
                                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                                        viewModel.onPlayerException(error)
                                    }
                                })
                                this.player = player
                                exoPlayer = player

                                val mediaBuilder = MediaItem.Builder().setUri(source.url)
                                source.drmInfo?.let { drm ->
                                    val license = drm.licenseUrl
                                    if (drm.drmScheme == "com.widevine.alpha" && license?.startsWith("https://") == true) {
                                        mediaBuilder.setDrmConfiguration(MediaItem.DrmConfiguration.Builder(androidx.media3.common.C.WIDEVINE_UUID)
                                            .setLicenseUri(license).setLicenseRequestHeaders(drm.drmHeaders).build())
                                    }
                                }
                                val mediaItem = mediaBuilder.build()
                                player.setMediaItem(mediaItem)
                                player.setPlaybackSpeed(uiState.playbackSpeed)
                                player.prepare()
                                player.play()

                                if (viewModel.progress.value.currentPosition > 0) {
                                    player.seekTo(viewModel.progress.value.currentPosition)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                         onRelease = { playerView ->
                            playerView.setControllerVisibilityListener(null as PlayerView.ControllerVisibilityListener?)
                            if (nativePlayerView == playerView) nativePlayerView = null
                            playerView.player?.release()
                            if (exoPlayer == playerView.player) {
                                exoPlayer = null
                            }
                        },
                        update = { playerView ->
                            val color = when (uiState.subtitleColor.lowercase()) {
                                "yellow" -> android.graphics.Color.YELLOW; "cyan" -> android.graphics.Color.CYAN
                                "green" -> android.graphics.Color.GREEN; "pink" -> 0xFFFF80AB.toInt()
                                "red" -> android.graphics.Color.RED; else -> android.graphics.Color.WHITE
                            }
                            playerView.subtitleView?.apply {
                                setApplyEmbeddedStyles(false)
                                setFixedTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, uiState.subtitleSize)
                                setStyle(androidx.media3.ui.CaptionStyleCompat(color, android.graphics.Color.TRANSPARENT,
                                    android.graphics.Color.TRANSPARENT, androidx.media3.ui.CaptionStyleCompat.EDGE_TYPE_OUTLINE,
                                    android.graphics.Color.BLACK, null))
                            }
                        }
                    )
                }
            }

            }

            // Custom Video Controls Overlay - Aesthetic & Functional
            if (!isTv && !isPipMode && uiState.showControls && uiState.selectedSource?.type != StreamType.OTHER) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .pointerInput(uiState.doubleTapToSeekSeconds) {
                            detectTapGestures(
                                onTap = { viewModel.onControlsVisibilityToggle() },
                                onDoubleTap = { offset ->
                                    val skipMs = uiState.doubleTapToSeekSeconds.coerceAtLeast(1) * 1000L
                                    val width = this.size.width
                                    when {
                                        offset.x < width / 3f -> seekBy(-skipMs)
                                        offset.x > width * 2f / 3f -> seekBy(skipMs)
                                    }
                                }
                            )
                        }
                ) {
                    // Center Controls
                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalArrangement = Arrangement.spacedBy(48.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { seekBy(-10_000L) }) {
                            Icon(Icons.Outlined.Replay10, contentDescription = "Rewind 10 seconds", tint = Color.White, modifier = Modifier.size(48.dp))
                        }

                        IconButton(onClick = { viewModel.onPlayPauseToggle() }) {
                            Icon(
                                if (uiState.isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                                contentDescription = if (uiState.isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(56.dp)
                            )
                        }

                        IconButton(onClick = { seekBy(10_000L) }) {
                            Icon(Icons.Outlined.Forward10, contentDescription = "Forward 10 seconds", tint = Color.White, modifier = Modifier.size(48.dp))
                        }

                        val nextEpisode = uiState.nextEpisode
                        if (nextEpisode != null) {
                            IconButton(onClick = {
                                playNextEpisode()
                            }) {
                                Icon(Icons.Outlined.SkipNext, contentDescription = "Next Episode", tint = Color.White, modifier = Modifier.size(48.dp))
                            }
                        }
                    }

                    // Skip Intro Button (Netflix Style)
                    if (observedProgress.currentPosition in 10_000L..90_000L) {
                        Button(
                            onClick = { seekBy(80_000L) },
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(bottom = 120.dp, start = 32.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text("Skip Intro", fontWeight = FontWeight.Bold)
                        }
                    }

                    // Next Episode Overlay (Netflix Style)
                    val nextEpisode = uiState.nextEpisode
                    val nearEpisodeEnd = nextEpisode != null && observedProgress.duration > 0 &&
                        observedProgress.currentPosition > observedProgress.duration - 20_000L

                    if (nearEpisodeEnd) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(bottom = 120.dp, end = 32.dp)
                                .width(220.dp),
                            color = Color.Black.copy(alpha = 0.8f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = if (uiState.autoplayNext && playbackEnded) "Next Episode starting…" else "Up next",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Accent
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(60.dp, 34.dp).clip(RoundedCornerShape(4.dp)).background(CardSurface)) {
                                        CoilImage(url = nextEpisode.thumbnailUrl, contentDescription = null, modifier = Modifier.fillMaxSize())
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "E${nextEpisode.episodeNumber} ${nextEpisode.title}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = PrimaryText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        playNextEpisode()
                                    },
                                    modifier = Modifier.fillMaxWidth().height(36.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                                    shape = RoundedCornerShape(4.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("Play Now", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Bottom Progress Area
                    val progress by viewModel.progress.collectAsStateWithLifecycle()
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 32.dp, start = 24.dp, end = 24.dp)
                    ) {
                        Slider(
                            value = progress.currentPosition.toFloat(),
                            // Scrub locally, then commit once the drag ends so the player is seeked
                            // exactly once per gesture.
                            onValueChange = { viewModel.onSeekPreview(it.toLong()) },
                            onValueChangeFinished = {
                                exoPlayer?.seekTo(progress.currentPosition)
                                viewModel.onSeekTo(progress.currentPosition)
                            },
                            valueRange = 0f..progress.duration.toFloat().coerceAtLeast(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = Accent,
                                activeTrackColor = Accent,
                                inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                            )
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = formatTime(progress.currentPosition),
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(
                                text = formatTime(progress.duration),
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }

                        // Native Media3 controls own TV input; no Vista chrome sits over provider controls.
                        if (!isTv && !isPipMode && (uiState.showControls || showPlaybackSettings)) {
                        // Top bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .align(Alignment.TopStart),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = PrimaryText)
                            }
                            
                            Text(
                                uiState.title,
                                color = PrimaryText,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
                            )

                            IconButton(onClick = {
                                navController.navigate(Screen.Downloads.createRoute(uiState.mediaId, uiState.title,
                                    if (uiState.isSeries) com.example.vista.domain.model.MediaType.SERIES else com.example.vista.domain.model.MediaType.MOVIE,
                                    uiState.seasonNumber ?: 0, uiState.episodeNumber ?: 0))
                            }) { Icon(Icons.Outlined.Download, "Download this movie or episode", tint = PrimaryText) }

                            IconButton(onClick = {
                                exoPlayer?.pause()
                                navController.navigate(Screen.AiAssistant.companion(uiState.mediaId, if (uiState.isSeries) "series" else "movie"))
                            }) { Icon(Icons.Outlined.AutoAwesome, "Ask movie companion", tint = PrimaryText) }

                            IconButton(onClick = { showBrightnessPanel = !showBrightnessPanel }) {
                                Icon(
                                    Icons.Outlined.LightMode,
                                    contentDescription = "Adjust brightness",
                                    tint = if (followsSystemBrightness) PrimaryText else Accent
                                )
                            }

                            IconButton(onClick = {
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                    activity?.enterPictureInPictureMode(
                                        android.app.PictureInPictureParams.Builder().build()
                                    )
                                }
                            }) {
                                Icon(Icons.Outlined.PictureInPicture, contentDescription = "PiP", tint = PrimaryText)
                            }

                            IconButton(onClick = { showPlaybackSettings = true }) {
                                Icon(Icons.Outlined.Settings, contentDescription = "Settings", tint = PrimaryText)
                            }

                if (showPlaybackSettings) {
                    ModalBottomSheet(
                        onDismissRequest = { showPlaybackSettings = false },
                        containerColor = CardSurface
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Playback Settings", style = MaterialTheme.typography.titleLarge, color = PrimaryText, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            // Source Selection
                            Text("Source", style = MaterialTheme.typography.labelLarge, color = Accent)
                            Text("Direct sources use Vista's player. Embedded sources use the provider's player and controls.",
                                color = SecondaryText, style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 6.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 12.dp)) {
                                items(uiState.streamSources) { source ->
                                    FilterChip(
                                        selected = uiState.selectedSource == source,
                                        onClick = { viewModel.onSourceChange(source) },
                                        label = { Text(source.name) },
                                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Accent)
                                    )
                                }
                            }

                            HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 8.dp))

                             // Embedded providers own their quality/speed controls.
                             if (uiState.selectedSource?.type != StreamType.OTHER) {
                            Text("Quality", style = MaterialTheme.typography.labelLarge, color = Accent)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 12.dp)) {
                                items(uiState.availableQualities) { quality ->
                                    FilterChip(
                                        selected = uiState.selectedQuality == quality,
                                        onClick = { viewModel.onQualityChange(quality) },
                                        label = { Text(quality) },
                                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Accent)
                                    )
                                }
                            }

                            HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 8.dp))

                            // Speed Selection
                            Text("Playback Speed", style = MaterialTheme.typography.labelLarge, color = Accent)
                            val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 12.dp)) {
                                items(speeds) { speed ->
                                    FilterChip(
                                        selected = uiState.playbackSpeed == speed,
                                        onClick = { viewModel.onPlaybackSpeedChange(speed) },
                                        label = { Text("${speed}x") },
                                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Accent)
                                    )
                                }
                            }

                            if (uiState.subtitles.isNotEmpty()) {
                                HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 8.dp))
                                Text("Subtitles", style = MaterialTheme.typography.labelLarge, color = Accent)
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 12.dp)) {
                                    items(uiState.subtitles) { subtitle ->
                                        FilterChip(
                                            selected = (subtitle == uiState.subtitles.firstOrNull()),
                                            onClick = { viewModel.onSubtitleSelected(subtitle) },
                                            label = { Text(subtitle.label.ifBlank { subtitle.language }) },
                                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Accent)
                                        )
                                    }
                                }
                            }

                            if (uiState.audioTracks.isNotEmpty()) {
                                HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 8.dp))
                                Text("Audio Track", style = MaterialTheme.typography.labelLarge, color = Accent)
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 12.dp)) {
                                    items(uiState.audioTracks) { track ->
                                        FilterChip(
                                            selected = (track == uiState.audioTracks.firstOrNull()),
                                            onClick = { viewModel.onAudioTrackSelected(track) },
                                            label = { Text(track.label.ifBlank { track.language }) },
                                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Accent)
                                        )
                                    }
                                }
                            }
                            } else {
                                Text("Use the embedded player's controls for quality, speed and seeking.", color = SecondaryText)
                            }
                            
                            Spacer(modifier = Modifier.height(32.dp))
                        }
                    }
                }
            }
                        }
        }
        if (isTv && showTvHint && !uiState.isLoading && uiState.error == null) {
            Text("Menu or hold OK: playback sources", color = PrimaryText,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(8.dp)).padding(12.dp))
        }
        autoplayRemaining?.let { seconds ->
            Surface(color = CardSurface, shape = RoundedCornerShape(16.dp),
                modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Next episode in $seconds…", color = PrimaryText, fontWeight = FontWeight.Bold)
                    Button(onClick = { autoplayCancelled = true; autoplayRemaining = null }) { Text("Cancel autoplay") }
                }
            }
        }
    }
}
