package com.example.vista.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * The single shimmer-gradient implementation in the app.
 *
 * Both the [Shimmer.kt][ShimmerBrush] helpers and the modifier API in `ShimmerEffect.kt`
 * (`shimmerEffect()`, `shimmerPulseEffect()`) draw from this one animated brush, so every loading
 * placeholder shares the same look.
 *
 * The animation itself lives in one place: [ProvideShimmerAnimation] owns a single
 * `rememberInfiniteTransition` for the whole app, and [LocalShimmerOffset] hands the value to every
 * placeholder. This matters on a loading grid - one shared transition costs one animation, while a
 * `rememberInfiniteTransition` per cell costs one per poster.
 */
@Composable
internal fun ProvideShimmerAnimation(content: @Composable () -> Unit) {
    val transition = rememberInfiniteTransition(label = "appShimmer")
    val offset = transition.animateFloat(
        initialValue = -1000f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerOffset"
    )
    CompositionLocalProvider(LocalShimmerOffset provides offset, content = content)
}

/** Shared shimmer sweep offset. Provided once above the app; reading it is all a cell needs to do. */
internal val LocalShimmerOffset = staticCompositionLocalOf<State<Float>> { mutableFloatStateOf(0f) }

/** The gradient colors every shimmer placeholder sweeps through. */
internal val ShimmerColors: List<Color> = listOf(
    Color.White.copy(alpha = 0.05f),
    Color.White.copy(alpha = 0.15f),
    Color.White.copy(alpha = 0.05f),
)

@Composable
fun ShimmerBrush(
    showShimmer: Boolean = true,
    targetValue: Float = 1000f
): Brush {
    if (!showShimmer) {
        return Brush.linearGradient(
            colors = listOf(Color.Transparent, Color.Transparent),
            start = Offset.Zero,
            end = Offset.Zero
        )
    }

    val sweep = LocalShimmerOffset.current.value

    return Brush.linearGradient(
        colors = ShimmerColors,
        start = Offset.Zero,
        end = Offset(x = sweep, y = sweep)
    )
}

@Composable
fun ShimmerMovieCard() {
    Column(modifier = Modifier.width(130.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(12.dp))
                .shimmerEffect()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(16.dp)
                .clip(RoundedCornerShape(4.dp))
                .shimmerEffect()
        )
    }
}

@Composable
fun ShimmerSection() {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .width(150.dp)
                .height(24.dp)
                .clip(RoundedCornerShape(4.dp))
                .shimmerEffect()
        )
        Spacer(modifier = Modifier.height(16.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(5) {
                ShimmerMovieCard()
            }
        }
    }
}