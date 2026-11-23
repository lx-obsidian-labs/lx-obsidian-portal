package com.example.vista.di

import android.content.Context
import com.example.vista.BuildConfig
import com.example.vista.data.remote.shoofaflam.ShoofaflamApiService
import com.example.vista.data.remote.tmdb.TmdbApiService
import com.example.vista.data.remote.tvmaze.TvmazeApiService
import com.example.vista.data.remote.watchmode.WatchmodeApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /** Disk-backed HTTP cache so repeat requests (home reloads, section revisits) skip the network. */
    @Provides
    @Singleton
    fun provideHttpCache(@ApplicationContext context: Context): Cache {
        val cacheDir = File(context.cacheDir, "http_cache")
        return Cache(cacheDir, 50L * 1024 * 1024)
    }

    @Provides
    @Singleton
    fun provideAuthInterceptor(): Interceptor {
        return Interceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("Authorization", "Bearer ${BuildConfig.TMDB_ACCESS_TOKEN}")
                .addHeader("accept", "application/json")
                .build()
            chain.proceed(request)
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: Interceptor, httpCache: Cache): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(com.example.vista.data.remote.MetadataCacheInterceptor())
            .cache(httpCache)
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(redactingLogger())
                }
            }
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .callTimeout(25, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Debug logger that keeps header and body visibility but never writes a credential.
     *
     * TMDB's read access token travels as `Authorization: Bearer ...`, and HEADERS level logs it
     * verbatim. Any app with READ_LOGS on a debuggable build - a bug report attachment, a screen
     * share, an `adb logcat` capture pasted into a chat - leaks a working credential. BASIC is
     * enough to diagnose requests; the redacted header set keeps the interesting custom headers
     * (accept, cache headers) visible without printing secrets.
     */
    private fun redactingLogger() = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.HEADERS
        redactHeader("Authorization")
        redactHeader("X-Api-Key")
        redactHeader("Cookie")
        redactHeader("Set-Cookie")
    }

    /** Shared, auth-free client used by all non-TMDB APIs to avoid duplicate pools/threads. */
    @Provides
    @Singleton
    @Named("shared")
    fun provideSharedOkHttpClient(httpCache: Cache): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(com.example.vista.data.remote.MetadataCacheInterceptor())
            .cache(httpCache)
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(redactingLogger())
                }
            }
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .callTimeout(25, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    @Named("tmdb")
    fun provideTmdbRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(TmdbApiService.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideTmdbApiService(@Named("tmdb") retrofit: Retrofit): TmdbApiService {
        return retrofit.create(TmdbApiService::class.java)
    }

    @Provides
    @Singleton
    @Named("tvmaze")
    fun provideTvmazeRetrofit(@Named("shared") sharedClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(TvmazeApiService.BASE_URL)
            .client(sharedClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideTvmazeApiService(@Named("tvmaze") retrofit: Retrofit): TvmazeApiService {
        return retrofit.create(TvmazeApiService::class.java)
    }

    @Provides
    @Singleton
    @Named("imdb")
    fun provideImdbRetrofit(@Named("shared") sharedClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(com.example.vista.data.remote.imdb.ImdbApiService.BASE_URL)
            .client(sharedClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideImdbApiService(@Named("imdb") retrofit: Retrofit): com.example.vista.data.remote.imdb.ImdbApiService {
        return retrofit.create(com.example.vista.data.remote.imdb.ImdbApiService::class.java)
    }

    @Provides
    @Singleton
    @Named("shoofaflam")
    fun provideShoofaflamRetrofit(@Named("shared") sharedClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(ShoofaflamApiService.BASE_URL)
            .client(sharedClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideShoofaflamApiService(@Named("shoofaflam") retrofit: Retrofit): ShoofaflamApiService {
        return retrofit.create(ShoofaflamApiService::class.java)
    }

    @Provides
    @Singleton
    @Named("watchmode")
    fun provideWatchmodeRetrofit(@Named("shared") sharedClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(WatchmodeApiService.BASE_URL)
            .client(sharedClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideWatchmodeApiService(@Named("watchmode") retrofit: Retrofit): WatchmodeApiService {
        return retrofit.create(WatchmodeApiService::class.java)
    }
}
