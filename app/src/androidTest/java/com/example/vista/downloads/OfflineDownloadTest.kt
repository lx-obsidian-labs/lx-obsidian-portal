package com.example.vista.downloads

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.vista.MainActivity
import com.example.vista.domain.model.StreamSource
import com.example.vista.domain.model.StreamType
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class OfflineDownloadTest {
    @Test fun adaptiveDownloadCompletesAndSeeksUsingStrictCacheOnlyPlayback() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val connectivity = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        assumeFalse("An unmetered test connection is required", connectivity.isActiveNetworkMetered)
        val downloads = EntryPointAccessors.fromApplication(context.applicationContext, OfflineDownloadsAccess::class.java).offlineDownloads()
        assumeTrue("Do not interrupt a viewer's active download", downloads.list().none { it.state == Download.STATE_DOWNLOADING })
        context.startActivity(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        delay(1_000)
        val target = DownloadTarget("offline-test-${System.currentTimeMillis()}", "Offline test clip", "movie")
        var downloadId: String? = null
        var player: ExoPlayer? = null
        try {
            downloadId = downloads.enqueue(target, StreamSource(
                "https://storage.googleapis.com/shaka-demo-assets/angel-one-hls/hls.m3u8",
                StreamType.HLS, null, null, "Public Shaka demo fixture"), 480)
            val id = downloadId
            withTimeout(150_000) {
                while (true) {
                    val entry = downloads.list().firstOrNull { it.id == id }
                    check(entry?.state != Download.STATE_FAILED) { "The public test fixture failed to download." }
                    if (entry?.state == Download.STATE_COMPLETED) { assertTrue(entry.bytes > 0); break }
                    delay(500)
                }
            }
            // The returned factory has NO upstream: a missing segment cannot silently use Wi-Fi.
            val (item, cacheOnlyFactory) = downloads.offlineItem(id)
            val ready = CountDownLatch(1)
            var failure: PlaybackException? = null
            instrumentation.runOnMainSync {
                player = ExoPlayer.Builder(context).setMediaSourceFactory(DefaultMediaSourceFactory(cacheOnlyFactory)).build().apply {
                    addListener(object : Player.Listener {
                        override fun onPlaybackStateChanged(state: Int) { if (state == Player.STATE_READY) ready.countDown() }
                        override fun onPlayerError(error: PlaybackException) { failure = error; ready.countDown() }
                    })
                    setMediaItem(item); prepare()
                }
            }
            assertTrue("Offline preparation timed out", ready.await(20, TimeUnit.SECONDS))
            assertNull("Offline source needed unavailable content: ${failure?.message}", failure)
            instrumentation.runOnMainSync { assertTrue(player!!.duration > 0); player!!.seekTo(player!!.duration - 1_000) }
            withTimeout(20_000) {
                var seekReady = false
                while (!seekReady) {
                    instrumentation.runOnMainSync { seekReady = player!!.playbackState == Player.STATE_READY && player!!.currentPosition >= player!!.duration - 1_500 }
                    check(failure == null) { "Offline end seek failed: ${failure?.message}" }
                    delay(100)
                }
            }
        } finally {
            instrumentation.runOnMainSync { player?.release() }
            downloadId?.let { downloads.remove(it) }
        }
    }
}
