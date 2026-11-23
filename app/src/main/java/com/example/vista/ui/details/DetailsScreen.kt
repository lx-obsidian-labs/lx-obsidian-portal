package com.example.vista.ui.details

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import com.example.vista.ui.components.PullToRefreshWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
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
import com.example.vista.data.remote.watchmode.WatchmodeSource
import com.example.vista.domain.model.*
import com.example.vista.ui.components.BrandAttribution
import com.example.vista.ui.components.CoilImage
import com.example.vista.ui.components.MediaCard
import com.example.vista.ui.components.ShareSheet
import com.example.vista.ui.components.shimmerEffect
import com.example.vista.navigation.Screen
import com.example.vista.ui.theme.*
import com.example.vista.util.AdManager
import com.example.vista.util.ViewUtils
import java.util.Locale

@Composable
fun DetailsScreen(
    navController: NavController,
    viewModel: DetailsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val activity = androidx.compose.ui.platform.LocalContext.current as? android.app.Activity
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    Box(Modifier.fillMaxSize()) {
    when {
        uiState.isLoading -> {
            DetailsLoadingSkeleton()
        }
        uiState.error != null -> {
            Box(modifier = Modifier.fillMaxSize().background(Background), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.ErrorOutline,
                        contentDescription = null,
                        tint = MutedText,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(ViewUtils.formatError(uiState.error), color = PrimaryText, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { 
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            viewModel.retry() 
                        }, 
                        colors = ButtonDefaults.buttonColors(containerColor = Accent),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Retry", color = PrimaryText, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        else -> {
            val movie = uiState.movie
            val series = uiState.series
            when {
                movie != null -> {
                    MovieDetailsContent(
                        movie = movie,
                        cast = uiState.cast,
                        videos = uiState.videos,
                        similarMovies = uiState.similarMovies,
                        isInWatchlist = uiState.isInWatchlist,
                        playbackProgress = uiState.playbackProgress,
                        streamingSources = uiState.streamingSources,
                        isRefreshing = uiState.isRefreshing,
                        onRefresh = { viewModel.refresh() },
                        onBackClick = { navController.popBackStack() },
                        onWatchClick = {
                            if (activity != null) {
                                AdManager.showInterstitialAd(activity) {
                                    navController.navigate(
                                        Screen.Player.createRoute(
                                            mediaId = movie.id,
                                            title = movie.title,
                                            mediaType = MediaType.MOVIE,
                                            poster = movie.posterUrl
                                        )
                                    )
                                }
                            } else {
                                navController.navigate(
                                    Screen.Player.createRoute(
                                        mediaId = movie.id,
                                        title = movie.title,
                                        mediaType = MediaType.MOVIE,
                                        poster = movie.posterUrl
                                    )
                                )
                            }
                        },
                        onWatchlistClick = { viewModel.toggleWatchlist() },
                        onTrailerClick = { videoKey, title ->
                            navController.navigate(Screen.Trailer.createRoute(videoKey, title))
                        },
                        onSimilarClick = { navController.navigate(Screen.MovieDetails.createRoute(it)) },
                        onCastClick = { personId ->
                            navController.navigate(Screen.CastDetails.createRoute(personId))
                        }
                    )
                }
                series != null -> {
SeriesDetailsContent(
                        series = series,
                        cast = uiState.cast,
                        episodes = uiState.episodes,
                        videos = uiState.videos,
                        similarSeries = uiState.similarSeries,
                        isEpisodesLoading = uiState.isEpisodesLoading,
                        isRefreshing = uiState.isRefreshing,
                        selectedSeason = uiState.selectedSeason,
                        isInWatchlist = uiState.isInWatchlist,
                        playbackProgress = uiState.playbackProgress,
                        streamingSources = uiState.streamingSources,
                        onBackClick = { navController.popBackStack() },
                        onWatchClick = {
                            // A series is always played as a concrete episode. Navigating without a
                            // season used to make the player treat the show as a movie and play the
                            // film that happened to share its numeric TMDB id.
                            val target = Screen.Player.createRoute(
                                mediaId = series.id,
                                title = series.title,
                                mediaType = MediaType.SERIES,
                                season = uiState.resumeSeason,
                                episode = uiState.resumeEpisode,
                                poster = series.posterUrl
                            )
                            if (activity != null) {
                                AdManager.showInterstitialAd(activity) {
                                    navController.navigate(target)
                                }
                            } else {
                                navController.navigate(target)
                            }
                        },
                        onWatchlistClick = { viewModel.toggleWatchlist() },
                        onRefresh = { viewModel.refresh() },
                        onSeasonSelect = { viewModel.selectSeason(it) },
                        onEpisodeClick = { seriesId, season, episode ->
                            val target = Screen.Player.createRoute(
                                mediaId = seriesId,
                                title = series.title,
                                mediaType = MediaType.SERIES,
                                season = season,
                                episode = episode,
                                poster = series.posterUrl
                            )
                            if (activity != null) {
                                AdManager.showInterstitialAd(activity) {
                                    navController.navigate(target)
                                }
                            } else {
                                navController.navigate(target)
                            }
                        },
                        onEpisodeDownload = { episode -> navController.navigate(Screen.Downloads.createRoute(
                            series.id, series.title, MediaType.SERIES, episode.seasonNumber, episode.episodeNumber
                        )) },
                        onTrailerClick = { videoKey, title ->
                            navController.navigate(Screen.Trailer.createRoute(videoKey, title))
                        },
                        onCastClick = { personId ->
                            navController.navigate(Screen.CastDetails.createRoute(personId))
                        },
                        onSimilarSeriesClick = { id ->
                            navController.navigate(Screen.SeriesDetails.createRoute(id))
                        }
                    )
                }
            }
        }
    }
        val companionId = uiState.movie?.id ?: uiState.series?.id
        if (companionId != null) {
            if (uiState.movie != null) {
                ExtendedFloatingActionButton(
                    onClick = { navController.navigate(Screen.Downloads.createRoute(companionId, uiState.movie?.title.orEmpty(), MediaType.MOVIE)) },
                    modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
                    icon = { Icon(Icons.Outlined.Download, null) }, text = { Text("Download") }
                )
            }
            ExtendedFloatingActionButton(
                onClick = { navController.navigate(Screen.AiAssistant.companion(companionId, if (uiState.series != null) "series" else "movie")) },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                icon = { Icon(Icons.Outlined.AutoAwesome, null) },
                text = { Text("Ask companion") }
            )
        }
    }
}

@Composable
private fun DetailsLoadingSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(550.dp)
                .shimmerEffect()
        )
        
        Column(modifier = Modifier.padding(20.dp)) {
            Box(modifier = Modifier.width(150.dp).height(24.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
            Spacer(modifier = Modifier.height(12.dp))
            Box(modifier = Modifier.fillMaxWidth().height(100.dp).clip(RoundedCornerShape(8.dp)).shimmerEffect())
            Spacer(modifier = Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                repeat(4) {
                    Box(modifier = Modifier.size(64.dp).clip(CircleShape).shimmerEffect())
                }
            }
        }
    }
}

@Composable
private fun MovieDetailsContent(
    movie: Movie,
    cast: List<Person>,
    videos: List<Video>,
    similarMovies: List<Movie>,
    isInWatchlist: Boolean,
    playbackProgress: PlaybackProgress?,
    streamingSources: List<WatchmodeSource>,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
    onBackClick: () -> Unit,
    onWatchClick: () -> Unit,
    onWatchlistClick: () -> Unit,
    onTrailerClick: (String, String) -> Unit,
    onSimilarClick: (String) -> Unit,
    onCastClick: (String) -> Unit
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val context = androidx.compose.ui.platform.LocalContext.current
    var showShareSheet by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }

    if (showShareSheet) {
        ShareSheet(title = movie.title, onDismiss = { showShareSheet = false })
    }
    
    PullToRefreshWrapper(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .verticalScroll(rememberScrollState())
    ) {
        // Hero Backdrop Section - Refined
        Box(modifier = Modifier.fillMaxWidth().height(550.dp)) {
            if (movie.backdropUrl != null) {
                CoilImage(
                    url = movie.backdropUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            // Stronger multi-layer gradient for better readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color.Black.copy(alpha = 0.5f),
                            0.4f to Color.Transparent,
                            1.0f to Background
                        )
                    )
            )

            // Header Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 48.dp)
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    onClick = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        onBackClick()
                    },
                    color = Color.Black.copy(alpha = 0.4f),
                    shape = CircleShape,
                    modifier = Modifier.size(44.dp).border(0.5.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = PrimaryText, modifier = Modifier.size(20.dp))
                    }
                }
                
                Surface(
                    onClick = { 
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        showShareSheet = true
                    },
                    color = Color.Black.copy(alpha = 0.4f),
                    shape = CircleShape,
                    modifier = Modifier.size(44.dp).border(0.5.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Share, contentDescription = "Share", tint = PrimaryText, modifier = Modifier.size(20.dp))
                    }
                }
            }

        // Title and Action Area
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(24.dp)
        ) {
            Text(
                text = movie.title,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1.5).sp,
                    lineHeight = 40.sp
                ),
                color = PrimaryText,
                maxLines = 3
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (movie.rating != null && movie.rating > 0) {
                    Surface(
                        color = RatingGold.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(0.5.dp, RatingGold.copy(alpha = 0.3f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(imageVector = Icons.Outlined.Star, contentDescription = null, tint = RatingGold, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = String.format(Locale.US, "%.1f", movie.rating),
                                style = MaterialTheme.typography.labelLarge,
                                color = RatingGold,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
                
                Text(
                    text = "${movie.releaseYear ?: "N/A"}  •  ${movie.runtimeMinutes ?: "?"} min",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SecondaryText,
                    fontWeight = FontWeight.Medium
                )

                if (movie.ageRating != null) {
                    Surface(
                        color = Color.White.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.2f))
                    ) {
                        Text(
                            text = movie.ageRating,
                            style = MaterialTheme.typography.labelSmall,
                            color = PrimaryText,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            onWatchClick()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = PrimaryText),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(54.dp)
                    ) {
                        Icon(Icons.Outlined.PlayArrow, contentDescription = null, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (playbackProgress != null) "Resume" else "Watch Now",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black
                        )
                    }
                    
                    Surface(
                        onClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            onWatchlistClick()
                        },
                        color = if (isInWatchlist) Accent.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.size(54.dp).border(
                            1.dp, 
if (isInWatchlist) Accent.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.1f),
                            RoundedCornerShape(12.dp)
                        )
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isInWatchlist) Icons.Outlined.Check else Icons.Outlined.Add,
                                contentDescription = null,
                                tint = if (isInWatchlist) Accent else PrimaryText
                            )
                        }
                    }
                }
            }
        }

        // Synopsis Section - Expandable
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            color = RaisedSurface.copy(alpha = 0.5f),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    "Overview",
                    style = MaterialTheme.typography.titleLarge,
                    color = PrimaryText,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                var expanded by remember { mutableStateOf(false) }
                Column(modifier = Modifier.clickable { expanded = !expanded }) {
                    Text(
                        text = ViewUtils.sanitizeText(movie.overview),
                        style = MaterialTheme.typography.bodyLarge,
                        color = SecondaryText,
                        lineHeight = 28.sp,
                        letterSpacing = 0.2.sp,
                        maxLines = if (expanded) Int.MAX_VALUE else 4,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (movie.overview.length > 150) {
                        Text(
                            text = if (expanded) "Show Less" else "Read More",
                            color = Accent,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(top = 8.dp),
                            fontWeight = FontWeight.Black
                        )
                    }
                }
                
                if (movie.genres.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(24.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(movie.genres) { genre ->
                            Surface(
                                color = Color.White.copy(alpha = 0.05f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f))
                            ) {
                                Text(
                                    text = genre.name,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MutedText,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Cast
        if (cast.isNotEmpty()) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
                Text(
                    text = "Top Cast",
                    style = MaterialTheme.typography.titleLarge,
                    color = PrimaryText,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(cast) { member ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .width(72.dp)
                                .clickable { onCastClick(member.id) }
                        ) {
                            CoilImage(
                                url = member.profileUrl,
                                contentDescription = member.name,
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = member.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = PrimaryText,
                                maxLines = 1,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Text(
                                text = member.character ?: "",
                                style = MaterialTheme.typography.labelSmall,
                                color = MutedText,
                                maxLines = 1,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // Streaming Sources
        if (streamingSources.isNotEmpty()) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
                Text(
                    text = "Where to Watch",
                    style = MaterialTheme.typography.titleLarge,
                    color = PrimaryText,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(streamingSources) { source ->
                        Surface(
                            color = CardSurface,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.border(0.5.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = source.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = PrimaryText,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = Accent.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = source.type.uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Accent,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Trailers
        if (videos.isNotEmpty()) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp)) {
                Text("Trailers & Videos", style = MaterialTheme.typography.titleMedium, color = PrimaryText, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(videos.filter { it.type == "Trailer" || it.type == "Teaser" }) { video ->
                        Column(
                            modifier = Modifier
                                .width(200.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(RaisedSurface)
                                .clickable { onTrailerClick(video.key, video.name) }
                        ) {
                            // Video thumbnail
                            Box(modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(CardSurface)) {
                                CoilImage(
                                    url = "https://img.youtube.com/vi/${video.key}/mqdefault.jpg",
                                    contentDescription = video.name,
                                    modifier = Modifier.fillMaxSize()
                                )
                                // Play button overlay
                                Icon(
                                    Icons.Outlined.PlayArrow,
                                    contentDescription = "Play",
                                    tint = Color.White,
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .size(48.dp)
                                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                        .padding(8.dp)
                                )
                            }
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    video.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = PrimaryText,
                                    maxLines = 1
                                )
                                Text(
                                    video.type,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MutedText
                                )
                            }
                        }
                    }
                }
            }
        }

        // Similar
        if (similarMovies.isNotEmpty()) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp)) {
                Text("More Like This", style = MaterialTheme.typography.titleMedium, color = PrimaryText, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(similarMovies) { movie ->
                        MediaCard(
                            item = movie.toMediaItem(),
                            onClick = { onSimilarClick(movie.id) },
                            modifier = Modifier.width(110.dp),
                            showTitle = false
                        )
                    }
                }
            }
        }

        BrandAttribution()
        Spacer(modifier = Modifier.height(32.dp))
    }
    } // Close PullToRefreshWrapper
}

@Composable
private fun SeriesDetailsContent(
    series: Series,
    cast: List<Person>,
    episodes: List<Episode>,
    videos: List<Video> = emptyList(),
    similarSeries: List<Series> = emptyList(),
    isEpisodesLoading: Boolean = false,
    isRefreshing: Boolean = false,
    selectedSeason: Int,
    isInWatchlist: Boolean,
    playbackProgress: PlaybackProgress?,
    streamingSources: List<WatchmodeSource>,
    onBackClick: () -> Unit,
    onWatchClick: () -> Unit,
    onWatchlistClick: () -> Unit,
    onSeasonSelect: (Int) -> Unit,
    onEpisodeClick: (String, Int, Int) -> Unit,
    onEpisodeDownload: (Episode) -> Unit,
    onRefresh: () -> Unit = {},
    onTrailerClick: (String, String) -> Unit,
    onCastClick: (String) -> Unit,
    onSimilarSeriesClick: (String) -> Unit
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val context = androidx.compose.ui.platform.LocalContext.current
    var showShareSheet by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }

    if (showShareSheet) {
        ShareSheet(title = series.title, onDismiss = { showShareSheet = false })
    }

    PullToRefreshWrapper(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .verticalScroll(rememberScrollState())
    ) {
        // Hero Backdrop Section
        Box(modifier = Modifier.fillMaxWidth().height(450.dp)) {
            if (series.backdropUrl != null) {
                CoilImage(
                    url = series.backdropUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color.Black.copy(alpha = 0.5f),
                            0.4f to Color.Transparent,
                            1.0f to Background
                        )
                    )
            )

            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 48.dp)
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    onClick = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        onBackClick()
                    },
                    color = Color.Black.copy(alpha = 0.4f),
                    shape = CircleShape,
                    modifier = Modifier.size(44.dp).border(0.5.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = PrimaryText, modifier = Modifier.size(20.dp))
                    }
                }
                
                Surface(
                    onClick = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        showShareSheet = true
                    },
                    color = Color.Black.copy(alpha = 0.4f),
                    shape = CircleShape,
                    modifier = Modifier.size(44.dp).border(0.5.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Share, contentDescription = "Share", tint = PrimaryText, modifier = Modifier.size(20.dp))
                    }
                }
            }

        // Title and Action
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(24.dp)
        ) {
            Text(
                text = series.title,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1.5).sp,
                    lineHeight = 40.sp
                ),
                color = PrimaryText,
                maxLines = 3
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (series.rating != null && series.rating > 0) {
                    Surface(
                        color = RatingGold.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(0.5.dp, RatingGold.copy(alpha = 0.3f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(imageVector = Icons.Outlined.Star, contentDescription = null, tint = RatingGold, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = String.format(Locale.US, "%.1f", series.rating),
                                style = MaterialTheme.typography.labelLarge,
                                color = RatingGold,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
                
                Text(
                    text = "${series.firstAirYear ?: "N/A"}  •  ${series.totalSeasons} Seasons",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SecondaryText,
                    fontWeight = FontWeight.Medium
                )

                if (series.ageRating != null) {
                    Surface(
                        color = Color.White.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.2f))
                    ) {
                        Text(
                            text = series.ageRating,
                            style = MaterialTheme.typography.labelSmall,
                            color = PrimaryText,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            onWatchClick()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = PrimaryText),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(54.dp)
                    ) {
                        Icon(Icons.Outlined.PlayArrow, contentDescription = null, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (playbackProgress != null) "Resume" else "Watch Now",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black
                        )
                    }
                    
                    Surface(
                        onClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            onWatchlistClick()
                        },
                        color = if (isInWatchlist) Accent.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.size(54.dp).border(
                            1.dp, 
                            if (isInWatchlist) Accent.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.1f), 
                            RoundedCornerShape(12.dp)
                        )
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isInWatchlist) Icons.Outlined.Check else Icons.Outlined.Add,
                                contentDescription = null,
                                tint = if (isInWatchlist) Accent else PrimaryText
                            )
                        }
                    }
                }
            }
        }

        // Synopsis
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            color = RaisedSurface.copy(alpha = 0.5f),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    "Overview",
                    style = MaterialTheme.typography.titleLarge,
                    color = PrimaryText,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                var expanded by remember { mutableStateOf(false) }
                Column(modifier = Modifier.clickable { expanded = !expanded }) {
                    Text(
                        text = ViewUtils.sanitizeText(series.overview),
                        style = MaterialTheme.typography.bodyLarge,
                        color = SecondaryText,
                        lineHeight = 28.sp,
                        letterSpacing = 0.2.sp,
                        maxLines = if (expanded) Int.MAX_VALUE else 4,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (series.overview.length > 150) {
                        Text(
                            text = if (expanded) "Show Less" else "Read More",
                            color = Accent,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(top = 8.dp),
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }

        // Cast
        if (cast.isNotEmpty()) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
                Text(
                    text = "Top Cast",
                    style = MaterialTheme.typography.titleLarge,
                    color = PrimaryText,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(cast) { member ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .width(72.dp)
                                .clickable { onCastClick(member.id) }
                        ) {
                            CoilImage(
                                url = member.profileUrl,
                                contentDescription = member.name,
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = member.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = PrimaryText,
                                maxLines = 1,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Text(
                                text = member.character ?: "",
                                style = MaterialTheme.typography.labelSmall,
                                color = MutedText,
                                maxLines = 1,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // Streaming Sources
        if (streamingSources.isNotEmpty()) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
                Text(
                    text = "Where to Watch",
                    style = MaterialTheme.typography.titleLarge,
                    color = PrimaryText,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(streamingSources) { source ->
                        Surface(
                            color = CardSurface,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.border(0.5.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = source.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = PrimaryText,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = Accent.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = source.type.uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Accent,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Trailers for series
        if (videos.isNotEmpty()) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp)) {
                Text("Trailers & Videos", style = MaterialTheme.typography.titleMedium, color = PrimaryText, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(videos.filter { it.type == "Trailer" || it.type == "Teaser" }) { video ->
                        Column(
                            modifier = Modifier
                                .width(200.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(RaisedSurface)
                                .clickable {
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    onTrailerClick(video.key, video.name)
                                }
                        ) {
                            Box(modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(CardSurface)) {
                                CoilImage(
                                    url = "https://img.youtube.com/vi/${video.key}/mqdefault.jpg",
                                    contentDescription = video.name,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Icon(
                                    Icons.Outlined.PlayArrow,
                                    contentDescription = "Play",
                                    tint = Color.White,
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .size(48.dp)
                                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                        .padding(8.dp)
                                )
                            }
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(video.name, style = MaterialTheme.typography.bodySmall, color = PrimaryText, maxLines = 1)
                                Text(video.type, style = MaterialTheme.typography.labelSmall, color = MutedText)
                            }
                        }
                    }
                }
            }
        }

        // Season selector
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp)) {
            Text("Episodes", style = MaterialTheme.typography.titleMedium, color = PrimaryText, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items((1..series.totalSeasons).toList()) { season ->
                    FilterChip(
                        selected = selectedSeason == season,
                        onClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            onSeasonSelect(season)
                        },
                        label = { Text("Season $season") },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = CardSurface,
                            selectedContainerColor = Accent
                        )
                    )
                }
            }
        }

        // Episodes list with crossfade animation on season switch
        if (isEpisodesLoading) {
            // Shimmer skeleton while loading episodes for a new season
            Column {
                repeat(3) {
                    EpisodeSkeleton()
                }
            }
        } else {
            AnimatedContent(
                targetState = episodes,
                label = "episodes_crossfade",
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) +
                        slideInVertically(animationSpec = tween(300)) { height -> height / 8 } togetherWith
                        fadeOut(animationSpec = tween(200)) +
                        slideOutVertically(animationSpec = tween(200)) { height -> -height / 8 }
                }
            ) { displayedEpisodes ->
                Column {
                    displayedEpisodes.forEach { episode ->
                        val key = "${episode.seasonNumber}:${episode.episodeNumber}"
                        EpisodeCard(
                            episode = episode,
                            onClick = { onEpisodeClick(series.id, episode.seasonNumber, episode.episodeNumber) },
                            onDownload = { onEpisodeDownload(episode) }
                        )
                    }

                    if (displayedEpisodes.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No episodes available for this season",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MutedText
                            )
                        }
                    }
                }
            }
        }

        // More Like This
        if (similarSeries.isNotEmpty()) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp)) {
                Text("More Like This", style = MaterialTheme.typography.titleMedium, color = PrimaryText, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(similarSeries) { series ->
                        MediaCard(
                            item = series.toMediaItem(),
                            onClick = { onSimilarSeriesClick(series.id) },
                            modifier = Modifier.width(110.dp),
                            showTitle = false
                        )
                    }
                }
            }
        }

        BrandAttribution()
        Spacer(modifier = Modifier.height(32.dp))
    }
    } // Close PullToRefreshWrapper
}

@Composable
private fun EpisodeCard(
    episode: Episode,
    onClick: () -> Unit,
    onDownload: () -> Unit
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = {
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                onClick()
            })
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail with Duration Overlay
        Box(
            modifier = Modifier
                .width(140.dp)
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(8.dp))
                .background(CardSurface)
        ) {
            CoilImage(
                url = episode.stillUrl ?: episode.thumbnailUrl,
                contentDescription = episode.title,
                modifier = Modifier.fillMaxSize()
            )
            
            // Duration Overlay
            if (episode.runtimeMinutes != null) {
                Surface(
                    color = Color.Black.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                ) {
                    Text(
                        text = "${episode.runtimeMinutes}m",
                        style = MaterialTheme.typography.labelSmall,
                        color = PrimaryText,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
            
            // Play Icon Overlay
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${episode.episodeNumber}. ${episode.title}",
                style = MaterialTheme.typography.titleMedium,
                color = PrimaryText,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            if (!episode.overview.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = ViewUtils.sanitizeText(episode.overview),
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryText,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )
            }
            TextButton(onClick = onDownload) {
                Icon(Icons.Outlined.Download, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Download episode")
            }
        }
    }
}

@Composable
private fun EpisodeSkeleton() {
    val brush = com.example.vista.ui.components.ShimmerBrush()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(RaisedSurface)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail skeleton
        Box(
            modifier = Modifier
                .size(100.dp, 56.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(brush)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(16.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(brush)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(brush)
            )
        }
    }
}
