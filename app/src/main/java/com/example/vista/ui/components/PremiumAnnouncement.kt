package com.example.vista.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vista.ui.theme.Accent
import com.example.vista.ui.theme.AmbientPurple
import com.example.vista.ui.theme.CardSurface
import com.example.vista.ui.theme.MutedText
import com.example.vista.ui.theme.PrimaryText
import com.example.vista.ui.theme.RaisedSurface
import com.example.vista.ui.theme.SecondaryText
import java.util.Calendar
import java.util.Locale

/**
 * Premium transition announcement for 1.0.5.
 *
 * Vista Watch premium planning information. A live
 * countdown runs everywhere the announcement appears so the change always feels current and
 * intentional rather than a wall of copy.
 */
enum class LaunchStatus {
    PLANNED,
    LIVE,
    PAUSED
}

object PremiumPlan {

    const val PRICE_PER_MONTH_ZAR = 20
    const val CURRENCY = "R"

    /** Server launch status override. Default PLANNED until verified server activation. */
    var serverLaunchStatus: LaunchStatus = LaunchStatus.PLANNED

    fun launchMillis(): Long = run {
        val c = Calendar.getInstance()
        c.set(2027, Calendar.JANUARY, 1, 0, 0, 0)
        c.set(Calendar.MILLISECOND, 0)
        c.timeInMillis
    }

    /** Remaining time in (days, hours, minutes, seconds); all zero once the launch has passed. */
    data class Remaining(val days: Long, val hours: Long, val minutes: Long, val seconds: Long) {
        val isZero: Boolean get() = days <= 0 && hours <= 0 && minutes <= 0 && seconds <= 0
    }

    fun remaining(): Remaining {
        val diffMs = (launchMillis() - System.currentTimeMillis()).coerceAtLeast(0L)
        val totalSeconds = diffMs / 1000L
        return Remaining(
            days = totalSeconds / 86_400L,
            hours = (totalSeconds % 86_400L) / 3_600L,
            minutes = (totalSeconds % 3_600L) / 60L,
            seconds = totalSeconds % 60L
        )
    }

    fun isLive(): Boolean {
        return serverLaunchStatus == LaunchStatus.LIVE
    }

    fun priceLabel(): String = "$CURRENCY$PRICE_PER_MONTH_ZAR / month"
}

/** Ticks once per second so every countdown box stays live. */
@Composable
fun rememberPremiumRemaining(): PremiumPlan.Remaining {
    var remaining by remember { mutableStateOf(PremiumPlan.remaining()) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000L)
            remaining = PremiumPlan.remaining()
        }
    }
    return remaining
}

// ── Standalone dialog (shown on first launch of 1.0.5) ──

@Composable
fun PremiumPromptDialog(
    onAcknowledge: () -> Unit,
    onDismiss: () -> Unit
) {
    val remaining = rememberPremiumRemaining()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardSurface,
        shape = RoundedCornerShape(28.dp),
        icon = {
            PremiumBadge(Modifier.size(64.dp))
        },
        title = {
            Text(
                text = "Vista Watch is going Premium",
                color = PrimaryText,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (com.example.vista.artwork.rememberArtworkResource(com.example.vista.artwork.VistaArtwork.PREMIUM) != null) {
                    com.example.vista.artwork.VistaHeroBackground(com.example.vista.artwork.VistaArtwork.PREMIUM,
                        Modifier.fillMaxWidth().height(160.dp))
                    Spacer(Modifier.height(16.dp))
                }
                Text(
                    text = "From 1 January 2027, Vista becomes ${PremiumPlan.priceLabel()}. We're building a quieter, faster, richer experience for the stories you love.",
                    color = SecondaryText,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                if (PremiumPlan.isLive()) {
                    Text(
                        text = "Premium is live — welcome to the next chapter.",
                        color = Accent,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    PremiumCountdownBoxes(remaining)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Memberships open soon. This is your heads-up — nothing changes today.",
                    color = MutedText,
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onAcknowledge,
                colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = PrimaryText),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("Got it", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {}
    )
}

private fun twoDigits(value: Long): String = String.format(Locale.US, "%02d", value)

/** Four value boxes: Days / Hours / Minutes / Seconds. */
@Composable
fun PremiumCountdownBoxes(remaining: PremiumPlan.Remaining) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        CountdownUnit(modifier = Modifier.weight(1f), label = "Days", value = remaining.days.toString())
        CountdownUnit(modifier = Modifier.weight(1f), label = "Hours", value = twoDigits(remaining.hours))
        CountdownUnit(modifier = Modifier.weight(1f), label = "Min", value = twoDigits(remaining.minutes))
        CountdownUnit(modifier = Modifier.weight(1f), label = "Sec", value = twoDigits(remaining.seconds))
    }
}

@Composable
private fun CountdownUnit(modifier: Modifier = Modifier, label: String, value: String) {
    Column(
        modifier = modifier
            .background(RaisedSurface, RoundedCornerShape(12.dp))
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = (-1).sp
            ),
            color = PrimaryText
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MutedText
        )
    }
}

// ── Compact card (used inside the notification bell sheet and profile) ──

@Composable
fun PremiumCard(
    modifier: Modifier = Modifier,
    onTap: () -> Unit
) {
    val remaining = rememberPremiumRemaining()

    Surface(
        modifier = modifier,
        color = Color.Transparent,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Accent.copy(alpha = 0.35f))
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        listOf(
                            Accent.copy(alpha = 0.16f),
                            AmbientPurple.copy(alpha = 0.10f),
                            Color.Transparent
                        )
                    )
                )
                .clickable(onClick = onTap)
                .padding(20.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PremiumBadge(Modifier.size(40.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Vista Watch is going Premium",
                            style = MaterialTheme.typography.titleSmall,
                            color = PrimaryText,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "From 1 Jan 2027 • ${PremiumPlan.priceLabel()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MutedText
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(
                        Icons.Outlined.Star,
                        contentDescription = null,
                        tint = Accent
                    )
                }

                if (!PremiumPlan.isLive()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Premium launches in",
                        style = MaterialTheme.typography.labelMedium,
                        color = MutedText
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    PremiumCountdownBoxes(remaining)
                } else {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Premium is live — welcome to the next chapter.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Accent,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun PremiumBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(Brush.linearGradient(listOf(Accent, AmbientPurple)), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Outlined.Star,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(32.dp)
        )
    }
}
