@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.vista.ui.player

import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.compose.foundation.BorderStroke
import com.example.vista.data.remote.playback.EmbedOrigins
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

    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }
    var isPipMode by remember { mutableStateOf(false) }

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
        navController.popBackStack()
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

    // Keep the real player in step with the transport controls: the play/pause toggle and speed
    // changes previously updated the UI state only, leaving the player untouched.
    LaunchedEffect(uiState.isPlaying, exoPlayer) {
        val player = exoPlayer ?: return@LaunchedEffect
        if (uiState.isPlaying) player.play() else player.pause()
    }

    LaunchedEffect(uiState.playbackSpeed, exoPlayer) {
        exoPlayer?.setPlaybackSpeed(uiState.playbackSpeed)
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
                if (uiState.isRecovering) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Verifying stream & recovering playback...", color = SecondaryText, style = MaterialTheme.typography.bodySmall)
                }
            }
        } else if (uiState.error != null) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
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
                Text(
                    text = ViewUtils.formatError(uiState.error),
                    color = PrimaryText,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
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
                    }
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text("Dismiss", color = MutedText)
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
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                webViewClient = object : WebViewClient() {
                                    /**
                                     * Advertising and analytics hosts, refused so they neither load nor
                                     * track. Not a security control - see [isTopLevelNavigationAllowed]
                                     * for that.
                                     */
                                    private val adDomains = listOf(
                                        "doubleclick.net", "googleadservices.com", "googlesyndication.com",
                                        "moatads.com", "popads.net", "propellerads.com", "adsterra.com",
                                        "exoclick.com", "juicyads.com", "onclickads.net", "adnxs.com",
                                        "adnuntius.com", "taboola.com", "outbrain.com", "histats.com",
                                        "popcash.net", "revcontent.com", "mgid.com", "zedo.com"
                                    )

                                    /**
                                     * True when a main-frame navigation stays on the provider host that
                                     * the app itself asked for. See [EmbedOrigins] for why only the
                                     * main frame is restricted.
                                     */
                                    private fun isTopLevelNavigationAllowed(url: String): Boolean =
                                        EmbedOrigins.allowsTopLevel(url)

                                    override fun shouldOverrideUrlLoading(
                                        view: WebView?,
                                        request: android.webkit.WebResourceRequest?
                                    ): Boolean {
                                        val url = request?.url?.toString().orEmpty()
                                        if (request?.isForMainFrame == true) {
                                            return !isTopLevelNavigationAllowed(url)
                                        }
                                        return adDomains.any { url.contains(it) }
                                    }

                                    override fun shouldInterceptRequest(
                                        view: WebView?,
                                        request: android.webkit.WebResourceRequest?
                                    ): android.webkit.WebResourceResponse? {
                                        val url = request?.url?.toString().orEmpty()
                                        if (adDomains.any { url.contains(it) }) {
                                            return android.webkit.WebResourceResponse("text/plain", "UTF-8", null)
                                        }
                                        // Belt and braces alongside the network security config: refuse a
                                        // cleartext sub-resource even if some path reaches this WebView
                                        // without inheriting the app-wide policy.
                                        if (EmbedOrigins.isCleartext(url)) {
                                            return android.webkit.WebResourceResponse("text/plain", "UTF-8", null)
                                        }
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
                                loadUrl(source.url)
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                        onRelease = { webView ->
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
                                useController = true
                                controllerShowTimeoutMs = 3000

                                val http = androidx.media3.datasource.DefaultHttpDataSource.Factory()
                                    .setDefaultRequestProperties(source.headers)
                                    .setConnectTimeoutMs(10_000)
                                    .setReadTimeoutMs(20_000)
                                val player = ExoPlayer.Builder(ctx)
                                    .setMediaSourceFactory(androidx.media3.exoplayer.source.DefaultMediaSourceFactory(http))
                                    .build()
                                player.addListener(object : androidx.media3.common.Player.Listener {
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
                            playerView.player?.release()
                            if (exoPlayer == playerView.player) {
                                exoPlayer = null
                            }
                        }
                    )
                }
            }

            }

            // Custom Video Controls Overlay - Aesthetic & Functional
            if (uiState.showControls) {
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
                                navController.navigate(Screen.Player.createRoute(
                                    mediaId = uiState.mediaId,
                                    title = uiState.title,
                                    mediaType = com.example.vista.domain.model.MediaType.SERIES,
                                    season = nextEpisode.seasonNumber,
                                    episode = nextEpisode.episodeNumber,
                                    poster = nextEpisode.thumbnailUrl
                                )) {
                                    popUpTo(Screen.Home.route) { saveState = true }
                                }
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

                    // "Autoplay next" setting: jump to the following episode a few seconds before
                    // the current one ends. The manual Play Now button below always remains.
                    val shouldAutoPlay = nearEpisodeEnd && uiState.autoplayNext && uiState.isPlaying
                    LaunchedEffect(shouldAutoPlay) {
                        if (shouldAutoPlay) {
                            delay(3000)
                            navController.navigate(
                                Screen.Player.createRoute(
                                    mediaId = uiState.mediaId,
                                    title = uiState.title,
                                    mediaType = com.example.vista.domain.model.MediaType.SERIES,
                                    season = nextEpisode.seasonNumber,
                                    episode = nextEpisode.episodeNumber,
                                    poster = nextEpisode.thumbnailUrl
                                )
                            ) {
                                popUpTo(Screen.Home.route) { saveState = true }
                            }
                        }
                    }

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
                                    text = if (uiState.autoplayNext) "Next Episode starting…" else "Next Episode",
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
                                        navController.navigate(Screen.Player.createRoute(
                                            mediaId = uiState.mediaId,
                                            title = uiState.title,
                                            mediaType = com.example.vista.domain.model.MediaType.SERIES,
                                            season = nextEpisode.seasonNumber,
                                            episode = nextEpisode.episodeNumber,
                                            poster = nextEpisode.thumbnailUrl
                                        )) {
                                            popUpTo(Screen.Home.route) { saveState = true }
                                        }
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

                            var showPlaybackSettings by remember { mutableStateOf(false) }

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

                            // Quality Selection
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
                            
                            Spacer(modifier = Modifier.height(32.dp))
                        }
                    }
                }
            }
        }
    }
}
