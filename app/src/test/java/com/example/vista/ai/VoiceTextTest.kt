package com.example.vista.ai

import org.junit.Assert.*
import org.junit.Test

class VoiceTextTest {
    @Test fun spokenResponsesDoNotReadPublisherUrlsOrCitationMarkers() {
        assertEquals("A good film.", VoiceText.prepare("**A good film.** [1] https://publisher.org/story"))
    }
    @Test fun longAnswersEndAtASentenceAndPointToTheRemainingOnscreenText() {
        val result = VoiceText.prepare("A useful sentence. ".repeat(150))
        assertTrue(result.length <= 1800)
        assertTrue(result.endsWith(". You can read the rest on screen."))
    }
}
