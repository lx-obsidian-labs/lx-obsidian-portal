package com.example.vista.ui.sharing

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.vista.domain.model.SharingMilestone
import com.example.vista.ui.theme.*
import com.example.vista.util.ViewUtils
import kotlinx.coroutines.delay

/**
 * Where a milestone notification always lands, so the viewer never has to go looking for a share
 * button.
 *
 * The message arrives pre-written and pre-filled, and the share action is the single obvious thing
 * on screen. Copy stays editable because people share things differently and a forced sentence
 * reads like an ad.
 */
@Composable
fun ShareMilestoneScreen(
    milestoneId: String,
    navController: NavController,
    viewModel: ShareMilestoneViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Pre-fill once from the resolved milestone, then let the viewer own the text.
    var message by remember { mutableStateOf("") }
    LaunchedEffect(uiState.milestone) {
        uiState.milestone?.let { message = it.fullShareText() }
    }

    BackHandler(enabled = true) {
        viewModel.onDeclined()
        navController.popBackStack()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            Text(text = "That deserves a mention", color = Accent, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)

            Spacer(modifier = Modifier.height(20.dp))

            // The pulsing outline is the only moving thing on the screen, and it stops on its own
            // after a few seconds. A permanently blinking element reads as an error state and is
            // hostile to anyone sensitive to motion.
            MilestoneCard(
                milestone = uiState.milestone,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "Message ready - edit it if you like",
                color = MutedText,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                maxLines = 8,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Accent,
                    unfocusedBorderColor = CardSurface,
                    focusedTextColor = PrimaryText,
                    unfocusedTextColor = PrimaryText,
                    cursorColor = Accent
                ),
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    viewModel.onShared()
                    // Reuses the app's existing share path, so the native chooser is pre-filled
                    // with the same message that is on screen.
                    ViewUtils.shareText(context, message)
                },
                enabled = message.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Color.Black)
            ) {
                Icon(Icons.Outlined.IosShare, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text("Share", fontWeight = FontWeight.Black, fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = {
                    viewModel.onDeclined()
                    navController.popBackStack()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Not now", color = MutedText, fontWeight = FontWeight.SemiBold)
            }

            Text(
                text = "We only ask after something you've actually achieved, and never more than twice a month.",
                color = MutedText.copy(alpha = 0.7f),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/** Emoji, headline and the glow, with the glow settling rather than blinking forever. */
@Composable
private fun MilestoneCard(
    milestone: SharingMilestone?,
    modifier: Modifier = Modifier
) {
    var pulsing by remember { mutableStateOf(true) }
    val transition = rememberInfiniteTransition(label = "milestoneGlow")
    val pulse by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "milestoneGlowAlpha"
    )

    // Roughly three blinks, then hold steady.
    LaunchedEffect(Unit) {
        delay(6_500)
        pulsing = false
    }

    val glow = if (pulsing) pulse else 1f

    Surface(
        modifier = modifier
            .shadow(18.dp, RoundedCornerShape(24.dp), ambientColor = Accent.copy(alpha = 0.5f * glow)),
        color = CardSurface,
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .border(
                    width = 2.dp,
                    brush = Brush.verticalGradient(
                        listOf(Accent.copy(alpha = glow), AmbientPurple.copy(alpha = glow * 0.6f))
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = milestone?.emoji ?: "🎉", fontSize = 44.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = milestone?.headline ?: "Nice work",
                color = PrimaryText,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black
            )
        }
    }
}

/** The pre-written post: the milestone copy plus the link people need for it to be an invitation. */
private fun SharingMilestone.fullShareText(): String =
    "$shareBody\n\n$SHARE_FOOTER"

private const val SHARE_FOOTER =
    "Get Vista Cinema - it's free, no card needed:\n${ViewUtils.SITE_URL}"