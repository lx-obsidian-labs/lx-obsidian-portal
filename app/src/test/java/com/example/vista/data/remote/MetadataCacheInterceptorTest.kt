package com.example.vista.data.remote

import io.mockk.*
import okhttp3.*
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class MetadataCacheInterceptorTest {
    private val interceptor = MetadataCacheInterceptor()

    private fun response(request: Request, code: Int) = Response.Builder()
        .request(request).protocol(Protocol.HTTP_1_1).code(code).message("Test")
        .body("{}".toResponseBody()).build()

    @Test fun outageUsesCacheOnlyWithoutAnotherNetworkAttempt() {
        val request = Request.Builder().url("https://api.themoviedb.org/3/movie/popular").build()
        val chain = mockk<Interceptor.Chain>()
        every { chain.request() } returns request
        every { chain.call().isCanceled() } returns false
        every { chain.proceed(any()) } answers {
            val attempted = firstArg<Request>()
            if (!attempted.cacheControl.onlyIfCached) throw IOException("DNS failure")
            response(attempted, 200)
        }
        interceptor.intercept(chain).use { assertEquals(200, it.code) }
        verify(exactly = 2) { chain.proceed(any()) }
        verify { chain.proceed(match { it.cacheControl.onlyIfCached && it.cacheControl.maxStaleSeconds == 604800 }) }
    }

    @Test fun missingCachePreservesConnectionFailure() {
        val request = Request.Builder().url("https://api.tvmaze.com/shows/1").build()
        val failure = IOException("Connection unavailable")
        val chain = mockk<Interceptor.Chain>()
        every { chain.request() } returns request
        every { chain.call().isCanceled() } returns false
        every { chain.proceed(any()) } answers {
            if (!firstArg<Request>().cacheControl.onlyIfCached) throw failure
            response(firstArg(), 504)
        }
        try { interceptor.intercept(chain); fail("Expected connection failure") }
        catch (caught: IOException) { assertSame(failure, caught) }
    }

    @Test fun cancelledRequestDoesNotReadStaleData() {
        val request = Request.Builder().url("https://api.tvmaze.com/shows/1").build()
        val chain = mockk<Interceptor.Chain>()
        every { chain.request() } returns request
        every { chain.call().isCanceled() } returns true
        every { chain.proceed(any()) } throws IOException("Cancelled")
        try { interceptor.intercept(chain); fail("Expected cancellation") } catch (_: IOException) { }
        verify(exactly = 1) { chain.proceed(any()) }
    }

    @Test fun providerPagesAndAiNeverUseMetadataFallback() {
        for (url in listOf("https://multiembed.mov/", "https://assistant.example/chat")) {
            val request = Request.Builder().url(url).build()
            val chain = mockk<Interceptor.Chain>()
            every { chain.request() } returns request
            every { chain.proceed(any()) } throws IOException("Unavailable")
            try { interceptor.intercept(chain); fail("Expected failure") } catch (_: IOException) { }
            verify(exactly = 1) { chain.proceed(any()) }
        }
    }

    @Test fun authorizationDenialIsNotHiddenByCache() {
        val request = Request.Builder().url("https://api.themoviedb.org/3/movie/1").build()
        val chain = mockk<Interceptor.Chain>()
        every { chain.request() } returns request
        every { chain.proceed(any()) } answers { response(firstArg(), 403) }
        interceptor.intercept(chain).use { assertEquals(403, it.code) }
        verify(exactly = 1) { chain.proceed(any()) }
    }
}
