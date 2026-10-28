package com.example.vista.data.playback

import androidx.media3.common.PlaybackException
import androidx.media3.datasource.HttpDataSource
import com.example.vista.data.cache.CacheManager
import com.example.vista.data.session.SessionManager
import com.example.vista.domain.model.StreamSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton

sealed class RecoveryResult {
    data class RetrySameSource(val savedPosition: Long) : RecoveryResult()
    data class SwitchToAlternativeSource(val newSource: StreamSource, val savedPosition: Long) : RecoveryResult()
    object RequiresNetwork : RecoveryResult()
    data class Failed(val userMessage: String, val errorType: PlaybackErrorType, val canRepairManually: Boolean = true) : RecoveryResult()
}

@Singleton
class PlaybackRecoveryManager @Inject constructor(
    private val cacheManager: CacheManager, private val sessionManager: SessionManager,
    private val monitor: PlayerHealthMonitor
) {
    fun classifyError(error: Throwable): PlaybackErrorType {
        val causes = generateSequence(error) { it.cause }.take(10).toList()
        val http = causes.filterIsInstance<HttpDataSource.InvalidResponseCodeException>().firstOrNull()?.responseCode
        return when {
            http == 401 || http == 403 -> PlaybackErrorType.AUTHORIZATION_EXPIRED
            causes.any { it is UnknownHostException || it is ConnectException || it is SocketTimeoutException } -> PlaybackErrorType.NETWORK_CONNECTIVITY
            http == 410 || causes.any { it is PlaybackException && it.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW } -> PlaybackErrorType.EXPIRED_LOCAL_CACHE
            http != null -> PlaybackErrorType.SERVER_FAILURE
            causes.any { it is PlaybackException && it.errorCode in 4000..4999 } -> PlaybackErrorType.DECODER_UNSUPPORTED
            causes.any { it is PlaybackException && it.errorCode in 6000..6999 } -> PlaybackErrorType.AUTHORIZATION_EXPIRED
            else -> PlaybackErrorType.UNKNOWN
        }
    }

    suspend fun attemptRecovery(
        mediaId: String, currentPosition: Long, error: Throwable, currentSource: StreamSource?,
        availableSources: List<StreamSource>, retryAttemptCount: Int,
        budget: RecoveryBudget = RecoveryBudget()
    ): RecoveryResult {
        val type = classifyError(error)
        monitor.recordError(mediaId, null, IllegalStateException(type.name), type)
        if (retryAttemptCount >= 2 || !budget.take(currentSource?.url)) return RecoveryResult.Failed("Automatic recovery stopped after two attempts. Check your connection or try Repair Playback.", type)
        if (!sessionManager.isSessionValid()) return RecoveryResult.Failed("Your session has ended. Please sign in.", PlaybackErrorType.AUTHORIZATION_EXPIRED, false)
        if (type == PlaybackErrorType.AUTHORIZATION_EXPIRED) {
            // Identity refresh is not a provider entitlement grant. Never switch mirrors to work
            // around a provider/DRM denial. The normal authorization flow must handle it.
            try { sessionManager.refreshSession() } catch (e: CancellationException) { throw e }
            return RecoveryResult.Failed("The provider denied playback authorization. Sign in again or contact the provider. Cache repair cannot grant access.", type, false)
        }
        if (type == PlaybackErrorType.NETWORK_CONNECTIVITY) return RecoveryResult.RequiresNetwork
        currentSource?.url?.let { cacheManager.invalidatePlaybackResponse(it) }
        delay(500L * (retryAttemptCount + 1))
        return RecoveryResult.RetrySameSource(currentPosition)
    }

    fun repairAndResetCache(): Boolean {
        if (cacheManager.isSessionActive()) return false
        cacheManager.clearPlayerTempCache()
        return true
    }
}
