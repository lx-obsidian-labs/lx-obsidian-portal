package com.example.vista.ai

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.*
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

/** One explicit start enables foreground, hands-free turns until Stop or screen exit. */
class VoiceSession(context: Context, private val state: (String) -> Unit, private val transcript: (String) -> Unit,
    private val onInterrupt: () -> Unit = {}) {
    private val preferences = context.applicationContext.getSharedPreferences("vista_voice", Context.MODE_PRIVATE)
    private var cloudEnabled = cloudVoiceEnabled(context)
    private val main = Handler(Looper.getMainLooper())
    private var closed = false
    private var ready = false
    private var active = false
    private var listening = false
    private var waiting = false
    private var retries = 0
    private var queuedSpeech: String? = null
    private val cloud = ElevenLabsSpeechPlayer(context.applicationContext)
    private var utteranceId: String? = null
    private var speechGeneration = 0
    private var speakingText: String? = null
    val isHandsFree: Boolean get() = active
    private val recognizer = if (SpeechRecognizer.isRecognitionAvailable(context)) SpeechRecognizer.createSpeechRecognizer(context) else null
    private val tts = TextToSpeech(context.applicationContext) { status ->
        main.post { configureVoice(status) }
    }
    private val listenAgain = Runnable { listen() }

    private fun configureVoice(status: Int) {
        if (closed) return
        ready = status == TextToSpeech.SUCCESS
        if (!ready) { state("Device voice unavailable; cloud speech requires sign-in and a connection."); return }
        val locale = Locale.getDefault()
        val language = tts.setLanguage(locale)
        if (language < TextToSpeech.LANG_AVAILABLE) tts.setLanguage(Locale.US)
        // Prefer an installed high-quality (often neural/network) voice in the user's locale.
        // Android's installed engine provides this without an app-owned paid TTS service.
        val selected = tts.voices.orEmpty().filter {
            it.locale.language == (if (language >= TextToSpeech.LANG_AVAILABLE) locale.language else "en") &&
                "notInstalled" !in it.features.orEmpty()
        }.sortedWith(compareByDescending<android.speech.tts.Voice> { it.quality }
            .thenByDescending { it.locale.country == locale.country }
            .thenBy { it.latency }).firstOrNull()
        selected?.let { tts.voice = it }
        tts.setSpeechRate(0.96f)
        tts.setPitch(1.0f)
        queuedSpeech?.let { queuedSpeech = null; speakLocal(it) }
    }

    init {
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) { main.post { if (!closed && id == utteranceId) playbackStarted("device voice") } }
            override fun onDone(id: String?) { main.post {
                if (!closed && id == utteranceId) { utteranceId = null; playbackDone() }
            } }
            @Deprecated("Platform callback") override fun onError(id: String?) { main.post {
                if (!closed && id == utteranceId) { stop(); state("Voice playback unavailable. Check your speech engine in Android settings.") }
            } }
        })
        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { if (active && !closed) state(if (speakingText != null) "Speaking — talk over me anytime, or tap Interrupt" else "Listening — hands-free • say ‘stop listening’ to end") }
            override fun onBeginningOfSpeech() { retries = 0 }
            override fun onRmsChanged(rmsdB: Float) { }
            override fun onBufferReceived(buffer: ByteArray?) { }
            override fun onEndOfSpeech() { if (active && speakingText == null) state("Got it — transcribing") }
            override fun onError(error: Int) {
                listening = false
                if (!active || closed || (waiting && speakingText == null)) return
                val recoverable = error in setOf(SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT, SpeechRecognizer.ERROR_RECOGNIZER_BUSY) || speakingText != null
                if (recoverable && error != SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS && retries++ < 3) {
                    resumeListening(if (speakingText != null) 700 else 1000)
                } else if (speakingText != null) {
                    interruptPlayback()
                    resumeListening(900)
                    state("Voice paused — say your request again or tap the microphone")
                } else {
                    stop()
                    state(if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) "Microphone permission required" else "Voice paused. Tap the microphone to try again.")
                }
            }
            override fun onResults(results: Bundle?) {
                listening = false
                if (!active || closed || (waiting && speakingText == null)) return
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.trim()
                if (text.isNullOrBlank()) { resumeListening(); return }
                speakingText?.let {
                    if (!VoiceInterruptionPolicy.shouldInterrupt(text, it, final = true)) { resumeListening(250); return }
                    interruptPlayback()
                }
                if (VoiceInterruptionPolicy.shouldPause(text)) { stop(); state("Voice paused — take your time"); return }
                waiting = true
                state("Thinking — hands-free • you can keep talking")
                transcript(text)
            }
            override fun onPartialResults(results: Bundle?) {
                if (!active || closed || (waiting && speakingText == null)) return
                results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.takeIf { it.isNotBlank() }?.let {
                    speakingText?.let { spoken ->
                        if (!VoiceInterruptionPolicy.shouldInterrupt(it, spoken)) return
                        interruptPlayback()
                    }
                    state("Listening: ${it.take(180)}")
                }
            }
            override fun onEvent(eventType: Int, params: Bundle?) { }
        })
    }

    fun start() {
        if (closed) return
        stop()
        if (recognizer == null) { state("Speech recognition is unavailable. Type instead."); return }
        active = true
        retries = 0
        listen()
    }

    private fun resumeListening(delay: Long = 600) {
        if (!active || closed) return
        if (speakingText == null) state("Ready for your next question — hands-free")
        main.removeCallbacks(listenAgain)
        main.postDelayed(listenAgain, delay)
    }

    private fun listen() {
        if (!active || closed || (waiting && speakingText == null) || listening) return
        listening = true
        if (speakingText == null) state("Starting microphone — hands-free")
        try {
            recognizer?.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            })
        } catch (_: Exception) {
            listening = false
            if (speakingText != null && retries++ < 2) resumeListening(800)
            else if (speakingText != null) {
                interruptPlayback()
                resumeListening(900)
                state("Voice paused — say your request again or tap the microphone")
            } else { stop(); state("Microphone unavailable. Check permission and try again.") }
        }
    }

    fun speak(text: String) {
        if (closed) return
        val spokenText = VoiceText.prepare(text)
        if (spokenText.isBlank()) { waiting = false; if (active) resumeListening() else state("Idle"); return }
        main.removeCallbacks(listenAgain)
        waiting = true
        utteranceId = null
        speakingText = spokenText
        if (active) {
            if (!listening) resumeListening(200)
        } else {
            listening = false
            recognizer?.cancel()
        }
        cloud.stop()
        tts.stop()
        val turn = ++speechGeneration
        if (!cloudEnabled) { speakLocal(spokenText); return }
        state(if (active) "Preparing ElevenLabs voice — talk to interrupt" else "Preparing ElevenLabs voice")
        cloud.play(spokenText,
            onStart = { if (!closed && turn == speechGeneration) { speakingText = spokenText; playbackStarted("ElevenLabs") } },
            onDone = { if (!closed && turn == speechGeneration) playbackDone() },
            onFallback = { if (!closed && turn == speechGeneration) {
                speakingText = spokenText
                state(if (active) "Using device voice — talk to interrupt" else "Using device voice")
                speakLocal(spokenText)
            } })
    }

    private fun playbackStarted(provider: String) {
        state(if (active) "Speaking — $provider • talk to interrupt" else "Speaking — $provider")
        if (active) resumeListening(350)
    }

    private fun playbackDone() {
        speakingText = null
        waiting = false
        if (!active) {
            listening = false
            recognizer?.cancel()
            state("Idle")
            return
        }
        if (listening) state("Listening — what else?") else resumeListening()
    }

    /** Stop only the current turn; preserve the recognizer's in-progress user utterance. */
    private fun interruptPlayback() {
        speechGeneration++
        utteranceId = null
        queuedSpeech = null
        speakingText = null
        cloud.stop()
        tts.stop()
        waiting = false
        retries = 0
        onInterrupt()
    }

    fun interrupt() {
        if (closed) return
        val wasListening = listening
        interruptPlayback()
        active = true
        waiting = false
        main.removeCallbacks(listenAgain)
        if (wasListening) {
            state("Got it — go ahead")
        } else {
            recognizer?.cancel()
            listening = false
            resumeListening(200)
        }
    }

    fun setCloudVoice(enabled: Boolean) {
        if (closed || cloudEnabled == enabled) return
        stop()
        cloudEnabled = enabled
        preferences.edit().putBoolean("elevenlabs_enabled", enabled).apply()
    }

    private fun speakLocal(text: String) {
        if (closed) return
        if (!ready) {
            queuedSpeech = text
            // A failed device engine cannot complete a queued utterance.
            main.postDelayed({ if (!closed && queuedSpeech != null && !ready) { stop(); state("Voice unavailable. You can keep chatting by typing.") } }, 3000)
            return
        }
        utteranceId = "vista-response-${speechGeneration}"
        if (tts.speak(text.take(3500), TextToSpeech.QUEUE_FLUSH, null, utteranceId) == TextToSpeech.ERROR) {
            stop(); state("Speech unavailable. Check your installed text-to-speech voice.")
        }
    }

    fun stop() {
        if (closed) return
        active = false
        speakingText = null
        speechGeneration++
        utteranceId = null
        cloud.stop()
        listening = false
        waiting = false
        queuedSpeech = null
        main.removeCallbacks(listenAgain)
        recognizer?.cancel()
        tts.stop()
        state("Idle")
    }

    fun close() {
        if (closed) return
        stop()
        closed = true
        main.removeCallbacksAndMessages(null)
        recognizer?.destroy()
        tts.shutdown()
        cloud.close()
    }

    companion object {
        fun cloudVoiceEnabled(context: Context): Boolean = context.applicationContext
            .getSharedPreferences("vista_voice", Context.MODE_PRIVATE).getBoolean("elevenlabs_enabled", true)
    }
}
