package com.example.vista.ui.tv

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.vista.ui.theme.Accent
import com.example.vista.util.LocalTvConfiguration

/** One focus-scroll owner: Foundation lazy layouts drive motion, not per-key coroutines. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TvFocusScrolling(content: @Composable () -> Unit) {
    val spec = remember {
        object : BringIntoViewSpec {
            override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float =
                TvFocusScrollPolicy.distance(offset, size, containerSize)
        }
    }
    CompositionLocalProvider(LocalBringIntoViewSpec provides spec, content = content)
}

/** Keep focus near the leading third, and keep the trailing edge of a fitting card visible. */
object TvFocusScrollPolicy {
    fun distance(offset: Float, size: Float, containerSize: Float): Float {
        if (!offset.isFinite() || !size.isFinite() || !containerSize.isFinite() || size <= 0f || containerSize <= 0f) return 0f
        val pivot = containerSize * 0.3f
        val target = if (size <= containerSize) minOf(pivot, containerSize - size) else 0f
        return offset - target
    }
}

/** No overshoot: focus should settle quickly when a remote direction is held down. */
internal val TvFocusScaleSpring = spring<Float>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMedium
)

@Composable
fun tvFocusScale(isFocused: Boolean): Float {
    val tvConfig = LocalTvConfiguration.current
    val scale by animateFloatAsState(
        targetValue = if (isFocused) tvConfig.focusScaleOnHover else 1f,
        animationSpec = TvFocusScaleSpring, label = "tvFocusScale"
    )
    return scale
}

@Composable
fun Modifier.tvOverscanPadding(): Modifier {
    val tvConfig = LocalTvConfiguration.current
    return if (tvConfig.isTv) padding(horizontal = tvConfig.overscanPaddingHorizontal,
        vertical = tvConfig.overscanPaddingVertical) else this
}

@Composable
fun Modifier.tvFocusHighlight(isFocused: Boolean, focusedBorderColor: Color = Accent, cornerRadius: Dp = 12.dp): Modifier {
    val tvConfig = LocalTvConfiguration.current
    return scale(tvFocusScale(isFocused)).then(
        if (isFocused && tvConfig.isTv) Modifier.border(2.5.dp, focusedBorderColor, RoundedCornerShape(cornerRadius))
        else Modifier
    )
}
