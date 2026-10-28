package com.example.vista.ui.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vista.ui.theme.Accent
import com.example.vista.ui.theme.AmbientRed
import com.example.vista.ui.theme.Background
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(onSplashComplete: () -> Unit) {
    val alpha = remember { Animatable(0f) }
    val scale = remember { Animatable(0.7f) }
    val ambientAlpha = remember { Animatable(0f) }
    val logoRotation = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Entry animations in parallel
        launch {
            alpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
            )
        }
        launch {
            scale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
        launch {
            logoRotation.animateTo(
                targetValue = 360f,
                animationSpec = tween(durationMillis = 1000, easing = LinearOutSlowInEasing)
            )
        }
        
        // Ambient pulse in parallel (Infinite, won't block)
        launch {
            ambientAlpha.animateTo(
                targetValue = 0.5f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2000, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
        }
        
        delay(2000)
        
        // Fade out
        alpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 300)
        )
        onSplashComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
        contentAlignment = Alignment.Center
    ) {
        // Ambient Cinematic Glow - Dynamic
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            AmbientRed.copy(alpha = ambientAlpha.value),
                            com.example.vista.ui.theme.AmbientPurple.copy(alpha = ambientAlpha.value * 0.5f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.alpha(alpha.value)
        ) {
            // Logo - Glass Box with Premium Border
            Surface(
                modifier = Modifier
                    .graphicsLayer {
                        rotationY = logoRotation.value
                        scaleX = scale.value
                        scaleY = scale.value
                    }
                    .size(120.dp),
                color = Color.White,
                shape = RoundedCornerShape(28.dp),
                shadowElevation = 16.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
            ) {
                Box(
                    modifier = Modifier.background(
                        Brush.linearGradient(
                            colors = listOf(Color.White, Color(0xFFF5F5F5))
                        )
                    ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "V",
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 64.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-4).sp
                        ),
                        color = Accent
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = "VISTA",
                style = MaterialTheme.typography.headlineLarge.copy(
                    letterSpacing = 16.sp,
                    fontWeight = FontWeight.Black,
                    fontSize = 32.sp
                ),
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "CINEMA",
                style = MaterialTheme.typography.titleMedium.copy(
                    letterSpacing = 8.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = Accent
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            LinearProgressIndicator(
                modifier = Modifier
                    .width(140.dp)
                    .height(2.dp)
                    .clip(CircleShape),
                color = Accent,
                trackColor = Color.White.copy(alpha = 0.1f)
            )
        }
    }
}
