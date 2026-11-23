package com.example.vista.ui.search

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.vista.domain.model.Movie
import com.example.vista.domain.model.toMediaItem
import com.example.vista.domain.model.watchlistKey
import com.example.vista.ui.components.MediaCard
import com.example.vista.domain.model.SearchType
import com.example.vista.domain.model.Series
import com.example.vista.navigation.Screen
import com.example.vista.ui.theme.*
import com.example.vista.util.ViewUtils
import androidx.compose.runtime.snapshotFlow

@Composable
fun SearchScreen(
    navController: NavController,
    viewModel: SearchViewModel = hiltViewModel(),
    initialQuery: String = ""
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val watchlistKeys by viewModel.watchlistKeys.collectAsStateWithLifecycle()
    val lazyGridState = rememberLazyGridState()
    LaunchedEffect(initialQuery) {
        if (initialQuery.isNotBlank()) viewModel.onQueryChange(initialQuery)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        // Search bar - Elevated
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = CardSurface.copy(alpha = 0.5f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = uiState.query,
                    onValueChange = { viewModel.onQueryChange(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Movies, series, actors...", color = MutedText) },
                    leadingIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = PrimaryText)
                        }
                    },
                    trailingIcon = {
                        if (uiState.query.isNotEmpty()) {
                            IconButton(onClick = { viewModel.clearQuery() }) {
                                Icon(Icons.Outlined.Close, contentDescription = "Clear", tint = PrimaryText)
                            }
                        } else {
                            Icon(Icons.Outlined.Search, null, tint = MutedText, modifier = Modifier.padding(end = 8.dp))
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Accent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = RaisedSurface,
                        unfocusedContainerColor = RaisedSurface,
                        cursorColor = Accent,
                        focusedTextColor = PrimaryText,
                        unfocusedTextColor = PrimaryText
                    ),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Tabs - Modern Underline Style
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SearchType.entries.forEach { type ->
                        val isSelected = uiState.selectedTab == type
                        Column(
                            modifier = Modifier
                                .clickable(
                                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                    indication = null,
                                    onClick = { viewModel.selectTab(type) }
                                )
                                .padding(vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = type.name.lowercase().replaceFirstChar { it.uppercase() },
                                color = if (isSelected) Accent else MutedText,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            androidx.compose.animation.AnimatedVisibility(
                                visible = isSelected,
                                enter = fadeIn() + expandHorizontally(),
                                exit = fadeOut() + shrinkHorizontally()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp, 2.dp)
                                        .background(Accent, RoundedCornerShape(2.dp))
                                )
                            }
                        }
                    }
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            when {
                uiState.isSearching -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Accent)
                    }
                }
                uiState.query.isNotEmpty() && (uiState.movies.isNotEmpty() || uiState.series.isNotEmpty() || uiState.people.isNotEmpty()) -> {
                    val hasMovies = uiState.movies.isNotEmpty()
                    val hasSeries = uiState.series.isNotEmpty()
                    val hasPeople = uiState.people.isNotEmpty()

                    if (!hasMovies && !hasSeries && !hasPeople) {
                        SearchEmptyContent()
                    } else {
                        LazyVerticalGrid(
                            state = lazyGridState,
                            columns = GridCells.Fixed(3),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 64.dp)
                        ) {
                            if (hasMovies) {
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    Text(
                                        "Movies",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = PrimaryText,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }
                                items(uiState.movies, key = { "movie_${it.id}" }) { movie ->
                                    MediaCard(
                                        item = movie.toMediaItem(),
                                        onClick = { navController.navigate(Screen.MovieDetails.createRoute(movie.id)) },
                                        titleOverlay = true,
                                        isInWatchlist = watchlistKeys.contains(movie.toMediaItem().watchlistKey()),
                                        onDoubleTap = { viewModel.toggleWatchlist(movie.toMediaItem()) }
                                    )
                                }
                            }
                            
                            if (hasSeries) {
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    Text(
                                        "TV Shows",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = PrimaryText,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                                    )
                                }
                                items(uiState.series, key = { "series_${it.id}" }) { series ->
                                    MediaCard(
                                        item = series.toMediaItem(),
                                        onClick = { navController.navigate(Screen.SeriesDetails.createRoute(series.id)) },
                                        titleOverlay = true,
                                        isInWatchlist = watchlistKeys.contains(series.toMediaItem().watchlistKey()),
                                        onDoubleTap = { viewModel.toggleWatchlist(series.toMediaItem()) }
                                    )
                                }
                            }
                            if (hasPeople) {
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    Text(
                                        "People",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = PrimaryText,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                                    )
                                }
                                items(uiState.people, key = { "person_${it.id}" }) { person ->
                                    PersonResultCard(
                                        person = person,
                                        onClick = {
                                            navController.navigate(Screen.CastDetails.createRoute(person.id))
                                        }
                                    )
                                }
                            }

                            // Loading footer for pagination
                            if (uiState.hasMore) {
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    SearchLoadingFooter(isLoading = uiState.isLoadingMore)
                                }
                            }
                        }

                        // Infinite scroll trigger
                        LaunchedEffect(lazyGridState) {
                            snapshotFlow { lazyGridState.layoutInfo.visibleItemsInfo }
                                .collect { visibleItems ->
                                    val lastVisible = visibleItems.lastOrNull()
                                    val totalItems = lazyGridState.layoutInfo.totalItemsCount
                                    if (lastVisible != null && totalItems > 0) {
                                        if (lastVisible.index >= totalItems - 6) {
                                            viewModel.loadMore()
                                        }
                                    }
                                }
                        }
                    }
                }
                uiState.query.isNotEmpty() && uiState.error != null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(ViewUtils.formatError(uiState.error), color = PrimaryText, style = MaterialTheme.typography.bodyLarge)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.search(uiState.query) },
                                colors = ButtonDefaults.buttonColors(containerColor = Accent)
                            ) {
                                Text("Retry", color = PrimaryText)
                            }
                        }
                    }
                }
                uiState.query.isNotEmpty() -> {
                    SearchEmptyContent()
                }
                else -> {
                    // Recent and trending searches
                    Column(modifier = Modifier.padding(top = 24.dp)) {
                        if (uiState.recentSearches.isNotEmpty()) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Recent Searches", style = MaterialTheme.typography.titleMedium, color = PrimaryText, fontWeight = FontWeight.Bold)
                                Text("Clear", style = MaterialTheme.typography.labelLarge, color = Accent, modifier = Modifier.clickable { viewModel.clearRecentSearches() })
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(uiState.recentSearches) { search ->
                                    Surface(
                                        onClick = { viewModel.search(search) },
                                        color = RaisedSurface,
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(search, color = SecondaryText, modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp), style = MaterialTheme.typography.labelLarge)
                                            IconButton(
                                                onClick = { viewModel.removeRecentSearch(search) },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    Icons.Outlined.Close,
                                                    contentDescription = "Remove \"$search\" from search history",
                                                    tint = MutedText,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(32.dp))
                        }

                        if (uiState.trendingSearches.isNotEmpty()) {
                            Text("Trending Now", style = MaterialTheme.typography.titleMedium, color = PrimaryText, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(16.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                uiState.trendingSearches.take(5).forEachIndexed { index, search ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { viewModel.search(search) }
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${index + 1}",
                                            style = MaterialTheme.typography.titleLarge,
                                            color = Accent,
                                            fontWeight = FontWeight.Black,
                                            modifier = Modifier.width(32.dp)
                                        )
                                        Text(search, color = PrimaryText, style = MaterialTheme.typography.bodyLarge)
                                        Spacer(modifier = Modifier.weight(1f))
                                        Icon(Icons.Outlined.TrendingUp, null, tint = MutedText, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PersonResultCard(
    person: com.example.vista.domain.model.Person,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        com.example.vista.ui.components.CoilImage(
            url = person.profileUrl,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            contentScale = ContentScale.Crop,
            cornerRadius = 28.dp
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = person.name,
                color = PrimaryText,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            person.department?.let {
                Text(
                    text = it,
                    color = MutedText,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Composable
fun SearchLoadingFooter(isLoading: Boolean) {
    if (isLoading) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = Accent,
                strokeWidth = 2.dp
            )
        }
    }
}

@Composable
fun SearchEmptyContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                tint = MutedText,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text("No results found", color = MutedText, style = MaterialTheme.typography.bodyLarge)
            Text("Try a different search term", color = MutedText, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
