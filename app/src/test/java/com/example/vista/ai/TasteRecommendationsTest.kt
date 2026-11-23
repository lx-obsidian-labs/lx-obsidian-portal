package com.example.vista.ai

import com.example.vista.data.local.PreferencesManager
import com.example.vista.domain.model.*
import com.example.vista.domain.repository.*
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class TasteRecommendationsTest {
    private val catalog = mockk<CatalogRepository>(relaxed = true)
    private val history = mockk<HistoryRepository>()
    private val taste = mockk<TasteStore>()
    private val settings = mockk<PreferencesManager>()
    private val watchlist = mockk<WatchlistRepository>()
    private fun movie(id: String, runtime: Int? = null, genre: String = "Comedy") = Movie(id, "Movie $id", "", null, null,
        2020, runtime, 7.0, 100, listOf(Genre("35", genre)), emptyList(), null, null)
    private fun setup(prefs: TastePreferences = TastePreferences()) {
        coEvery { taste.read() } returns prefs
        every { settings.settings } returns flowOf(AppSettings())
        every { history.getWatchHistory() } returns flowOf(emptyList())
        every { watchlist.getWatchlist() } returns flowOf(emptyList())
        coEvery { catalog.getGenres() } returns listOf(Genre("35", "Comedy"))
    }
    @Test fun shortRequestHydratesMissingRuntimeAndExcludesUnknownOrLongTitles() = runTest {
        setup()
        coEvery { catalog.getPopularMovies(any()) } returns listOf(movie("1"), movie("2"), movie("3"))
        coEvery { catalog.getMovieDetails("1") } returns movie("1", 90)
        coEvery { catalog.getMovieDetails("2") } returns movie("2", 130)
        coEvery { catalog.getMovieDetails("3") } returns null
        val result = TasteRecommendations(catalog, history, taste, settings, watchlist).suggestions("a short movie")
        assertEquals(listOf("1"), result.map { it.item.id })
    }
    @Test fun explicitDislikeSurvivesPersonalizationOptOutAndTypedSeriesIdDoesNotHideMovie() = runTest {
        setup(TastePreferences(enabled = false, disliked = setOf("movie:1", "series:2")))
        coEvery { catalog.getPopularMovies(any()) } returns listOf(movie("1", 90), movie("2", 90))
        val result = TasteRecommendations(catalog, history, taste, settings, watchlist).suggestions()
        assertEquals(listOf("2"), result.map { it.item.id })
    }
    @Test fun savedMovieSeedsRelatedSuggestionsInsteadOfOnlyPopularity() = runTest {
        setup()
        every { watchlist.getWatchlist() } returns flowOf(listOf(MediaItem.MovieItem("saved", "Saved", null, null, 2020, null, null)))
        coEvery { catalog.getRecommendedMovies("saved") } returns listOf(movie("related", 90))
        coEvery { catalog.getPopularMovies(any()) } returns emptyList()
        val result = TasteRecommendations(catalog, history, taste, settings, watchlist).suggestions()
        assertEquals(listOf("related"), result.map { it.item.id })
        coVerify(exactly = 1) { catalog.getRecommendedMovies("saved") }
    }
    @Test fun failedPopularPoolDoesNotCancelHealthyGenreSuggestions() = runTest {
        setup()
        coEvery { catalog.getPopularMovies(any()) } throws java.io.IOException("catalog unavailable")
        coEvery { catalog.getMoviesByGenre("35", any()) } returns listOf(movie("genre", 90))
        val result = TasteRecommendations(catalog, history, taste, settings, watchlist).suggestions("funny")
        assertEquals(listOf("genre"), result.map { it.item.id })
    }
}
