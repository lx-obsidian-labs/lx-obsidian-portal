package com.example.vista.ui.player

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.*
import com.example.vista.domain.model.StreamSource
import com.example.vista.domain.model.StreamType
import com.example.vista.ui.theme.*

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvPlaybackOptions(sources: List<StreamSource>, selected: StreamSource?, autoplay: Boolean,
    hasNextEpisode: Boolean, onSelect: (StreamSource) -> Unit, onAutoplayChange: (Boolean) -> Unit,
    onNextEpisode: () -> Unit, onHelp: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(colors = SurfaceDefaults.colors(containerColor = CardSurface), shape = RoundedCornerShape(24.dp),
            modifier = Modifier.padding(32.dp).widthIn(max = 800.dp).fillMaxWidth().heightIn(max = 620.dp)) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(28.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Text("Playback sources", color = PrimaryText, style = MaterialTheme.typography.headlineSmall)
                Text("Choose with your remote. Back returns to the video.", color = SecondaryText)
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = 240.dp), contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(sources.distinctBy { it.url }, key = { it.url }) { source ->
                        Button(onClick = { onSelect(source) }, modifier = Modifier.fillMaxWidth()) {
                            Text("${if (source == selected) "✓ " else ""}${source.name} · ${if (source.type == StreamType.OTHER) "Provider player" else "Direct stream"}")
                        }
                    }
                }
                Text("Automatic next episode uses confirmed direct-stream completion. For embedded providers, use Play next episode here.", color = SecondaryText)
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Button(onClick = { onAutoplayChange(!autoplay) }) { Text("Autoplay next: ${if (autoplay) "On" else "Off"}") }
                    if (hasNextEpisode) Button(onClick = onNextEpisode) { Text("Play next episode") }
                    Button(onClick = onHelp) { Text("Help & Support") }
                    Button(onClick = onDismiss) { Text("Return to video") }
                }
            }
        }
    }
}
