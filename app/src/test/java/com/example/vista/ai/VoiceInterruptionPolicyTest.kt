package com.example.vista.ai

import org.junit.Assert.*
import org.junit.Test

class VoiceInterruptionPolicyTest {
    private val reply = "Try a thoughtful science fiction movie tonight. It has a warm, hopeful ending."

    @Test fun rejectsSpeakerEchoIncludingDroppedWordsAndPunctuation() {
        assertFalse(VoiceInterruptionPolicy.shouldInterrupt("SCIENCE fiction movie", reply))
        assertFalse(VoiceInterruptionPolicy.shouldInterrupt("a warm hopeful ending", reply, final = true))
        assertFalse(VoiceInterruptionPolicy.shouldInterrupt("thoughtful fiction tonight", reply))
    }

    @Test fun acceptsNewQuestionWhileAssistantIsTalking() {
        assertTrue(VoiceInterruptionPolicy.shouldInterrupt("Actually recommend comedy", reply))
        assertTrue(VoiceInterruptionPolicy.shouldInterrupt("What about something shorter", reply))
        assertTrue(VoiceInterruptionPolicy.shouldInterrupt("stop listening", reply))
        assertTrue(VoiceInterruptionPolicy.shouldInterrupt("wait", reply))
    }

    @Test fun waitsForMoreThanASingleUncertainPartialWord() {
        assertFalse(VoiceInterruptionPolicy.shouldInterrupt("Comedy", reply))
        assertTrue(VoiceInterruptionPolicy.shouldInterrupt("Comedy", reply, final = true))
        assertFalse(VoiceInterruptionPolicy.shouldInterrupt("  ", reply, final = true))
    }

    @Test fun ignoresBackchannelsEvenWhenFinal() {
        listOf("yeah", "okay", "mm hmm", "got it", "i see").forEach {
            assertFalse(VoiceInterruptionPolicy.shouldInterrupt(it, reply, final = true))
        }
    }

    @Test fun recognizesPauseRequests() {
        assertTrue(VoiceInterruptionPolicy.shouldPause("Give me a minute"))
        assertTrue(VoiceInterruptionPolicy.shouldPause("TAKE YOUR TIME."))
        assertTrue(VoiceInterruptionPolicy.shouldPause("never mind"))
        assertFalse(VoiceInterruptionPolicy.shouldPause("tell me more"))
    }
}
