@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
package com.example.vista.ui.seeall

import androidx.lifecycle.SavedStateHandle
import com.example.vista.data.local.PreferencesManager
import com.example.vista.domain.model.*
import com.example.vista.domain.repository.*
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.IOException

class SeeAllViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val catalog = mockk<CatalogRepository>()
    private val watchlist = mockk<WatchlistRepository>()
    private val preferences = mockk<PreferencesManager>(relaxed = true)
    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
        every { watchlist.getWatchlist() } returns flowOf(emptyList())
        every { preferences.settings } returns flowOf(AppSettings())
    }
    @After fun teardown() { Dispatchers.resetMain() }

    @Test fun failedPagePreservesTitlesAndOnlyRetriesOnExplicitAction() = runTest(dispatcher) {
        val first = MediaItem.MovieItem("1", "Example", null, null, null, null, null)
        val second = first.copy(id = "2")
        coEvery { catalog.getSection(CatalogSection.TRENDING_MOVIES, 1) } returns listOf(first)
        coEvery { catalog.getSection(CatalogSection.TRENDING_MOVIES, 2) } throws IOException("Try again") andThen listOf(second)
        val vm = SeeAllViewModel(SavedStateHandle(mapOf("sectionId" to "trending_movies", "title" to "Trending")), catalog, watchlist, preferences)
        advanceUntilIdle()
        vm.loadMore()
        advanceUntilIdle()
        assertEquals(listOf("1"), vm.uiState.value.items.map { it.id })
        assertEquals(1, vm.uiState.value.currentPage)
        assertNotNull(vm.uiState.value.error)
        vm.loadMore()
        advanceUntilIdle()
        coVerify(exactly = 1) { catalog.getSection(CatalogSection.TRENDING_MOVIES, 2) }
        vm.retryLoadMore()
        advanceUntilIdle()
        assertEquals(listOf("1", "2"), vm.uiState.value.items.map { it.id })
        assertNull(vm.uiState.value.error)
        assertEquals(2, vm.uiState.value.currentPage)
    }
}
