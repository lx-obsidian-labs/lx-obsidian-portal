@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)

package com.example.vista.ui.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.*
import com.example.vista.ai.AiChatScreen
import com.example.vista.ai.AiChatViewModel
import com.example.vista.ui.components.PullToRefreshWrapper
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.vista.domain.model.*
import com.example.vista.ui.components.AdBanner
import com.example.vista.ui.components.BrandAttribution
import com.example.vista.ui.components.CoilImage
import com.example.vista.ui.components.MediaCard
import com.example.vista.ui.components.ShimmerSection
import com.example.vista.ui.components.ShimmerBrush
import com.example.vista.navigation.Screen
import com.example.vista.ui.theme.*
import com.example.vista.util.AdManager
import com.example.vista.util.ViewUtils
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val hiddenFestiveSeason by viewModel.hiddenFestiveSeason.collectAsStateWithLifecycle()
    val watchlistKeys by viewModel.watchlistKeys.collectAsStateWithLifecycle()
    val displayName by viewModel.displayName.collectAsStateWithLifecycle()
    val bellUnread by viewModel.bellUnread.collectAsStateWithLifecycle()
    val announcements by viewModel.announcements.collectAsStateWithLifecycle()
    val premiumAcked by viewModel.premiumAcked.collectAsStateWithLifecycle()
    val movieOfTheDay by viewModel.movieOfTheDay.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    val scrollState = rememberScrollState()

    // Notification bell sheet + premium dialog state
    var showAnnouncements by remember { mutableStateOf(false) }
    var showPremiumDialog by remember { mutableStateOf(false) }
    var showAssistant by remember { mutableStateOf(false) }
    val connectionMessages = remember { SnackbarHostState() }
    LaunchedEffect(uiState.error, uiState.isOffline) {
        val message = uiState.error ?: if (uiState.isOffline) "You're offline. Previously loaded titles are still available." else null
        if (message != null && uiState.sections.isNotEmpty()) {
            if (connectionMessages.showSnackbar(message, "Retry") == SnackbarResult.ActionPerformed) viewModel.retry()
        }
    }
    val floatingAssistant: AiChatViewModel = hiltViewModel(key = "floating-assistant")

    // Quick Preview State
    var previewItem by remember { mutableStateOf<MediaItem?>(null) }

    LaunchedEffect(Unit) {
        AdManager.loadInterstitialAd(context)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            PullToRefreshWrapper(
                isRefreshing = uiState.isRefreshing,
                onRefresh = { viewModel.refresh() },
                modifier = Modifier.fillMaxSize()
            ) {
                when {
                    uiState.isLoading -> {
                        HomeLoadingSkeleton()
                    }
                    uiState.error != null && uiState.sections.isEmpty() -> {
                        ErrorContent(
                            message = uiState.error ?: "Unknown error",
                            onRetry = { viewModel.retry() },
                            isOffline = uiState.isOffline,
                            onOpenLibrary = { navController.navigate(Screen.Library.route) }
                        )
                    }
                    else -> {
                        HomeContent(
                            hiddenFestiveSeason = hiddenFestiveSeason,
                            onHideFestive = viewModel::hideFestiveSeason,
                            navController = navController,
                            sections = uiState.sections,
                            featuredMovies = uiState.featuredMovies,
                            continueWatching = uiState.continueWatching,
                            becauseYouWatched = uiState.becauseYouWatched,
                            suggestionReasons = uiState.suggestionReasons,
                            onDismissSuggestion = viewModel::dismissSuggestion,
                            watchlistKeys = watchlistKeys,
                            onToggleWatchlist = { viewModel.toggleWatchlist(it) },
                            sectionPages = uiState.sectionPages,
                            sectionHasMore = uiState.sectionHasMore,
                            sectionLoadMoreLoading = uiState.sectionLoadMoreLoading,
                            sectionLoadMoreErrors = uiState.sectionLoadMoreErrors,
                            onLoadMore = { viewModel.loadMoreSection(it) },
                            scrollState = scrollState,
                            onLongClick = { previewItem = it },
                            movieOfTheDay = movieOfTheDay,
                            onOpenMovieOfTheDay = {
                                viewModel.onMovieOfTheDayOpened()
                                movieOfTheDay?.let {
                                    navController.navigate(Screen.MovieDetails.createRoute(it.movie.tmdbId.toString()))
                                }
                            },
                            onDismissMovieOfTheDay = { viewModel.onMovieOfTheDayDismissed() },
                            onNotInterested = { viewModel.onMovieOfTheDayDismissed(notInterested = true) }
                        )
                    }
                }
            }
        }
        
        // Top bar - Overlays the content
        HomeTopBar(
            navController = navController,
            scrollState = scrollState,
            displayName = displayName,
            bellUnread = bellUnread,
            onBellClick = {
                viewModel.markBellSeen()
                showAnnouncements = true
            }
        )
        SnackbarHost(connectionMessages, modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp))
        // Netflix-style Quick Preview Sheet
        val preview = previewItem
        if (preview != null) {            QuickPreviewSheet(
                item = preview,
                isInWatchlist = watchlistKeys.contains(preview.watchlistKey()),
                onDismiss = { previewItem = null },
                onPlay = { item ->
                    previewItem = null
                    val route = when (item) {
                        is MediaItem.MovieItem -> Screen.MovieDetails.createRoute(item.id)
                        is MediaItem.SeriesItem -> Screen.SeriesDetails.createRoute(item.id)
                        else -> ""
                    }
                    if (route.isNotEmpty()) navController.navigate(route)
                },
                onToggleWatchlist = { 
                    viewModel.toggleWatchlist(it)
                }
            )
        }
    }


    // Notification bell sheet
    if (showAnnouncements) {
        AnnouncementsSheet(
            announcements = announcements,
            hasPremiumPending = !premiumAcked,
            onDismiss = { showAnnouncements = false },
            onOpenPremium = {
                showAnnouncements = false
                showPremiumDialog = true
            },
            onGoToSettings = {
                showAnnouncements = false
                navController.navigate(com.example.vista.navigation.Screen.Settings.createRoute("NOTIFICATIONS"))
            },
            onAckPremium = { viewModel.acknowledgePremium() }
        )
    }

    // Premium announcement dialog (opens from the bell)
    if (showPremiumDialog) {
        com.example.vista.ui.components.PremiumPromptDialog(
            onAcknowledge = {
                viewModel.acknowledgePremium()
                showPremiumDialog = false
            },
            onDismiss = { showPremiumDialog = false }
        )
    }

    if (showAssistant) {
        ModalBottomSheet(
            onDismissRequest = { floatingAssistant.cancel(); showAssistant = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Background,
            dragHandle = { BottomSheetDefaults.DragHandle(color = MutedText) }
        ) {
            Box(Modifier.fillMaxWidth().fillMaxHeight(0.92f)) {
                AiChatScreen(
                    navController = navController,
                    viewModel = floatingAssistant,
                    floating = true,
                    onDismiss = { floatingAssistant.cancel(); showAssistant = false }
                )
            }
        }
    }

    // Floating chat preserves the current tab and its scroll position.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        androidx.compose.material3.FloatingActionButton(
            onClick = { showAssistant = true },
            containerColor = Accent,
            contentColor = com.example.vista.ui.theme.PrimaryText,
            modifier = Modifier
                .size(56.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.SmartToy,
                contentDescription = "AI Assistant",
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun HomeTopBar(
    navController: NavController,
    scrollState: androidx.compose.foundation.ScrollState,
    displayName: String,
    bellUnread: Boolean,
    onBellClick: () -> Unit
) {
    // Dynamic background based on scroll. Reading the raw scroll value would recompose HomeTopBar
    // on every scroll pixel; derivedStateOf collapses it to "crossed the 100 px threshold" flips.
    val solidBackground by remember(scrollState) {
        derivedStateOf { scrollState.value > 100 }
    }
    val alpha by animateFloatAsState(
        targetValue = if (solidBackground) 0.95f else 0f,
        animationSpec = tween(300),
        label = "topBarAlpha"
    )
    
    Surface(
        color = Color.Black.copy(alpha = alpha),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "VISTA WATCH",
                        style = MaterialTheme.typography.titleLarge,
                        color = Accent,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    if (displayName.isNotBlank()) {
                        Text(
                            text = "${timeGreeting()}, $displayName",
                            style = MaterialTheme.typography.labelSmall,
                            color = MutedText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBellClick) {
                    Box {
                        Icon(Icons.Outlined.Notifications, "Notifications", tint = PrimaryText, modifier = Modifier.size(24.dp))
                        if (bellUnread) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(9.dp)
                                    .background(Accent, CircleShape)
                                    .border(2.dp, Color.Black, CircleShape)
                            )
                        }
                    }
                }
                IconButton(onClick = { navController.navigate(Screen.Search.route) }) {
                    Icon(Icons.Outlined.Search, "Search", tint = PrimaryText, modifier = Modifier.size(24.dp))
                }
                IconButton(onClick = { navController.navigate(Screen.Profile.route) }) {
                    com.example.vista.artwork.CurrentUserAvatar(size = 32.dp)
                }
            }
        }
    }
}

private fun timeGreeting(): String {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return when {
        hour in 5..11 -> "Good morning"
        hour in 12..17 -> "Good afternoon"
        else -> "Good evening"
    }
}

/** Bottom sheet: premium transition card pinned on top, then push announcements. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AnnouncementsSheet(
    announcements: List<com.example.vista.util.AnnouncementsStore.Announcement>,
    hasPremiumPending: Boolean,
    onDismiss: () -> Unit,
    onOpenPremium: () -> Unit,
    onGoToSettings: () -> Unit,
    onAckPremium: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = CardSurface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Notifications",
                        style = MaterialTheme.typography.headlineSmall,
                        color = PrimaryText,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "News from the Vista team",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedText
                    )
                }
                IconButton(onClick = onGoToSettings) {
                    Icon(Icons.Outlined.Settings, "Notification settings", tint = MutedText)
                }
            }

            com.example.vista.ui.components.PremiumCard(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                onTap = onOpenPremium
            )

            val items = announcements
            if (items.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                val header = if (hasPremiumPending) "ANNOUNCEMENTS" else "RECENT ANNOUNCEMENTS"
                Text(
                    text = header,
                    style = MaterialTheme.typography.labelLarge,
                    color = Accent,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 8.dp)
                )
                items.forEachIndexed { index, a ->
                    AnnouncementRow(item = a)
                    if (index != items.lastIndex) {
                        Divider(color = Color.White.copy(alpha = 0.06f), modifier = Modifier.padding(start = 16.dp, end = 16.dp))
                    }
                }
            } else {
                Row(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                    Icon(
                        Icons.Outlined.NotificationsNone,
                        contentDescription = null,
                        tint = MutedText,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "You're all caught up. New announcements will appear here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedText
                    )
                }
            }
        }
    }
}

@Composable
private fun AnnouncementRow(item: com.example.vista.util.AnnouncementsStore.Announcement) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Accent.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.Campaign,
                contentDescription = null,
                tint = Accent,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = PrimaryText,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = com.example.vista.util.DateUtils.timeAgo(item.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedText
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = item.body,
                style = MaterialTheme.typography.bodySmall,
                color = SecondaryText,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun QuickPreviewSheet(
    item: MediaItem,
    isInWatchlist: Boolean,
    onDismiss: () -> Unit,
    onPlay: (MediaItem) -> Unit,
    onToggleWatchlist: (MediaItem) -> Unit
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = CardSurface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
            // Backdrop
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
            ) {
                val backdropUrl = when (item) {
                    is MediaItem.MovieItem -> item.backdropUrl ?: item.posterUrl
                    is MediaItem.SeriesItem -> item.backdropUrl ?: item.posterUrl
                    else -> item.posterUrl
                }
                CoilImage(
                    url = backdropUrl,
                    contentDescription = item.title,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Transparent,
                                1f to CardSurface
                            )
                        )
                )
            }

            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = PrimaryText,
                    fontWeight = FontWeight.Black,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(10.dp))

                val meta = when (item) {
                    is MediaItem.MovieItem -> buildList {
                        item.year?.let { add(it.toString()) }
                        if (item.rating != null && item.rating!! > 0) {
                            add(String.format(Locale.US, "%.1f", item.rating))
                        }
                    }
                    is MediaItem.SeriesItem -> buildList {
                        item.year?.let { add(it.toString()) }
                        item.totalSeasons?.let { add(if (it == 1) "1 Season" else "$it Seasons") }
                        if (item.rating != null && item.rating!! > 0) {
                            add(String.format(Locale.US, "%.1f", item.rating))
                        }
                    }
                    else -> emptyList()
                }

                val ageRating = when (item) {
                    is MediaItem.MovieItem -> item.ageRating
                    is MediaItem.SeriesItem -> item.ageRating
                    else -> null
                }

                if (meta.isNotEmpty() || ageRating != null) {
                    Text(
                        text = buildList {
                            addAll(meta)
                            ageRating?.let { add(it) }
                        }.joinToString("  •  "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = SecondaryText,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            onPlay(item)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = PrimaryText),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Icon(Icons.Outlined.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Play", fontWeight = FontWeight.Black)
                    }

                    Surface(
                        onClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            onToggleWatchlist(item)
                        },
                        color = if (isInWatchlist) Accent.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.size(48.dp).border(
                            1.dp,
                            if (isInWatchlist) Accent.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.1f),
                            RoundedCornerShape(12.dp)
                        )
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isInWatchlist) Icons.Outlined.Check else Icons.Outlined.Add,
                                contentDescription = if (isInWatchlist) "Remove from My List" else "Add to My List",
                                tint = if (isInWatchlist) Accent else PrimaryText
                            )
                        }
                    }

                    Surface(
                        onClick = onDismiss,
                        color = Color.White.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.Close, contentDescription = "Close preview", tint = MutedText)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeContent(
    hiddenFestiveSeason: Int,
    onHideFestive: (Int) -> Unit,
    navController: NavController,
    sections: List<HomeSection>,
    featuredMovies: List<MediaItem>,
    continueWatching: List<MediaItem.ContinueWatchingItem>,
    becauseYouWatched: List<MediaItem>,
    suggestionReasons: Map<String, String>,
    onDismissSuggestion: (MediaItem) -> Unit,
    watchlistKeys: Set<String> = emptySet(),
    onToggleWatchlist: (MediaItem) -> Unit = {},
    sectionPages: Map<String, Int> = emptyMap(),
    sectionHasMore: Map<String, Boolean> = emptyMap(),
    sectionLoadMoreLoading: Map<String, Boolean> = emptyMap(),
    sectionLoadMoreErrors: Map<String, String> = emptyMap(),
    onLoadMore: (String) -> Unit = {},
    scrollState: androidx.compose.foundation.ScrollState,
    onLongClick: (MediaItem) -> Unit = {},
    movieOfTheDay: com.example.vista.motd.MovieOfTheDay? = null,
    onOpenMovieOfTheDay: () -> Unit = {},
    onDismissMovieOfTheDay: () -> Unit = {},
    onNotInterested: () -> Unit = {}
) {
    val pagerState = rememberPagerState(pageCount = { featuredMovies.size })

    // Auto-scroll for featured carousel
    LaunchedEffect(pagerState) {
        while (true) {
            delay(8000)
            if (featuredMovies.isNotEmpty()) {
                val nextPage = (pagerState.currentPage + 1) % featuredMovies.size
                pagerState.animateScrollToPage(nextPage)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        // Sits above the hero rather than below it: the hero is a 600dp carousel that would push
        // the suggestion off-screen, and the suggestion is the one thing here with a reason to act.
        if (movieOfTheDay != null) {
            Spacer(modifier = Modifier.height(96.dp))
            com.example.vista.ui.components.MovieOfTheDayCard(
                suggestion = movieOfTheDay,
                onOpen = onOpenMovieOfTheDay,
                onDismiss = onDismissMovieOfTheDay,
                onNotInterested = onNotInterested
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Featured Hero Section with Parallax
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height((androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp * 0.65f).dp.coerceIn(360.dp, 600.dp))
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                Box(modifier = Modifier.fillMaxSize().graphicsLayer {
                    this.translationY = scrollState.value * 0.5f
                    this.alpha = (1f - (scrollState.value / 1500f)).coerceIn(0f, 1f)
                }) {
                    FeaturedMovieCard(
                        item = featuredMovies[page],
                        isInWatchlist = watchlistKeys.contains(featuredMovies[page].watchlistKey()),
                        onToggleWatchlist = { onToggleWatchlist(featuredMovies[page]) },
                        onClick = {
                            val route = when (featuredMovies[page]) {
                                is MediaItem.MovieItem -> Screen.MovieDetails.createRoute(featuredMovies[page].id)
                                is MediaItem.SeriesItem -> Screen.SeriesDetails.createRoute(featuredMovies[page].id)
                                else -> ""
                            }
                            if (route.isNotEmpty()) navController.navigate(route)
                        }
                    )
                }
            }

            // Page Indicators
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(featuredMovies.size) { index ->
                    val width by animateDpAsState(if (pagerState.currentPage == index) 16.dp else 6.dp, label = "width")
                    Box(
                        modifier = Modifier
                            .height(6.dp)
                            .width(width)
                            .clip(CircleShape)
                            .background(
                                if (pagerState.currentPage == index) Accent
                                else Color.White.copy(alpha = 0.5f)
                            )
                    )
                }
            }
        }

        // Category chips
        CategoryChips(onCategoryClick = { category ->
            navController.navigate(Screen.Discover.createRoute(HomeCategoryTab.tabFor(category)))
        })

        val artworkContext = androidx.compose.ui.platform.LocalContext.current
        val availableCampaigns = remember(artworkContext) {
            setOf(com.example.vista.artwork.VistaArtwork.MOVIE_NIGHT, com.example.vista.artwork.VistaArtwork.RELAX)
                .filter { com.example.vista.artwork.VistaArtworkRegistry.resolve(artworkContext, it) != null }.toSet()
        }
        val campaign = com.example.vista.artwork.VistaArtworkRegistry.movieNight(com.example.vista.artwork.rememberCampaignDate().toEpochDay(), availableCampaigns)
        com.example.vista.artwork.VistaArtworkBanner(
            artwork = campaign,
            heading = if (campaign == com.example.vista.artwork.VistaArtwork.RELAX) "Sit back. Press play." else "Make tonight a movie night.",
            subtitle = if (campaign == com.example.vista.artwork.VistaArtwork.RELAX) "Your next favourite is waiting." else "Find your next favourite story.",
            actionLabel = "Explore Movies",
            onClick = { navController.navigate(Screen.Discover.createRoute("MOVIES")) },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )

        if (sections.any { it.isSavedCatalog }) {
            Surface(color = RaisedSurface, shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.CloudOff, null, tint = SecondaryText)
                    Spacer(Modifier.width(12.dp))
                    Text("Saved catalog · Some collections are from your last successful visit. Pull to refresh when connected.",
                        color = SecondaryText, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        com.example.vista.ui.components.FestiveHomeSection(
            hiddenSeason = hiddenFestiveSeason, savedKeys = watchlistKeys, onHide = onHideFestive,
            onOpen = { navController.navigate(Screen.MovieDetails.createRoute(it)) }, onSave = onToggleWatchlist
        )

        // Continue Watching
        com.example.vista.feedback.FeedbackPollCard(onSignIn = { navController.navigate(Screen.Auth.route) })

        if (continueWatching.isNotEmpty()) {
            SectionWithSeeAll(
                title = "Continue Watching",
                // Wired rather than left as an empty lambda: a tappable "see all" that does nothing
                // is worse than no affordance. The full, unabridged set lives in the Library.
                onSeeAllClick = { navController.navigate(Screen.Library.route) }
            ) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(continueWatching, key = { it.watchlistKey() }) { item ->
                        ContinueWatchingCard(
                            item = item,
                            onClick = {
                                navController.navigate(Screen.Player.createRoute(
                                    mediaId = item.id,
                                    title = item.title,
                                    mediaType = if (item.isSeries) {
                                        com.example.vista.domain.model.MediaType.SERIES
                                    } else {
                                        com.example.vista.domain.model.MediaType.MOVIE
                                    },
                                    season = item.seasonNumber,
                                    episode = item.episodeNumber,
                                    poster = item.posterUrl
                                ))
                            }
                        )
                    }
                }
            }
            
            // Ad Banner after Continue Watching
            AdBanner(modifier = Modifier.padding(top = 16.dp))
        }

        // Because You Watched
        if (becauseYouWatched.isNotEmpty()) {
            SectionWithSeeAll(
                title = "For your taste",
                onSeeAllClick = { /* Page folds into the recommendation API; no separate list. */ },
                trailing = {
                    PlaySomethingButton(items = becauseYouWatched, navController = navController)
                }
            ) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(becauseYouWatched, key = { it.watchlistKey() }) { item ->
                        Column(Modifier.width(150.dp)) {
                        MediaCard(
                            item = item,
                            onClick = {
                                val route = when (item) {
                                    is MediaItem.MovieItem -> Screen.MovieDetails.createRoute(item.id)
                                    is MediaItem.SeriesItem -> Screen.SeriesDetails.createRoute(item.id)
                                    else -> ""
                                }
                                if (route.isNotEmpty()) navController.navigate(route)
                            },
                            isInWatchlist = watchlistKeys.contains(item.watchlistKey()),
                            onDoubleTap = { onToggleWatchlist(item) },
                            onLongClick = { onLongClick(item) },
                            modifier = Modifier.width(135.dp)
                        )
                        Text(suggestionReasons[item.watchlistKey()].orEmpty(), color = SecondaryText,
                            style = MaterialTheme.typography.labelSmall, minLines = 2, maxLines = 2,
                            modifier = Modifier.padding(top = 6.dp))
                        TextButton(onClick = { onDismissSuggestion(item) }) {
                            Icon(Icons.Outlined.ThumbDownOffAlt, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Not for me", style = MaterialTheme.typography.labelSmall)
                        }
                        }
                    }
                }
            }
            AdBanner(modifier = Modifier.padding(top = 16.dp))
        }

        // Other sections from repository
        sections.filter { it.type != HomeSectionType.FEATURED_CAROUSEL && it.type != HomeSectionType.CONTINUE_WATCHING }.forEachIndexed { index, section ->
            if (section.items.isNotEmpty()) {
                val isLoadingMore = sectionLoadMoreLoading[section.id] == true
                val hasMore = sectionHasMore[section.id] == true
                
                SectionWithSeeAll(
                    title = section.title,
                    onSeeAllClick = { 
                        navController.navigate(Screen.SeeAll.createRoute(section.id, section.title))
                    },
                    trailing = {
                        PlaySomethingButton(items = section.items, navController = navController)
                    }
                ) {
                    if (section.isSavedCatalog) Text(
                        "Saved catalog · ${section.catalogSavedAt?.let { com.example.vista.util.DateUtils.formatDate(it, "d MMM, HH:mm") }.orEmpty()}",
                        color = SecondaryText, style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    sectionLoadMoreErrors[section.id]?.let { message ->
                        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(message, color = SecondaryText, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                            TextButton(onClick = { onLoadMore(section.id) }) { Text("Retry", color = PrimaryText) }
                        }
                    }
                    if (section.id.contains("trending") && index < 2) {
                        // Netflix-style Top 10 Row
                        Top10Row(
                            items = section.items.take(10),
                            onItemClick = { item ->
                                val route = when (item) {
                                    is MediaItem.MovieItem -> Screen.MovieDetails.createRoute(item.id)
                                    is MediaItem.SeriesItem -> Screen.SeriesDetails.createRoute(item.id)
                                    else -> ""
                                }
                                if (route.isNotEmpty()) navController.navigate(route)
                            },
                            onLongClick = onLongClick
                        )
                    } else {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(section.items, key = { "${section.id}_${it.watchlistKey()}" }) { item ->
                                when (item) {
                                    is MediaItem.SocialBuzzItem -> SocialBuzzCard(
                                        item = item,
                                        onClick = {
                                            item.mediaId?.let { id ->
                                                navController.navigate(Screen.MovieDetails.createRoute(id))
                                            }
                                        }
                                    )
                                    else -> MediaCard(
                                        item = item,
                                        onClick = {
                                            val route = when (item) {
                                                is MediaItem.MovieItem -> Screen.MovieDetails.createRoute(item.id)
                                                is MediaItem.SeriesItem -> Screen.SeriesDetails.createRoute(item.id)
                                                else -> ""
                                            }
                                            if (route.isNotEmpty()) navController.navigate(route)
                                        },
                                        isInWatchlist = watchlistKeys.contains(item.watchlistKey()),
                                        onDoubleTap = { onToggleWatchlist(item) },
                                        onLongClick = { onLongClick(item) },
                                        modifier = Modifier.width(135.dp)
                                    )
                                }
                            }
                            if (hasMore) {
                                item {
                                    LoadMoreFooter(
                                        isLoading = isLoadingMore,
                                        onClick = { if (!isLoadingMore) onLoadMore(section.id) }
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Ad Banner every 3 sections
                if ((index + 1) % 3 == 0) {
                    AdBanner(modifier = Modifier.padding(top = 24.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        BrandAttribution()
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun FeaturedMovieCard(
    item: MediaItem,
    isInWatchlist: Boolean = false,
    onToggleWatchlist: () -> Unit = {},
    onClick: () -> Unit
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(onClick = onClick)
    ) {
        // Backdrop image with better content scale
        val backdropUrl = when (item) {
            is MediaItem.MovieItem -> item.backdropUrl
            is MediaItem.SeriesItem -> item.backdropUrl
            else -> null
        }
        
        if (backdropUrl != null) {
            CoilImage(
                url = backdropUrl,
                contentDescription = item.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        // Netflix-style Overlay - Stronger bottom and top gradients
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Black.copy(alpha = 0.6f),
                        0.3f to Color.Transparent,
                        0.7f to Color.Black.copy(alpha = 0.5f),
                        1.0f to Background
                    )
                )
        )

        // Centered Content - Netflix Style
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 24.dp, vertical = 56.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = item.title.uppercase(),
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    lineHeight = 36.sp
                ),
                color = PrimaryText,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Metadata Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val info = when (item) {
                    is MediaItem.MovieItem -> "${item.year ?: ""} • Movie"
                    is MediaItem.SeriesItem -> "${item.year ?: ""} • ${item.totalSeasons ?: 1} Season"
                    else -> ""
                }
                
                Text(
                    text = info,
                    style = MaterialTheme.typography.bodyMedium,
                    color = PrimaryText,
                    fontWeight = FontWeight.Bold
                )
                
                if (item.rating != null && item.rating!! > 0) {
                    Surface(
                        color = Accent.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(0.5.dp, Accent.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = String.format(Locale.US, "%.1f", item.rating),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Accent,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // My List Button
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onToggleWatchlist() },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = if (isInWatchlist) Icons.Outlined.Check else Icons.Outlined.Add,
                        contentDescription = null,
                        tint = PrimaryText,
                        modifier = Modifier.size(28.dp)
                    )
                    Text("My List", style = MaterialTheme.typography.labelMedium, color = PrimaryText)
                }

                // Main Play Button
                Button(
                    onClick = onClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .weight(1.5f),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.Outlined.PlayArrow, null, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Play", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }

                // Info Button
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onClick() },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = PrimaryText,
                        modifier = Modifier.size(28.dp)
                    )
                    Text("Info", style = MaterialTheme.typography.labelMedium, color = PrimaryText)
                }
            }
        }
    }
}

@Composable
private fun SocialBuzzCard(
    item: MediaItem.SocialBuzzItem,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(280.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(CardSurface.copy(alpha = 0.4f))
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Platform Icon/Label
            Surface(
                color = when(item.platform) {
                    "Reddit" -> Color(0xFFFF4500).copy(alpha = 0.1f)
                    else -> Accent.copy(alpha = 0.1f)
                },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = item.platform.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = if(item.platform == "Reddit") Color(0xFFFF4500) else Accent,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    fontWeight = FontWeight.Black
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Hotness Score
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Star,
                    contentDescription = null,
                    tint = RatingGold,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${item.hotnessScore}% Hot",
                    style = MaterialTheme.typography.labelSmall,
                    color = RatingGold,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(verticalAlignment = Alignment.Top) {
            CoilImage(
                url = item.posterUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(60.dp, 90.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = PrimaryText,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.discussionTopic,
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryText,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

data class CategoryChip(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

/**
 * Maps the home category chips to the Discover tab each one opens.
 * "All" and "Movies" both land on the plain Movies tab so a user sees the full catalog.
 */
private object HomeCategoryTab {
    fun tabFor(label: String): String = when (label) {
        "TV Shows" -> "TV_SHOWS"
        "Anime" -> "ANIME"
        "Action" -> "ACTION"
        "Comedy" -> "COMEDY"
        "Horror" -> "HORROR"
        "Sci-Fi" -> "SCIFI"
        "Drama" -> "DRAMA"
        "Romance" -> "ROMANCE"
        else -> "MOVIES"
    }
}

@Composable
private fun CategoryChips(onCategoryClick: (String) -> Unit = {}) {
    val categories = listOf(
        CategoryChip("All", Icons.Outlined.Apps),
        CategoryChip("Movies", Icons.Outlined.Movie),
        CategoryChip("TV Shows", Icons.Outlined.Tv),
        CategoryChip("Anime", Icons.Outlined.Animation),
        CategoryChip("Action", Icons.Outlined.FlashOn),
        CategoryChip("Comedy", Icons.Outlined.SentimentSatisfied),
        CategoryChip("Horror", Icons.Outlined.BugReport),
        CategoryChip("Sci-Fi", Icons.Outlined.Rocket),
        CategoryChip("Drama", Icons.Outlined.TheaterComedy),
        CategoryChip("Romance", Icons.Outlined.Favorite),
    )
    var selectedFilter by remember { mutableStateOf("All") }

    LazyRow(
        modifier = Modifier.padding(vertical = 12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(categories) { category ->
            FilterChip(
                selected = selectedFilter == category.label,
                onClick = { 
                    selectedFilter = category.label
                    onCategoryClick(category.label)
                },
                leadingIcon = {
                    Icon(
                        imageVector = category.icon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                label = {
                    Text(
                        category.label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (selectedFilter == category.label) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = CardSurface,
                    selectedContainerColor = Accent,
                    labelColor = SecondaryText,
                    selectedLabelColor = PrimaryText
                ),
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}

@Composable
private fun SectionWithSeeAll(
    title: String,
    onSeeAllClick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = PrimaryText, fontWeight = FontWeight.Bold)
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                trailing?.invoke()
                Text("See All", style = MaterialTheme.typography.labelLarge, color = Accent, modifier = Modifier.clickable(onClick = onSeeAllClick))
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        content()
    }
}

@Composable
private fun PlaySomethingButton(
    items: List<MediaItem>,
    navController: NavController
) {
    IconButton(
        onClick = {
            val pool = items.filterNot { it is MediaItem.SocialBuzzItem }
            val pick = pool.randomOrNull() ?: return@IconButton
            when (pick) {
                is MediaItem.MovieItem -> navController.navigate(
                    Screen.Player.createRoute(
                        mediaId = pick.id,
                        title = pick.title,
                        mediaType = com.example.vista.domain.model.MediaType.MOVIE,
                        poster = pick.posterUrl
                    )
                )
                is MediaItem.SeriesItem -> navController.navigate(
                    Screen.Player.createRoute(
                        mediaId = pick.id,
                        title = pick.title,
                        mediaType = com.example.vista.domain.model.MediaType.SERIES,
                        season = 1,
                        episode = 1,
                        poster = pick.posterUrl
                    )
                )
                else -> {}
            }
        }
    ) {
        Icon(
            imageVector = Icons.Outlined.Shuffle,
            contentDescription = "Play something",
            tint = Accent,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun Top10Row(
    items: List<MediaItem>,
    onItemClick: (MediaItem) -> Unit,
    onLongClick: (MediaItem) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        itemsIndexed(items, key = { _, item -> item.watchlistKey() }) { index, item ->
            Top10Card(
                rank = index + 1,
                item = item,
                onClick = { onItemClick(item) },
                onLongClick = { onLongClick(item) }
            )
        }
    }
}

@Composable
private fun Top10Card(
    rank: Int,
    item: MediaItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(160.dp)
            .height(200.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        // Large Rank Number behind
        Text(
            text = rank.toString(),
            style = MaterialTheme.typography.displayLarge.copy(
                fontSize = 120.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-12).sp
            ),
            color = Color.White.copy(alpha = 0.2f),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-20).dp, y = 10.dp)
        )
        
        // Poster on top, shifted right
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(130.dp)
                .align(Alignment.CenterEnd)
                .clip(RoundedCornerShape(8.dp))
                .background(CardSurface)
                .border(BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f)), RoundedCornerShape(8.dp))
        ) {
            CoilImage(
                url = item.posterUrl,
                contentDescription = item.title,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun ContinueWatchingCard(
    item: MediaItem.ContinueWatchingItem,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(160.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
                .clip(RoundedCornerShape(8.dp))
        ) {
            Box(modifier = Modifier.fillMaxSize().background(RaisedSurface))

            // Portrait poster cropped to a 16:9 tile. playback_history has no backdrop column, so
            // this is the poster scaled to fill with the top and bottom of the artwork cropped -
            // dimmed enough that the title area still reads as the poster rather than a colour
            // smear. Storing a real backdrop needs a schema change; see the migration note before
            // assuming this is as good as it gets.
            CoilImage(
                url = item.posterUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Color.Black.copy(alpha = 0.45f),
                        RoundedCornerShape(8.dp)
                    )
            )

            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(36.dp)
                    .background(Color.Black.copy(alpha = 0.6f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.PlayArrow, contentDescription = "Play", tint = PrimaryText, modifier = Modifier.size(20.dp))
            }
            LinearProgressIndicator(
                progress = { item.progress },
                modifier = Modifier.fillMaxWidth().height(3.dp).align(Alignment.BottomCenter),
                color = ProgressIndicator,
                trackColor = ProgressTrack,
                strokeCap = StrokeCap.Round
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(item.title, style = MaterialTheme.typography.bodySmall, color = PrimaryText, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            text = continueWatchingSubtitle(item),
            style = MaterialTheme.typography.labelSmall,
            color = MutedText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * "S2 E4 · 38m left" for a series, "38m left" for a film.
 *
 * Built with helpers rather than raw arithmetic because the old inline `(duration - currentTime) /
 * 60000` printed "0m left" whenever the provider reported a zero or missing duration, and would
 * print a negative number if the stored position ever ran past the stored duration.
 */
private fun continueWatchingSubtitle(item: MediaItem.ContinueWatchingItem): String {
    val remainingMs = (item.duration - item.currentTime).coerceAtLeast(0L)
    // Under a minute reads better as "Left off" than as "1m left".
    val remaining = when {
        remainingMs < 60_000L -> "Left off"
        else -> "${(remainingMs / 60_000L)}m left"
    }
    return if (item.isSeries) {
        listOfNotNull(item.episodeTitle, remaining).joinToString(" · ")
    } else {
        remaining
    }
}

@Composable
private fun LoadMoreFooter(
    isLoading: Boolean,
    onClick: () -> Unit
) {
    if (isLoading) {
        Box(
            modifier = Modifier
                .width(130.dp)
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(12.dp))
                .background(CardSurface),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = Accent,
                strokeWidth = 2.dp
            )
        }
    } else {
        Box(
            modifier = Modifier
                .width(130.dp)
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(12.dp))
                .background(CardSurface)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = "Load more",
                tint = PrimaryText,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun HomeLoadingSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Hero Shimmer
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(550.dp)
                .background(ShimmerBrush())
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Section Shimmers
        repeat(3) {
            ShimmerSection()
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
    isOffline: Boolean,
    onOpenLibrary: () -> Unit
) {
    com.example.vista.ui.components.RecoveryState(
        title = if (isOffline) "You're offline" else "Let's get your movies back",
        message = if (isOffline) "Reconnect to load the catalog. You can still open your library and completed downloads."
            else ViewUtils.formatError(message),
        icon = if (isOffline) Icons.Outlined.WifiOff else Icons.Outlined.Movie,
        primaryLabel = "Try again", onPrimary = onRetry,
        secondaryLabel = "Open library", onSecondary = onOpenLibrary,
        modifier = Modifier.padding(top = 80.dp)
    )
}
