package com.example.vista.data.playback

import com.example.vista.domain.model.Episode
import com.example.vista.domain.model.Series
import com.example.vista.domain.repository.CatalogRepository
import io.mockk.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class NextEpisodeResolverTest {
    private val catalog = mockk<CatalogRepository>()
    private fun episode(season: Int, number: Int) = Episode("42", season, number, "Episode $number", null, null, null, null, null, null)
    @Test fun usesConfirmedFollowingEpisode() = runTest {
        coEvery { catalog.getEpisodes("42", 1) } returns listOf(episode(1, 1), episode(1, 2))
        assertEquals(2, NextEpisodeResolver.resolve(catalog, "42", 1, 1)?.episodeNumber)
        coVerify(exactly = 0) { catalog.getSeriesDetails(any()) }
    }
    @Test fun advancesAcrossAConfirmedSeasonBoundary() = runTest {
        coEvery { catalog.getEpisodes("42", 1) } returns listOf(episode(1, 1), episode(1, 2))
        coEvery { catalog.getSeriesDetails("42") } returns Series("42", "Series", "", null, null,
            null, null, null, null, emptyList(), emptyList(), emptyList(), 2, 3, null, null)
        coEvery { catalog.getEpisodes("42", 2) } returns listOf(episode(2, 1))
        assertEquals(episode(2, 1), NextEpisodeResolver.resolve(catalog, "42", 1, 2))
    }
    @Test fun missingMetadataDoesNotGuessTheNextSeason() = runTest {
        coEvery { catalog.getEpisodes("42", 1) } returns emptyList()
        assertNull(NextEpisodeResolver.resolve(catalog, "42", 1, 2))
        coVerify(exactly = 0) { catalog.getSeriesDetails(any()) }
    }
    @Test fun specialsDoNotAutomaticallyJumpIntoTheMainStory() = runTest {
        coEvery { catalog.getEpisodes("42", 0) } returns listOf(episode(0, 1))
        assertNull(NextEpisodeResolver.resolve(catalog, "42", 0, 1))
    }
    @Test fun cancellationIsPreserved() = runTest {
        coEvery { catalog.getEpisodes("42", 1) } throws CancellationException()
        try { NextEpisodeResolver.resolve(catalog, "42", 1, 1); fail("Expected cancellation") }
        catch (_: CancellationException) { }
    }
}
