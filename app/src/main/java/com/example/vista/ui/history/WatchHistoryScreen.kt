package com.example.vista.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.vista.domain.model.MediaType
import com.example.vista.domain.model.PlaybackProgress
import com.example.vista.navigation.Screen
import com.example.vista.ui.components.CoilImage
import com.example.vista.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun WatchHistoryScreen(
    navController: NavController,
    viewModel: WatchHistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showClearDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back",
                    tint = PrimaryText
                )
            }
            Text(
                text = "Watch History",
                style = MaterialTheme.typography.titleLarge,
                color = PrimaryText,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            if (uiState.entries.isNotEmpty()) {
                TextButton(onClick = { showClearDialog = true }) {
                    Text("Clear All", color = Accent)
                }
            }
        }

        OutlinedTextField(
            value = uiState.query,
            onValueChange = { viewModel.onQueryChange(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            placeholder = { Text("Search your watch history", color = MutedText) },
            leadingIcon = {
                Icon(Icons.Outlined.Search, contentDescription = null, tint = MutedText)
            },
            trailingIcon = {
                if (uiState.query.isNotEmpty()) {
                    IconButton(onClick = { viewModel.onQueryChange("") }) {
                        Icon(
                            Icons.Outlined.DeleteOutline,
                            contentDescription = "Clear search",
                            tint = MutedText
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Accent,
                unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                focusedTextColor = PrimaryText,
                unfocusedTextColor = PrimaryText,
                cursorColor = Accent
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        when {
            uiState.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Accent)
                }
            }

            uiState.isFilteringWithoutResults -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                            tint = MutedText,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No matches for \"${uiState.query.trim()}\"", color = PrimaryText, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Try a different title.",
                            color = MutedText,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            uiState.isEmpty -> {
                EmptyHistory()
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.filteredEntries, key = { "${it.mediaType.name.lowercase()}_${it.mediaId}" }) { entry ->
                        HistoryRow(
                            entry = entry,
                            onResume = {
                                val target = viewModel.resumeTarget(entry)
                                navController.navigate(
                                    Screen.Player.createRoute(
                                        mediaId = target.mediaId,
                                        title = target.title,
                                        mediaType = target.mediaType,
                                        season = target.season,
                                        episode = target.episode,
                                        poster = target.posterUrl
                                    )
                                )
                            },
                            onRemove = { viewModel.removeEntry(entry) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(32.dp)) }
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            containerColor = RaisedSurface,
            title = { Text("Clear watch history?", color = PrimaryText) },
            text = {
                Text(
                    "This removes every entry and resets Continue Watching progress.",
                    color = SecondaryText
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearAll()
                    showClearDialog = false
                }) {
                    Text("Clear", color = Accent)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel", color = PrimaryText)
                }
            }
        )
    }
}

@Composable
private fun HistoryRow(
    entry: PlaybackProgress,
    onResume: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CardSurface)
            .clickable(onClick = onResume)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CoilImage(
            url = entry.posterUrl,
            contentDescription = null,
            modifier = Modifier.size(width = 56.dp, height = 84.dp),
            cornerRadius = 8.dp
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.title,
                color = PrimaryText,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = episodeLabel(entry),
                color = MutedText,
                style = MaterialTheme.typography.labelMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { entry.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = Accent,
                trackColor = Color.White.copy(alpha = 0.12f)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "${remainingLabel(entry)} • ${relativeDay(entry.lastPlayedTimestamp)}",
                color = MutedText,
                style = MaterialTheme.typography.labelSmall
            )
        }

        IconButton(onClick = onRemove) {
            Icon(
                Icons.Outlined.DeleteOutline,
                contentDescription = "Remove ${entry.title} from watch history",
                tint = MutedText
            )
        }
    }
}

@Composable
private fun EmptyHistory() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.History,
                contentDescription = null,
                tint = MutedText,
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text("Nothing watched yet", color = PrimaryText, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Titles you play will show up here so you can pick up where you left off.",
                color = MutedText,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun episodeLabel(entry: PlaybackProgress): String {
    return if (entry.mediaType == MediaType.SERIES) {
        val season = entry.seasonNumber ?: 1
        val episode = entry.episodeNumber ?: 1
        "Series • S${season}E${episode}"
    } else {
        "Movie"
    }
}

private fun remainingLabel(entry: PlaybackProgress): String {
    if (entry.isCompleted) return "Finished"
    if (entry.duration <= 0L) return "Not started"
    val minutes = entry.timeRemaining / 60_000
    return when {
        minutes <= 0L -> "Almost done"
        minutes < 60 -> "${minutes}m left"
        else -> "${minutes / 60}h ${minutes % 60}m left"
    }
}

/**
 * Uses java.util calendar rather than java.time: the module does not enable core library
 * desugaring, so java.time is not available on every supported API level.
 */
private fun relativeDay(timestamp: Long): String {
    if (timestamp <= 0L) return "Unknown date"
    val then = Calendar.getInstance().apply { timeInMillis = timestamp }
    val now = Calendar.getInstance()
    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }

    fun isSameDay(a: Calendar, b: Calendar): Boolean =
        a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
            a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)

    return when {
        isSameDay(then, now) -> "Today"
        isSameDay(then, yesterday) -> "Yesterday"
        else -> SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(timestamp))
    }
}
