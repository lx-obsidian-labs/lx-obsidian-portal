package com.example.vista.data.cache

import android.content.Context
import coil.ImageLoader
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.Cache
import java.io.File
import java.util.Collections
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Smart Cache Manager for Vista Watch.
 *
 * Responsibilities:
 * - Manages app-owned designated temporary player files (`cacheDir/player_temp`).
 * - Tracks active playback sessions so active ExoPlayer Media3 cache/temp files are NEVER deleted while in use.
 * - Enforces configurable cache size limits (e.g. 250MB, 500MB, 1GB, 2GB).
 * - Performs repair and cleanup of stale or corrupted temporary playback files.
 */
@Singleton
class CacheManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val httpCache: Cache,
    private val imageLoader: ImageLoader
) {
    val playerTempDir: File = File(context.cacheDir, "player_temp").apply {
        if (!exists()) mkdirs()
    }

    private val activeSessionIds = Collections.synchronizedSet(HashSet<String>())

    fun registerActiveSession(sessionId: String) {
        activeSessionIds.add(sessionId)
    }

    fun unregisterActiveSession(sessionId: String) {
        activeSessionIds.remove(sessionId)
    }

    fun isSessionActive(): Boolean = activeSessionIds.isNotEmpty()

    /** Returns total size in bytes of the player temporary directory. */
    fun getPlayerTempCacheSize(): Long {
        return calculateDirSize(playerTempDir)
    }

    /** Returns total size in bytes of OkHttp response cache. */
    fun getHttpCacheSize(): Long {
        return try {
            httpCache.size()
        } catch (_: Exception) {
            0L
        }
    }

    /** Returns combined cache size across player temp, HTTP cache, and app cache directory. */
    fun getTotalCacheSize(): Long {
        return getPlayerTempCacheSize() + getHttpCacheSize()
    }

    /**
     * Clears only the app's designated player temporary cache directory.
     *
     * Will NOT delete files if a playback session is currently active unless [force] is true,
     * ensuring active Media3 streams are never corrupted during playback.
     */
    fun clearPlayerTempCache(force: Boolean = false): Long {
        if (isSessionActive() && !force) {
            return 0L
        }

        var bytesFreed = 0L
        try {
            val files = playerTempDir.listFiles() ?: return 0L
            for (file in files) {
                val size = file.length()
                if (file.isDirectory) {
                    bytesFreed += deleteDirContents(file)
                }
                if (file.delete()) {
                    bytesFreed += size
                }
            }
        } catch (_: Exception) {
            // Best effort
        }
        return bytesFreed
    }

    /** Clears HTTP response cache. */
    fun clearHttpCache(): Boolean {
        return try {
            httpCache.evictAll()
            true
        } catch (_: Exception) {
            false
        }
    }

    /** Invalidates only the known descriptor response, never cookies, catalog or user data. */
    fun invalidatePlaybackResponse(url: String) {
        try {
            val urls = httpCache.urls()
            while (urls.hasNext()) if (urls.next() == url) urls.remove()
        } catch (_: Exception) { }
    }

    /** Clears all temporary caches (player temp, HTTP cache, Coil image cache). */
    @OptIn(coil.annotation.ExperimentalCoilApi::class)
    fun clearAllCache(force: Boolean = false) {
        clearPlayerTempCache(force = force)
        clearHttpCache()
        try {
            imageLoader.diskCache?.clear()
            imageLoader.memoryCache?.clear()
        } catch (_: Exception) {
            // Best effort
        }
    }

    /**
     * Enforces user's configured cache size limit in megabytes.
     * Removes stale temp files (>24 hours) first, then oldest temp files if over threshold.
     */
    fun enforceCacheLimit(limitMb: Int) {
        val maxBytes = limitMb.toLong() * 1024 * 1024L
        val currentSize = getTotalCacheSize()

        if (currentSize <= maxBytes) return

        // Step 1: Remove player temp files older than 24 hours
        val dayOldMs = System.currentTimeMillis() - (24 * 60 * 60 * 1000L)
        try {
            val files = playerTempDir.listFiles() ?: emptyArray()
            for (file in files) {
                if (file.lastModified() < dayOldMs) {
                    if (isSessionActive()) break
                    file.deleteRecursively()
                }
            }
        } catch (_: Exception) {
            // Ignore
        }

        // Step 2: If still over limit and no active playback session, trim oldest temp files
        if (getTotalCacheSize() > maxBytes && !isSessionActive()) {
            try {
                val sortedFiles = (playerTempDir.listFiles() ?: emptyArray())
                    .sortedBy { it.lastModified() }
                for (file in sortedFiles) {
                    if (getTotalCacheSize() <= maxBytes) break
                    file.deleteRecursively()
                }
            } catch (_: Exception) {
                // Ignore
            }
        }
    }

    /**
     * Executes manual playback repair: clears player temporary files and invalidates HTTP cache
     * so stale or corrupted video manifest/chunks are re-fetched cleanly.
     */
    fun repairPlaybackCache() {
        check(!isSessionActive()) { "Close playback before repairing temporary files." }
        clearPlayerTempCache()
    }

    private fun calculateDirSize(dir: File): Long {
        if (!dir.exists()) return 0L
        var size = 0L
        val files = dir.listFiles() ?: return 0L
        for (file in files) {
            size += if (file.isDirectory) calculateDirSize(file) else file.length()
        }
        return size
    }

    private fun deleteDirContents(dir: File): Long {
        var freed = 0L
        val files = dir.listFiles() ?: return 0L
        for (file in files) {
            val fileLen = file.length()
            if (file.isDirectory) {
                freed += deleteDirContents(file)
            }
            if (file.delete()) {
                freed += fileLen
            }
        }
        return freed
    }
}
