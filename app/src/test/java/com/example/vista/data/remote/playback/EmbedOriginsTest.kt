package com.example.vista.data.remote.playback

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The allowlist is a security boundary, so the adversarial cases are the point of these tests.
 * A regression here silently re-opens arbitrary-JavaScript-in-the-app.
 */
class EmbedOriginsTest {

    @Test
    fun `provider origins are allowed`() {
        assertTrue(EmbedOrigins.allowsTopLevel("https://vidsrc.to/embed/movie/tt0111161"))
        assertTrue(EmbedOrigins.allowsTopLevel("https://vidsrc.pro/embed/tv/1396/1/1"))
        assertTrue(EmbedOrigins.allowsTopLevel("https://www.2embed.cc/embed/550"))
        assertTrue(EmbedOrigins.allowsTopLevel("https://vidsrc.me/embed/movie?tt0111161"))
        assertTrue(EmbedOrigins.allowsTopLevel("https://multiembed.mov/?video_id=550&tmdb=1"))
    }

    @Test
    fun `subdomains of a provider are allowed`() {
        assertTrue(EmbedOrigins.allowsTopLevel("https://player.vidsrc.to/embed/movie/550"))
    }

    @Test
    fun `a lookalike host is rejected`() {
        // Suffix anchoring must require the dot, or these all pass as vidsrc.to.
        assertFalse(EmbedOrigins.allowsTopLevel("https://vidsrc.to.evil.example/embed/550"))
        assertFalse(EmbedOrigins.allowsTopLevel("https://notvidsrc.to/embed/550"))
        assertFalse(EmbedOrigins.allowsTopLevel("https://vidsrcXto/embed/550"))
    }

    @Test
    fun `arbitrary origins are rejected`() {
        assertFalse(EmbedOrigins.allowsTopLevel("https://evil.example/phish"))
        assertFalse(EmbedOrigins.allowsTopLevel("http://localhost:8080/admin"))
        assertFalse(EmbedOrigins.allowsTopLevel(""))
    }

    @Test
    fun `non-http schemes are rejected`() {
        // about:blank is what onRelease loads to tear the WebView down; if it were allowed through
        // shouldOverrideUrlLoading the teardown path could be redirected.
        assertFalse(EmbedOrigins.allowsTopLevel("about:blank"))
        assertFalse(EmbedOrigins.allowsTopLevel("javascript:alert(1)"))
        assertFalse(EmbedOrigins.allowsTopLevel("data:text/html,<script>alert(1)</script>"))
        assertFalse(EmbedOrigins.allowsTopLevel("file:///data/data/com.example.vista/databases/vista.db"))
    }

    @Test
    fun `userinfo cannot disguise the host`() {
        // Without userinfo stripping, the authority ends up as "vidsrc.to@evil.example" and a naive
        // endsWith check would let it through.
        assertFalse(EmbedOrigins.allowsTopLevel("https://vidsrc.to@evil.example/x"))
        assertFalse(EmbedOrigins.allowsTopLevel("https://user@evil.example/x"))
    }

    @Test
    fun `port and path are ignored for matching`() {
        assertTrue(EmbedOrigins.allowsTopLevel("https://vidsrc.to:8443/embed/movie/550"))
        assertTrue(EmbedOrigins.allowsTopLevel("https://vidsrc.to/embed/movie/550?a=b#c"))
    }

    @Test
    fun `cleartext detection covers mixed case and padding`() {
        assertTrue(EmbedOrigins.isCleartext("http://vidsrc.to/embed/550"))
        assertTrue(EmbedOrigins.isCleartext("HTTP://vidsrc.to/embed/550"))
        assertTrue(EmbedOrigins.isCleartext("  http://vidsrc.to/x"))
        assertFalse(EmbedOrigins.isCleartext("https://vidsrc.to/embed/550"))
    }

    @Test
    fun `every configured origin yields a host`() {
        // Guards the hostOf parsing used to build the list; an empty entry would silently widen
        // nothing but a missing one would silently narrow the allowlist and break playback.
        assertTrue(EmbedOrigins.hosts.isNotEmpty())
        assertTrue(EmbedOrigins.hosts.none { it.isEmpty() })
        assertFalse(EmbedOrigins.hosts.any { it.contains("://") })
    }
}