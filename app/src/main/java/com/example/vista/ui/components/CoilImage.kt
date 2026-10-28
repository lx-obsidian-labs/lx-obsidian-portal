package com.example.vista.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrokenImage
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.size.Size
import com.example.vista.ui.theme.MutedText
import com.example.vista.ui.theme.RaisedSurface

/** Fallback decode cap used until the layout is measured once (never worse than a plain 1024 cap). */
private const val FALLBACK_DECODE_SIZE = 1024

/**
 * A reusable image composable that shows loading effects while images are loading from network.
 */
@Composable
fun CoilImage(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    cornerRadius: Dp = 0.dp,
    loadingEffect: LoadingEffectType = LoadingEffectType.SHIMMER
) {
    // Coil 2 decodes at full source resolution unless the request pins a size. Instead of a fixed
    // 1024 px cap, decode at the measured display size so small poster cells only pay for the
    // pixels they actually draw. Measured once per layout pass, so recomposition cost ~0.
    var targetSize by remember { mutableStateOf(Size(FALLBACK_DECODE_SIZE, FALLBACK_DECODE_SIZE)) }
    val painter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(LocalContext.current)
            .data(url)
            .size(targetSize)
            .build()
    )
    
    val state = painter.state

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(RaisedSurface)
            .onSizeChanged {
                targetSize = Size(it.width, it.height)
            }
    ) {
        androidx.compose.foundation.Image(
            painter = painter,
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = Modifier.fillMaxSize()
        )

        when (state) {
            is AsyncImagePainter.State.Loading -> {
                LoadingPlaceholder(
                    modifier = Modifier.fillMaxSize(),
                    effectType = loadingEffect
                )
            }
            is AsyncImagePainter.State.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(RaisedSurface),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.BrokenImage,
                        contentDescription = "Failed to load image",
                        tint = MutedText,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            else -> {}
        }
    }
}

/**
 * A simplified image composable for quick use with default shimmer effect.
 */
@Composable
fun NetworkImage(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    CoilImage(
        url = url,
        contentDescription = contentDescription,
        modifier = modifier,
        cornerRadius = 0.dp,
        loadingEffect = LoadingEffectType.SHIMMER
    )
}
