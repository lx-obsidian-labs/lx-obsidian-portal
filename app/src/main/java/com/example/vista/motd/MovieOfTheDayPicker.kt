package com.example.vista.motd

import java.time.LocalDate

/** Today's suggestion plus the context needed to present it. */
data class MovieOfTheDay(
    val movie: SeasonalMovie,
    val eventLabel: String,
    val isSeasonal: Boolean
)

/**
 * Chooses the day's film.
 *
 * Deliberately pure and date-seeded rather than randomised or server-driven: every install shows the
 * same title on the same day, so a recommendation can be discussed, shared, or complained about, and
 * nothing about this feature needs a backend or a network call to work offline.
 */
object MovieOfTheDayPicker {

    /**
     * The pick for [date], excluding anything in [watchedTmdbIds].
     *
     * Seeded on the epoch day so the rotation walks the list and then starts again, and so two
     * devices cannot disagree. If the viewer has somehow watched the whole pool, the exclusion is
     * dropped rather than returning nothing - a repeat is a better failure than silence.
     */
    fun pick(date: LocalDate, watchedTmdbIds: Set<String> = emptySet()): MovieOfTheDay {
        val event = SeasonalCatalog.activeEventFor(date)
        val pool = event?.movies?.takeIf { it.isNotEmpty() } ?: SeasonalCatalog.everydayPool
        val eventLabel = event?.label ?: "Today's pick"
        val isSeasonal = event != null

        val unwatched = pool.filterNot { it.tmdbId.toString() in watchedTmdbIds }
        val candidates = unwatched.ifEmpty { pool }

        // Epoch day is large and positive, but the double modulo keeps this correct if that ever
        // stops being true - a negative index would throw rather than wrap.
        val index = ((date.toEpochDay() % candidates.size) + candidates.size) % candidates.size
        val movie = candidates[index.toInt()]
        return MovieOfTheDay(movie = movie, eventLabel = eventLabel, isSeasonal = isSeasonal)
    }
}
