package com.example.vista.ui.seeall

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.vista.domain.model.MediaItem
import com.example.vista.domain.model.watchlistKey
import com.example.vista.ui.components.MediaCard
import com.example.vista.navigation.Screen
import com.example.vista.ui.theme.*
import com.example.vista.util.ViewUtils
import androidx.compose.runtime.snapshotFlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeeAllScreen(
    navController: NavController,
    viewModel: SeeAllViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val watchlistKeys by viewModel.watchlistKeys.collectAsStateWithLifecycle()
    val lazyListState = rememberLazyGridState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        uiState.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryText
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back",
                            tint = PrimaryText
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Background
                )
            )
        },
        containerColor = Background
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (uiState.isLoading && uiState.items.isEmpty()) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(104.dp),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(12) {
                        ShimmerGridItem()
                    }
                }
            } else if (uiState.error != null && uiState.items.isEmpty()) {
                SeeAllErrorContent(
                    message = ViewUtils.formatError(uiState.error),
                    onRetry = { viewModel.retry() }
                )
            } else if (uiState.items.isEmpty() && !uiState.isLoading) {
                SeeAllEmptyContent(onBack = { navController.popBackStack() })
            } else {
                LazyVerticalGrid(
                    state = lazyListState,
                    columns = GridCells.Adaptive(104.dp),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.items, key = { it.watchlistKey() }) { item ->
                        MediaCard(
                            item = item,
                            isInWatchlist = watchlistKeys.contains(item.watchlistKey()),
                            onDoubleTap = { viewModel.toggleWatchlist(item) },
                            onClick = {
                                val route = when (item) {
                                    is MediaItem.MovieItem -> Screen.MovieDetails.createRoute(item.id)
                                    is MediaItem.SeriesItem -> Screen.SeriesDetails.createRoute(item.id)
                                    else -> ""
                                }
                                if (route.isNotEmpty()) navController.navigate(route)
                            }
                        )
                    }
                    if (uiState.error != null) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("Couldn't load more titles", color = PrimaryText, style = MaterialTheme.typography.titleSmall)
                                Text(uiState.error.orEmpty(), color = SecondaryText,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                OutlinedButton(onClick = viewModel::retryLoadMore) { Text("Try again", color = PrimaryText) }
                            }
                        }
                    } else if (uiState.hasMore) {
                        item(
                            span = { GridItemSpan(maxLineSpan) }
                        ) {
                            SeeAllLoadingFooter(isLoading = uiState.isLoadingMore)
                        }
                    }
                }

                // Infinite scroll trigger
                LaunchedEffect(lazyListState, uiState.error) {
                    snapshotFlow { lazyListState.layoutInfo.visibleItemsInfo }
                        .collect { visibleItems ->
                            val lastVisible = visibleItems.lastOrNull()
                            val totalItems = lazyListState.layoutInfo.totalItemsCount
                            if (uiState.error == null && lastVisible != null && totalItems > 0) {
                                if (lastVisible.index >= totalItems - 6) {
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
private fun ShimmerGridItem() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.67f)
            .clip(RoundedCornerShape(8.dp))
            .background(CardSurface)
    )
}

@Composable
private fun SeeAllLoadingFooter(isLoading: Boolean) {
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
private fun SeeAllErrorContent(
    message: String,
    onRetry: () -> Unit
) {
    com.example.vista.ui.components.RecoveryState(
        title = "Titles couldn't load", message = message, icon = Icons.Outlined.CloudOff,
        primaryLabel = "Try again", onPrimary = onRetry
    )
}

@Composable
private fun SeeAllEmptyContent(onBack: () -> Unit) {
    com.example.vista.ui.components.RecoveryState(
        title = "More stories are on the way",
        message = "There are no titles in this collection right now. Explore another collection to find your next watch.",
        icon = Icons.Outlined.Movie, primaryLabel = "Back to browsing", onPrimary = onBack
    )
}
