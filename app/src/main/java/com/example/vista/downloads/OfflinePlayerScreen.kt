package com.example.vista.downloads

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import javax.inject.Inject

@HiltViewModel
class OfflinePlayerViewModel @Inject constructor(val downloads: OfflineDownloads, state: SavedStateHandle) : ViewModel() {
    val id: String = state["downloadId"] ?: ""
}

@Composable
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
fun OfflinePlayerScreen(navController: NavController, viewModel: OfflinePlayerViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var player by remember { mutableStateOf<ExoPlayer?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val positionPrefs = remember { context.getSharedPreferences("offline_positions", android.content.Context.MODE_PRIVATE) }
    LaunchedEffect(viewModel.id) {
        try {
            val owner = viewModel.downloads.owner()
            val (item, factory) = viewModel.downloads.offlineItem(viewModel.id)
            player = ExoPlayer.Builder(context).setMediaSourceFactory(DefaultMediaSourceFactory(factory)).build().apply {
                addListener(object : androidx.media3.common.Player.Listener {
                    override fun onPlayerError(failure: androidx.media3.common.PlaybackException) {
                        error = "This offline copy could not be played. It may be incomplete or unsupported on this device. Delete and download it again when connected."
                    }
                })
                setMediaItem(item)
                seekTo(positionPrefs.getLong(viewModel.id, 0))
                prepare(); play()
            }
            while (true) {
                delay(2_000)
                check(viewModel.downloads.owner() == owner) { "Your profile changed. Reopen Downloads with the original profile." }
                player?.let { positionPrefs.edit().putLong(viewModel.id, it.currentPosition).apply() }
            }
        } catch (e: kotlinx.coroutines.CancellationException) { throw e }
        catch (e: Exception) { player?.pause(); error = e.message ?: "Couldn't open this download." }
    }
    DisposableEffect(lifecycle, viewModel.id) {
        viewModel.downloads.beginReading(viewModel.id)
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_PAUSE) player?.pause()
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            player?.let { positionPrefs.edit().putLong(viewModel.id, it.currentPosition).apply(); it.release() }
            viewModel.downloads.endReading(viewModel.id)
        }
    }
    Column(Modifier.fillMaxSize().background(Color.Black)) {
        TextButton(onClick = { navController.popBackStack() }) { Text("Back to Downloads · Offline playback") }
        if (error != null) Text(error.orEmpty(), color = Color.White, modifier = Modifier.padding(24.dp))
        else if (player == null) LinearProgressIndicator(Modifier.fillMaxWidth())
        player?.let { actual -> AndroidView(factory = { PlayerView(it).apply { this.player = actual; keepScreenOn = true } },
            modifier = Modifier.fillMaxWidth().weight(1f), onRelease = { it.player = null }) }
    }
}
