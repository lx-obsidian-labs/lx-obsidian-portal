package com.example.vista.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.vista.domain.model.MediaItem
import com.example.vista.domain.model.MediaType
import com.example.vista.navigation.Screen
import com.example.vista.ui.components.AdBanner
import com.example.vista.ui.components.BrandAttribution
import com.example.vista.ui.components.CoilImage
import com.example.vista.ui.components.MediaCard
import com.example.vista.ui.components.ShimmerMovieCard
import com.example.vista.ui.theme.*

/**
 * The viewer's own collection: saved titles and watch history in one place.
 *
 * This replaces both the separate Saved tab and the Downloads tab that occupied this slot. The two
 * lists are genuinely different - one is intent to watch, the other is a record of having watched -
 * so they are a filter over one list rather than two lists the viewer has to cross-reference.
 */
@Composable
fun LibraryScreen(
    navController: NavController,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    /**
     * Swipe and the X button both mean "remove", and mean the same thing here.
     *
     * Watchlist wins when a title is in both tables, because unsaving is the cheaper mistake to
     * make and to undo - history is recoverable from the title itself, a watchlist entry is not.
     */
    val onRemoveEntry: (LibraryEntry) -> Unit = { entry ->
        if (entry.inWatchlist) viewModel.removeFromWatchlist(entry)
        else viewModel.removeFromHistory(entry)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(AmbientPurple, Color.Transparent)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Library",
                style = MaterialTheme.typography.headlineMedium,
                color = PrimaryText,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.5).sp
            )
            Text(
                text = "${uiState.watchlistCount} saved  ·  ${uiState.watchedCount} watched",
                style = MaterialTheme.typography.labelMedium,
                color = Accent,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(onClick = { navController.navigate(Screen.Downloads.route) }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.DownloadForOffline, null)
                Spacer(Modifier.width(8.dp))
                Text("Downloads · Watch offline")
            }
            LibraryFilterRow(
                selected = uiState.filter,
                onSelect = viewModel::selectFilter
            )
            Spacer(modifier = Modifier.height(10.dp))
            LibrarySortRow(
                selected = uiState.sort,
                enabled = !uiState.isLoading,
                completedCount = uiState.completedCount,
                onSelect = viewModel::selectSort,
                onClearWatched = viewModel::clearWatched
            )
            Spacer(modifier = Modifier.height(12.dp))

            when {
                uiState.isLoading -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(9) { ShimmerMovieCard() }
                    }
                }

                uiState.error != null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = uiState.error ?: "",
                            color = MutedText,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                uiState.isEmpty -> {
                    LibraryEmptyState(filter = uiState.filter)
                }

                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.entries, key = { it.watchlistKey }) { entry ->
                            SwipeToDismissBox(
                                state = rememberSwipeToDismissBoxState(
                                    // Returning true dismisses the row; the underlying flow then
                                    // removes it and the composable leaves composition, so the state
                                    // never needs resetting here. Returning false for Settled is what
                                    // lets the row spring back when the swipe is abandoned.
                                    confirmValueChange = { value ->
                                        if (value != SwipeToDismissBoxValue.Settled) {
                                            onRemoveEntry(entry)
                                            true
                                        } else {
                                            false
                                        }
                                    }
                                ),
                                backgroundContent = { RemoveSwipeBackground(entry) },
                                // One direction only: right-to-left is the conventional delete swipe,
                                // and allowing both gives the row two different meanings at once.
                                enableDismissFromStartToEnd = false
                            ) {
                                LibraryCard(
                                    entry = entry,
                                    onClick = { navController.navigate(entry.detailsRoute()) },
                                    onResume = entry.resumeRouteOrNull()?.let { route ->
                                        { navController.navigate(route) }
                                    },
                                    onRemove = { onRemoveEntry(entry) }
                                )
                            }
                        }

                        item(span = { GridItemSpan(maxLineSpan) }) {
                            AdBanner(modifier = Modifier.padding(top = 16.dp))
                        }
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            BrandAttribution(modifier = Modifier.padding(top = 24.dp))
                        }
                    }
                }
            }
        }
    }
}

/** Sends a title to the details screen for its own type, never inferred from the id. */
private fun LibraryEntry.detailsRoute(): String = when (mediaType) {
    MediaType.SERIES -> Screen.SeriesDetails.createRoute(id)
    MediaType.MOVIE -> Screen.MovieDetails.createRoute(id)
}

/**
 * Player route for the exact unfinished episode, or null when there is nothing to resume.
 *
 * A finished title deliberately returns null: "Resume" on something already watched would replay it
 * from the beginning while claiming to continue, and the details screen is the better destination
 * for it anyway.
 */
private fun LibraryEntry.resumeRouteOrNull(): String? {
    if (!isResumeable) return null
    return Screen.Player.createRoute(
        mediaId = id,
        title = title,
        mediaType = mediaType,
        season = seasonNumber,
        episode = episodeNumber,
        poster = posterUrl
    )
}

@Composable
private fun LibrarySortRow(
    selected: LibrarySort,
    enabled: Boolean,
    completedCount: Int,
    onSelect: (LibrarySort) -> Unit,
    onClearWatched: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LibrarySort.entries.forEach { option ->
            val active = option == selected
            TextButton(
                onClick = { onSelect(option) },
                enabled = enabled,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = if (active) Accent else MutedText
                )
            ) {
                Text(
                    text = option.label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Only rendered when it has work to do, so the row does not carry a permanently dead button.
        if (completedCount > 0) {
            TextButton(
                onClick = onClearWatched,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                colors = ButtonDefaults.textButtonColors(contentColor = MutedText)
            ) {
                Text(
                    text = "Clear watched",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

/** Revealed behind a row as it is swiped away. */
@Composable
private fun RemoveSwipeBackground(entry: LibraryEntry) {
    val unsaving = entry.inWatchlist
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(ErrorAccent.copy(alpha = 0.18f)),
        contentAlignment = Alignment.CenterEnd
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(end = 20.dp)
        ) {
            Text(
                text = if (unsaving) "Unsave" else "Remove",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = PrimaryText
            )
            Icon(
                imageVector = if (unsaving) Icons.Outlined.BookmarkRemove else Icons.Outlined.Delete,
                contentDescription = null,
                tint = PrimaryText,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun LibraryFilterRow(
    selected: LibraryFilter,
    onSelect: (LibraryFilter) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LibraryFilter.entries.forEach { filter ->
            val active = filter == selected
            Surface(
                onClick = { onSelect(filter) },
                color = if (active) Accent else Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(
                    text = filter.label,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                    color = if (active) Color.Black else PrimaryText
                )
            }
        }
    }
}

@Composable
private fun LibraryEmptyState(filter: LibraryFilter) {
    // Copy names the filter, because "your library is empty" is wrong and unhelpful when the
    // viewer has a full watchlist and has simply filtered to Watched.
    val (icon, title, body) = when (filter) {
        LibraryFilter.ALL ->
            Triple(
                Icons.Outlined.VideoLibrary,
                "Nothing here yet",
                "Save something to watch later, or start watching to build your history"
            )
        LibraryFilter.WATCHLIST ->
            Triple(
                Icons.Outlined.BookmarkBorder,
                "Your list is empty",
                "Add movies and shows to watch later"
            )
        LibraryFilter.WATCHED ->
            Triple(
                Icons.Outlined.History,
                "No watch history",
                "Titles you start watching show up here"
            )
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MutedText,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                color = PrimaryText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = body,
                color = MutedText,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

/**
 * Poster with a resume bar and status chips.
 *
 * Hand-rolled rather than reusing [MediaCard] because that takes a MediaItem, and the library row
 * is a merge of two tables that no single MediaItem can express - it needs both "saved" and the
 * resume position at once.
 */
@Composable
private fun LibraryCard(
    entry: LibraryEntry,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    onResume: (() -> Unit)? = null
) {
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.67f)
                .clip(RoundedCornerShape(12.dp))
                .background(RaisedSurface)
                .clickable(onClick = onClick)
        ) {
            CoilImage(
                url = entry.posterUrl,
                contentDescription = entry.title,
                modifier = Modifier.fillMaxSize()
            )

            if (onResume != null) {
                // Resume sits on its own button rather than being folded into the card tap. Tapping
                // the card going to details is the safe default; making the whole card jump into the
                // player would remove the one-tap route to cast info, seasons and the cast list.
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Accent)
                        .clickable(onClick = onResume),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PlayArrow,
                        contentDescription = "Resume",
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            if (entry.isResumeable) {
                // Resume position, so a partially-watched title is visibly different from an
                // untouched one - the reason this list beats the old plain watchlist.
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(Color.White.copy(alpha = 0.25f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(entry.progressFraction)
                            .fillMaxHeight()
                            .background(Accent)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (entry.inWatchlist) {
                    Icon(
                        imageVector = Icons.Outlined.Bookmark,
                        contentDescription = "Saved",
                        tint = Accent,
                        modifier = Modifier
                            .size(20.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.55f))
                            .padding(3.dp)
                    )
                }
                if (entry.isCompleted) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = "Watched",
                        tint = PrimaryText,
                        modifier = Modifier
                            .size(20.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.55f))
                            .padding(3.dp)
                    )
                }
            }

            IconButton(
                onClick = onRemove,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(24.dp)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            ) {
                Icon(
                    imageVector = if (entry.inWatchlist) Icons.Outlined.BookmarkRemove else Icons.Outlined.Delete,
                    contentDescription = if (entry.inWatchlist) "Remove from list" else "Remove from history",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = entry.title,
            style = MaterialTheme.typography.labelMedium,
            color = PrimaryText,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = subtitleFor(entry),
            style = MaterialTheme.typography.labelSmall,
            color = MutedText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun subtitleFor(entry: LibraryEntry): String = when {
    entry.isResumeable && entry.mediaType == MediaType.SERIES ->
        "S${entry.seasonNumber ?: 1} E${entry.episodeNumber ?: 1}"
    entry.isResumeable -> "${(entry.progressFraction * 100).toInt()}% watched"
    entry.isCompleted -> "Watched"
    else -> if (entry.mediaType == MediaType.SERIES) "Series" else "Movie"
}
