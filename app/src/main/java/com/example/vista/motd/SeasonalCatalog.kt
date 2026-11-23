package com.example.vista.motd

import java.time.LocalDate

/**
 * One Movie of the Day candidate.
 *
 * The blurb ships with the entry rather than being fetched, because the daily notification is built
 * on a background worker with no guarantee of a live network by the time it runs. A suggestion that
 * silently has no text to show is worse than no suggestion.
 */
data class SeasonalMovie(
    val tmdbId: Int,
    val title: String,
    val year: Int,
    val blurb: String
)

/**
 * A date window plus the films that suit it.
 *
 * [matches] is a predicate rather than a start/end pair because Easter and the Easter school holidays
 * move with the calendar; a fixed window would have to be rewritten every year and would be wrong
 * within a couple of years.
 */
data class SeasonalEvent(
    val name: String,
    val label: String,
    val matches: (LocalDate) -> Boolean,
    val movies: List<SeasonalMovie>
)

/**
 * Seasonal and occasion picks.
 *
 * Order is precedence: [activeEventFor] returns the first match, so the narrow windows sit above the
 * wide ones. Overlaps are deliberate - the festive fortnight outranks the summer holiday run, and
 * a specific public holiday outranks the month it sits in.
 *
 * Curated ids are TMDB movie ids. They are deliberately limited to titles that travel well in South
 * Africa and internationally; breadth beyond this list comes from the top-rated pool, so a thin
 * window never has to guess at an id.
 */
object SeasonalCatalog {

    private fun between(date: LocalDate, start: Pair<Int, Int>, end: Pair<Int, Int>): Boolean {
        val value = date.monthValue * 100 + date.dayOfMonth
        return value in (start.first * 100 + start.second)..(end.first * 100 + end.second)
    }

    /**
     * Anonymous Gregorian computus.
     *
     * Easter is the only movable feast in the catalogue that is worth computing exactly; "around
     * April" is how a feature ends up pointing at the wrong film in the wrong years.
     */
    fun easterSunday(year: Int): LocalDate {
        val a = year % 19
        val b = year / 100
        val c = year % 100
        val d = b / 4
        val e = b % 4
        val f = (b + 8) / 25
        val g = (b - f + 1) / 3
        val h = (19 * a + b - d - g + 15) % 30
        val i = c / 4
        val k = c % 4
        val l = (32 + 2 * e + 2 * i - h - k) % 7
        val m = (a + 11 * h + 22 * l) / 451
        val month = (h + l - 7 * m + 114) / 31
        val day = (h + l - 7 * m + 114) % 31 + 1
        return LocalDate.of(year, month, day)
    }

    private fun christmas(
        tmdbId: Int, title: String, year: Int, blurb: String
    ) = SeasonalMovie(tmdbId, title, year, blurb)

    /** Narrow windows first, wide windows last. See [activeEventFor]. */
    val events: List<SeasonalEvent> = listOf(
        SeasonalEvent(
            name = "new_year",
            label = "New Year pick",
            matches = { between(it, 12 to 26, 12 to 31) || between(it, 1 to 1, 1 to 3) },
            movies = listOf(
                christmas(157336, "Interstellar", 2014, "A father leaves his daughter behind to save humanity. Big enough for midnight."),
                christmas(769, "Goodfellas", 1990, "New Year's Eve, 26 December 1969, and it only gets better from there."),
                christmas(545611, "Everything Everywhere All at Once", 2022, "The one to start the year on if you have not seen it.")
            )
        ),
        SeasonalEvent(
            name = "valentines",
            label = "Valentine's pick",
            matches = { between(it, 2 to 7, 2 to 15) },
            movies = listOf(
                christmas(508, "Love Actually", 2003, "Ten stories, one Heathrow, and a very public confession."),
                christmas(11036, "The Notebook", 2004, "For the people who still believe in the rain scene."),
                christmas(769, "Goodfellas", 1990, "Technically not romance. Undoubtedly a great night in.")
            )
        ),
        SeasonalEvent(
            name = "easter",
            label = "Easter pick",
            matches = { date ->
                val easter = easterSunday(date.year)
                !date.isBefore(easter.minusDays(3)) && !date.isAfter(easter.plusDays(9))
            },
            movies = listOf(
                christmas(129, "Spirited Away", 2001, "A girl works her way through a spirit world to get her parents home."),
                christmas(128, "Princess Mononoke", 1997, "Long, strange and beautiful. The one that holds a whole family together."),
                christmas(862, "Toy Story", 1995, "Small, funny, and the correct amount of chaos for a family afternoon.")
            )
        ),
        SeasonalEvent(
            name = "freedom_day",
            label = "Freedom Day pick",
            matches = { between(it, 4 to 27, 4 to 27) },
            movies = listOf(
                christmas(324857, "Into the Spider-Verse", 2018, "A young hero who never asked for the job, on the day he learns what it costs."),
                christmas(27205, "Inception", 2010, "Something worth fighting for, folded inside something worth thinking about."),
                christmas(14160, "Up", 2009, "Short, warm, and ends exactly as it should.")
            )
        ),
        SeasonalEvent(
            name = "heritage_month",
            label = "Heritage Month pick",
            matches = { between(it, 3 to 1, 3 to 31) },
            movies = listOf(
                christmas(8587, "The Lion King", 1994, "South Africa's own Nkosi. Half the audience will be singing."),
                christmas(19404, "Dilwale Dulhania Le Jayenge", 1995, "India's answer to the same question, and a long-running South African favourite."),
                christmas(13, "Forrest Gump", 1994, "A long walk, a box of chocolates, and history in the background.")
            )
        ),
        SeasonalEvent(
            name = "womens_day",
            label = "Women's Day pick",
            matches = { between(it, 8 to 9, 8 to 9) },
            movies = listOf(
                christmas(546554, "Knives Out", 2019, "A loud, funny family and a very sharp investigator."),
                christmas(376867, "Moonlight", 2016, "Quiet, precise, and worth the attention it asks for."),
                christmas(419430, "Get Out", 2017, "Two hours that keep paying attention afterwards.")
            )
        ),
        SeasonalEvent(
            name = "youth_day",
            label = "Youth Day pick",
            matches = { between(it, 6 to 16, 6 to 16) },
            movies = listOf(
                christmas(10681, "The Incredibles", 2004, "A superhero family who would rather just be a family."),
                christmas(150540, "Inside Out", 2015, "Six inside one head, and every one of them is right."),
                christmas(9879, "Elf", 2003, "Maximum cheer, minimum required effort.")
            )
        ),
        SeasonalEvent(
            name = "mandela_day",
            label = "Mandela Day pick",
            matches = { between(it, 7 to 18, 7 to 18) },
            movies = listOf(
                christmas(238, "The Godfather", 1972, "Long, patient, and still the standard the rest are measured against."),
                christmas(155, "The Dark Knight", 2008, "A city-sized argument about where the line sits."),
                christmas(120, "The Lord of the Rings: The Fellowship of the Ring", 2001, "Start here if you have not.")
            )
        ),
        SeasonalEvent(
            name = "heritage_day",
            label = "Heritage Day pick",
            matches = { between(it, 9 to 24, 9 to 24) },
            movies = listOf(
                christmas(13, "Forrest Gump", 1994, "One man's very long, very American answer to history."),
                christmas(120, "The Lord of the Rings: The Fellowship of the Ring", 2001, "An epic about holding a fragile thing together."),
                christmas(354912, "Coco", 2017, "Family, memory, and music. Hard to dislike.")
            )
        ),
        SeasonalEvent(
            name = "spooky",
            label = "Spooky pick",
            matches = { between(it, 10 to 26, 11 to 2) },
            movies = listOf(
                christmas(9479, "The Nightmare Before Christmas", 1993, "Halloween, but the good kind."),
                christmas(680, "Pulp Fiction", 1994, "Wrong for every other night of the year. Right for this one."),
                christmas(603, "The Matrix", 1999, "Red pill, green pill, and a very 1999 soundtrack.")
            )
        ),
        SeasonalEvent(
            name = "festive",
            label = "Festive pick",
            matches = { between(it, 12 to 1, 12 to 25) },
            movies = FestiveSeason.collections.flatMap { it.movies }.distinctBy { it.tmdbId }
        ),
        SeasonalEvent(
            name = "summer",
            label = "Summer pick",
            matches = { between(it, 11 to 20, 12 to 31) },
            movies = listOf(
                christmas(597, "Titanic", 1997, "Still nearly four hours and still worth it."),
                christmas(64328, "The Muppets", 2011, "Chaos in a good mood."),
                christmas(10625, "Mean Girls", 2004, "The one everybody quotes at each other.")
            )
        )
    )

    /**
     * The event that governs [date], or null when nothing seasonal applies.
     *
     * Falls through to a general pick on the [everydayPool] rather than showing nothing, because a
     * feature that is empty on ordinary Tuesdays reads as broken.
     */
    fun activeEventFor(date: LocalDate): SeasonalEvent? = events.firstOrNull { it.matches(date) }

    fun isEveryday(date: LocalDate): Boolean = activeEventFor(date) == null

    /**
     * The non-seasonal pool. Rotated by day so the general suggestion still feels picked rather than
     * fixed, and deliberately small and well known, since a suggestion with no name attached does not
     * sell a film.
     */
    val everydayPool: List<SeasonalMovie> = listOf(
        SeasonalMovie(278, "The Shawshank Redemption", 1994, "Hopeless, patient, and eventually free."),
        SeasonalMovie(13, "Forrest Gump", 1994, "A box of chocolates and a life that keeps going."),
        SeasonalMovie(157336, "Interstellar", 2014, "Time, gravity, and a promise kept across both."),
        SeasonalMovie(27205, "Inception", 2010, "Heist, dream, and a question about what is real."),
        SeasonalMovie(129, "Spirited Away", 2001, "A child alone in a world that does not explain itself."),
        SeasonalMovie(120, "The Lord of the Rings: The Fellowship of the Ring", 2001, "Nine walking days and one ring."),
        SeasonalMovie(155, "The Dark Knight", 2008, "Still the one people argue about."),
        SeasonalMovie(769, "Goodfellas", 1990, "Thirty years of a life, in one long ascent and one long fall."),
        SeasonalMovie(324857, "Into the Spider-Verse", 2018, "Every style of animation in the same city."),
        SeasonalMovie(545611, "Everything Everywhere All at Once", 2022, "Loud, sad, and oddly moving.")
    )
}
