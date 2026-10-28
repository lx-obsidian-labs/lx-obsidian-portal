package com.example.vista.ui.tv

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * A horizontal row that scrolls itself to whatever has focus.
 *
 * A plain [androidx.compose.foundation.lazy.LazyRow] on TV is the wrong tool: D-pad focus moves
 * instantly while the list scrolls afterwards, so the focused card arrives at the screen edge with
 * no warning and the row reads as jumping. Driving the scroll from focus means the motion and the
 * highlight happen together, which is what a viewer expects.
 */
class TvRowScroller internal constructor(
    val state: LazyListState,
    private val focusTo: (Int) -> Unit
) {
    /** Call from an item's `onFocusChanged` when it gains focus. */
    fun onItemFocused(index: Int) = focusTo(index)
}

/** How far a focused row is pulled back from the left edge, so its neighbour stays visible. */
private val RowLeadIn: Dp = 96.dp

/**
 * Creates a [TvRowScroller] for a row of [itemCount] cards.
 *
 * The focused card is scrolled to one slot in from the left rather than flush against it, so the
 * viewer can see what they just came from. Near the start of the row that offset is dropped,
 * because there is nothing before it to reveal.
 */
@Composable
fun rememberTvRowScroller(itemCount: Int): TvRowScroller {
    val state = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val leadInPx = with(LocalDensity.current) { RowLeadIn.toPx() }.toInt()

    return remember(state, scope, itemCount, leadInPx) {
        TvRowScroller(state) { index ->
            if (itemCount > 0) {
                val lastIndex = itemCount - 1
                val target = (index - 1).coerceIn(0, lastIndex)
                val offset = if (target == 0) 0 else -leadInPx
                scope.launch {
                    state.animateScrollToItem(target, offset)
                }
            }
        }
    }
}

/** Spring used for focus scale on cards. Snappy on the way in, settled on the way out. */
internal val TvFocusScaleSpring: SpringSpec<Float> = spring(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessLow
)

/**
 * Animates a card's scale on focus.
 *
 * TV Material's own [androidx.tv.material3.Surface] focus treatment is subtle on a dark background
 * at viewing distance; the scale is what actually tells the viewer which card the D-pad is on.
 */
@Composable
internal fun tvFocusScale(isFocused: Boolean): Float {
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.08f else 1f,
        animationSpec = TvFocusScaleSpring,
        label = "tvFocusScale"
    )
    return scale
}
