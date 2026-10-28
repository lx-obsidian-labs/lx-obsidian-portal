package com.example.vista.data.remote.playback

/**
 * The provider origins this app renders, and the only ones it will follow a top-level navigation to.
 *
 * Why this is a single source of truth: the player WebView runs JavaScript, so an arbitrary origin
 * loaded in it means arbitrary script in this app. The navigation allowlist therefore has to name
 * the same providers the resolver actually builds URLs for. Keeping the hosts derived from the base
 * URLs - rather than typed out again in the UI layer - means adding or retiring a provider cannot
 * leave the security check quietly disagreeing with the resolver.
 *
 * Deliberately *not* an allowlist for sub-resources. The embeds stream from CDNs on unrelated
 * hosts, and an earlier attempt to allowlist every request blanked the player frame entirely.
 * Sub-resources cannot navigate the user anywhere, so they are left unrestricted.
 */
object EmbedOrigins {

    const val VIDSRUC = "https://vidsrc.to"
    const val VIDSRCPRO = "https://vidsrc.pro"
    const val TWOE = "https://www.2embed.cc"
    const val VIDSRCME = "https://vidsrc.me"
    const val MULTIEMBED = "https://multiembed.mov"

    /** Every origin that may host a main-frame page, as [host] values. */
    val hosts: List<String> = listOf(VIDSRUC, VIDSRCPRO, TWOE, VIDSRCME, MULTIEMBED)
        .map { hostOf(it) }
        .filter { it.isNotEmpty() }

    /**
     * True when [url] sits on a provider origin or one of its subdomains.
     *
     * Fails closed: anything without a parseable host - `about:blank`, `data:`, `javascript:`, a
     * relative path - is rejected. Subdomain matching is anchored on a dot so that
     * `https://vidsrc.to.evil.example` cannot pass as `vidsrc.to`.
     */
    fun allowsTopLevel(url: String): Boolean {
        val host = hostOf(url)
        if (host.isEmpty()) return false
        return hosts.any { host == it || host.endsWith(".$it") }
    }

    /** True when [url] is plaintext http, which the app refuses to load. */
    fun isCleartext(url: String): Boolean =
        url.trimStart().lowercase().startsWith("http://")

    /**
     * Host of [url], or an empty string when there is not one.
     *
     * Hand-rolled rather than `android.net.Uri` so the whole allowlist stays plain Kotlin and
     * testable without Robolectric. Strips scheme, userinfo, path, query and port.
     */
    private fun hostOf(url: String): String {
        val afterScheme = url.substringAfter("//", "")
        if (afterScheme.isEmpty() || afterScheme == url) {
            // No "//" at all - `javascript:`, `data:`, `about:blank`. No host to compare.
            if (!url.contains("://")) return ""
        }
        val authority = afterScheme
            .substringBefore('/')
            .substringBefore('?')
            .substringBefore('#')
            .substringAfterLast('@')
        return authority.substringBefore(':').lowercase()
    }
}