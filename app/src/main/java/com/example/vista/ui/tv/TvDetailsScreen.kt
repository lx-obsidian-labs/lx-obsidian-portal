package com.example.vista.ui.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.*
import com.example.vista.ui.details.DetailsViewModel
import com.example.vista.ui.theme.Background
import com.example.vista.ui.theme.PrimaryText
import com.example.vista.ui.theme.SecondaryText

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvDetailsScreen(
    viewModel: DetailsViewModel = hiltViewModel(),
    onBackClick: () -> Unit,
    onWatchClick: (String, String, com.example.vista.domain.model.MediaType, Int?, Int?, String?) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        val movie = uiState.movie
        val series = uiState.series
        val title = movie?.title ?: series?.title ?: ""
        val backdrop = movie?.backdropUrl ?: series?.backdropUrl ?: movie?.posterUrl ?: series?.posterUrl
        val rating = movie?.rating ?: series?.rating
        val overview = movie?.overview ?: series?.overview ?: ""
        val id = movie?.id ?: series?.id ?: ""
        val poster = movie?.posterUrl ?: series?.posterUrl

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                androidx.compose.material3.CircularProgressIndicator(color = com.example.vista.ui.theme.Accent)
            }
        } else if (uiState.error != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = uiState.error ?: "Failed to load content",
                        style = MaterialTheme.typography.bodyLarge,
                        color = com.example.vista.ui.theme.MutedText
                    )
                    Spacer(Modifier.height(24.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        androidx.tv.material3.Button(onClick = { viewModel.retry() }) {
                            Text("Retry")
                        }
                        androidx.tv.material3.OutlinedButton(onClick = onBackClick) {
                            Text("Go Back")
                        }
                    }
                }
            }
        } else if (id.isNotEmpty()) {
            // Backdrop
            com.example.vista.ui.components.CoilImage(
                url = backdrop,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            
            // Cinematic Gradient Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            0f to Background,
                            0.7f to Background.copy(alpha = 0.9f),
                            1f to Color.Transparent,
                            start = androidx.compose.ui.geometry.Offset.Zero,
                            end = androidx.compose.ui.geometry.Offset(1800f, 0f)
                        )
                    )
            )

            // Content
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.6f)
                    .padding(horizontal = 64.dp, vertical = 48.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-2).sp
                    ),
                    color = PrimaryText,
                    maxLines = 2
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    val year = if (uiState.isSeries) series?.firstAirYear else movie?.releaseYear
                    if (year != null) {
                        Text(text = year.toString(), color = SecondaryText, style = MaterialTheme.typography.titleMedium)
                    }
                    
                    if (rating != null && rating > 0) {
                        Text(
                            text = "⭐ ${String.format(java.util.Locale.US, "%.1f", rating)}",
                            style = MaterialTheme.typography.titleMedium,
                            color = com.example.vista.ui.theme.RatingGold,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (uiState.isSeries) {
                        Box(
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.1f), androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "SERIES",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = overview,
                    style = MaterialTheme.typography.bodyLarge,
                    color = SecondaryText,
                    maxLines = 6,
                    lineHeight = 28.sp
                )
                
                Spacer(modifier = Modifier.height(48.dp))
                
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(
                        onClick = {
                            val isSeries = uiState.isSeries
                            onWatchClick(
                                id,
                                title,
                                if (isSeries) com.example.vista.domain.model.MediaType.SERIES
                                else com.example.vista.domain.model.MediaType.MOVIE,
                                if (isSeries) uiState.resumeSeason else null,
                                if (isSeries) uiState.resumeEpisode else null,
                                poster
                            )
                        },
                        scale = ButtonDefaults.scale(focusedScale = 1.1f)
                    ) {
                        Text("Watch Now", fontWeight = FontWeight.Bold)
                    }
                    
                    OutlinedButton(
                        onClick = onBackClick,
                        scale = ButtonDefaults.scale(focusedScale = 1.1f)
                    ) {
                        Text("Go Back")
                    }
                }
            }
        }
    }
}
