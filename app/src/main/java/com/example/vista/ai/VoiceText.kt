package com.example.vista.ai

/** Keep spoken turns concise and avoid reading citations/URLs as dialogue. */
object VoiceText {
    fun prepare(text: String): String {
        val readable = text.replace(Regex("https?://\\S+"), "")
            .replace(Regex("\\[\\d+\\]"), "").replace(Regex("[*#`]"), "")
            .replace(Regex("\\s+"), " ").trim()
        if (readable.length <= 1700) return readable
        val prefix = readable.take(1700)
        val boundary = prefix.lastIndexOfAny(charArrayOf('.', '!', '?')).takeIf { it >= 900 }
            ?: prefix.lastIndexOf(' ').coerceAtLeast(0)
        return prefix.take(if (boundary > 0) boundary + 1 else 1700).trim() + " You can read the rest on screen."
    }
}
