package com.example.vista.ui.tv

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.*
import androidx.tv.material3.SurfaceDefaults
import com.example.vista.domain.model.HomeSection
import com.example.vista.domain.model.MediaItem
import com.example.vista.domain.model.watchlistKey
import com.example.vista.ui.home.HomeViewModel
import com.example.vista.ui.theme.Accent
import com.example.vista.ui.theme.Background
import com.example.vista.ui.theme.PrimaryText

/**
 * How much of the row above stays visible when a row is brought into view.
 *
 * Without it the incoming row lands flush against the top edge, and its title sits under the edge of
 * the screen on overscan TVs where the border is cropped.
 */
private val RowPeekDp: androidx.compose.ui.unit.Dp = 72.dp

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvHomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onMediaClick: (MediaItem) -> Unit,
    onSearchClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // Negative, so the target row stops short of the top edge and the row above stays partly
    // visible. A positive offset would scroll past it and push its own title under the bezel.
    val peekOffsetPx = with(LocalDensity.current) { -RowPeekDp.toPx() }.toInt()

    when {
        uiState.isLoading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Background),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.CircularProgressIndicator(color = com.example.vista.ui.theme.Accent)
            }
        }
        uiState.error != null && uiState.sections.isEmpty() -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Background),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = uiState.error ?: "Failed to load content",
                        color = com.example.vista.ui.theme.MutedText,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = { viewModel.retry() }) {
                        Text("Retry")
                    }
                }
            }
        }
        else -> {
            // Row-by-row paging. Free scrolling on a television means rows stop at arbitrary
            // offsets and the viewer loses track of where they are; this advances a whole row at a
            // time, driven by which row currently holds focus.
            val listState = rememberLazyListState()
            var focusedSection by remember { mutableStateOf(-1) }

            // Empty sections are dropped before indexing, not filtered inside the item block. The
            // vertical paging below maps a section's position to a LazyColumn item index, so the two
            // lists have to agree on how many rows there are - filtering inside the block would make
            // every row after an empty one scroll to the wrong place, and eventually past the end.
            val visibleSections = remember(uiState.sections) {
                uiState.sections.filter { it.items.isNotEmpty() }
            }

            // LazyColumn item 0 is the header row; the hero occupies the next slot only when there is
            // something to show, so section indices have to be offset by both.
            val headerCount = 1
            val heroCount = if (uiState.featuredMovies.isNotEmpty()) 1 else 0

            LaunchedEffect(focusedSection) {
                if (focusedSection < 0) return@LaunchedEffect
                val target = headerCount + heroCount + focusedSection
                if (listState.layoutInfo.visibleItemsInfo.none { it.index == target }) {
                    listState.animateScrollToItem(target, peekOffsetPx)
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Background),
                contentPadding = PaddingValues(bottom = 64.dp)
            ) {
                // Header: brand + search entry. Search is a destination on TV (voice-first),
                // so it lives here rather than as a phone-style icon in a corner.
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 64.dp, vertical = 24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "VISTA",
                            style = MaterialTheme.typography.headlineLarge,
                            color = com.example.vista.ui.theme.Accent
                        )
                        Button(onClick = onSearchClick) {
                            Text("Search")
                        }
                    }
                }

                // Featured Section
                if (uiState.featuredMovies.isNotEmpty()) {
                    item {
                        val movie = uiState.featuredMovies.first()
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(400.dp)
                                .clickable { onMediaClick(movie) }
                        ) {
                            com.example.vista.ui.components.CoilImage(
                                url = movie.posterUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize()
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            0f to Color.Black.copy(alpha = 0.4f),
                                            0.4f to Color.Transparent,
                                            1f to Background
                                        )
                                    )
                            )

                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(horizontal = 64.dp, vertical = 48.dp)
                            ) {
                                Text(
                                    text = movie.title,
                                    style = MaterialTheme.typography.displayLarge,
                                    color = PrimaryText
                                )
                            }
                        }
                    }
                }

                // Sections
                itemsIndexed(visibleSections, key = { _, section -> section.title }) { index, section ->
                    TvSection(
                        section = section,
                        onMediaClick = onMediaClick,
                        onSectionFocused = { focusedSection = index }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvSection(
    section: HomeSection,
    onMediaClick: (MediaItem) -> Unit,
    onSectionFocused: () -> Unit = {}
) {
    val scroller = rememberTvRowScroller(section.items.size)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        Text(
            text = section.title,
            style = MaterialTheme.typography.headlineMedium,
            color = PrimaryText,
            modifier = Modifier.padding(horizontal = 48.dp, vertical = 8.dp)
        )

        LazyRow(
            state = scroller.state,
            contentPadding = PaddingValues(horizontal = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            itemsIndexed(section.items, key = { _, item -> item.watchlistKey() }) { index, item ->
                TvMediaCard(
                    item = item,
                    onClick = { onMediaClick(item) },
                    onFocused = {
                        // Both scrollers move together: this row slides the focused card into its
                        // resting position, and the parent column advances the row itself.
                        scroller.onItemFocused(index)
                        onSectionFocused()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvMediaCard(
    item: MediaItem,
    onClick: () -> Unit,
    onFocused: () -> Unit = {}
) {
    var focused by remember { mutableStateOf(false) }
    val scale = tvFocusScale(focused)
    val borderColor by animateColorAsState(
        targetValue = if (focused) Accent else Color.Transparent,
        label = "tvFocusBorder"
    )

    Surface(
        onClick = onClick,
        modifier = Modifier
            .width(180.dp)
            .aspectRatio(0.67f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .onFocusChanged { state ->
                if (state.isFocused && !focused) {
                    focused = true
                    onFocused()
                } else if (!state.isFocused) {
                    focused = false
                }
            }
            .border(2.dp, borderColor, RoundedCornerShape(6.dp))
    ) {
        com.example.vista.ui.components.CoilImage(
            url = item.posterUrl,
            contentDescription = item.title,
            modifier = Modifier.fillMaxSize()
        )
    }
}
