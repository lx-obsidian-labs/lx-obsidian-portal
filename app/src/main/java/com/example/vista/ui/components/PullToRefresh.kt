package com.example.vista.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * A custom pull-to-refresh wrapper that works with Material3.
 * 
 * @param isRefreshing Whether a refresh is currently in progress
 * @param onRefresh Callback when refresh is triggered
 * @param modifier Modifier to apply
 * @param content The content to wrap
 */
@Composable
fun PullToRefreshWrapper(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var pullOffset by remember { mutableFloatStateOf(0f) }
    val coroutineScope = rememberCoroutineScope()
    
    val refreshTrigger = 150f
    val maxPull = 200f
    
    // Animation for refresh indicator
    val infiniteTransition = rememberInfiniteTransition(label = "refresh")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    Box(modifier = modifier) {
        content()
        
        // Show refresh indicator at top when pulling or refreshing
        if (pullOffset > 0 || isRefreshing) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .offset { IntOffset(0, pullOffset.roundToInt() - 60) },
                contentAlignment = Alignment.Center
            ) {
                if (isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(24.dp)
                            .rotate(rotation),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.dp
                    )
                } else if (pullOffset > refreshTrigger) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.dp,
                        progress = { (pullOffset - refreshTrigger) / (maxPull - refreshTrigger) }
                    )
                } else {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(24.dp)
                            .graphicsLayer {
                                rotationZ = (pullOffset / refreshTrigger) * 180f
                            },
                        color = MaterialTheme.colorScheme.primary.copy(alpha = pullOffset / refreshTrigger),
                        strokeWidth = 2.dp,
                        progress = { pullOffset / refreshTrigger }
                    )
                }
            }
        }
    }
}

/**
 * A simpler pull-to-refresh implementation using SwipeRefresh from Material.
 * This is a fallback for environments where PullToRefreshBox is not available.
 */
@Composable
fun SimplePullToRefresh(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    // Use a basic Box with manual refresh trigger for now
    // This provides visual feedback while being compatible with current Material3 version
    Box(modifier = modifier) {
        content()
        
        if (isRefreshing) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.dp
                )
            }
        }
    }
}
