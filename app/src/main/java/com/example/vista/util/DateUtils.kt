package com.example.vista.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Date helpers built on [Calendar]/[SimpleDateFormat] rather than `java.time`.
 *
 * The module targets minSdk 24 and does not enable core library desugaring, so `java.time` is only
 * available from API 26. The TV guide used `LocalDate` directly, which threw NoClassDefFoundError on
 * Android 7.x devices the app still declares support for.
 *
 * Dates are passed around as ISO `yyyy-MM-dd` strings, which sort and compare correctly as plain
 * strings and map straight onto what the TV Maze API expects.
 */
object DateUtils {

    const val ISO_PATTERN = "yyyy-MM-dd"

    private fun parser(): SimpleDateFormat =
        SimpleDateFormat(ISO_PATTERN, Locale.US).apply { isLenient = false }

    private fun formatter(pattern: String): SimpleDateFormat =
        SimpleDateFormat(pattern, Locale.getDefault())

    fun todayIso(): String = parser().format(Date())

    /** ISO date [days] from today, e.g. 0 for today, 1 for tomorrow. */
    fun isoDaysFromToday(days: Int): String {
        val calendar = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, days) }
        return parser().format(calendar.time)
    }

    fun fromEpochMillis(millis: Long): String = parser().format(Date(millis))

    /** Parses an ISO date, returning null instead of throwing on malformed input. */
    fun parseIsoOrNull(iso: String?): Long? {
        if (iso.isNullOrBlank()) return null
        return try {
            parser().parse(iso)?.time
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Normalises a date that may be an ISO date or an ISO date-time, so schedule entries with either
     * shape group under the same day.
     */
    fun toIsoOrNull(raw: String?): String? {
        val trimmed = raw?.trim().orEmpty()
        if (trimmed.length < 10) return null
        val candidate = trimmed.substring(0, 10)
        return if (parseIsoOrNull(candidate) != null) candidate else null
    }

    /** Formats an ISO date with [pattern]; returns the input unchanged if it cannot be parsed. */
    fun formatIso(iso: String, pattern: String): String {
        val millis = parseIsoOrNull(iso) ?: return iso
        return formatter(pattern).format(Date(millis))
    }

    /** Formats an epoch-millis timestamp as a friendly local date, e.g. "3 Sep 2026". */
    fun formatDate(epochMillis: Long, pattern: String = "d MMM yyyy"): String {
        if (epochMillis <= 0L) return "—"
        return try {
            formatter(pattern).format(Date(epochMillis))
        } catch (_: Exception) {
            "—"
        }
    }

    /** Compact relative time for the announcement bell: "now", "5m", "3h", "2d", else a date. */
    fun timeAgo(epochMillis: Long): String {
        if (epochMillis <= 0L) return ""
        val diffMs = System.currentTimeMillis() - epochMillis
        val minutes = diffMs / 60_000L
        return when {
            minutes < 1 -> "now"
            minutes < 60 -> "${minutes}m"
            minutes < 60 * 24 -> "${minutes / 60}h"
            minutes < 60 * 24 * 7 -> "${minutes / (60 * 24)}d"
            else -> formatDate(epochMillis)
        }
    }

    /** Whole days between a timestamp and now, for the "welcome back" screen. */
    fun daysSince(epochMillis: Long): Int {
        if (epochMillis <= 0L) return 0
        val then = Calendar.getInstance().apply { timeInMillis = epochMillis }
        val now = Calendar.getInstance()
        then.set(Calendar.HOUR_OF_DAY, 0)
        then.set(Calendar.MINUTE, 0)
        then.set(Calendar.SECOND, 0)
        then.set(Calendar.MILLISECOND, 0)
        now.set(Calendar.HOUR_OF_DAY, 0)
        now.set(Calendar.MINUTE, 0)
        now.set(Calendar.SECOND, 0)
        now.set(Calendar.MILLISECOND, 0)
        return ((now.timeInMillis - then.timeInMillis) / (24 * 60 * 60 * 1000L)).toInt()
    }
}
