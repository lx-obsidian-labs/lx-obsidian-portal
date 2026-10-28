package com.example.vista.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vista.domain.model.MediaItem
import com.example.vista.ui.theme.*
import java.util.Locale

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun MediaCard(
    item: MediaItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    onDoubleTap: (() -> Unit)? = null,
    isInWatchlist: Boolean = false,
    showTitle: Boolean = true,
    showRating: Boolean = true,
    titleOverlay: Boolean = false,
    aspectRatio: Float = 0.67f
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isFocused by interactionSource.collectIsFocusedAsState()
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    
    val active = isPressed || isHovered || isFocused
    
    val scale by animateFloatAsState(
        targetValue = if (active) 1.03f else 1f,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 400f),
        label = "card_scale"
    )
    
    val elevation by animateDpAsState(
        targetValue = if (active) 16.dp else 2.dp,
        label = "elevation"
    )
    
    val glowAlpha by animateFloatAsState(
        targetValue = if (active) 0.25f else 0f,
        animationSpec = tween(500),
        label = "glow_alpha"
    )

    val baseModifier = modifier.graphicsLayer {
        this.scaleX = scale
        this.scaleY = scale
        this.shadowElevation = elevation.toPx()
    }

    // When double-tap (save) is enabled we cannot use combinedClickable as-is: detectTapGestures
    // disambiguates a single tap from a double tap, which combinedClickable does not.
    val tapModifier = if (onDoubleTap != null) {
        Modifier.pointerInput(Unit) {
            detectTapGestures(
                onTap = {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    onClick()
                },
                onDoubleTap = {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    onDoubleTap()
                },
                onLongPress = onLongClick?.let {
                    {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        it()
                    }
                }
            )
        }
    } else {
        Modifier.combinedClickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = {
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                onClick()
            },
            onLongClick = onLongClick?.let {
                {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    it()
                }
            }
        )
    }

    Column(
        modifier = baseModifier.then(tapModifier)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(aspectRatio)
                .clip(RoundedCornerShape(16.dp))
                .background(RaisedSurface)
                .border(
                    BorderStroke(
                        width = 1.dp,
                        color = if (active) Accent.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.08f)
                    ),
                    RoundedCornerShape(16.dp)
                )
        ) {
            CoilImage(
                url = item.posterUrl,
                contentDescription = item.title,
                modifier = Modifier.fillMaxSize()
            )
            
            // Outer Glow Effect
            if (active) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Accent.copy(alpha = glowAlpha), Color.Transparent),
                                center = androidx.compose.ui.geometry.Offset.Unspecified
                            )
                        )
                )
            }

            // Saved badge shown after a double-tap toggles the title into the watchlist.
            if (isInWatchlist) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp),
                    color = Accent.copy(alpha = 0.9f),
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = "In My List",
                        tint = Color.White,
                        modifier = Modifier
                            .padding(5.dp)
                            .size(13.dp)
                    )
                }
            }

            // 2026 Deep Cinema Gradient - Multi-layered
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.6f to Color.Black.copy(alpha = 0.2f),
                            1f to Color.Black.copy(alpha = 0.8f)
                        )
                    )
            )
            
            // Rating Badge - Floating Glass Style
            if (showRating && item.rating != null && item.rating!! > 0) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp),
                    color = Color.Black.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Star,
                            contentDescription = null,
                            tint = RatingGold,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = String.format(Locale.US, "%.1f", item.rating),
                            style = MaterialTheme.typography.labelSmall,
                            color = PrimaryText,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            if (titleOverlay && showTitle) {
                // Liquid Glass Overlay for metadata
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.labelLarge,
                        color = PrimaryText,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 16.sp
                    )
                    
                    val subtitle = when (item) {
                        is MediaItem.MovieItem -> item.year?.toString()
                        is MediaItem.SeriesItem -> item.year?.toString()
                        else -> null
                    }
                    
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedText,
                            maxLines = 1,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
        
        if (!titleOverlay && showTitle) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.2.sp
                ),
                color = PrimaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            val subtitle = when (item) {
                is MediaItem.MovieItem -> item.year?.toString()
                is MediaItem.SeriesItem -> item.year?.toString()
                else -> null
            }
            
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedText,
                    maxLines = 1
                )
            }
        }
    }
}
