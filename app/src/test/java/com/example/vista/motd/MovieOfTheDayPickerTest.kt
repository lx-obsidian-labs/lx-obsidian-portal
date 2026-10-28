package com.example.vista.motd

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class MovieOfTheDayPickerTest {

    /**
     * Known Gregorian Easter Sundays. If this drifts the seasonal catalogue quietly points at the
     * wrong film in the wrong years, which is the kind of bug nobody reports.
     */
    @Test
    fun `easter falls on the known dates`() {
        val known = mapOf(
            2024 to LocalDate.of(2024, 3, 31),
            2025 to LocalDate.of(2025, 4, 20),
            2026 to LocalDate.of(2026, 4, 5),
            2027 to LocalDate.of(2027, 3, 28),
            2028 to LocalDate.of(2028, 4, 16)
        )
        known.forEach { (year, expected) ->
            assertEquals("Easter $year", expected, SeasonalCatalog.easterSunday(year))
        }
    }

    @Test
    fun `easter window covers the holiday but not the month around it`() {
        val easter = SeasonalCatalog.easterSunday(2026)
        assertEquals("easter", SeasonalCatalog.activeEventFor(easter)?.name)
        assertEquals("easter", SeasonalCatalog.activeEventFor(easter.plusDays(7))?.name)
        // Two weeks later is a normal day, and must not still be Easter.
        assertNull(SeasonalCatalog.activeEventFor(easter.plusDays(21)))
    }

    @Test
    fun `public holidays resolve to their own event`() {
        assertEquals("heritage_month", SeasonalCatalog.activeEventFor(LocalDate.of(2026, 3, 15))?.name)
        // Freedom Day is 27 April, a month after Heritage Month closes. Guards against a month-wide
        // window being widened later and quietly swallowing it.
        assertEquals("freedom_day", SeasonalCatalog.activeEventFor(LocalDate.of(2026, 4, 27))?.name)
        assertEquals("youth_day", SeasonalCatalog.activeEventFor(LocalDate.of(2026, 6, 16))?.name)
        assertEquals("mandela_day", SeasonalCatalog.activeEventFor(LocalDate.of(2026, 7, 18))?.name)
        assertEquals("womens_day", SeasonalCatalog.activeEventFor(LocalDate.of(2026, 8, 9))?.name)
        assertEquals("heritage_day", SeasonalCatalog.activeEventFor(LocalDate.of(2026, 9, 24))?.name)
    }

    @Test
    fun `festive outranks the summer run`() {
        assertEquals("festive", SeasonalCatalog.activeEventFor(LocalDate.of(2026, 12, 10))?.name)
        assertEquals("summer", SeasonalCatalog.activeEventFor(LocalDate.of(2026, 11, 25))?.name)
    }

    @Test
    fun `an ordinary day has no event`() {
        assertNull(SeasonalCatalog.activeEventFor(LocalDate.of(2026, 5, 14)))
    }

    @Test
    fun `the same day always gives the same film`() {
        val date = LocalDate.of(2026, 12, 24)
        val first = MovieOfTheDayPicker.pick(date)
        repeat(5) {
            assertEquals(first.movie.tmdbId, MovieOfTheDayPicker.pick(date).movie.tmdbId)
        }
    }

    @Test
    fun `consecutive days do not repeat the same film while a pool lasts`() {
        var date = LocalDate.of(2026, 5, 1)
        val seen = mutableSetOf<Int>()
        repeat(SeasonalCatalog.everydayPool.size) {
            seen.add(MovieOfTheDayPicker.pick(date).movie.tmdbId)
            date = date.plusDays(1)
        }
        assertEquals(SeasonalCatalog.everydayPool.size, seen.size)
    }

    @Test
    fun `watched films are excluded`() {
        val date = LocalDate.of(2026, 5, 1)
        val expected = MovieOfTheDayPicker.pick(date).movie.tmdbId
        val watched = setOf(expected.toString())

        val next = MovieOfTheDayPicker.pick(date.plusDays(1), watched)
        assertTrue("picked a film the viewer has watched", next.movie.tmdbId.toString() !in watched)
    }

    /** A fully watched pool must still produce a suggestion rather than nothing. */
    @Test
    fun `a fully watched pool still returns something`() {
        val date = LocalDate.of(2026, 5, 1)
        val everything = SeasonalCatalog.everydayPool.map { it.tmdbId.toString() }.toSet()
        assertNotNull(MovieOfTheDayPicker.pick(date, everything))
    }

    @Test
    fun `every curated entry has a title and a blurb`() {
        val entries = SeasonalCatalog.events.flatMap { it.movies } + SeasonalCatalog.everydayPool
        assertTrue(entries.isNotEmpty())
        entries.forEach {
            assertTrue("blank title on ${it.tmdbId}", it.title.isNotBlank())
            assertTrue("blank blurb on ${it.tmdbId}", it.blurb.isNotBlank())
            assertTrue("implausible year on ${it.tmdbId}", it.year in 1930..2026)
        }
    }

    @Test
    fun `no film is listed twice`() {
        val entries = SeasonalCatalog.events.flatMap { it.movies } + SeasonalCatalog.everydayPool
        val duplicates = entries.groupBy { it.tmdbId }.filterValues { it.size > 1 }.keys
        // Duplicates across different windows are intentional; only same-window repeats are a bug.
        SeasonalCatalog.events.forEach { event ->
            val withinEvent = event.movies.groupBy { it.tmdbId }.filterValues { it.size > 1 }.keys
            assertTrue("repeated within ${event.name}: $withinEvent", withinEvent.isEmpty())
        }
        assertTrue(duplicates.isNotEmpty())
    }
}
