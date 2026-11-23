package com.example.vista.domain.model

object PlaybackPreferencesPolicy {
    fun maxHeight(quality: String): Int = Regex("(360|480|720|1080|2160)p", RegexOption.IGNORE_CASE)
        .find(quality)?.groupValues?.get(1)?.toIntOrNull() ?: Int.MAX_VALUE
    fun canonicalQuality(quality: String): String = maxHeight(quality).let { if (it == Int.MAX_VALUE) "Auto" else "${it}p" }
    fun language(value: String): String? = when (value.lowercase()) {
        "english" -> "en"; "french" -> "fr"; "spanish" -> "es"; "german" -> "de"
        "portuguese" -> "pt"; "japanese" -> "ja"; else -> null
    }
}
