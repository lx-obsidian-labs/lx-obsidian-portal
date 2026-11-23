package com.example.vista.artwork

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.example.vista.domain.model.MediaItem
import com.example.vista.domain.model.watchlistKey
import com.example.vista.ui.theme.CardSurface
import com.example.vista.ui.theme.PrimaryText
import com.example.vista.ui.tv.TvMediaCard

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvArtworkCollection(items: List<MediaItem>, onDismiss: () -> Unit, onOpen: (MediaItem) -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(shape = RoundedCornerShape(24.dp), colors = androidx.tv.material3.SurfaceDefaults.colors(containerColor = CardSurface),
            modifier = Modifier.padding(32.dp).fillMaxWidth().heightIn(max = 640.dp)) {
            Column(Modifier.padding(28.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Text("Explore movies", color = PrimaryText)
                LazyVerticalGrid(columns = GridCells.Adaptive(160.dp), modifier = Modifier.weight(1f, fill = false),
                    contentPadding = PaddingValues(16.dp), horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    items(items, key = { it.watchlistKey() }) { item -> TvMediaCard(item, onClick = { onOpen(item) }) }
                }
                Button(onClick = onDismiss) { Text("Close") }
            }
        }
    }
}
