package com.example.vista.ai

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import com.example.vista.BuildConfig
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Signed-in, cancellable cloud speech. The ElevenLabs key never enters the APK. */
class ElevenLabsSpeechPlayer(private val context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val client = OkHttpClient.Builder().callTimeout(25, TimeUnit.SECONDS).build()
    private var job: Job? = null
    private var call: Call? = null
    private var player: MediaPlayer? = null
    private var file: File? = null
    private var generation = 0
    private var closed = false

    fun play(text: String, onStart: () -> Unit, onDone: () -> Unit, onFallback: () -> Unit) {
        if (closed) return
        stop()
        val turn = generation
        job = scope.launch {
            var downloaded: File? = null
            try {
                val user = FirebaseAuth.getInstance().currentUser ?: throw IOException("Sign in for cloud speech")
                val token = user.getIdToken(false).await().token ?: throw IOException("Session unavailable")
                val base = BuildConfig.VISTA_AI_BASE_URL.trimEnd('/')
                require(base.startsWith("https://"))
                val body = JSONObject().put("text", text.take(1800)).toString()
                val readyFile = download(Request.Builder().url("$base/api/voice").header("Authorization", "Bearer $token")
                    .post(body.toRequestBody("application/json".toMediaType())).build())
                downloaded = readyFile
                ensureActive()
                check(FirebaseAuth.getInstance().currentUser?.uid == user.uid) { "Profile changed" }
                if (turn != generation || closed) return@launch
                file = downloaded
                val mediaPlayer = MediaPlayer()
                player = mediaPlayer
                mediaPlayer.apply {
                    setAudioAttributes(AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA).build())
                    setDataSource(readyFile.absolutePath)
                    setOnPreparedListener {
                        if (turn == generation && !closed) { onStart(); it.start() }
                    }
                    setOnCompletionListener {
                        if (turn == generation && !closed) { releasePlayback(); onDone() }
                    }
                    setOnErrorListener { _, _, _ ->
                        if (turn == generation && !closed) { releasePlayback(); onFallback() }
                        true
                    }
                    prepareAsync()
                }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) {
                if (turn == generation && !closed) { releasePlayback(); onFallback() }
            } finally {
                if (file != downloaded) downloaded?.delete()
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun download(request: Request): File = suspendCancellableCoroutine { continuation ->
        val requestCall = client.newCall(request)
        call = requestCall
        continuation.invokeOnCancellation { requestCall.cancel() }
        requestCall.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { if (continuation.isActive) continuation.resumeWithException(e) }
            override fun onResponse(call: Call, response: Response) {
                var temporary: File? = null
                try {
                    response.use {
                        if (!it.isSuccessful || it.body?.contentType()?.toString()?.startsWith("audio/") != true) throw IOException("Cloud speech unavailable")
                        temporary = File.createTempFile("vista-voice-", ".mp3", context.cacheDir)
                        it.body!!.byteStream().use { source ->
                            temporary!!.outputStream().use { output ->
                                val buffer = ByteArray(8192)
                                var total = 0
                                while (true) {
                                    if (!continuation.isActive) throw IOException("Speech cancelled")
                                    val count = source.read(buffer)
                                    if (count < 0) break
                                    total += count
                                    if (total > 2_000_000) throw IOException("Speech response too large")
                                    output.write(buffer, 0, count)
                                }
                                if (total == 0) throw IOException("Empty speech response")
                            }
                        }
                    }
                    val complete = temporary!!
                    if (continuation.isActive) continuation.resume(complete) { complete.delete() }
                    else temporary?.delete()
                } catch (e: Exception) {
                    temporary?.delete()
                    if (continuation.isActive) continuation.resumeWithException(e)
                }
            }
        })
    }

    private fun releasePlayback() {
        player?.release(); player = null
        file?.delete(); file = null
    }

    fun stop() {
        generation++
        job?.cancel(); job = null
        call?.cancel(); call = null
        releasePlayback()
    }

    fun close() {
        if (closed) return
        closed = true
        stop()
        scope.cancel()
    }
}
