package com.example.vista.data.analytics

import android.content.Context
import android.util.Log
import com.example.vista.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * First-party, privacy-safe analytics for Vista Watch.
 *
 * Events are fire-and-forget: a failed network call must never affect the calling screen, so
 * sending happens on a background scope and every failure is swallowed. Nothing personal is ever
 * sent - the payload maps are small string maps of field names like `mediaType`, and the same
 * allowlist on the server rejects anything the client is not supposed to invent.
 *
 * The batch stays in memory only. A viewer with no signal simply loses the events, which is the
 * correct trade for telemetry that must cost nobody their battery or their privacy.
 */
@Singleton
class VistaAnalytics @Inject constructor(
    @ApplicationContext private val context: Context,
    @Named("shared") private val client: OkHttpClient
) {

    companion object {
        /** Bridge for non-injectable call sites (mostly the [android.content.Intent] shims). */
        @Volatile
        var instance: VistaAnalytics? = null

        private const val TAG = "VistaAnalytics"
        private const val MAX_BATCH = 32
        private const val BATCH_WINDOW_MS = 400L
        private const val MAX_PROPERTY_LENGTH = 200
        private val JSON = "application/json; charset=utf-8".toMediaType()
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val queue = Channel<AnalyticsEvent>(Channel.UNLIMITED)

    private val enabled: Boolean = BuildConfig.VISTA_ANALYTICS_KEY.isNotBlank() &&
        BuildConfig.VISTA_ANALYTICS_BASE_URL.isNotBlank()

    init {
        instance = this
        if (enabled) {
            scope.launch { flushLoop() }
        }
    }

    /**
     * Records a single event. Safe to call from any thread; returns immediately and never throws.
     */
    fun track(
        name: String,
        properties: Map<String, String> = emptyMap(),
        isGuest: Boolean = false
    ) {
        if (!enabled) return
        val payload = properties
            .map { (k, v) -> k.take(48) to v.take(MAX_PROPERTY_LENGTH) }
            .filter { (k, v) -> k.isNotBlank() && v.isNotBlank() }
            .toMap()
        queue.trySend(
            AnalyticsEvent(
                name = name.take(64),
                appVersion = BuildConfig.VERSION_NAME,
                isGuest = isGuest,
                locale = Locale.getDefault().toLanguageTag(),
                payload = payload
            )
        )
    }

    /** Sends events in small debounced batches. */
    private suspend fun flushLoop() {
        while (true) {
            val batch = mutableListOf<AnalyticsEvent>()
            try {
                batch += queue.receive()
            } catch (_: Exception) {
                continue
            }
            // Absorb anything that arrives in the next 400 ms so a burst becomes one request.
            val deadline = System.currentTimeMillis() + BATCH_WINDOW_MS
            while (batch.size < MAX_BATCH && System.currentTimeMillis() < deadline) {
                batch += queue.tryReceive().getOrNull() ?: break
            }
            sendBatch(batch)
        }
    }

    private fun sendBatch(events: List<AnalyticsEvent>) {
        if (events.isEmpty()) return
        try {
            val array = JSONArray()
            for (event in events) {
                array.put(
                    JSONObject()
                        .put("name", event.name)
                        .put("appVersion", event.appVersion)
                        .put("isGuest", event.isGuest)
                        .put("locale", event.locale)
                        .put("payload", JSONObject(event.payload))
                )
            }
            val body = JSONObject().put("events", array).toString()
            val request = Request.Builder()
                .url(BuildConfig.VISTA_ANALYTICS_BASE_URL.trimEnd('/') + "/api/collect")
                .addHeader("X-Vista-Analytics-Key", BuildConfig.VISTA_ANALYTICS_KEY)
                .post(body.toRequestBody(JSON))
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "Collect failed HTTP ${response.code}")
                }
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.d(TAG, "Analytics send skipped", e)
        }
    }
}

data class AnalyticsEvent(
    val name: String,
    val appVersion: String,
    val isGuest: Boolean,
    val locale: String,
    val payload: Map<String, String>
)
