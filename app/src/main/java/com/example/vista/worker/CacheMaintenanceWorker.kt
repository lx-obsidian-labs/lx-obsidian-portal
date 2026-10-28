package com.example.vista.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.example.vista.data.cache.CacheManager
import com.example.vista.data.local.PreferencesManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * Background WorkManager worker for Vista Cinema cache maintenance.
 *
 * Runs periodically to enforce user's configured cache size limits and purge stale player
 * temporary files (>24 hours) without affecting active playback sessions.
 */
@HiltWorker
class CacheMaintenanceWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val cacheManager: CacheManager,
    private val preferencesManager: PreferencesManager
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val settings = preferencesManager.settings.first()
            cacheManager.enforceCacheLimit(settings.cacheLimitMb)
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}

object CacheMaintenanceScheduler {
    private const val WORK_NAME = "vista_cache_maintenance"

    fun schedule(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiresCharging(false)
            .build()

        val request = PeriodicWorkRequestBuilder<CacheMaintenanceWorker>(
            repeatInterval = 24,
            repeatIntervalTimeUnit = TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
