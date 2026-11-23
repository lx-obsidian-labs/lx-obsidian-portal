package com.example.vista.data.playback

import org.junit.Assert.*
import org.junit.Test

class PlaybackUiPolicyTest {
    @Test fun autoplayRequiresARealEndEventAndAnActiveUnblockedSession() {
        assertTrue(PlaybackUiPolicy.canAutoplay(true, true, true, true, false, false, false, false))
        assertFalse(PlaybackUiPolicy.canAutoplay(false, true, true, true, false, false, false, false))
        assertFalse(PlaybackUiPolicy.canAutoplay(true, false, true, true, false, false, false, false))
        assertFalse(PlaybackUiPolicy.canAutoplay(true, true, false, true, false, false, false, false))
        assertFalse(PlaybackUiPolicy.canAutoplay(true, true, true, false, false, false, false, false))
        assertFalse(PlaybackUiPolicy.canAutoplay(true, true, true, true, true, false, false, false))
        assertFalse(PlaybackUiPolicy.canAutoplay(true, true, true, true, false, true, false, false))
        assertFalse(PlaybackUiPolicy.canAutoplay(true, true, true, true, false, false, true, false))
        assertFalse(PlaybackUiPolicy.canAutoplay(true, true, true, true, false, false, false, true))
    }
    @Test fun normalRemoteSelectAndArrowsRemainWithThePlayer() {
        assertFalse(PlaybackUiPolicy.opensTvOptions(23, true, false))
        assertFalse(PlaybackUiPolicy.opensTvOptions(19, true, false))
        assertFalse(PlaybackUiPolicy.opensTvOptions(20, true, false))
        assertFalse(PlaybackUiPolicy.opensTvOptions(82, false, false))
        assertTrue(PlaybackUiPolicy.opensTvOptions(82, true, false))
        assertTrue(PlaybackUiPolicy.opensTvOptions(23, true, true))
    }
}
