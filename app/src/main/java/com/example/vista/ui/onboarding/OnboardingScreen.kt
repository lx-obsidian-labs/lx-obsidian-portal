package com.example.vista.ui.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.vista.navigation.Screen
import com.example.vista.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Cinematic onboarding.
 *
 * First-time viewers see a three-act brand introduction (the Vista story, why Vista, then a
 * personal name step). Returning viewers who have been away for a while are welcomed back with a
 * single graceful screen instead of being re-dropped into the full tour.
 */
@Composable
fun OnboardingScreen(
    navController: NavController,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isWelcomeBack = uiState.isWelcomeBack

    // Divided pager with animated hero + feature slides.
    if (!isWelcomeBack) {
        WelcomeSequence(navController, viewModel, uiState)
    } else {
        WelcomeBackScreen(navController, viewModel, uiState)
    }
}

// ── Full first-time sequence ──

@Composable
private fun WelcomeSequence(
    navController: NavController,
    viewModel: OnboardingViewModel,
    uiState: OnboardingUiState
) {
    var step by remember { mutableStateOf(0) }
    var name by remember { mutableStateOf(uiState.suggestedName) }
    val transition = remember { MutableTransitionState(initialState = false) }
    transition.targetState = true

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        // Ambient cinematic glow that intensifies on the final step.
        val glowAlpha by animateFloatAsState(
            targetValue = if (step == 2) 0.5f else 0.22f,
            animationSpec = tween(700),
            label = "glow"
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            AmbientRed.copy(alpha = glowAlpha),
                            AmbientPurple.copy(alpha = glowAlpha * 0.45f),
                            Color.Transparent
                        )
                    )
                )
        )

        AnimatedContent(
            targetState = step,
            transitionSpec = {
                (fadeIn(tween(450)) + slideInHorizontally { it / 4 }).togetherWith(
                    fadeOut(tween(300)) + slideOutHorizontally { -it / 4 }
                )
            },
            label = "onboarding_steps"
        ) { current ->
            when (current) {
                0 -> ActOneBrand(uiState)
                1 -> ActTwoWhyVista(uiState)
                else -> ActThreePersonalName(uiState, name, { name = it }, screenHeightFraction = 1f)
            }
        }

        // Skip affordance (only for the first-time flow; name is the last step anyway).
        TextButton(
            onClick = { viewModel.finish(name); navController.navigateToHome() },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(16.dp)
                .alpha(if (step == 2) 0.4f else 1f)
        ) {
            Text(
                if (step == 2) "Continue without naming me" else "Skip",
                color = MutedText,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )
        }

        // Bottom controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 32.dp, vertical = 24.dp)
        ) {
            ProgressDots(current = step, total = 3)

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    if (currentStepContinue(step)) {
                        step++
                    } else {
                        viewModel.finish(name)
                        navController.navigateToHome()
                    }
                },
                enabled = step != 2 || name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Accent,
                    contentColor = PrimaryText,
                    disabledContainerColor = Color.White.copy(alpha = 0.08f),
                    disabledContentColor = MutedText
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Text(
                    text = when (step) {
                        0 -> "Begin"
                        1 -> "Next"
                        else -> "Open Vista Cinema"
                    },
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                )
            }
        }
    }
}

private fun currentStepContinue(step: Int): Boolean = step < 2

// ── Act I: the Vista story ──

@Composable
private fun ActOneBrand(uiState: OnboardingUiState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AnimatedLogo()

        Spacer(modifier = Modifier.height(40.dp))

        AnimatedTextEntry {
            Text(
                text = "VISTA",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 14.sp,
                    fontSize = 52.sp
                ),
                color = PrimaryText
            )
        }

        AnimatedTextEntry(delay = 250) {
            Text(
                text = "C I N E M A",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 10.sp
                ),
                color = Accent
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        AnimatedTextEntry(delay = 500) {
            Text(
                text = "Every great story deserves\nan extraordinary stage.",
                style = MaterialTheme.typography.bodyLarge.copy(
                    lineHeight = 26.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = SecondaryText,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        AnimatedTextEntry(delay = 700) {
            Text(
                text = "Welcome to the screen built for the stories\nthat stay with you.",
                style = MaterialTheme.typography.bodyMedium,
                color = MutedText,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(0.9f)
            )
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
private fun AnimatedLogo(
    size: Int = 120,
    accent: Color = Accent
) {
    val scale = remember { Animatable(0.7f) }
    val alpha = remember { Animatable(0f) }
    val rotation = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch { scale.animateTo(1f, tween(900, easing = FastOutSlowInEasing)) }
        launch { alpha.animateTo(1f, tween(600)) }
        launch { rotation.animateTo(360f, tween(1400, easing = LinearOutSlowInEasing)) }
    }

    Surface(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
                rotationZ = rotation.value
                this.alpha = alpha.value
            }
            .size(size.dp),
        color = Color.White,
        shape = RoundedCornerShape(28.dp),
        shadowElevation = 20.dp
    ) {
        Box(
            modifier = Modifier.background(Brush.linearGradient(listOf(Color.White, Color(0xFFF1F1F1)))),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "V",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-4).sp,
                    fontSize = (size - 40).sp
                ),
                color = accent
            )
        }
    }
}

@Composable
private fun AnimatedTextEntry(
    delay: Int = 0,
    content: @Composable () -> Unit
) {
    val transition = remember { MutableTransitionState(false) }
    LaunchedEffect(Unit) {
        delay(delay.toLong())
        transition.targetState = true
    }

    AnimatedVisibility(
        visibleState = transition,
        enter = fadeIn(tween(700)) + slideInVertically { it / 2 }
    ) {
        content()
    }
}

// ── Act II: why Vista ──

private data class OnboardingFeature(
    val icon: ImageVector,
    val title: String,
    val detail: String
)

private val VistaFeatures = listOf(
    OnboardingFeature(
        icon = Icons.Outlined.Theaters,
        title = "A curated catalogue",
        detail = "Films and series drawn from the world's best libraries, presented with care."
    ),
    OnboardingFeature(
        icon = Icons.Outlined.AutoAwesome,
        title = "Recommendations that learn you",
        detail = "Every watch sharpens your personal home screen. No noise, just what you'll love."
    ),
    OnboardingFeature(
        icon = Icons.Outlined.Favorite,
        title = "Your list, your place",
        detail = "Watchlists and Continue Watching follow you anywhere, precisely where you left off."
    )
)

@Composable
private fun ActTwoWhyVista(uiState: OnboardingUiState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 32.dp)
            .statusBarsPadding()
            .padding(top = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AnimatedTextEntry {
            Text(
                text = "Why Vista Cinema",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).sp
                ),
                color = PrimaryText
            )
        }
        AnimatedTextEntry(delay = 150) {
            Text(
                text = "Designed for people who take stories seriously.",
                style = MaterialTheme.typography.bodyLarge,
                color = SecondaryText,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        VistaFeatures.forEachIndexed { index, feature ->
            AnimatedTextEntry(delay = 300 + index * 200) {
                FeatureRow(feature = feature, index = index)
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
private fun FeatureRow(feature: OnboardingFeature, index: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = CardSurface.copy(alpha = 0.6f),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(gradientFor(index), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(feature.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = feature.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = PrimaryText,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = feature.detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryText,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

private fun gradientFor(index: Int): Brush = when (index % 3) {
    0 -> Brush.linearGradient(listOf(Accent, AmbientRed))
    1 -> Brush.linearGradient(listOf(AmbientPurple, Accent))
    else -> Brush.linearGradient(listOf(Accent, AmbientPurple))
}

// ── Act III: personal ──

@Composable
private fun ActThreePersonalName(
    uiState: OnboardingUiState,
    name: String,
    onNameChange: (String) -> Unit,
    screenHeightFraction: Float = 1f
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 32.dp)
            .statusBarsPadding()
            .padding(top = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AnimatedTextEntry {
            Text(
                text = if (name.isBlank()) "Make it yours" else "Perfect, $name",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1).sp
                ),
                color = PrimaryText
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        AnimatedTextEntry(delay = 150) {
            Text(
                text = "Tell us what to call you.",
                style = MaterialTheme.typography.bodyLarge,
                color = SecondaryText
            )
        }

        Spacer(modifier = Modifier.height(36.dp))

        AnimatedTextEntry(delay = 300) {
            OutlinedTextField(
                value = name,
                onValueChange = { onNameChange(it.take(32)) },
                placeholder = { Text("Your name", color = MutedText) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Accent,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                    focusedTextColor = PrimaryText,
                    unfocusedTextColor = PrimaryText,
                    cursorColor = Accent,
                    focusedContainerColor = RaisedSurface.copy(alpha = 0.5f),
                    unfocusedContainerColor = RaisedSurface.copy(alpha = 0.5f)
                )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        AnimatedTextEntry(delay = 450) {
            Text(
                text = "We'll greet you by name, every time you open Vista.",
                style = MaterialTheme.typography.labelMedium,
                color = MutedText
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

// ── Welcome back (returning viewer) ──

@Composable
private fun WelcomeBackScreen(
    navController: NavController,
    viewModel: OnboardingViewModel,
    uiState: OnboardingUiState
) {
    val greeting by deriveTimeGreeting()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            AmbientPurple.copy(alpha = 0.28f),
                            AmbientRed.copy(alpha = 0.12f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AnimatedLogo(size = 96)

            Spacer(modifier = Modifier.height(32.dp))

            AnimatedTextEntry {
                Text(
                    text = "$greeting${if (uiState.suggestedName.isNotBlank()) ", ${uiState.suggestedName}" else ""}",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-1).sp
                    ),
                    color = PrimaryText,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            AnimatedTextEntry(delay = 200) {
                Text(
                    text = if (uiState.daysAway > 0) {
                        "It's been ${uiState.daysAway} day${if (uiState.daysAway > 1) "s" else ""}. The screen kept your seat warm."
                    } else {
                        "Good to have you back on the big screen."
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = SecondaryText,
                    textAlign = TextAlign.Center,
                    lineHeight = 26.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            AnimatedTextEntry(delay = 350) {
                Text(
                    text = "Continue Watching is exactly where you left it.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedText,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            AnimatedTextEntry(delay = 500) {
                Button(
                    onClick = {
                        viewModel.finish(uiState.suggestedName)
                        navController.navigateToHome()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Accent,
                        contentColor = PrimaryText
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    Text(
                        text = "Back to the screen",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun ProgressDots(current: Int, total: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(total) { index ->
            val width by animateDpAsState(
                targetValue = if (index == current) 22.dp else 8.dp,
                animationSpec = tween(300),
                label = "dotWidth"
            )
            Box(
                modifier = Modifier
                    .height(8.dp)
                    .width(width)
                    .clip(CircleShape)
                    .background(
                        if (index <= current) Accent
                        else Color.White.copy(alpha = 0.25f)
                    )
            )
        }
    }
}

/** Time-of-day greeting ("Good morning", "Good afternoon", "Good evening"). */
private fun timeGreeting(): String {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return when {
        hour in 5..11 -> "Good morning"
        hour in 12..17 -> "Good afternoon"
        else -> "Good evening"
    }
}

@Composable
private fun deriveTimeGreeting(): State<String> {
    val greetingState = remember { mutableStateOf(timeGreeting()) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(60_000)
            greetingState.value = timeGreeting()
        }
    }
    return greetingState
}

private fun NavController.navigateToHome() {
    navigate(Screen.Home.route) {
        popUpTo(Screen.Splash.route) { inclusive = true }
    }
}