@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.vista.ui.schedule

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.vista.domain.repository.ScheduleItem
import com.example.vista.ui.components.BrandAttribution
import com.example.vista.ui.components.PullToRefreshWrapper
import com.example.vista.ui.components.ShimmerBrush
import com.example.vista.navigation.Screen
import com.example.vista.ui.theme.*
import com.example.vista.util.ViewUtils
import java.util.Locale

@Composable
fun ScheduleScreen(
    navController: NavController,
    viewModel: ScheduleViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lookupMessage by viewModel.lookupMessage.collectAsStateWithLifecycle()

    LaunchedEffect(lookupMessage) {
        if (lookupMessage != null) {
            kotlinx.coroutines.delay(3000)
            viewModel.clearLookupMessage()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        // Ambient background glow
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(AmbientBlue, Color.Transparent)
                    )
                )
        )

        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "TV Guide",
                        style = MaterialTheme.typography.headlineMedium,
                        color = PrimaryText,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        "Live & Upcoming",
                        style = MaterialTheme.typography.labelMedium,
                        color = Accent,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // 2026 Style "Today" Jump Button
                TextButton(
                    onClick = { viewModel.selectToday() },
                    colors = ButtonDefaults.textButtonColors(contentColor = Accent),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Accent.copy(alpha = 0.1f))
                ) {
                    Text("Today", fontWeight = FontWeight.Bold)
                }
            }

            DateSelectorChips(
                selectedDate = uiState.selectedDate,
                onDateSelected = { viewModel.selectDate(it) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            PullToRefreshWrapper(
                isRefreshing = uiState.isRefreshing,
                onRefresh = { viewModel.refresh() },
                modifier = Modifier.fillMaxSize()
            ) {
                when {
                    uiState.isLoading -> {
                        ScheduleLoadingContent()
                    }
                    uiState.error != null -> {
                        ScheduleErrorContent(
                            message = uiState.error ?: "Something went wrong",
                            onRetry = { viewModel.refresh() }
                        )
                    }
                    uiState.schedule.isEmpty() -> {
                        ScheduleEmptyContent(
                            date = uiState.selectedDate
                        )
                    }
                    else -> {
                        ScheduleContent(
                            schedule = uiState.schedule,
                            selectedDate = uiState.selectedDate,
                            onEpisodeClick = { showName ->
                                viewModel.resolveSeriesId(showName) { seriesId ->
                                    if (seriesId != null) {
                                        navController.navigate(Screen.SeriesDetails.createRoute(seriesId))
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }

        // Feedback when a scheduled show can't be matched to the catalogue.
        lookupMessage?.let { message ->
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(24.dp),
                color = CardSurface,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = message,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    color = PrimaryText,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun DateSelectorChips(
    selectedDate: String,
    onDateSelected: (String) -> Unit
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Show 14 days starting from today
        (0..13).map { daysFromNow ->
            val date = com.example.vista.util.DateUtils.isoDaysFromToday(daysFromNow)
            val isSelected = date == selectedDate

            val dayName = when (daysFromNow) {
                0 -> "Today"
                1 -> "Tomorrow"
                else -> com.example.vista.util.DateUtils.formatIso(date, "EEE")
            }
            val dayNumber = com.example.vista.util.DateUtils.formatIso(date, "dd")
            val monthName = com.example.vista.util.DateUtils.formatIso(date, "MMM")

            Column(
                modifier = Modifier
                    .width(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) Accent else CardSurface)
                    .clickable { onDateSelected(date) }
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = monthName.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) PrimaryText else MutedText,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = dayNumber,
                    style = MaterialTheme.typography.titleLarge,
                    color = if (isSelected) PrimaryText else SecondaryText,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = dayName,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) PrimaryText.copy(alpha = 0.8f) else MutedText,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun ScheduleContent(
    schedule: List<ScheduleItem>,
    selectedDate: String,
    onEpisodeClick: (String) -> Unit
) {
    // Group by show
    val groupedByShow = schedule.groupBy { it.showId to it.showName }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Date header
        item {
            Column {
                Text(
                    text = com.example.vista.util.DateUtils.formatIso(selectedDate, "EEEE, MMMM d"),
                    style = MaterialTheme.typography.titleLarge,
                    color = PrimaryText,
                    fontWeight = FontWeight.ExtraBold
                )
                val isToday = selectedDate == com.example.vista.util.DateUtils.todayIso()
                val airLabel = if (isToday) "today" else "on ${com.example.vista.util.DateUtils.formatIso(selectedDate, "d MMM")}"
                Text(
                    text = "${schedule.size} ${if (schedule.size == 1) "episode" else "episodes"} airing $airLabel",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Accent,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Schedule items grouped by show
        groupedByShow.forEach { (showInfo, episodes) ->
            item {
                ScheduleShowSection(
                    showId = showInfo.first,
                    showName = showInfo.second,
                    episodes = episodes,
                    selectedDate = selectedDate,
                    onClick = { onEpisodeClick(showInfo.second) }
                )
            }
        }
        
        item {
            BrandAttribution(modifier = Modifier.padding(vertical = 16.dp))
        }
        
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
private fun ScheduleShowSection(
    showId: Int,
    showName: String,
    episodes: List<ScheduleItem>,
    selectedDate: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(RaisedSurface)
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            val firstImage = episodes.firstOrNull()?.showImage
            if (firstImage != null) {
                com.example.vista.ui.components.CoilImage(
                    url = firstImage,
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp, 60.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = showName,
                    style = MaterialTheme.typography.titleMedium,
                    color = PrimaryText,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (episodes.size > 1) "${episodes.size} Episodes" else "New Episode",
                    style = MaterialTheme.typography.labelSmall,
                    color = Accent
                )
            }
            
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = MutedText,
                modifier = Modifier.size(20.dp)
            )
        }

        HorizontalDivider(color = Color.White.copy(alpha = 0.05f))

        episodes.forEach { episode ->
            ScheduleEpisodeItem(episode = episode, selectedDate = selectedDate)
        }
    }
}

@Composable
private fun ScheduleEpisodeItem(episode: ScheduleItem, selectedDate: String) {
    // A listing is live only when it airs today and the current time falls inside its hour window.
    val isLive = isNowWithinAirWindow(selectedDate, episode.airDate, episode.airTime)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Time with indicator - Compressed
        Column(
            modifier = Modifier.width(60.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = episode.airTime ?: "--:--",
                style = MaterialTheme.typography.titleMedium,
                color = if (isLive) Accent else PrimaryText,
                fontWeight = FontWeight.Black
            )
            
            if (isLive) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(6.dp).background(Accent, CircleShape))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "LIVE",
                        style = MaterialTheme.typography.labelSmall,
                        color = Accent,
                        fontWeight = FontWeight.Black,
                        fontSize = 9.sp
                    )
                }
            } else {
                val amPm = if ((episode.airTime?.substringBefore(":")?.toIntOrNull() ?: 0) >= 12) "PM" else "AM"
                Text(
                    text = amPm,
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedText
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = episode.episodeTitle ?: "Episode ${episode.episodeNumber}",
                style = MaterialTheme.typography.bodyMedium,
                color = PrimaryText,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "S${episode.season} • E${episode.episodeNumber} · New",
                style = MaterialTheme.typography.labelSmall,
                color = MutedText
            )
        }

        if (episode.rating != null && episode.rating > 0) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Icon(Icons.Outlined.Star, contentDescription = null, tint = RatingGold, modifier = Modifier.size(10.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = String.format(Locale.US, "%.1f", episode.rating),
                    style = MaterialTheme.typography.labelSmall,
                    color = PrimaryText,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ScheduleLoadingContent() {
    val brush = ShimmerBrush()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(6) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(brush)
            )
        }
    }
}

@Composable
private fun ScheduleEmptyContent(date: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Outlined.CalendarToday,
                contentDescription = null,
                tint = MutedText,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "No shows scheduled",
                style = MaterialTheme.typography.titleMedium,
                color = PrimaryText,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "No TV shows are scheduled for ${com.example.vista.util.DateUtils.formatIso(date, "MMMM d")}",
                style = MaterialTheme.typography.bodyMedium,
                color = MutedText
            )
        }
    }
}

@Composable
private fun ScheduleErrorContent(
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
                Icons.Outlined.ErrorOutline,
                contentDescription = null,
                tint = MutedText,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Error loading schedule",
                style = MaterialTheme.typography.titleMedium,
                color = PrimaryText,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                ViewUtils.formatError(message),
                style = MaterialTheme.typography.bodyMedium,
                color = MutedText
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
}

/**
 * Whether [airTime] (expected as "HH:mm") is currently within its broadcast window, i.e. now falls
 * in the hour starting at the listed time. Listing is ignored unless it airs on the viewed day, so
 * past days never show anything as live.
 */
private fun isNowWithinAirWindow(selectedDate: String, airDate: String?, airTime: String?): Boolean {
    if (airDate == null || airTime == null) return false
    if (airDate != selectedDate || selectedDate != com.example.vista.util.DateUtils.todayIso()) return false

    val parts = airTime.split(":", limit = 2)
    if (parts.size < 2) return false
    val airHour = parts[0].toIntOrNull() ?: return false
    val airMinute = parts[1].toIntOrNull() ?: return false
    if (airHour !in 0..23 || airMinute !in 0..59) return false

    val now = java.util.Calendar.getInstance()
    val nowMinutes = now.get(java.util.Calendar.HOUR_OF_DAY) * 60 + now.get(java.util.Calendar.MINUTE)
    val airMinutes = airHour * 60 + airMinute
    return nowMinutes in airMinutes until (airMinutes + 60)
}
