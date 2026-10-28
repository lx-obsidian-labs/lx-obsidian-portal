package com.example.vista.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.vista.domain.model.formatWatchTime
import com.example.vista.ui.theme.*

/**
 * Lifetime viewing figures.
 *
 * Every number here is computed from the local history table, so the screen works offline and needs
 * no account. It deliberately reports *time reached* rather than time spent: see [WatchStats].
 */
@Composable
fun StatsScreen(
    navController: NavController,
    viewModel: StatsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .background(
                    Brush.verticalGradient(listOf(AmbientPurple, Color.Transparent))
                )
        )

        if (uiState.isEmpty) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Outlined.Insights,
                    contentDescription = null,
                    tint = MutedText,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Nothing to show yet",
                    color = PrimaryText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Start watching something and your viewing stats will appear here",
                    color = MutedText,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            return
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Your Stats",
                    style = MaterialTheme.typography.headlineMedium,
                    color = PrimaryText,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = "All time, on this device",
                    style = MaterialTheme.typography.labelMedium,
                    color = Accent,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            item {
                StatTile(
                    icon = Icons.Outlined.Schedule,
                    value = formatWatchTime(uiState.stats.secondsReached),
                    label = "Watched",
                    // Stated plainly so the figure is not read as precise measured sitting time.
                    caption = "Time reached across all titles"
                )
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile(
                        icon = Icons.Outlined.CheckCircle,
                        value = uiState.stats.titlesCompleted.toString(),
                        label = "Finished",
                        modifier = Modifier.weight(1f)
                    )
                    StatTile(
                        icon = Icons.Outlined.PlayCircle,
                        value = uiState.stats.titlesInProgress.toString(),
                        label = "In progress",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (uiState.stats.currentStreakDays > 0) {
                item {
                    StatTile(
                        icon = Icons.Outlined.LocalFireDepartment,
                        value = "${uiState.stats.currentStreakDays} " +
                            if (uiState.stats.currentStreakDays == 1) "day" else "days",
                        label = "Current streak",
                        caption = "Consecutive days with something played"
                    )
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile(
                        icon = Icons.Outlined.Movie,
                        value = uiState.stats.moviesWatched.toString(),
                        label = "Films",
                        modifier = Modifier.weight(1f)
                    )
                    StatTile(
                        icon = Icons.Outlined.Tv,
                        value = uiState.stats.seriesWatched.toString(),
                        label = "Series",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (uiState.stats.watchedThisWeek > 0) {
                item {
                    StatTile(
                        icon = Icons.Outlined.Today,
                        value = uiState.stats.watchedThisWeek.toString(),
                        label = "Last 7 days",
                        caption = "Titles played in the past week"
                    )
                }
            }

            uiState.stats.longestTitle?.let { longest ->
                item {
                    StatTile(
                        icon = Icons.Outlined.EmojiEvents,
                        value = longest.title,
                        label = "Longest title reached",
                        caption = formatWatchTime(longest.currentPosition) +
                            if (longest.isCompleted) " · finished" else " · unfinished",
                        // A long title is common, so the value is a title and needs to wrap.
                        valueIsTitle = true
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun StatTile(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    caption: String? = null,
    valueIsTitle: Boolean = false
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = CardSurface,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Accent,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = value,
                color = PrimaryText,
                fontWeight = FontWeight.Black,
                fontSize = if (valueIsTitle) 18.sp else 28.sp,
                maxLines = if (valueIsTitle) 2 else 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = label,
                color = MutedText,
                style = MaterialTheme.typography.labelMedium
            )
            caption?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = it,
                    color = MutedText.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}