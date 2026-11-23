package com.example.vista.downloads

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.exoplayer.offline.Download
import androidx.navigation.NavController
import com.example.vista.navigation.Screen
import com.example.vista.ui.theme.*
import java.util.Locale

@Composable
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
fun DownloadsScreen(navController: NavController, viewModel: DownloadsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var height by rememberSaveable { mutableIntStateOf(720) }
    LaunchedEffect(state.defaultHeight) { height = state.defaultHeight }
    var deleting by remember { mutableStateOf<OfflineEntry?>(null) }
    LazyColumn(Modifier.fillMaxSize().background(Background), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back", tint = PrimaryText) }
                Column {
                    Text("Downloads", style = MaterialTheme.typography.headlineMedium, color = PrimaryText, fontWeight = FontWeight.Bold)
                    Text("Your movie nights, even offline", color = SecondaryText)
                }
            }
        }
        item {
            Text("Unmetered connections only · No roaming · One transfer at a time", color = Accent, style = MaterialTheme.typography.labelMedium)
            Text("Downloads use separate storage. Clearing temporary cache won't delete them.", color = SecondaryText, style = MaterialTheme.typography.bodySmall)
        }
        state.target?.let { target -> item {
            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(target.label, style = MaterialTheme.typography.titleMedium)
                    Text("Check whether a direct media source is available. Embedded pages cannot be saved as movies.", style = MaterialTheme.typography.bodySmall)
                    Button(onClick = viewModel::checkSources, enabled = !state.busy) { Text("Find download sources") }
                    if (state.sources.isNotEmpty()) {
                        Text("Quality preference (actual selected quality is shown after preparation):", style = MaterialTheme.typography.bodySmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(480 to "Saver", 720 to "Balanced", Int.MAX_VALUE to "Best").forEach { (value, label) ->
                                FilterChip(selected = height == value, onClick = { height = value }, label = { Text(label) })
                            }
                        }
                        Text("Size is unknown until the provider exposes it. Adaptive video can use several GB. English subtitles are selected when available.", style = MaterialTheme.typography.bodySmall)
                        state.sources.forEach { source ->
                            OutlinedButton(onClick = { viewModel.start(source, height) }, enabled = !state.busy) {
                                Icon(Icons.Outlined.Download, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                                Text("${source.name} · ${source.type.name}")
                            }
                        }
                    }
                }
            }
        } }
        if (state.loading || state.busy) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        state.message?.let { message -> item { Text(message, color = Accent, style = MaterialTheme.typography.bodyMedium) } }
        if (!state.loading && state.entries.isEmpty()) item {
            Column(Modifier.fillMaxWidth().padding(vertical = 36.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Outlined.DownloadForOffline, null, tint = Accent, modifier = Modifier.size(48.dp))
                Text("Make room for a movie night", color = PrimaryText, style = MaterialTheme.typography.titleMedium)
                Text("Open a movie or episode's details and tap Download. Completed titles appear here for offline playback.", color = SecondaryText)
            }
        }
        items(state.entries, key = { it.id }) { entry ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(entry.metadata.target.label, fontWeight = FontWeight.SemiBold)
                    Text("${entry.metadata.quality} · ${entry.metadata.provider}", style = MaterialTheme.typography.labelMedium)
                    val label = when (entry.state) {
                        Download.STATE_COMPLETED -> "Ready offline"
                        Download.STATE_DOWNLOADING -> "Downloading"
                        Download.STATE_STOPPED -> when (entry.stopReason) {
                            3 -> "Waiting until roaming ends"
                            2 -> "Paused after profile change · Resume when ready"
                            else -> "Paused · Tap Resume to continue"
                        }
                        Download.STATE_FAILED -> "Download failed · Check connection/storage; Refresh & retry resolves a new source"
                        Download.STATE_REMOVING, Download.STATE_RESTARTING -> "Removing media…"
                        else -> if (entry.waitingForNetwork) "Waiting for an unmetered, non-roaming connection" else "Queued"
                    }
                    Text(label, color = if (entry.state == Download.STATE_COMPLETED) Accent else MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${bytes(entry.bytes)}${if (entry.totalBytes > 0) " / ${bytes(entry.totalBytes)}" else " downloaded"}", style = MaterialTheme.typography.bodySmall)
                    if (entry.state == Download.STATE_DOWNLOADING) {
                        if (entry.percent >= 0) LinearProgressIndicator(progress = { entry.percent / 100f }, modifier = Modifier.fillMaxWidth())
                        else LinearProgressIndicator(Modifier.fillMaxWidth())
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        when (entry.state) {
                            Download.STATE_COMPLETED -> Button(onClick = { navController.navigate(Screen.OfflinePlayer.createRoute(entry.id)) }) { Text("Play offline") }
                            Download.STATE_STOPPED -> TextButton(onClick = { viewModel.resume(entry.id) }, enabled = !state.busy) { Text("Resume") }
                            Download.STATE_FAILED -> TextButton(onClick = { viewModel.retry(entry) }, enabled = !state.busy) { Text("Refresh & retry") }
                            Download.STATE_DOWNLOADING, Download.STATE_QUEUED -> TextButton(onClick = { viewModel.pause(entry.id) }, enabled = !state.busy) { Text("Pause") }
                        }
                        TextButton(onClick = { deleting = entry }, enabled = !state.busy && entry.state != Download.STATE_REMOVING) { Text("Delete") }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(32.dp)) }
    }
    deleting?.let { entry ->
        AlertDialog(onDismissRequest = { deleting = null }, title = { Text("Delete download?") },
            text = { Text("Remove ${entry.metadata.target.label} and its downloaded media from this device?") },
            confirmButton = { TextButton(onClick = { viewModel.remove(entry.id); deleting = null }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Keep") } })
    }
}

private fun bytes(value: Long): String = if (value >= 1_000_000_000) String.format(Locale.US, "%.1f GB", value / 1_000_000_000.0)
    else String.format(Locale.US, "%.1f MB", value.coerceAtLeast(0) / 1_000_000.0)
