package com.example.vista.downloads

import com.example.vista.domain.model.*
import org.junit.Assert.*
import org.junit.Test

class DownloadPolicyTest {
    private fun source(type: StreamType = StreamType.HLS, expiry: Long? = null) = StreamSource(
        "https://media.example/title.m3u8", type, null, null, expiresAt = expiry)
    @Test fun identitySeparatesOwnersTypesAndEpisodesWithoutIncludingUrl() {
        val movie = DownloadPolicy.id("alice", "1", "movie", 0, 0)
        assertEquals(movie, DownloadPolicy.id("alice", "1", "movie", 0, 0))
        assertEquals(64, movie.length)
        assertNotEquals(movie, DownloadPolicy.id("bob", "1", "movie", 0, 0))
        assertNotEquals(movie, DownloadPolicy.id("alice", "1", "series", 1, 1))
        assertNotEquals(DownloadPolicy.id("alice", "1", "series", 1, 1), DownloadPolicy.id("alice", "1", "series", 1, 2))
    }
    @Test fun embedAndDrmAndAdvertisingAreNotAcceptedAsOfflineVideo() {
        assertNotNull(DownloadPolicy.unavailableReason(source(StreamType.OTHER), 0))
        assertNotNull(DownloadPolicy.unavailableReason(source().copy(drmInfo = DrmInfo(null, "widevine")), 0))
        assertNotNull(DownloadPolicy.unavailableReason(source().copy(url = "https://tagivi.com/ad.mp4"), 0))
        assertNull(DownloadPolicy.unavailableReason(source(), 0))
    }
    @Test fun expiryInSecondsOrMillisecondsUsesSameDeadline() {
        val now = 1_800_000_000_000L
        assertNotNull(DownloadPolicy.unavailableReason(source(expiry = now / 1_000), now))
        assertNotNull(DownloadPolicy.unavailableReason(source(expiry = now + 30_000), now))
        assertNull(DownloadPolicy.unavailableReason(source(expiry = (now + 120_000) / 1_000), now))
    }
}
