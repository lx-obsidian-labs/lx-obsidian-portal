package com.example.vista.data.repository

import com.example.vista.data.local.catalog.CatalogSnapshotStore
import com.example.vista.data.local.catalog.SavedCatalogRow
import com.example.vista.domain.model.*
import com.example.vista.domain.repository.CatalogRepository
import io.mockk.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class HomeCatalogLoaderTest {
    private val now = 2_000_000_000_000L
    private val repository = mockk<CatalogRepository>()
    private val store = mockk<CatalogSnapshotStore>()
    private val savedMovie = MediaItem.MovieItem("42", "Saved", null, 7.0, 2024, null, null)
    private val oldRow = SavedCatalogRow(CatalogSection.TRENDING_MOVIES, now - 5000, listOf(savedMovie))

    private fun loader(): HomeCatalogLoader {
        coEvery { store.read() } returns listOf(oldRow)
        coEvery { store.write(any()) } just Runs
        coEvery { repository.getSection(any(), 1) } returns emptyList()
        return HomeCatalogLoader(repository, store) { now }
    }

    @Test fun savedCatalogIsEmittedBeforeAnyNetworkRequest() = runTest {
        val result = loader().load().first()
        assertTrue(result.refreshing)
        assertTrue(result.sections.all { it.isSavedCatalog })
        assertEquals(oldRow.savedAt, result.sections.first().catalogSavedAt)
        coVerify(exactly = 0) { repository.getSection(any(), any()) }
    }

    @Test fun partialOutageRetainsOnlyFailedRowsAndDoesNotRetimestampThem() = runTest {
        val loader = loader()
        val fresh = savedMovie.copy(id = "99", title = "Fresh")
        coEvery { repository.getSection(CatalogSection.TRENDING_MOVIES, 1) } throws IOException("down")
        coEvery { repository.getSection(CatalogSection.POPULAR_MOVIES, 1) } returns listOf(fresh)
        val emissions = loader.load().toList()
        val final = emissions.last()
        val retained = final.sections.first { it.id == "trending_movies" }
        val updated = final.sections.first { it.id == "popular_movies" }
        assertTrue(retained.isSavedCatalog)
        assertEquals(oldRow.savedAt, retained.catalogSavedAt)
        assertFalse(updated.isSavedCatalog)
        assertEquals(now, updated.catalogSavedAt)
        assertFalse(final.refreshing)
        assertNotNull(final.error)
        coVerify { store.write(match { rows -> rows.any { it.section == CatalogSection.TRENDING_MOVIES && it.savedAt == oldRow.savedAt } }) }
    }

    @Test fun confirmedEmptyRowsRemoveOldDataInsteadOfBecomingPerpetualFallback() = runTest {
        val final = loader().load().last()
        assertTrue(final.sections.isEmpty())
        coVerify { store.write(emptyList()) }
    }

    @Test fun totalOutagePreservesExistingDiskSnapshot() = runTest {
        val loader = loader()
        coEvery { repository.getSection(any(), 1) } throws IOException("down")
        val final = loader.load().last()
        assertTrue(final.sections.isNotEmpty())
        assertTrue(final.sections.all { it.isSavedCatalog })
        coVerify(exactly = 0) { store.write(any()) }
    }

    @Test fun firstInstallFailureIsAnErrorNotAnEmptySuccessfulCatalog() = runTest {
        val loader = loader()
        coEvery { store.read() } returns emptyList()
        coEvery { repository.getSection(any(), 1) } throws IOException("private-url?apikey=secret")
        val final = loader.load().last()
        assertTrue(final.sections.isEmpty())
        assertNotNull(final.error)
        assertFalse(final.error!!.contains("secret"))
    }

    @Test fun diskFailureDoesNotDiscardAlreadyUsefulInMemoryRows() = runTest {
        val loader = loader()
        coEvery { store.read() } throws IOException("disk")
        coEvery { repository.getSection(any(), 1) } throws IOException("down")
        val final = loader.load(listOf(oldRow.toHomeSection(false))).last()
        assertEquals("42", final.sections.first().items.single().id)
        assertTrue(final.sections.first().isSavedCatalog)
    }

    @Test fun cancellationNeverReplacesSnapshotWithAnEmptyWrite() = runTest {
        val loader = loader()
        coEvery { repository.getSection(any(), 1) } throws CancellationException("cancel")
        try { loader.load().toList(); fail("Expected cancellation") }
        catch (_: CancellationException) { }
        coVerify(exactly = 0) { store.write(any()) }
    }
}
