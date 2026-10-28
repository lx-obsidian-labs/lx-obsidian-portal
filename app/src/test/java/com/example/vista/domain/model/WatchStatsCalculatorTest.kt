package com.example.vista.domain.model

import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * These run on the JVM because [WatchStatsCalculator] takes its clock as a parameter, so the
 * time-relative figures - the weekly count and the streak - can be pinned to a fixed date instead
 * of only being verifiable on whatever day the test happens to run.
 */
class WatchStatsCalculatorTest {

    private val zone: ZoneId = ZoneId.of("UTC")
    private val today: LocalDate = LocalDate.of(2026, 6, 15)
    private val nowMillis: Long = today.atStartOfDay(zone).toInstant().toEpochMilli()

    private fun progress(
        id: String,
        position: Long,
        duration: Long,
        completed: Boolean = false,
        type: MediaType = MediaType.MOVIE,
        daysAgo: Long = 0
    ) = PlaybackProgress(
        mediaId = id,
        mediaType = type,
        title = "Title $id",
        currentPosition = position,
        duration = duration,
        lastPlayedTimestamp = today.minusDays(daysAgo).atStartOfDay(zone).toInstant().toEpochMilli(),
        isCompleted = completed
    )

    @Test
    fun `empty history produces zeroed stats`() {
        val stats = WatchStatsCalculator.compute(emptyList(), nowMillis, zone)
        assertEquals(0, stats.titlesStarted)
        assertEquals(0L, stats.secondsReached)
        assertEquals(0, stats.currentStreakDays)
        assertNull(stats.longestTitle)
        assertEquals(false, stats.hasAnything)
    }

    @Test
    fun `time reached is capped at duration`() {
        // A provider that reports a position past the duration must not inflate the total.
        val stats = WatchStatsCalculator.compute(
            listOf(progress("1", position = 9_000, duration = 3_600)),
            nowMillis, zone
        )
        assertEquals(3_600L, stats.secondsReached)
    }

    @Test
    fun `zero duration does not poison the total`() {
        // duration 0 previously made currentPosition/duration infinite.
        val stats = WatchStatsCalculator.compute(
            listOf(progress("1", position = 120, duration = 0)),
            nowMillis, zone
        )
        assertEquals(120L, stats.secondsReached)
    }

    @Test
    fun `negative position is ignored`() {
        val stats = WatchStatsCalculator.compute(
            listOf(progress("1", position = -50, duration = 600)),
            nowMillis, zone
        )
        assertEquals(0L, stats.secondsReached)
    }

    @Test
    fun `completed and in-progress are counted separately`() {
        val stats = WatchStatsCalculator.compute(
            listOf(
                progress("1", 3_600, 3_600, completed = true),
                progress("2", 600, 3_600)
            ),
            nowMillis, zone
        )
        assertEquals(2, stats.titlesStarted)
        assertEquals(1, stats.titlesCompleted)
        assertEquals(1, stats.titlesInProgress)
    }

    @Test
    fun `movies and series are split by type not inferred`() {
        val stats = WatchStatsCalculator.compute(
            listOf(
                progress("1", 600, 600, completed = true, type = MediaType.MOVIE),
                progress("2", 600, 600, completed = true, type = MediaType.SERIES),
                progress("3", 600, 600, completed = true, type = MediaType.SERIES)
            ),
            nowMillis, zone
        )
        assertEquals(1, stats.moviesWatched)
        assertEquals(2, stats.seriesWatched)
    }

    @Test
    fun `streak counts consecutive days ending today`() {
        val stats = WatchStatsCalculator.compute(
            listOf(
                progress("1", 600, 600, daysAgo = 0),
                progress("2", 600, 600, daysAgo = 1),
                progress("3", 600, 600, daysAgo = 2)
            ),
            nowMillis, zone
        )
        assertEquals(3, stats.currentStreakDays)
    }

    @Test
    fun `streak still stands when nothing has been played today`() {
        // Otherwise the streak reads 0 every morning and permanently looks broken.
        val stats = WatchStatsCalculator.compute(
            listOf(
                progress("1", 600, 600, daysAgo = 1),
                progress("2", 600, 600, daysAgo = 2)
            ),
            nowMillis, zone
        )
        assertEquals(2, stats.currentStreakDays)
    }

    @Test
    fun `streak breaks after a missed day`() {
        val stats = WatchStatsCalculator.compute(
            listOf(
                progress("1", 600, 600, daysAgo = 0),
                // day 2 missing
                progress("2", 600, 600, daysAgo = 3)
            ),
            nowMillis, zone
        )
        assertEquals(1, stats.currentStreakDays)
    }

    @Test
    fun `streak is zero when the last play was long ago`() {
        val stats = WatchStatsCalculator.compute(
            listOf(progress("1", 600, 600, daysAgo = 10)),
            nowMillis, zone
        )
        assertEquals(0, stats.currentStreakDays)
    }

    @Test
    fun `two plays on one day do not double the streak`() {
        val stats = WatchStatsCalculator.compute(
            listOf(
                progress("1", 600, 600, daysAgo = 0),
                progress("2", 600, 600, daysAgo = 0)
            ),
            nowMillis, zone
        )
        assertEquals(1, stats.currentStreakDays)
    }

    @Test
    fun `this week includes today back seven days`() {
        val stats = WatchStatsCalculator.compute(
            listOf(
                progress("today", 600, 600, daysAgo = 0),
                progress("six", 600, 600, daysAgo = 6),
                progress("seven", 600, 600, daysAgo = 7)
            ),
            nowMillis, zone
        )
        assertEquals(2, stats.watchedThisWeek)
    }

    @Test
    fun `all completed is only true when nothing is in progress`() {
        val all = WatchStatsCalculator.compute(
            listOf(progress("1", 600, 600, completed = true)),
            nowMillis, zone
        )
        assertEquals(true, all.allCompleted)

        val partial = WatchStatsCalculator.compute(
            listOf(progress("1", 600, 600, completed = true), progress("2", 60, 600)),
            nowMillis, zone
        )
        assertEquals(false, partial.allCompleted)
    }

    @Test
    fun `timestamps of zero are ignored for streaks and weekly counts`() {
        val stats = WatchStatsCalculator.compute(
            listOf(
                PlaybackProgress(
                    mediaId = "1",
                    mediaType = MediaType.MOVIE,
                    title = "No timestamp",
                    currentPosition = 600,
                    duration = 600,
                    lastPlayedTimestamp = 0L
                )
            ),
            nowMillis, zone
        )
        assertEquals(0, stats.currentStreakDays)
        assertEquals(0, stats.watchedThisWeek)
        // Still counted as a started title; only the date-derived figures ignore it.
        assertEquals(1, stats.titlesStarted)
    }

    @Test
    fun `time formatting does not report zero for sub-minute playback`() {
        assertEquals("12h 30m", formatWatchTime(45_000))
        assertEquals("45m", formatWatchTime(2_700))
        assertEquals("<1m", formatWatchTime(30))
        assertEquals("<1m", formatWatchTime(0))
        assertEquals("<1m", formatWatchTime(-10))
    }
}