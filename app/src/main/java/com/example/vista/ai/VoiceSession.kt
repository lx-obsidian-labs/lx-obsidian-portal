package com.example.vista.ai

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.*
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

/** Screen-owned, user-activated microphone. Never starts or restarts listening automatically. */
class VoiceSession(context: Context, private val state: (String) -> Unit, private val transcript: (String) -> Unit) {
    private var closed = false
    private var ready = false
    private val recognizer = if (SpeechRecognizer.isRecognitionAvailable(context)) SpeechRecognizer.createSpeechRecognizer(context) else null
    private val tts = TextToSpeech(context.applicationContext) { status -> ready = status == TextToSpeech.SUCCESS }

    init {
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) { if (!closed) state("Speaking") }
            override fun onDone(id: String?) { if (!closed) state("Idle") }
            @Deprecated("Platform callback") override fun onError(id: String?) { if (!closed) state("Speech unavailable") }
        })
        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { state("Listening — tap Stop to interrupt") }
            override fun onBeginningOfSpeech() { }
            override fun onRmsChanged(rmsdB: Float) { }
            override fun onBufferReceived(buffer: ByteArray?) { }
            override fun onEndOfSpeech() { state("Transcribing") }
            override fun onError(error: Int) { state(if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) "Microphone permission required" else "Voice unavailable ($error). You can type instead.") }
            override fun onResults(results: Bundle?) {
                state("Idle")
                results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let(transcript)
            }
            override fun onPartialResults(results: Bundle?) { }
            override fun onEvent(eventType: Int, params: Bundle?) { }
        })
    }
    fun start() {
        if (closed) return
        tts.stop()
        if (recognizer == null) { state("Speech recognition is unavailable. Type instead."); return }
        state("Starting microphone")
        try { recognizer.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }) } catch (_: SecurityException) { state("Microphone permission required. Type instead.") }
    }
    fun speak(text: String) {
        if (!closed && ready) tts.speak(text.take(3500), TextToSpeech.QUEUE_FLUSH, null, "vista-response")
    }
    fun stop() { if (closed) return; recognizer?.cancel(); tts.stop(); state("Idle") }
    fun close() { if (closed) return; closed = true; recognizer?.cancel(); recognizer?.destroy(); tts.stop(); tts.shutdown() }
}
