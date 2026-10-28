package com.example.vista.ai

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VoiceLifecycleTest {
    @Test fun leavingScreenPreventsMicrophoneReactivation() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val states = mutableListOf<String>()
        instrumentation.runOnMainSync {
            val voice = VoiceSession(instrumentation.targetContext, { states.add(it) }, { error("No capture was requested") })
            voice.stop()
            voice.close()
            val size = states.size
            voice.start(); voice.speak("Should never speak after leaving"); voice.stop(); voice.close()
            assertEquals(size, states.size)
        }
    }
}
