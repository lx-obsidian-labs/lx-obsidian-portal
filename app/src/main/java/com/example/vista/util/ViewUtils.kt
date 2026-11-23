package com.example.vista.util

import com.example.vista.domain.model.MediaItem

object ViewUtils {
    fun formatRuntime(minutes: Int?): String? {
        if (minutes == null || minutes <= 0) return null
        val hours = minutes / 60
        val remainingMinutes = minutes % 60
        return if (hours > 0) {
            "${hours}h ${remainingMinutes}m"
        } else {
            "${remainingMinutes}m"
        }
    }

    fun sanitizeText(text: String?): String {
        if (text == null || text == "null" || text.isEmpty()) return "Synopsis unavailable"
        var sanitized = text
        if (sanitized.endsWith("nullm")) sanitized = sanitized.replace("nullm", "").trim()
        if (sanitized.contains("null")) sanitized = sanitized.replace("null", "").trim()
        return sanitized.ifEmpty { "Synopsis unavailable" }
    }

    fun getAgeRatingColor(rating: String?): androidx.compose.ui.graphics.Color {
        return when (rating?.uppercase()) {
            "G", "TV-G" -> androidx.compose.ui.graphics.Color(0xFF4CAF50)
            "PG", "TV-PG" -> androidx.compose.ui.graphics.Color(0xFFFFEB3B)
            "PG-13", "TV-14" -> androidx.compose.ui.graphics.Color(0xFFFF9800)
            "R", "NC-17", "TV-MA" -> androidx.compose.ui.graphics.Color(0xFFF44336)
            else -> androidx.compose.ui.graphics.Color.Gray
        }
    }

    /**
     * Public page viewers are sent to. Keep this on the canonical `www` host: the update manifest is
     * fetched from the apex host, and a cross-host redirect strips query parameters, which would
     * silently break referral codes appended to this URL later.
     */
    const val SITE_URL = "https://www.lxobsidianportal.co.za"

    /**
     * Shares a title through the native chooser.
     *
     * The copy reads like a person recommending the app rather than the app recommending itself -
     * a recommendation from a known contact converts several times better than brand marketing, and
     * "free for now" is the strongest honest hook while access stays open.
     */
    fun shareMedia(context: android.content.Context, title: String) {
        shareText(context, buildShareMessage(title))
    }

    /**
     * The message every share target sends.
     *
     * Kept in one place because the share sheet and the fallback chooser must not drift apart - two
     * slightly different blurbs across channels is how marketing copy ends up inconsistent.
     */
    fun buildShareMessage(title: String): String =
        "You're watching this for free? Try Vista Watch.\n\n" +
            "\"$title\" and thousands more movies and series - all free for now, no subscription needed.\n\n" +
            SITE_URL

    /**
     * One-tap WhatsApp share. WhatsApp is the primary share surface in South Africa, so it is worth a
     * dedicated path rather than making people hunt through the chooser.
     *
     * Uses the bare `wa.me` host with no phone number: it opens the contact picker with the message
     * already written, which is the only form that works when the sender's number is unknown. The
     * `runCatching` fallback matters - without it, a viewer without WhatsApp installed hits a dead
     * end and the share is lost.
     */
    fun shareToWhatsApp(context: android.content.Context, message: String) {
        com.example.vista.data.analytics.VistaAnalytics.instance?.track("share_whatsapp", mapOf("channel" to "whatsapp"))
        val intent = android.content.Intent(
            android.content.Intent.ACTION_VIEW,
            android.net.Uri.parse("https://wa.me/?text=${android.net.Uri.encode(message)}")
        )
        runCatching { context.startActivity(intent) }
            .onFailure { shareText(context, message, trackChannel = false) }
    }

    /**
     * Posts to X. Prefers the installed app, because the web intent route drops the text out of the
     * composer on some browsers and lands on a login wall with the draft lost.
     */
    fun shareToX(context: android.content.Context, message: String) {
        com.example.vista.data.analytics.VistaAnalytics.instance?.track("share_x", mapOf("channel" to "x"))
        val appIntent = sendTextIntent(message).setPackage("com.twitter.x")
        if (launchApp(context, appIntent)) return
        openWeb(
            context,
            "https://twitter.com/intent/tweet?text=${android.net.Uri.encode(message)}"
        )
    }

    /**
     * Shares to Facebook.
     *
     * Only the installed app is used. Facebook's website sharer accepts a URL and nothing else, so
     * routing through it would silently drop the message text - the fallback is deliberately the
     * chooser rather than a downgraded share.
     */
    fun shareToFacebook(context: android.content.Context, message: String) {
        com.example.vista.data.analytics.VistaAnalytics.instance?.track("share_facebook", mapOf("channel" to "facebook"))
        if (launchApp(context, sendTextIntent(message).setPackage("com.facebook.katana"))) return
        shareText(context, message, trackChannel = false)
    }

    private fun sendTextIntent(message: String): android.content.Intent =
        android.content.Intent().apply {
            action = android.content.Intent.ACTION_SEND
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_TEXT, message)
        }

    /**
     * `startActivity` on a package-scoped intent throws when nothing is installed, which is the
     * signal we want here: it turns a missing app into a normal false instead of a crash.
     */
    private fun launchApp(context: android.content.Context, intent: android.content.Intent): Boolean =
        runCatching { context.startActivity(intent) }.isSuccess

    private fun openWeb(context: android.content.Context, url: String) {
        val intent = android.content.Intent(
            android.content.Intent.ACTION_VIEW,
            android.net.Uri.parse(url)
        )
        if (!launchApp(context, intent)) shareText(context, SITE_URL)
    }

    /** Native share sheet - the path for every channel that is not WhatsApp-specific. */
    fun shareText(
        context: android.content.Context,
        message: String,
        chooserTitle: String = "Share via",
        trackChannel: Boolean = true
    ) {
        if (trackChannel) {
            com.example.vista.data.analytics.VistaAnalytics.instance?.track("share_sheet", mapOf("channel" to "native"))
        }
        val sendIntent = android.content.Intent().apply {
            action = android.content.Intent.ACTION_SEND
            putExtra(android.content.Intent.EXTRA_TEXT, message)
            type = "text/plain"
        }
        context.startActivity(android.content.Intent.createChooser(sendIntent, chooserTitle))
    }

    fun shareViaCopyLink(context: android.content.Context) {
        com.example.vista.data.analytics.VistaAnalytics.instance?.track("share_copy_link", mapOf("channel" to "copy"))
    }

    fun formatError(error: String?): String {
        if (error == null) return "An unexpected error occurred"
        val lowercase = error.lowercase()
        return when {
            lowercase.contains("404") || lowercase.contains("not found") -> "Content not found"
            lowercase.contains("no playable source") || lowercase.contains("no sources") || lowercase.contains("could not be loaded") -> "No playback sources found. This title may not be available yet or try another server."
            lowercase.contains("500") || lowercase.contains("internal server error") -> "Server error. Please try again later."
            lowercase.contains("unknownhost") || lowercase.contains("timeout") || lowercase.contains("no address associated") -> "Connection error. Please check your internet."
            lowercase.contains("auth") || lowercase.contains("permission") -> "Authentication failed. Please sign in again."
            else -> "Something went wrong. Please try again."
        }
    }
}
