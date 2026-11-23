package com.example.vista.downloads

import com.example.vista.domain.model.StreamSource
import com.example.vista.domain.model.StreamType
import com.example.vista.data.remote.playback.EmbedAdPolicy
import java.security.MessageDigest

object DownloadPolicy {
    fun id(owner: String, mediaId: String, type: String, season: Int, episode: Int): String =
        MessageDigest.getInstance("SHA-256").digest("$owner|$type|$mediaId|$season|$episode".toByteArray())
            .joinToString("") { "%02x".format(it) }

    fun unavailableReason(source: StreamSource, nowMs: Long): String? = when {
        source.type == StreamType.OTHER -> "This source supports embedded streaming only."
        !source.url.startsWith("https://") || EmbedAdPolicy.blocks(source.url) -> "This is not a supported media source."
        source.drmInfo != null -> "This source requires an offline license that Vista does not support yet."
        source.expiresAt != null && expiresAtMs(source.expiresAt) <= nowMs + 60_000 -> "This source has expired or is about to expire. Refresh availability."
        else -> null
    }

    fun expiresAtMs(value: Long): Long = if (value < 1_000_000_000_000L) value * 1_000L else value
}

data class DownloadTarget(
    val mediaId: String, val title: String, val type: String,
    val season: Int = 0, val episode: Int = 0
) {
    val label: String get() = if (type == "series") "$title · S$season E$episode" else title
}

data class OfflineMetadata(
    val owner: String, val target: DownloadTarget, val headers: Map<String, String>,
    val provider: String, val quality: String, val expiresAtMs: Long? = null
)

data class OfflineEntry(
    val id: String, val metadata: OfflineMetadata, val state: Int,
    val bytes: Long, val totalBytes: Long, val percent: Float, val waitingForNetwork: Boolean,
    val stopReason: Int = 0
)
