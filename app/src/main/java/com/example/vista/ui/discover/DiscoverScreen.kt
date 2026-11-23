package com.example.vista.ui.discover

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import com.example.vista.ui.components.AdBanner
import com.example.vista.ui.components.PullToRefreshWrapper
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.vista.domain.model.Genre
import com.example.vista.domain.model.MediaItem
import com.example.vista.domain.model.watchlistKey
import com.example.vista.ui.components.BrandAttribution
import com.example.vista.ui.components.CoilImage
import com.example.vista.ui.components.MediaCard
import com.example.vista.ui.components.shimmerEffect
import com.example.vista.navigation.Screen
import com.example.vista.ui.theme.*
import com.example.vista.util.ViewUtils
import androidx.compose.runtime.snapshotFlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(
    navController: NavController,
    viewModel: DiscoverViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val watchlistKeys by viewModel.watchlistKeys.collectAsStateWithLifecycle()
    val lazyGridState = rememberLazyGridState()

    // Ambient Cinematic Background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        // Subtle ambient glow at the top
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(AmbientRed, Color.Transparent)
                    )
                )
        )

        Column(modifier = Modifier.fillMaxSize()) {
            DiscoverHeader(
                selectedTab = uiState.selectedTab,
                onTabSelected = { viewModel.selectTab(it) }
            )

            com.example.vista.artwork.VistaArtworkBanner(
                artwork = com.example.vista.artwork.VistaArtwork.DISCOVER,
                heading = "Find your next obsession", subtitle = "Explore stories beyond your usual favourites.",
                actionLabel = "Search stories", onClick = { navController.navigate(Screen.Search.route) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Genres - 2026 Floating Chips
            if (uiState.genres.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(uiState.genres, key = { it.id }) { genre ->
                        val isSelected = uiState.selectedGenre?.id == genre.id
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                viewModel.selectGenre(
                                    if (isSelected) null else genre
                                )
                            },
                            label = { Text(genre.name) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = if (isSelected) Accent.copy(alpha = 0.2f) else CardSurface.copy(alpha = 0.6f),
                                labelColor = if (isSelected) Accent else SecondaryText,
                                selectedContainerColor = Accent.copy(alpha = 0.3f),
                                selectedLabelColor = Accent
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = Color.Transparent,
                                selectedBorderColor = Accent.copy(alpha = 0.5f),
                                borderWidth = 1.dp,
                                selectedBorderWidth = 1.dp
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            PullToRefreshWrapper(
                isRefreshing = uiState.isRefreshing,
                onRefresh = { viewModel.refresh() },
                modifier = Modifier.fillMaxSize()
            ) {
                when {
                    uiState.isLoading && uiState.items.isEmpty() -> {
                        DiscoverLoadingGrid()
                    }
                    uiState.error != null && uiState.items.isEmpty() -> {
                        DiscoverErrorContent(
                            message = uiState.error ?: "Something went wrong",
                            onRetry = { viewModel.retry() }
                        )
                    }
                    uiState.items.isEmpty() && !uiState.isLoading -> {
                        DiscoverEmptyContent(
                            message = "No ${uiState.selectedTab.displayName.lowercase()} found",
                            icon = when (uiState.selectedTab) {
                                DiscoverTab.ANIME -> Icons.Outlined.Animation
                                DiscoverTab.ACTION -> Icons.Outlined.FlashOn
                                DiscoverTab.COMEDY -> Icons.Outlined.SentimentSatisfied
                                DiscoverTab.HORROR -> Icons.Outlined.BugReport
                                DiscoverTab.SCIFI -> Icons.Outlined.Rocket
                                DiscoverTab.DRAMA -> Icons.Outlined.TheaterComedy
                                DiscoverTab.THRILLER -> Icons.Outlined.Warning
                                DiscoverTab.ROMANCE -> Icons.Outlined.Favorite
                                DiscoverTab.DOCUMENTARIES -> Icons.Outlined.Info
                                else -> Icons.Outlined.Search
                            }
                        )
                    }
                    else -> {
                        LazyVerticalGrid(
                            state = lazyGridState,
                            columns = GridCells.Fixed(3),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp, top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(uiState.items, key = { "${it.id}_${it.title}" }) { item ->
                                MediaCard(
                                    item = item,
                                    titleOverlay = true,
                                    isInWatchlist = watchlistKeys.contains(item.watchlistKey()),
                                    onDoubleTap = { viewModel.toggleWatchlist(item) },
                                    onClick = {
                                        val route = when (item) {
                                            is MediaItem.MovieItem -> Screen.MovieDetails.createRoute(item.id)
                                            is MediaItem.SeriesItem -> Screen.SeriesDetails.createRoute(item.id)
                                            else -> null
                                        }
                                        route?.let { navController.navigate(it) }
                                    }
                                )
                            }
                            
                            if (uiState.hasMore) {
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    DiscoverLoadingFooter(isLoading = uiState.isLoadingMore)
                                }
                            }

                            // 2026 Native Ad Placement
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                AdBanner(
                                    modifier = Modifier
                                        .padding(vertical = 16.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(CardSurface.copy(alpha = 0.4f))
                                )
                            }
                            
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                BrandAttribution()
                            }
                        }

                        // 2026 Predictive Infinite Scroll
                        val shouldLoadMore = remember {
                            derivedStateOf {
                                val lastVisibleItem = lazyGridState.layoutInfo.visibleItemsInfo.lastOrNull()
                                    ?: return@derivedStateOf false
                                // Trigger much earlier for zero-latency feel
                                lastVisibleItem.index >= lazyGridState.layoutInfo.totalItemsCount - 12
                            }
                        }

                        LaunchedEffect(shouldLoadMore.value) {
                            if (shouldLoadMore.value && uiState.hasMore && !uiState.isLoadingMore && !uiState.isLoading) {
                                viewModel.loadMore()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DiscoverHeader(
    selectedTab: DiscoverTab,
    onTabSelected: (DiscoverTab) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(
            text = "Discover",
            style = MaterialTheme.typography.headlineMedium,
            color = PrimaryText,
            fontWeight = FontWeight.Black,
            letterSpacing = (-0.5).sp
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        // Glassmorphic Tabs - Scrollable to avoid wrapping
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CardSurface.copy(alpha = 0.4f))
                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)), RoundedCornerShape(16.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(DiscoverTab.entries.toList()) { tab ->
                val isSelected = selectedTab == tab
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) Accent else Color.Transparent)
                        .clickable { onTabSelected(tab) }
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tab.displayName,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isSelected) PrimaryText else MutedText,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun DiscoverLoadingFooter(isLoading: Boolean) {
    if (isLoading) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(32.dp),
                color = Accent,
                strokeWidth = 3.dp
            )
        }
    }
}

@Composable
private fun DiscoverLoadingGrid() {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(12) {
            DiscoverShimmerCell()
        }
    }
}

/** Loading placeholder that fills its grid cell, matching the real [MediaCard] proportions. */
@Composable
private fun DiscoverShimmerCell() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(16.dp))
                .shimmerEffect()
        )
        Spacer(modifier = Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(14.dp)
                .clip(RoundedCornerShape(4.dp))
                .shimmerEffect()
        )
    }
}

@Composable
private fun DiscoverErrorContent(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.ErrorOutline,
                contentDescription = null,
                tint = MutedText,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                ViewUtils.formatError(message),
                color = PrimaryText,
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = Accent)
            ) {
                Text("Retry", color = PrimaryText)
            }
        }
    }
}@Composable
private fun DiscoverEmptyContent(
    message: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Outlined.Search
) {
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
                message,
                color = PrimaryText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Try a different category or check back later",
                color = MutedText,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
