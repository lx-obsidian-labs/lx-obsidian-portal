package com.example.vista.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.vista.ui.theme.CardSurface
import com.example.vista.ui.theme.MutedText
import com.example.vista.ui.theme.SecondaryText

/**
 * Required attribution for The Movie Database (TMDB).
 * Per TMDB guidelines, apps using their API must display this notice and logo.
 */
@Composable
fun TmdbAttribution(
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(CardSurface, RoundedCornerShape(12.dp))
            .clickable { uriHandler.openUri("https://www.themoviedb.org") }
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CoilImage(
            url = "https://www.themoviedb.org/assets/2/v4/logos/v2/blue_short-8e7b30f73a4020692ccca9c88bafe5dcb6f8a62a4c6bc55cd9ba82bb2cd95f6c.svg",
            contentDescription = "The Movie Database (TMDB)",
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(40.dp),
            cornerRadius = 0.dp
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "This product uses the TMDB API but is not endorsed or certified by TMDB.",
            style = MaterialTheme.typography.bodySmall,
            color = SecondaryText,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Movies, TV shows and cast data provided by The Movie Database.",
            style = MaterialTheme.typography.labelSmall,
            color = MutedText,
            textAlign = TextAlign.Center
        )
    }
}
