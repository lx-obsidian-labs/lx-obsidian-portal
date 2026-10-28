package com.example.vista.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset

/**
 * Loading effect types for image placeholders.
 */
enum class LoadingEffectType {
    SHIMMER,      // Animated gradient from left to right
    PULSE,        // Fade in/out opacity animation
    SHIMMER_PULSE // Combined shimmer with subtle pulse
}

/**
 * Modifier extension that adds a shimmer loading effect to any composable.
 *
 * Reads the shared sweep offset ([LocalShimmerOffset]) in the draw phase, so the animated gradient
 * redraws without re-running composition per frame.
 */
fun Modifier.shimmerEffect(): Modifier = composed {
    val offset = LocalShimmerOffset.current
    Modifier.drawBehind {
        drawRect(
            brush = Brush.linearGradient(
                colors = ShimmerColors,
                start = Offset.Zero,
                end = Offset(x = offset.value, y = offset.value)
            )
        )
    }
}

/**
 * Modifier extension that adds a pulse loading effect to any composable.
 * The pulse fades the content in and out smoothly.
 */
fun Modifier.pulseEffect(): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "pulse")
    val alpha = transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 800,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Modifier.graphicsLayer { this.alpha = alpha.value }
}

/**
 * Modifier extension that combines shimmer and pulse effects for a more dynamic loading state.
 */
fun Modifier.shimmerPulseEffect(): Modifier = composed {
    val offset = LocalShimmerOffset.current

    // Subtle pulse over the same animated gradient used by shimmerEffect().
    val transition = rememberInfiniteTransition(label = "shimmer_pulse")
    val pulseAlpha = transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 600,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Modifier.drawBehind {
        drawRect(
            brush = Brush.linearGradient(
                colors = ShimmerColors,
                start = Offset.Zero,
                end = Offset(x = offset.value, y = offset.value)
            ),
            alpha = pulseAlpha.value
        )
    }
}

/**
 * Modifier extension that applies the specified loading effect type.
 */
fun Modifier.loadingEffect(type: LoadingEffectType = LoadingEffectType.SHIMMER): Modifier {
    return when (type) {
        LoadingEffectType.SHIMMER -> shimmerEffect()
        LoadingEffectType.PULSE -> pulseEffect()
        LoadingEffectType.SHIMMER_PULSE -> shimmerPulseEffect()
    }
}

/**
 * A composable that displays a loading placeholder with the specified effect type.
 *
 * @param modifier Modifier to apply to the placeholder
 * @param effectType The type of loading effect to use
 */
@Composable
fun LoadingPlaceholder(
    modifier: Modifier = Modifier,
    effectType: LoadingEffectType = LoadingEffectType.SHIMMER
) {
    Box(
        modifier = modifier.loadingEffect(effectType)
    )
}

/**
 * A composable that displays a shimmer placeholder.
 */
@Composable
fun ShimmerPlaceholder(
    modifier: Modifier = Modifier
) {
    LoadingPlaceholder(
        modifier = modifier,
        effectType = LoadingEffectType.SHIMMER
    )
}

/**
 * A composable that displays a pulse placeholder.
 */
@Composable
fun PulsePlaceholder(
    modifier: Modifier = Modifier
) {
    LoadingPlaceholder(
        modifier = modifier,
        effectType = LoadingEffectType.PULSE
    )
}