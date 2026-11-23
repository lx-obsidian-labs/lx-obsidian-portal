package com.example.vista.data.remote.playback

import com.example.vista.domain.model.MediaType
import com.example.vista.domain.model.StreamSource
import com.example.vista.domain.model.StreamType
import okhttp3.HttpUrl.Companion.toHttpUrl

/** Optional, embed-only fallback: NexStream manages its own source switching in its player. */
object BackupMovieSource {
    fun create(tmdbId: String?, mediaType: MediaType, apiKey: String): StreamSource? {
        val key = apiKey.trim()
        if (mediaType != MediaType.MOVIE || tmdbId?.toIntOrNull()?.let { it > 0 } != true ||
            key.isBlank() || key == "backup_movies_api_key_here" || key == "your_nexstream_api_key_here"
        ) return null

        val url = EmbedOrigins.NEXSTREAM.toHttpUrl().newBuilder()
            .addPathSegments("embed/movie")
            .addPathSegment(tmdbId)
            .addQueryParameter("apikey", key)
            .build()
        return StreamSource(
            url = url.toString(),
            type = StreamType.OTHER,
            quality = "Auto",
            bitrate = null,
            name = "NexStream (backup movie)"
        )
    }
}
