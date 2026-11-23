package com.example.vista.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.example.vista.domain.model.MediaItem
import com.example.vista.domain.model.watchlistKey
import com.example.vista.motd.FestiveSeason
import com.example.vista.motd.SeasonalMovie
import com.example.vista.ui.theme.*
import kotlinx.coroutines.delay
import java.time.LocalDate
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun FestiveHomeSection(
    hiddenSeason: Int,
    savedKeys: Set<String>,
    onHide: (Int) -> Unit,
    onOpen: (String) -> Unit,
    onSave: (MediaItem) -> Unit
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var date by remember { mutableStateOf(LocalDate.now()) }
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) { date = LocalDate.now(); delay(60_000) }
        }
    }
    val moment = FestiveSeason.moment(date) ?: return
    if (moment.seasonYear == hiddenSeason) return
    var selected by rememberSaveable { mutableIntStateOf(0) }
    val collection = FestiveSeason.collections[selected.coerceIn(FestiveSeason.collections.indices)]
    val gold = Color(0xFFFFD180)
    val pick = FestiveSeason.dailyPick(date)
    var showCollection by rememberSaveable { mutableStateOf(false) }
    Surface(shape = RoundedCornerShape(20.dp), color = CardSurface, modifier = Modifier.padding(16.dp)) {
        Column(Modifier.padding(bottom = 16.dp)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = { onHide(moment.seasonYear) }) {
                    Icon(Icons.Outlined.Close, "Hide festive section for this season", tint = PrimaryText)
                }
            }
            com.example.vista.artwork.VistaArtworkBanner(
                artwork = com.example.vista.artwork.VistaArtwork.CHRISTMAS,
                heading = moment.heading, subtitle = moment.countdown,
                description = "Good company. Great stories. Your kind of festive season.",
                actionLabel = "Explore Festive Movies", onClick = { showCollection = true },
                contentAlignment = Alignment.End, showFallback = true
            )
            TextButton(onClick = { onOpen(pick.tmdbId.toString()) }, modifier = Modifier.padding(horizontal = 8.dp)) {
                Icon(Icons.Outlined.AutoAwesome, null, tint = gold, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Today's festive pick: ${pick.title}", color = gold)
            }
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(FestiveSeason.collections.size) { index ->
                    FilterChip(selected = selected == index, onClick = { selected = index },
                        label = { Text(FestiveSeason.collections[index].title) })
                }
            }
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(collection.movies, key = { it.tmdbId }) { movie ->
                    val item = movie.asItem()
                    Surface(onClick = { onOpen(item.id) }, shape = RoundedCornerShape(12.dp), color = Background.copy(alpha = 0.65f),
                        modifier = Modifier.width(210.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            Text(movie.title, color = PrimaryText, fontWeight = FontWeight.SemiBold, minLines = 2)
                            Text(movie.year.toString(), color = gold, style = MaterialTheme.typography.labelSmall)
                            Text(movie.blurb, color = SecondaryText, style = MaterialTheme.typography.bodySmall,
                                minLines = 3, maxLines = 3, modifier = Modifier.padding(top = 8.dp))
                            TextButton(onClick = { onSave(item) }) {
                                val saved = item.watchlistKey() in savedKeys
                                Icon(if (saved) Icons.Outlined.Check else Icons.Outlined.Add, null, modifier = Modifier.size(18.dp))
                                Text(if (saved) "Saved" else "Save for later")
                            }
                        }
                    }
                }
            }
            Text("Check title details for ratings and playback availability.", color = SecondaryText,
                style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp))
        }
    }
    if (showCollection) FestiveCollectionDialog(onDismiss = { showCollection = false },
        onOpen = { showCollection = false; onOpen(it) })
}

@Composable
fun FestiveCollectionDialog(onDismiss: () -> Unit, onOpen: (String) -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(color = CardSurface, shape = RoundedCornerShape(24.dp),
            modifier = Modifier.padding(20.dp).widthIn(max = 800.dp).fillMaxWidth().heightIn(max = 650.dp)) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Your festive movie nights", color = PrimaryText, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                if (com.example.vista.artwork.rememberArtworkResource(com.example.vista.artwork.VistaArtwork.CHRISTMAS_ALT) != null) {
                    com.example.vista.artwork.VistaHeroBackground(com.example.vista.artwork.VistaArtwork.CHRISTMAS_ALT,
                        Modifier.fillMaxWidth().height(180.dp))
                }
                Text("Explore a collection, then open a title to check its details and playback availability.", color = SecondaryText)
                FestiveSeason.collections.forEach { collection ->
                    Text(collection.title, color = PrimaryText, fontWeight = FontWeight.Bold)
                    collection.movies.forEach { movie ->
                        OutlinedButton(onClick = { onOpen(movie.tmdbId.toString()) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                            Text("${movie.title} · ${movie.year}", color = PrimaryText)
                        }
                    }
                }
                TextButton(onClick = onDismiss) { Text("Close", color = PrimaryText) }
            }
        }
    }
}

private fun SeasonalMovie.asItem() = MediaItem.MovieItem(tmdbId.toString(), title, null, null, year, null, null)
