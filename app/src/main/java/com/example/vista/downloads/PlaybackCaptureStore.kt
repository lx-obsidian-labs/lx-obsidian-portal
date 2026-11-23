package com.example.vista.downloads

import com.example.vista.data.remote.playback.EmbedAdPolicy
import com.example.vista.domain.model.StreamSource
import com.example.vista.domain.model.StreamType
import com.google.firebase.auth.FirebaseAuth
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import javax.inject.Inject
import javax.inject.Singleton

/** Ephemeral player observations, scoped to profile + exact movie/episode, never a durable URL catalog. */
@Singleton
class PlaybackCaptureStore @Inject constructor(private val auth: FirebaseAuth) {
    private data class Capture(val capturedAt: Long, val sources: List<StreamSource>)
    private val entries = mutableMapOf<String, Capture>()
    private fun key(target: DownloadTarget) = DownloadPolicy.id(auth.currentUser?.uid ?: "local-guest",
        target.mediaId, target.type, target.season, target.episode)

    @Synchronized fun record(target: DownloadTarget, provider: String, url: String, headers: Map<String, String>) {
        val parsed = url.toHttpUrlOrNull() ?: return
        if (parsed.scheme != "https" || EmbedAdPolicy.blocks(url)) return
        val type = when (parsed.encodedPath.substringAfterLast('.').lowercase()) {
            "m3u8" -> StreamType.HLS
            "mpd" -> StreamType.DASH
            "mp4", "m4v", "webm", "mkv", "mov" -> StreamType.MP4
            else -> return
        }
        val expires = listOf("exp", "expires", "expire").firstNotNullOfOrNull { parsed.queryParameter(it)?.toLongOrNull() }
        val source = StreamSource(url, type, null, null, "$provider · detected in player",
            headers.filterKeys { it.lowercase() in setOf("referer", "origin", "user-agent") }, expiresAt = expires)
        val key = key(target)
        if (entries.size >= 20 && key !in entries) entries.clear()
        entries[key] = Capture(System.currentTimeMillis(), (listOf(source) + entries[key]?.sources.orEmpty()).distinctBy { it.url }.take(8))
    }

    @Synchronized fun candidates(target: DownloadTarget): List<StreamSource> {
        val capture = entries[key(target)] ?: return emptyList()
        if (System.currentTimeMillis() - capture.capturedAt > 5 * 60_000) { entries.remove(key(target)); return emptyList() }
        return capture.sources.filter { DownloadPolicy.unavailableReason(it, System.currentTimeMillis()) == null }
    }
}
