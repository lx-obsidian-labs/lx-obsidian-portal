package com.example.vista.ui.tv

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.zIndex
import androidx.activity.compose.BackHandler
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.*
import com.example.vista.domain.model.HomeSection
import com.example.vista.domain.model.MediaItem
import com.example.vista.domain.model.watchlistKey
import com.example.vista.ui.home.HomeViewModel
import com.example.vista.ui.theme.Accent
import com.example.vista.ui.theme.Background
import com.example.vista.ui.theme.PrimaryText

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvHomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onMediaClick: (MediaItem) -> Unit,
    onSearchClick: () -> Unit = {},
    onSupportClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchFocus = remember { FocusRequester() }
    val avatarFocus = remember { FocusRequester() }
    var lastFocusedCard by rememberSaveable { mutableStateOf<String?>(null) }
    var showAvatarPicker by rememberSaveable { mutableStateOf(false) }
    var showMovieCollection by rememberSaveable { mutableStateOf(false) }
    var showFestiveCollection by rememberSaveable { mutableStateOf(false) }
    val campaignDate = com.example.vista.artwork.rememberCampaignDate()
    val festive = com.example.vista.motd.FestiveSeason.moment(campaignDate)
    val hiddenFestive by viewModel.hiddenFestiveSeason.collectAsStateWithLifecycle()
    val movieItems = uiState.sections.flatMap { it.items }.filterIsInstance<MediaItem.MovieItem>().distinctBy { it.watchlistKey() }

    if (showAvatarPicker) com.example.vista.artwork.VistaAvatarPicker(onDismiss = { showAvatarPicker = false })
    if (showMovieCollection) com.example.vista.artwork.TvArtworkCollection(movieItems,
        onDismiss = { showMovieCollection = false }, onOpen = { showMovieCollection = false; onMediaClick(it) })
    if (showFestiveCollection) com.example.vista.ui.components.FestiveCollectionDialog(
        onDismiss = { showFestiveCollection = false },
        onOpen = { id ->
            showFestiveCollection = false
            val pick = com.example.vista.motd.FestiveSeason.collections.flatMap { it.movies }.first { it.tmdbId.toString() == id }
            onMediaClick(MediaItem.MovieItem(id, pick.title, null, null, pick.year, null, null))
        })

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
            // Foundation owns focus-driven scrolling under TvFocusScrolling's shared pivot.
            val listState = rememberLazyListState()
            val navigationScope = rememberCoroutineScope()
            val canReturnToTop by remember { derivedStateOf { listState.canScrollBackward } }
            BackHandler(enabled = canReturnToTop && !showAvatarPicker && !showMovieCollection && !showFestiveCollection) {
                lastFocusedCard = "search"
                navigationScope.launch {
                    listState.scrollToItem(0)
                    withFrameNanos { }
                    searchFocus.requestFocus()
                }
            }

            // Stable section keys keep row state attached to the right content after refresh.
            val visibleSections = remember(uiState.sections) {
                uiState.sections.filter { it.items.isNotEmpty() }
            }

            LaunchedEffect(visibleSections.map { section -> section.id to section.items.map { it.watchlistKey() } }) {
                val current = lastFocusedCard
                val known = current == null || current in setOf("search", "avatar", "promo_movies", "promo_festive") ||
                    current == uiState.featuredMovies.firstOrNull()?.let { "hero:${it.watchlistKey()}" } ||
                    visibleSections.any { section -> section.items.any { "${section.id}:${it.watchlistKey()}" == current } }
                if (!known) {
                    lastFocusedCard = "search"
                    listState.scrollToItem(0)
                    withFrameNanos { }
                    searchFocus.requestFocus()
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Background),
                contentPadding = PaddingValues(top = 28.dp, bottom = 64.dp)
            ) {
                // Header: brand + search entry. Search is a destination on TV (voice-first),
                // so it lives here rather than as a phone-style icon in a corner.
                item(key = "header") {
                    LaunchedEffect(Unit) {
                        withFrameNanos { }
                        when (lastFocusedCard) {
                            null, "search" -> searchFocus.requestFocus()
                            "avatar" -> avatarFocus.requestFocus()
                        }
                    }
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
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Button(onClick = { showAvatarPicker = true }, modifier = Modifier.focusRequester(avatarFocus)
                                .onFocusChanged { if (it.isFocused) lastFocusedCard = "avatar" }) {
                                com.example.vista.artwork.CurrentUserAvatar(size = 36.dp)
                                Spacer(Modifier.width(8.dp))
                                Text("Edit Avatar")
                            }
                            Button(onClick = onSearchClick, modifier = Modifier.focusRequester(searchFocus)
                                .onFocusChanged { if (it.isFocused) lastFocusedCard = "search" }) { Text("Search") }
                            Button(onClick = onSupportClick) { Text("Help") }
                        }
                    }
                }

                // Featured Section
                if (uiState.featuredMovies.isNotEmpty()) {
                    item(key = "hero") {
                        val movie = uiState.featuredMovies.first()
                        val heroFocus = remember { FocusRequester() }
                        LaunchedEffect(Unit) {
                            if (lastFocusedCard == "hero:${movie.watchlistKey()}") heroFocus.requestFocus()
                        }
                        Surface(
                            onClick = { onMediaClick(movie) },
                            scale = ClickableSurfaceDefaults.scale(focusedScale = 1.015f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 48.dp, vertical = 12.dp)
                                .height((androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp * 0.45f).dp.coerceIn(200.dp, 320.dp))
                                .focusRequester(heroFocus)
                                .onFocusChanged { if (it.isFocused) lastFocusedCard = "hero:${movie.watchlistKey()}" }
                        ) {
                            Box(Modifier.fillMaxSize()) {
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
                }

                // Sections
                if (movieItems.isNotEmpty()) item(key = "artwork_movie_night") {
                    val focus = remember { FocusRequester() }
                    LaunchedEffect(Unit) { if (lastFocusedCard == "promo_movies") focus.requestFocus() }
                    com.example.vista.artwork.VistaArtworkBanner(com.example.vista.artwork.VistaArtwork.MOVIE_NIGHT,
                        "Make tonight a movie night.", "Find your next favourite story.", "Explore Movies",
                        onClick = { showMovieCollection = true }, modifier = Modifier.padding(horizontal = 48.dp, vertical = 16.dp)
                            .focusRequester(focus).onFocusChanged { if (it.isFocused) lastFocusedCard = "promo_movies" })
                }
                if (festive != null && hiddenFestive != festive.seasonYear) item(key = "artwork_festive") {
                    val focus = remember { FocusRequester() }
                    LaunchedEffect(Unit) { if (lastFocusedCard == "promo_festive") focus.requestFocus() }
                    com.example.vista.artwork.VistaArtworkBanner(com.example.vista.artwork.VistaArtwork.CHRISTMAS,
                        festive.heading, festive.countdown, "Explore Festive Movies", onClick = { showFestiveCollection = true },
                        modifier = Modifier.padding(horizontal = 48.dp, vertical = 16.dp)
                            .focusRequester(focus).onFocusChanged { if (it.isFocused) lastFocusedCard = "promo_festive" }, contentAlignment = Alignment.End)
                }
                itemsIndexed(visibleSections, key = { _, section -> section.id }) { _, section ->
                    TvSection(
                        section = section,
                        onMediaClick = onMediaClick,
                        restoreFocusKey = lastFocusedCard,
                        onItemFocused = { lastFocusedCard = it }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalFoundationApi::class, androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun TvSection(
    section: HomeSection,
    onMediaClick: (MediaItem) -> Unit,
    restoreFocusKey: String? = null,
    onItemFocused: (String) -> Unit = {}
) {
    val rowState = rememberLazyListState()

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
        if (section.isSavedCatalog) Text(
            text = "Saved catalog · ${section.catalogSavedAt?.let { com.example.vista.util.DateUtils.formatDate(it, "d MMM, HH:mm") }.orEmpty()}",
            color = com.example.vista.ui.theme.SecondaryText,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 48.dp, vertical = 4.dp)
        )

        LazyRow(
            state = rowState,
            modifier = Modifier.focusRestorer().focusGroup(),
            contentPadding = PaddingValues(horizontal = 48.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            itemsIndexed(section.items, key = { _, item -> item.watchlistKey() }) { _, item ->
                val focusKey = "${section.id}:${item.watchlistKey()}"
                val focus = remember { FocusRequester() }
                LaunchedEffect(Unit) {
                    if (restoreFocusKey == focusKey) focus.requestFocus()
                }
                TvMediaCard(
                    item = item,
                    onClick = { onMediaClick(item) },
                    modifier = Modifier.focusRequester(focus),
                    onFocused = { onItemFocused(focusKey) }
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
    modifier: Modifier = Modifier,
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
        // Avoid compounding TV Surface's own default scale with our animated focus scale.
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        modifier = modifier
            .zIndex(if (focused) 1f else 0f)
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
