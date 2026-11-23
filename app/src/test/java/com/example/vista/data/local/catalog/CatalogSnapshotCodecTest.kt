package com.example.vista.data.local.catalog

import com.example.vista.domain.model.CatalogSection
import com.example.vista.domain.model.MediaItem
import org.junit.Assert.*
import org.junit.Test

class CatalogSnapshotCodecTest {
    private val now = 2_000_000_000_000L
    private val movie = MediaItem.MovieItem("42", "Movie", null, 7.0, 2024, "90m", null)
    private val series = MediaItem.SeriesItem("42", "Series", null, 8.0, 2023, 2, null)

    @Test fun typedIdsAndOriginalTimestampsSurviveRoundTrip() {
        val rows = listOf(
            SavedCatalogRow(CatalogSection.TRENDING_MOVIES, now - 1000, listOf(movie)),
            SavedCatalogRow(CatalogSection.POPULAR_SERIES, now - 2000, listOf(series))
        )
        assertEquals(rows, CatalogSnapshotCodec.decode(CatalogSnapshotCodec.encode(rows), now))
    }

    @Test fun expiredAndFutureRowsAreNotShownAsUsefulSavedData() {
        val rows = listOf(
            SavedCatalogRow(CatalogSection.TRENDING_MOVIES, now - CatalogSnapshotCodec.MAX_AGE_MS - 1, listOf(movie)),
            SavedCatalogRow(CatalogSection.POPULAR_SERIES, now + 1, listOf(series))
        )
        assertTrue(CatalogSnapshotCodec.decode(CatalogSnapshotCodec.encode(rows), now).isEmpty())
    }

    @Test fun corruptOrUnsupportedSnapshotsFallBackToNetwork() {
        assertTrue(CatalogSnapshotCodec.decode("broken json", now).isEmpty())
        assertTrue(CatalogSnapshotCodec.decode("{\"schema\":2,\"rows\":[]}", now).isEmpty())
        assertTrue(CatalogSnapshotCodec.decode("x".repeat(CatalogSnapshotCodec.MAX_BYTES + 1), now).isEmpty())
    }

    @Test fun personalViewingProgressIsNeverWrittenIntoPublicCatalog() {
        val progress = MediaItem.ContinueWatchingItem("42", "Private viewing", null, null, 0.5f, 10, 20, null, false)
        val json = CatalogSnapshotCodec.encode(listOf(SavedCatalogRow(CatalogSection.TRENDING_MOVIES, now, listOf(movie, progress))))
        assertFalse(json.contains("Private viewing"))
        assertFalse(json.contains("currentTime"))
        assertEquals(listOf(movie), CatalogSnapshotCodec.decode(json, now).single().items)
    }

    @Test fun snapshotBoundsEachRowToTheFirstPage() {
        val items = (1..100).map { movie.copy(id = it.toString()) }
        val decoded = CatalogSnapshotCodec.decode(CatalogSnapshotCodec.encode(
            listOf(SavedCatalogRow(CatalogSection.TRENDING_MOVIES, now, items))), now)
        assertEquals(20, decoded.single().items.size)
    }
}
