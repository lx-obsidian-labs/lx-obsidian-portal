package com.example.vista.util

/** Current public identity; also normalizes labels from older stored announcements/push payloads. */
object VistaBrand {
    const val NAME = "Vista Watch"
    const val WORDMARK = "VISTA WATCH"

    fun normalize(text: String): String = text
        .replace("Vista Cinema", NAME)
        .replace("VISTA CINEMA", WORDMARK)
}
