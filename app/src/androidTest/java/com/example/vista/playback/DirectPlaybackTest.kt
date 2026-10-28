package com.example.vista.playback

import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DirectPlaybackTest {
    @Test fun publicDemoStreamPreparesAndReportsDuration() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val ready = CountDownLatch(1)
        var failure: PlaybackException? = null
        lateinit var player: ExoPlayer
        instrumentation.runOnMainSync {
            player = ExoPlayer.Builder(instrumentation.targetContext).build()
            player.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) { if (playbackState == Player.STATE_READY) ready.countDown() }
                override fun onPlayerError(error: PlaybackException) { failure = error; ready.countDown() }
            })
            // Public Google/Shaka demo fixture, unrelated to provider entitlement or user data.
            player.setMediaItem(MediaItem.fromUri("https://storage.googleapis.com/shaka-demo-assets/angel-one-hls/hls.m3u8"))
            player.prepare()
        }
        try {
            assertTrue("Stream prepare timed out", ready.await(40, TimeUnit.SECONDS))
            assertNull("Demo playback failed: ${failure?.message}", failure)
            instrumentation.runOnMainSync { assertTrue(player.duration > 0) }
        } finally { instrumentation.runOnMainSync { player.release() } }
    }
}
