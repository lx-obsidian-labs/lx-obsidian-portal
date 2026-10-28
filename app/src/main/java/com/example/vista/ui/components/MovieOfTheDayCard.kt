package com.example.vista.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.vista.motd.MovieOfTheDay
import com.example.vista.ui.theme.Accent
import com.example.vista.ui.theme.MutedText
import com.example.vista.ui.theme.PrimaryText
import com.example.vista.ui.theme.ThemeColors

/**
 * The daily suggestion, shown once on the home screen.
 *
 * A card rather than a modal: a dialog on every launch would be an ambush, and the whole point is to
 * give a reason to open the app without demanding anything first. It is dismissible and does not
 * come back the same day.
 */
@Composable
fun MovieOfTheDayCard(
    suggestion: MovieOfTheDay,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    onNotInterested: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            Accent.copy(alpha = 0.22f),
                            ThemeColors.raisedSurface
                        )
                    )
                )
                .border(
                    1.dp,
                    Accent.copy(alpha = 0.35f),
                    RoundedCornerShape(16.dp)
                )
                .clickable(onClick = onOpen)
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    tint = Accent,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = suggestion.eventLabel.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = Accent,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onDismiss)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Dismiss",
                        tint = MutedText,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = suggestion.movie.title,
                style = MaterialTheme.typography.titleMedium,
                color = PrimaryText,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = suggestion.movie.blurb,
                style = MaterialTheme.typography.bodySmall,
                color = MutedText,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${suggestion.movie.year}  -  Free on Vista",
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedText
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "Not interested",
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedText,
                    modifier = Modifier
                        .clickable { onNotInterested?.invoke() ?: onDismiss() }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Watch now",
                    style = MaterialTheme.typography.labelMedium,
                    color = Accent,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = null,
                    tint = Accent,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
