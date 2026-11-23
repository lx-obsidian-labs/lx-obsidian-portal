package com.example.vista.motd

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class FestiveSeasonTest {
    @Test fun seasonStartsInNovemberAndEndsAfterNewYearWithoutNegativeCountdowns() {
        assertNull(FestiveSeason.moment(LocalDate.of(2026, 10, 31)))
        assertNotNull(FestiveSeason.moment(LocalDate.of(2026, 11, 1)))
        assertEquals("1 day to Christmas", FestiveSeason.moment(LocalDate.of(2026, 12, 24))?.countdown)
        assertEquals("Merry Christmas from Vista", FestiveSeason.moment(LocalDate.of(2026, 12, 25))?.heading)
        assertEquals(2026, FestiveSeason.moment(LocalDate.of(2027, 1, 5))?.seasonYear)
        assertNull(FestiveSeason.moment(LocalDate.of(2027, 1, 6)))
        assertEquals(2027, FestiveSeason.moment(LocalDate.of(2027, 11, 1))?.seasonYear)
    }
    @Test fun dailyRotationCanExcludeWatchedTitlesAndSurvivesFullyWatchedCollection() {
        val date = LocalDate.of(2026, 12, 20)
        val first = FestiveSeason.dailyPick(date)
        assertEquals(first, FestiveSeason.dailyPick(date))
        assertNotEquals(first.tmdbId, FestiveSeason.dailyPick(date, setOf(first.tmdbId.toString())).tmdbId)
        assertNotNull(FestiveSeason.dailyPick(date, FestiveSeason.collections.flatMap { it.movies }.map { it.tmdbId.toString() }.toSet()))
    }
    @Test fun festiveMovieOfDayUsesTheSameChristmasCatalog() {
        val pool = FestiveSeason.collections.flatMap { it.movies }.map { it.tmdbId }.toSet()
        assertTrue(MovieOfTheDayPicker.pick(LocalDate.of(2026, 12, 20)).movie.tmdbId in pool)
        assertEquals(508, SeasonalCatalog.events.first { it.name == "valentines" }.movies.first().tmdbId)
    }
}
