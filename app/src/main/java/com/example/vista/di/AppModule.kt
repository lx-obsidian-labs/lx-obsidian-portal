package com.example.vista.di

import android.content.Context
import androidx.room.Room
import com.example.vista.data.local.AppDatabase
import com.example.vista.data.local.PreferencesManager
import com.example.vista.data.local.dao.*
import com.example.vista.data.remote.tvmaze.TvmazeScheduleRepository
import com.example.vista.data.repository.*
import com.example.vista.domain.repository.*
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {
    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseAnalytics(@ApplicationContext context: Context): FirebaseAnalytics = FirebaseAnalytics.getInstance(context)

    @Provides
    @Singleton
    fun provideFirebaseCrashlytics(): FirebaseCrashlytics = FirebaseCrashlytics.getInstance()
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "vista_database"
        ).addMigrations(
            AppDatabase.MIGRATION_1_2,
            AppDatabase.MIGRATION_2_3,
            AppDatabase.MIGRATION_3_4,
            AppDatabase.MIGRATION_4_5,
            AppDatabase.MIGRATION_5_6
        )
            // No destructive fallback on purpose. It only ever fires on a version bump with no
            // matching migration, and the cost is silently deleting a user's watchlist, playback
            // history and downloads. A crash on upgrade is the better failure mode.
            .build()
    }

    @Provides
    fun provideWatchlistDao(database: AppDatabase): WatchlistDao = database.watchlistDao()

    @Provides
    fun providePlaybackHistoryDao(database: AppDatabase): PlaybackHistoryDao = database.playbackHistoryDao()

    @Provides
    fun provideRecentSearchDao(database: AppDatabase): RecentSearchDao = database.recentSearchDao()

    @Provides
    @Singleton
    fun providePreferencesManager(@ApplicationContext context: Context): PreferencesManager {
        return PreferencesManager(context)
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindCatalogSnapshotStore(impl: com.example.vista.data.local.catalog.DiskCatalogSnapshotStore):
        com.example.vista.data.local.catalog.CatalogSnapshotStore

    @Binds
    @Singleton
    abstract fun bindCatalogRepository(impl: TmdbCatalogRepository): CatalogRepository

    companion object {
        @Provides
        @Singleton
        fun provideTvmazeCatalogRepository(apiService: com.example.vista.data.remote.tvmaze.TvmazeApiService): TvmazeCatalogRepository {
            return TvmazeCatalogRepository(apiService)
        }
    }

    @Binds
    @Singleton
    abstract fun bindPlaybackRepository(impl: com.example.vista.data.remote.playback.VidsrcPlaybackRepository): PlaybackRepository

    @Binds
    @Singleton
    abstract fun bindWatchlistRepository(impl: LocalWatchlistRepository): WatchlistRepository

    @Binds
    @Singleton
    abstract fun bindHistoryRepository(impl: LocalHistoryRepository): HistoryRepository

    @Binds
    @Singleton
    abstract fun bindSearchRepository(impl: com.example.vista.data.remote.tmdb.TmdbSearchRepository): SearchRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: LocalSettingsRepository): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindScheduleRepository(impl: TvmazeScheduleRepository): ScheduleRepository
}
