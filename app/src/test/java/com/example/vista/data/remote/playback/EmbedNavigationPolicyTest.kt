package com.example.vista.data.remote.playback

import org.junit.Assert.*
import org.junit.Test

class EmbedNavigationPolicyTest {
    @Test fun browserAndStoreIntentsAreRejectedEvenWithAnAllowedProviderHost() {
        for (url in listOf("intent://vidsrc.to/#Intent;scheme=https;package=com.opera.mini.native;end",
            "operamini://open?url=https://vidsrc.to", "market://details?id=com.opera.mini.native",
            "https://user@vidsrc.to/embed/movie/597", "http://vidsrc.to/embed/movie/597")) {
            assertTrue(url, EmbedNavigationPolicy.blocks(url, mainFrame = true))
        }
        assertTrue(EmbedNavigationPolicy.blocks("intent://vidsrc.to/#Intent;package=com.opera.mini.native;end", mainFrame = false))
    }
    @Test fun providerPagesAndIndependentHttpsMediaFramesStillWork() {
        assertFalse(EmbedNavigationPolicy.blocks("https://vidsrc.to/embed/movie/597", true))
        assertFalse(EmbedNavigationPolicy.blocks("https://cdn.example/player/index.html", false))
        assertFalse(EmbedNavigationPolicy.blocks("about:blank", false))
        assertTrue(EmbedNavigationPolicy.blocks("https://download.opera.com/installer", true))
        assertTrue(EmbedNavigationPolicy.blocks("https://ads.doubleclick.net/ad", false))
    }
}
