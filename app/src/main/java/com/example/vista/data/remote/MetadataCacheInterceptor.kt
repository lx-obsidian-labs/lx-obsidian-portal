package com.example.vista.data.remote

import okhttp3.CacheControl
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Last-known public metadata only. Never caches identity, AI, signed media or provider pages. */
class MetadataCacheInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.method != "GET" || request.url.host !in HOSTS) return chain.proceed(request)
        val networkResponse = try {
            chain.proceed(request)
        } catch (failure: IOException) {
            if (chain.call().isCanceled()) throw failure
            val cached = chain.proceed(request.newBuilder().cacheControl(STALE).build())
            if (cached.code != 504) return cached
            cached.close()
            throw failure
        }
        if (networkResponse.code !in setOf(500, 502, 503, 504)) return networkResponse
        networkResponse.close()
        // Cache-only lookup: no second network request and no retry storm during an outage.
        val cached = chain.proceed(request.newBuilder().cacheControl(STALE).build())
        if (cached.code != 504) return cached
        cached.close()
        throw IOException("The catalog service is temporarily unavailable. Please try again.")
    }

    companion object {
        private val HOSTS = setOf("api.themoviedb.org", "api.tvmaze.com")
        private val STALE = CacheControl.Builder().onlyIfCached().maxStale(7, TimeUnit.DAYS).build()
    }
}
