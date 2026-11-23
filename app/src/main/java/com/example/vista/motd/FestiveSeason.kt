package com.example.vista.motd

import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class FestiveCollection(val id: String, val title: String, val movies: List<SeasonalMovie>)
data class FestiveMoment(val seasonYear: Int, val heading: String, val countdown: String)

/** Local, year-aware editorial content; no promises of provider availability or age suitability. */
object FestiveSeason {
    val collections = listOf(
        FestiveCollection("classics", "Christmas favourites", listOf(
            SeasonalMovie(771, "Home Alone", 1990, "A house full of ingenious holiday chaos."),
            SeasonalMovie(772, "Home Alone 2: Lost in New York", 1992, "Kevin takes his holiday mischief to New York."),
            SeasonalMovie(9879, "Elf", 2003, "A very big elf brings a little Christmas spirit to the city."),
            SeasonalMovie(5255, "The Polar Express", 2004, "An extraordinary train ride to the North Pole.")
        )),
        FestiveCollection("animated", "Animated adventures", listOf(
            SeasonalMovie(508965, "Klaus", 2019, "An unlikely friendship starts a tradition of kindness."),
            SeasonalMovie(360920, "The Grinch", 2018, "A grumpy neighbour meets a town overflowing with cheer."),
            SeasonalMovie(9479, "The Nightmare Before Christmas", 1993, "Two holidays collide in a musical adventure."),
            SeasonalMovie(17979, "A Christmas Carol", 2009, "A familiar Christmas tale with a ghostly twist.")
        )),
        FestiveCollection("together", "A festive night together", listOf(
            SeasonalMovie(1581, "The Holiday", 2006, "Two homes, two fresh starts and a holiday romance."),
            SeasonalMovie(508, "Love Actually", 2003, "Intertwining stories of love around Christmas."),
            SeasonalMovie(10312, "Patch Adams", 1998, "A warm-hearted story about connection and compassion.")
        ))
    )

    fun moment(date: LocalDate): FestiveMoment? {
        if (date.monthValue !in listOf(11, 12) && !(date.monthValue == 1 && date.dayOfMonth <= 5)) return null
        val seasonYear = if (date.monthValue == 1) date.year - 1 else date.year
        val christmas = LocalDate.of(seasonYear, 12, 25)
        val days = ChronoUnit.DAYS.between(date, christmas)
        return when {
            days > 0 -> FestiveMoment(seasonYear, "Your festive movie nights", "$days ${if (days == 1L) "day" else "days"} to Christmas")
            days == 0L -> FestiveMoment(seasonYear, "Merry Christmas from Vista", "Make room for a great story")
            date.monthValue == 12 -> FestiveMoment(seasonYear, "Keep the festive feeling", "More stories before the new year")
            else -> FestiveMoment(seasonYear, "New year, new movie nights", "A wonderful year of stories awaits")
        }
    }

    fun dailyPick(date: LocalDate, excludedIds: Set<String> = emptySet()): SeasonalMovie {
        val all = collections.flatMap { it.movies }.distinctBy { it.tmdbId }
        val available = all.filterNot { it.tmdbId.toString() in excludedIds }.ifEmpty { all }
        return available[Math.floorMod(date.toEpochDay(), available.size.toLong()).toInt()]
    }
}
