package com.example.vista.ai

/** Conservative transcript-based echo rejection; platform recognizers vary by audio route. */
internal object VoiceInterruptionPolicy {
    private val backchannels = setOf("yeah", "yes", "yep", "ok", "okay", "right", "sure", "mm", "mhm", "mm hmm", "uh huh", "got it", "i see")
    private val pauseRequests = setOf("stop", "wait", "pause", "hold on", "hang on", "give me a minute", "take your time", "stop listening", "stop voice chat", "end voice chat", "never mind", "nevermind")

    private fun words(text: String) = Regex("[\\p{L}\\p{N}]+")
        .findAll(text.lowercase(java.util.Locale.ROOT)).map { it.value }.toList()

    fun shouldPause(heard: String): Boolean = words(heard).joinToString(" ") in pauseRequests

    fun shouldInterrupt(heard: String, spoken: String, final: Boolean = false): Boolean {
        val input = words(heard)
        if (input.isEmpty()) return false
        val phrase = input.joinToString(" ")
        if (phrase in backchannels) return false
        if (phrase in pauseRequests) return true
        val output = words(spoken)
        // Recognition often drops punctuation and small words from speaker echo.
        var next = 0
        for (word in output) if (next < input.size && word == input[next]) next++
        if (next == input.size) return false
        return input.size >= if (final) 1 else 2
    }
}
