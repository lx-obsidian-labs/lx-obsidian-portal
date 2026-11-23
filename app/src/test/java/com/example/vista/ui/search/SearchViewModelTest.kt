@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.example.vista.ui.search

import com.example.vista.data.local.PreferencesManager
import com.example.vista.domain.model.AppSettings
import com.example.vista.domain.model.SearchResult
import com.example.vista.domain.repository.SearchRepository
import com.example.vista.domain.repository.WatchlistRepository
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SearchViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = mockk<SearchRepository>(relaxed = true)
    private val watchlist = mockk<WatchlistRepository>()
    private val preferences = mockk<PreferencesManager>(relaxed = true)

    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
        every { repository.getRecentSearches() } returns flowOf(emptyList())
        every { repository.getTrendingSearches() } returns flowOf(emptyList())
        every { watchlist.getWatchlist() } returns flowOf(emptyList())
        every { preferences.settings } returns flowOf(AppSettings())
    }

    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun lateNonCooperativeSearchCannotOverwriteSubmittedQuery() = runTest(dispatcher) {
        coEvery { repository.search("old", any()) } coAnswers {
            withContext(NonCancellable) { delay(1_000) }
            SearchResult(emptyList(), emptyList(), emptyList(), 99)
        }
        coEvery { repository.search("new", any()) } returns SearchResult(emptyList(), emptyList(), emptyList(), 2)
        val viewModel = SearchViewModel(repository, watchlist, preferences)
        viewModel.search("old")
        runCurrent()
        viewModel.search("new")
        advanceUntilIdle()
        assertEquals("new", viewModel.uiState.value.query)
        assertEquals(2, viewModel.uiState.value.totalCount)
        assertNull(viewModel.uiState.value.error)
    }

    @Test fun clearingQueryCancelsPendingDebounceAndResetsCounts() = runTest(dispatcher) {
        val viewModel = SearchViewModel(repository, watchlist, preferences)
        viewModel.onQueryChange("movie")
        runCurrent()
        viewModel.clearQuery()
        advanceUntilIdle()
        coVerify(exactly = 0) { repository.search(any(), any()) }
        assertEquals("", viewModel.uiState.value.query)
        assertEquals(0, viewModel.uiState.value.totalCount)
        assertFalse(viewModel.uiState.value.isSearching)
    }
}
