package com.example.vista.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Aggregate figures over everything the viewer has played.
 *
 * "Time watched" is deliberately *time reached*, not time spent: [PlaybackProgress.currentPosition]
 * is the furthest point reached in a title and is overwritten on every rewatch, so a film watched
 * three times counts once. That is the only figure the stored data can honestly support - a true
 * "minutes sat watching" number would need per-session telemetry this app does not record.
 */
data class WatchStats(
    val titlesStarted: Int = 0,
    val titlesCompleted: Int = 0,
    val titlesInProgress: Int = 0,
    val moviesWatched: Int = 0,
    val seriesWatched: Int = 0,
    val secondsReached: Long = 0L,
    val watchedThisWeek: Int = 0,
    val currentStreakDays: Int = 0,
    val longestTitle: PlaybackProgress? = null,
    val mostRecent: PlaybackProgress? = null
) {
    val hasAnything: Boolean get() = titlesStarted > 0

    /** True when every started title was finished - "a perfect run", shown as its own message. */
    val allCompleted: Boolean get() = titlesStarted > 0 && titlesCompleted == titlesStarted
}

/**
 * Derives [WatchStats] from raw history.
 *
 * Pure and clock-injected so it is testable without a device or a fixed "now", which matters
 * because the streak and the this-week count are both time-relative and would otherwise be
 * untestable except on the day they were written.
 */
object WatchStatsCalculator {

    /**
     * A title counts towards the current streak if it was played on that day.
     *
     * A streak that requires an entry dated *today* would read as broken every morning, before the
     * viewer has watched anything, and would therefore always display 0 for anyone who does not
     * watch daily. Allowing the streak to stand if yesterday was played, and only then breaking it,
     * matches what the number means to a viewer.
     */
    fun compute(
        history: List<PlaybackProgress>,
        nowMillis: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault()
    ): WatchStats {
        if (history.isEmpty()) return WatchStats()

        val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()

        // Guard the arithmetic: a provider reporting duration 0 would otherwise make the ratio
        // infinite and poison the running total with nonsense.
        val reachedSeconds = history.sumOf { entry ->
            val position = entry.currentPosition.coerceAtLeast(0L)
            if (entry.duration > 0) position.coerceAtMost(entry.duration) else position
        }

        val playedDays = history
            .filter { it.lastPlayedTimestamp > 0L }
            .map { Instant.ofEpochMilli(it.lastPlayedTimestamp).atZone(zone).toLocalDate() }
            .toSet()

        return WatchStats(
            titlesStarted = history.size,
            titlesCompleted = history.count { it.isCompleted },
            titlesInProgress = history.count { !it.isCompleted && it.progress > 0f },
            moviesWatched = history.count { it.mediaType == MediaType.MOVIE },
            seriesWatched = history.count { it.mediaType == MediaType.SERIES },
            secondsReached = reachedSeconds,
            watchedThisWeek = history.count { entry ->
                entry.lastPlayedTimestamp > 0L &&
                    !Instant.ofEpochMilli(entry.lastPlayedTimestamp).atZone(zone).toLocalDate()
                        .isBefore(today.minus(6, ChronoUnit.DAYS))
            },
            currentStreakDays = streakFrom(playedDays, today),
            longestTitle = history.maxByOrNull { it.currentPosition },
            mostRecent = history.maxByOrNull { it.lastPlayedTimestamp }
        )
    }

    /** Counts back from today (or yesterday) while consecutive days are present in [playedDays]. */
    private fun streakFrom(playedDays: Set<LocalDate>, today: LocalDate): Int {
        if (playedDays.isEmpty()) return 0

        var cursor = when {
            playedDays.contains(today) -> today
            playedDays.contains(today.minusDays(1)) -> today.minusDays(1)
            else -> return 0
        }

        var streak = 0
        while (playedDays.contains(cursor)) {
            streak++
            cursor = cursor.minusDays(1)
        }
        return streak
    }
}

/** "12h 30m", or "45m" under an hour. Avoids showing "0m" for anything non-zero. */
fun formatWatchTime(seconds: Long): String {
    val safe = seconds.coerceAtLeast(0L)
    val hours = safe / 3600
    val minutes = (safe % 3600) / 60
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m"
        // Under a minute still happened; reporting "0m" reads as a bug rather than a short clip.
        else -> "<1m"
    }
}