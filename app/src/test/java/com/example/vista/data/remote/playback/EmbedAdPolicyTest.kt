package com.example.vista.data.remote.playback

import org.junit.Assert.*
import org.junit.Test

class EmbedAdPolicyTest {
    @Test fun blocksKnownAdvertisingHostsAndSubdomains() {
        assertTrue(EmbedAdPolicy.blocks("https://ads.doubleclick.net/video.mp4"))
        assertTrue(EmbedAdPolicy.blocks("https://imasdk.googleapis.com/js/sdkloader/ima3.js"))
        assertTrue(EmbedAdPolicy.blocks("https://adsco.re/player.js"))
    }

    @Test fun doesNotBreakMediaWhenQueryOrPathMentionsAdDomain() {
        assertFalse(EmbedAdPolicy.blocks("https://cdn.example/video.mp4?referer=doubleclick.net"))
        assertFalse(EmbedAdPolicy.blocks("https://cdn.example/adsco.re/master.m3u8"))
        assertFalse(EmbedAdPolicy.blocks("https://notdoubleclick.net/video.mp4"))
        assertFalse(EmbedAdPolicy.blocks("https://doubleclick.net.example/video.mp4"))
    }

    @Test fun embedProvidersAndUnrelatedCdnsRemainAvailable() {
        EmbedOrigins.hosts.forEach { assertFalse(EmbedAdPolicy.blocks("https://$it/embed/movie/1")) }
        assertFalse(EmbedAdPolicy.blocks("https://cdn.example/master.m3u8"))
    }

    @Test fun blocksObservedFakeRobotInjectionWithoutBlockingWholeMediaCdn() {
        assertTrue(EmbedAdPolicy.blocks("https://createlouisville.org/14/template"))
        assertTrue(EmbedAdPolicy.blocks("https://tagivi.com/1/placement"))
        assertTrue(EmbedAdPolicy.blocks("https://interlinecustomroofingllc.com/4/placement"))
        assertTrue(EmbedAdPolicy.blocks("https://soycdn.lol/__ads/vidsrcto.js?v=1"))
        assertTrue(EmbedAdPolicy.blocks("https://cdn.cloudpiko.com/sb/notifications/utility/default/robot/2/js/jquery.min.js"))
        assertFalse(EmbedAdPolicy.blocks("https://soycdn.lol/player.js"))
        assertFalse(EmbedAdPolicy.blocks("https://cdn.cloudpiko.com/other/resource.js"))
    }

    @Test fun genuineVerificationWidgetsAreNotClassifiedAsAds() {
        assertFalse(EmbedAdPolicy.blocks("https://challenges.cloudflare.com/turnstile/v0/api.js"))
        assertFalse(EmbedAdPolicy.blocks("https://www.google.com/recaptcha/api.js"))
        assertFalse(EmbedAdPolicy.blocks("https://www.gstatic.com/recaptcha/releases/example.js"))
    }
}
