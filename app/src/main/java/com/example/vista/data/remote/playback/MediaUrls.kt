package com.example.vista.data.remote.playback

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * Resolves a possibly relative media URL against the page or playlist it was found in.
 *
 * Shared by [StreamResolver] and anything else that walks the manifest hierarchy -
 * embed page to manifest, manifest to variant - and the rules about what counts as a usable
 * absolute URL must not drift between them.
 *
 * Directory-relative references (`1080/index.m3u8`) are resolved properly rather than rejected.
 * They are the norm in HLS: the specification requires variant URIs to be resolved against the
 * playlist they appear in, so a resolver that only understood absolute and root-relative URLs
 * silently dropped nearly every real manifest.
 *
 * `null` means "not a usable URL": a reference with no usable base, or cleartext `http://`. Only
 * https is ever returned, so a provider cannot reintroduce cleartext by emitting a relative or
 * http reference.
 */
internal fun absolutiseMediaUrl(url: String, base: String): String? {
    val trimmed = url.trim().trim('"', '\'')
    val baseUrl = base.toHttpUrlOrNull() ?: return null

    val resolved = when {
        trimmed.startsWith("https://") -> trimmed
        trimmed.startsWith("http://") -> return null
        // Everything else - protocol-relative, root-relative and directory-relative - goes through
        // the same RFC 3986 resolver OkHttp already implements.
        else -> baseUrl.resolve(trimmed)?.toString() ?: return null
    }
    return resolved.takeIf { it.startsWith("https://") }
}

/** The scheme-and-host of an embed page, used as the Referer and Origin a host expects. */
internal fun originOf(url: String): String {
    val parsed = url.toHttpUrlOrNull() ?: return url
    return "${parsed.scheme}://${parsed.host}"
}