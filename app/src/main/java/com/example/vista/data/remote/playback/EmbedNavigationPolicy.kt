package com.example.vista.data.remote.playback

/** Never hand a provider's navigation to Opera, an app store, a downloader or another app. */
object EmbedNavigationPolicy {
    fun blocks(url: String, mainFrame: Boolean): Boolean {
        if (mainFrame) return !EmbedOrigins.allowsTopLevel(url)
        val parsed = try { java.net.URI(url.trim()) } catch (_: Exception) { return true }
        return when (parsed.scheme?.lowercase()) {
            "https" -> EmbedAdPolicy.blocks(url)
            "about" -> url != "about:blank"
            "blob", "data" -> false // In-document frames, not external applications.
            else -> true
        }
    }
}
