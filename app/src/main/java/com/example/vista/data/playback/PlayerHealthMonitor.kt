package com.example.vista.data.playback

import com.example.vista.data.analytics.VistaAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import javax.inject.Inject
import javax.inject.Singleton

enum class PlaybackErrorType {
    EXPIRED_LOCAL_CACHE,
    NETWORK_CONNECTIVITY,
    AUTHORIZATION_EXPIRED,
    SERVER_FAILURE,
    DECODER_UNSUPPORTED,
    UNKNOWN
}

data class PlaybackErrorRecord(
    val timestamp: Long = System.currentTimeMillis(),
    val mediaId: String,
    val streamUrl: String?,
    val errorType: PlaybackErrorType,
    val message: String
)

/**
 * Player Health Monitor for Vista Watch.
 *
 * Records playback errors, tracks failure frequencies per stream/host, and detects recurring issues
 * to trigger automatic recovery strategies and populate diagnostic tools.
 */
@Singleton
class PlayerHealthMonitor @Inject constructor(
    private val vistaAnalytics: VistaAnalytics,
    private val crashlytics: FirebaseCrashlytics
) {
    private val errorHistory = ConcurrentLinkedQueue<PlaybackErrorRecord>()
    private val failureCounts = ConcurrentHashMap<String, Int>()

    fun recordError(
        mediaId: String,
        streamUrl: String?,
        error: Throwable,
        errorType: PlaybackErrorType
    ) {
        val count = (failureCounts[mediaId] ?: 0) + 1
        failureCounts[mediaId] = count

        val record = PlaybackErrorRecord(
            mediaId = mediaId,
            streamUrl = streamUrl,
            errorType = errorType,
            message = error.message ?: "Unknown playback error"
        )

        errorHistory.add(record)
        while (errorHistory.size > 50) {
            errorHistory.poll()
        }

        vistaAnalytics.track(
            "player_error",
            properties = mapOf(
                "mediaId" to mediaId,
                "errorType" to errorType.name,
                "failureCount" to count.toString(),
                "message" to (error.message ?: "Unknown")
            )
        )

        crashlytics.recordException(error)
    }

    fun recordSuccess(mediaId: String) {
        failureCounts.remove(mediaId)
    }

    fun getFailureCount(mediaId: String): Int {
        return failureCounts[mediaId] ?: 0
    }

    fun hasRecurringFailures(mediaId: String): Boolean {
        return getFailureCount(mediaId) >= 3
    }

    fun getHealthDiagnostics(): String {
        val totalErrors = errorHistory.size
        val recentErrors = errorHistory.toList().takeLast(5).joinToString("\n") {
            "[${it.errorType}] ${it.mediaId}: ${it.message}"
        }
        return "Total recorded errors: $totalErrors\nRecent:\n$recentErrors"
    }
}
