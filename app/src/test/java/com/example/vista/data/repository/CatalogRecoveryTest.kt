package com.example.vista.data.repository

import android.util.Log
import com.example.vista.data.remote.shoofaflam.ShoofaflamRepository
import com.example.vista.data.remote.tmdb.TmdbApiService
import com.example.vista.data.remote.tmdb.TmdbMovie
import com.example.vista.data.remote.tmdb.TmdbResponse
import com.example.vista.data.remote.tvmaze.TvmazeApiService
import com.example.vista.domain.model.CatalogSection
import com.example.vista.domain.model.Series
import io.mockk.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.net.UnknownHostException

class CatalogRecoveryTest {
    private val tmdb = mockk<TmdbApiService>()
    private val tvmazeApi = mockk<TvmazeApiService>()
    private val tvmaze = mockk<TvmazeCatalogRepository>()
    private val extras = mockk<ShoofaflamRepository>()
    private val repository = TmdbCatalogRepository(tmdb, extras, tvmaze, tvmazeApi, mockk())

    @Before fun stubLog() {
        mockkStatic(Log::class)
        every { Log.e(any(), any<String>()) } returns 0
    }
    @After fun clearLog() { unmockkStatic(Log::class) }

    @Test fun `total catalog failure is reported as failure rather than empty success`() = runTest {
        coEvery { tmdb.getTrendingMovies(any()) } throws UnknownHostException("private details")
        try {
            repository.getHomeSections()
            fail("Expected actionable catalog failure")
        } catch (e: CatalogLoadException) {
            assertTrue(e.message!!.contains("DNS"))
            assertFalse(e.message!!.contains("private details"))
        }
    }

    @Test fun `one failed section does not remove healthy movie rows`() = runTest {
        coEvery { tmdb.getTrendingMovies(any()) } returns TmdbResponse(1, listOf(TmdbMovie(597, "Titanic")))
        val sections = repository.getHomeSections()
        assertEquals("597", sections.first { it.id == CatalogSection.TRENDING_MOVIES.id }.items.single().id)
    }

    @Test fun `TVMaze series survive TMDB failure`() = runTest {
        coEvery { tmdb.getPopularTvShows(any()) } throws UnknownHostException()
        coEvery { tvmaze.searchSeries(any()) } returns listOf(Series(
            "tvmaze_42", "Example", "", null, null, 2020, null, null, null,
            emptyList(), emptyList(), emptyList(), 1, 1, null, null
        ))
        assertEquals("tvmaze_42", repository.getSection(CatalogSection.POPULAR_SERIES, 1).single().id)
    }

    @Test fun `catalog cancellation is not converted to empty results`() = runTest {
        coEvery { tmdb.getTrendingMovies(any()) } throws CancellationException("cancelled")
        try {
            repository.getSection(CatalogSection.TRENDING_MOVIES, 1)
            fail("Expected cancellation")
        } catch (_: CancellationException) { }
    }

    @Test fun `authorization errors are actionable without leaking bodies`() {
        val failure = HttpException(Response.error<Any>(401, "secret-token".toResponseBody()))
        val message = CatalogLoadException.from(failure).message!!
        assertTrue(message.contains("credentials"))
        assertFalse(message.contains("secret-token"))
    }

    @Test fun `TVMaze retries immediately after a failed request`() = runTest {
        val service = mockk<TvmazeApiService>()
        coEvery { service.searchShows("example") } throws UnknownHostException() andThen emptyList()
        val catalog = TvmazeCatalogRepository(service)
        catalog.searchSeries("example")
        catalog.searchSeries("example")
        coVerify(exactly = 2) { service.searchShows("example") }
    }
}
