package com.example.vista.ui.cast

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.vista.domain.model.PersonDetails
import com.example.vista.domain.model.PersonMovieCredit
import com.example.vista.domain.model.PersonTvCredit
import com.example.vista.domain.model.toMediaItem
import com.example.vista.ui.components.CoilImage
import com.example.vista.ui.components.MediaCard
import com.example.vista.navigation.Screen
import com.example.vista.ui.theme.*
import com.example.vista.util.ViewUtils

@Composable
fun CastDetailsScreen(
    navController: NavController,
    viewModel: CastDetailsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when {
        uiState.isLoading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Background),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Accent)
            }
        }
        uiState.error != null -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Background),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Outlined.Person,
                        contentDescription = null,
                        tint = MutedText,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(ViewUtils.formatError(uiState.error), color = PrimaryText)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.retry() },
                        colors = ButtonDefaults.buttonColors(containerColor = Accent)
                    ) {
                        Text("Retry", color = PrimaryText)
                    }
                }
            }
        }
        else -> {
            val person = uiState.person
            if (person != null) {
                CastDetailsContent(
                    person = person,
                    onBackClick = { navController.popBackStack() },
                    onMovieClick = { movieId ->
                        navController.navigate(Screen.MovieDetails.createRoute(movieId))
                    },
                    onSeriesClick = { seriesId ->
                        navController.navigate(Screen.SeriesDetails.createRoute(seriesId))
                    }
                )
            }
        }
    }
}

@Composable
private fun CastDetailsContent(
    person: PersonDetails,
    onBackClick: () -> Unit,
    onMovieClick: (String) -> Unit,
    onSeriesClick: (String) -> Unit
) {
    var expandedBio by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        // Hero section with profile image
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
            ) {
                // Profile image
                if (person.profileUrl != null) {
                    CoilImage(
                        url = person.profileUrl,
                        contentDescription = person.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(CardSurface)
                    )
                }

                // Gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Transparent,
                                    Color.Transparent,
                                    Background
                                )
                            )
                        )
                )

                // Back button
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.TopStart)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            }
        }

        // Person info
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .offset(y = (-32).dp)
            ) {
                // Name
                Text(
                    text = person.name,
                    style = MaterialTheme.typography.headlineMedium,
                    color = PrimaryText,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1).sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Known for
                if (person.knownForDepartment != null) {
                    Text(
                        text = person.knownForDepartment.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = Accent,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Detailed Info
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (person.birthday != null) {
                        DetailRow(label = "Born", value = person.birthday)
                    }
                    if (person.placeOfBirth != null) {
                        DetailRow(label = "Birthplace", value = person.placeOfBirth)
                    }
                }
            }
        }

        // Biography
        if (!person.biography.isNullOrBlank()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Biography",
                        style = MaterialTheme.typography.titleMedium,
                        color = PrimaryText,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = person.biography,
                        style = MaterialTheme.typography.bodyMedium,
                        color = SecondaryText,
                        lineHeight = 22.sp,
                        maxLines = if (expandedBio) Int.MAX_VALUE else 5,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (person.biography.length > 200) {
                        TextButton(onClick = { expandedBio = !expandedBio }) {
                            Text(
                                text = if (expandedBio) "Show less" else "Read more",
                                color = Accent
                            )
                        }
                    }
                }
            }
        }

        // Movie credits
        if (person.movieCredits.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                ) {
                    Text(
                        text = "Movies (${person.movieCredits.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = PrimaryText,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(person.movieCredits.sortedByDescending { it.releaseYear ?: 0 }) { credit ->
                            MediaCard(
                                item = credit.toMediaItem(),
                                onClick = { onMovieClick(credit.id) },
                                modifier = Modifier.width(110.dp),
                                showTitle = true,
                                aspectRatio = 0.67f
                            )
                        }
                    }
                }
            }
        }

        // TV credits
        if (person.tvCredits.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                ) {
                    Text(
                        text = "TV Shows (${person.tvCredits.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = PrimaryText,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(person.tvCredits.sortedByDescending { it.firstAirYear ?: 0 }) { credit ->
                            MediaCard(
                                item = credit.toMediaItem(),
                                onClick = { onSeriesClick(credit.id) },
                                modifier = Modifier.width(110.dp),
                                showTitle = true,
                                aspectRatio = 0.67f
                            )
                        }
                    }
                }
            }
        }

        // Bottom spacer
        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodyMedium,
            color = MutedText,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(80.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = SecondaryText
        )
    }
}
