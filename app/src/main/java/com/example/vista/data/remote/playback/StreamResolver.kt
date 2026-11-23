package com.example.vista.data.remote.playback

import android.util.Log
import com.example.vista.domain.model.DrmInfo
import com.example.vista.domain.model.StreamSource
import com.example.vista.domain.model.StreamType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Turns an embed page URL into a real, directly playable media URL.
 *
 * ## Why this exists
 *
 * [VidsrcPlaybackRepository] can only produce iframe embed pages. Those pages carry no media URL
 * the app can read: the manifest lives behind a script the WebView evaluates. That left every
 * source stamped [StreamType.OTHER], which forces the WebView branch in the player, and meant no
 * title ever reported a duration, so no title could be resumed or downloaded.
 *
 * This class fetches the embed HTML and reads the player configuration out of it, returning a
 * [StreamSource] carrying the real manifest plus the headers needed to fetch it.
 *
 * ## Reliability contract
 *
 * Resolution is best-effort by design. Providers rotate their page structure without notice, so
 * every extraction step can fail. Callers must treat a `null` result as "carry on with the embed
 * page" and must never let a failure here break playback - the embed still works.
 *
 * ## Known limits
 *
 *  - Signed URLs expire. [StreamSource.expiresAt] carries the expiry so a download started too
 *    late can fail fast instead of producing a truncated file.
 *  - DRM'd titles cannot be resolved. Widevine keys are not extractable and will not be attempted.
 *  - No provider-specific handling. Extraction is pattern-based, so a provider that changes its
 *    player simply stops resolving until its patterns are added.
 */
@Singleton
class StreamResolver @Inject constructor(
    @Named("shared") private val client: OkHttpClient
) {
    // Reuse DNS, sockets and dispatcher; never persist expiring provider pages in metadata cache.
    private val resolverClient = client.newBuilder()
        .cache(null)
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .callTimeout(10, TimeUnit.SECONDS)
        .build()

    /**
     * Resolves [embedUrl] to the single best playable source, or returns `null` when the page cannot
     * be resolved. A `null` result is an expected outcome, not an error.
     *
     * Equivalent to the first entry of [resolveAll], kept because playback only needs one source and
     * the player lands on the first entry anyway.
     */
    suspend fun resolve(embedUrl: String, providerName: String): StreamSource? =
        resolveAll(embedUrl, providerName).firstOrNull()

    /**
     * Resolves [embedUrl] to every playable source the page advertises, most preferred first.
     *
     * Returning all of them rather than the first match is what makes offline downloads possible.
     * These pages almost always carry an HLS manifest *and* a progressive file, and the manifest
     * wins the ordering below because it is the better source for streaming. A caller that only
     * needs one source therefore gets HLS - as before - while a caller that specifically needs a
     * file to save can still see the `.mp4` the same page offered.
     *
     * Ordering across sources follows [PATTERNS]: HLS, then DASH, then progressive last. Within a
     * single pattern, document order is kept.
     */
    suspend fun resolveAll(embedUrl: String, providerName: String): List<StreamSource> =
        withContext(Dispatchers.IO) {
            // Only https pages are fetched. The provider list is built from templates we control,
            // but refusing cleartext here keeps a future provider from silently reintroducing it.
            if (!embedUrl.startsWith("https://")) return@withContext emptyList()

            val html = fetchHtml(embedUrl) ?: return@withContext emptyList()
            val candidates = extractCandidates(html)
            if (candidates.isEmpty()) {
                Log.d(TAG, "No playable source found in $providerName embed")
                return@withContext emptyList()
            }

            val drm = detectDrm(html)

            candidates.mapNotNull { candidate ->
                val url = absolutiseMediaUrl(candidate.url, embedUrl) ?: return@mapNotNull null
                val type = detectType(url) ?: run {
                    Log.d(TAG, "Unrecognised manifest type in $providerName embed: $url")
                    return@mapNotNull null
                }

                StreamSource(
                    url = url,
                    type = type,
                    quality = candidate.quality,
                    bitrate = null,
                    name = providerName,
                    // Referer and UA are mandatory for most of these hosts. Without them the CDN
                    // returns 403 even for a URL we resolved correctly.
                    headers = buildMap {
                        put("Referer", originOf(embedUrl))
                        put("User-Agent", DESKTOP_UA)
                        put("Origin", originOf(embedUrl))
                    },
                    expiresAt = candidate.expiresAt,
                    drmInfo = drm
                )
            }
        }.also { sources ->
            if (sources.isNotEmpty()) {
                Log.i(TAG, "Resolved $providerName -> ${sources.joinToString { it.type.name }}")
            }
        }

    /**
     * Fetches with a reusable cache-free client. Parent cancellation closes the request socket.
     */
    private suspend fun fetchHtml(url: String): String? {
        return try {
            val call = resolverClient.newCall(
                Request.Builder()
                    .url(url)
                    .header("User-Agent", DESKTOP_UA)
                    .header("Accept", "text/html,application/xhtml+xml")
                    .build()
            )
            // Cancellation closes the real socket, including while the response body is read.
            suspendCancellableCoroutine { continuation ->
                continuation.invokeOnCancellation { call.cancel() }
                call.enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        continuation.resumeWithException(e)
                    }

                    override fun onResponse(call: Call, response: Response) {
                        response.use {
                            try {
                                val html = if (response.isSuccessful) response.body?.string() else null
                                continuation.resume(html)
                            } catch (e: IOException) {
                                continuation.resumeWithException(e)
                            }
                        }
                    }
                })
            }
        } catch (e: kotlinx.coroutines.CancellationException) { throw e
        } catch (e: Exception) {
            // Provider unreachable, TLS failure, malformed URL - all mean "no resolution", never
            // an error the user should see.
            Log.d(TAG, "Embed fetch failed for $url: ${e.javaClass.simpleName}")
            null
        }
    }

    private data class Candidate(
        val url: String,
        val quality: String? = null,
        val expiresAt: Long? = null
    )

    /**
     * Pulls every media URL out of the embed HTML.
     *
     * Ordered by how much the host is willing to tell us, so the returned list is HLS-first: an
     * explicit manifest in a `<source>` tag or `file:` entry is the most reliable signal and is
     * matched first; the mp4 pattern is the last resort. Every pattern is applied, not just the
     * first that hits, because a page carrying both a manifest and a progressive file is the normal
     * case rather than an odd one.
     */
    private fun extractCandidates(html: String): List<Candidate> {
        val found = LinkedHashMap<String, Candidate>()

        for (pattern in PATTERNS) {
            for (match in pattern.regex.findAll(html)) {
                val groups = match.groupValues
                val url = groups.getOrNull(pattern.urlGroup)?.takeIf { it.isNotBlank() } ?: continue
                val quality = groups.getOrNull(pattern.qualityGroup)?.takeIf { it.isNotBlank() }
                val expires = groups.getOrNull(pattern.expiresGroup)?.toLongOrNull()

                // A resolved URL still has to look like media. This filters out the tracking pixels,
                // ad scripts and API endpoints that also match a bare URL pattern.
                if (!looksLikeMedia(url)) continue

                // First pattern to claim a URL wins, so the earlier, better-labelled patterns set
                // the quality and expiry even when a later pattern rediscovers the same URL.
                found.putIfAbsent(url, Candidate(url, quality, expires))
            }
        }
        return found.values.toList()
    }

    private data class Pattern(
        val regex: Regex,
        val urlGroup: Int,
        val qualityGroup: Int = -1,
        val expiresGroup: Int = -1
    )

    /**
     * Classifies a resolved URL by container.
     *
     * `.mkv` and `.mov` are included because they are progressive files just like `.mp4`, and the
     * download selector accepts them. They were previously accepted by [looksLikeMedia] but
     * rejected here, which silently discarded them and left the selector's `.mkv` support
     * unreachable.
     *
     * `.ts` is deliberately excluded: it is the HLS *segment* extension, so a page that links its
     * segments would otherwise classify a raw transport stream as a downloadable movie file.
     */
    private fun detectType(url: String): StreamType? {
        val path = url.substringBefore('?').lowercase()
        return when {
            path.endsWith(".m3u8") -> StreamType.HLS
            path.endsWith(".mpd") -> StreamType.DASH
            path.endsWith(".mp4") || path.endsWith(".m4v") || path.endsWith(".webm") ||
                    path.endsWith(".mkv") || path.endsWith(".mov") -> StreamType.MP4
            else -> null
        }
    }

    private fun detectDrm(html: String): DrmInfo? {
        val licenseUrl = LICENSE_URL_REGEX.find(html)?.groupValues?.getOrNull(1)
        val scheme = DRM_SCHEME_REGEX.find(html)?.groupValues?.getOrNull(1)
        if (licenseUrl.isNullOrBlank() && scheme.isNullOrBlank()) return null
        return DrmInfo(
            licenseUrl = licenseUrl,
            drmScheme = scheme,
            drmHeaders = mapOf("Content-Type" to "application/octet-stream")
        )
    }

    /**
     * Rejects a resolved URL that is plainly not media. Without this the mp4 fallback happily
     * returns an ad or a tracking URL, and the player then fails with a far less obvious error.
     */
    private fun looksLikeMedia(url: String): Boolean {
        if (EmbedAdPolicy.blocks(url)) return false
        val lower = url.lowercase()
        val host = lower.substringAfter("//", "").substringBefore('/')
        if (BLOCKED_HOSTS.any { host.contains(it) }) return false
        val path = lower.substringBefore('?')
        return MEDIA_EXTENSIONS.any { path.endsWith(it) } || MEDIA_HINTS.any { lower.contains(it) }
    }

    companion object {
        private const val TAG = "StreamResolver"

        /**
         * Providers serve different markup to desktop agents, and some only expose a manifest to
         * one. The player WebView already uses this string for the same reason.
         */
        const val DESKTOP_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/116.0.0.0 Safari/537.36"

        /**
         * `.ts` is absent on purpose: it is the HLS segment extension, and matching it would let a
         * page that merely links its segments present a raw transport stream as a video file.
         */
        private val MEDIA_EXTENSIONS =
            listOf(".m3u8", ".mpd", ".mp4", ".m4v", ".webm", ".mkv", ".mov")

        private val MEDIA_HINTS = listOf("/hls", "/master", "manifest")

        private val BLOCKED_HOSTS = listOf(
            "googletagmanager", "doubleclick", "googleadservices", "adnxs", "popads",
            "exoclick", "adsterra", "juicyads", "trafficjunky", "countdown", "propellerads"
        )

        private val LICENSE_URL_REGEX = Regex(
            """["'](https?://[^"']*?license[^"']*?\.ism[^"']*)["']""",
            RegexOption.IGNORE_CASE
        )
        private val DRM_SCHEME_REGEX = Regex(
            """keySystem\s*[=:]\s*["']?(com\.widevine\.alpha|com\.microsoft\.playready)""",
            RegexOption.IGNORE_CASE
        )

        /**
         * Extraction patterns, most reliable first.
         *
         * Each carries a url group (1) plus optional quality (2) and expiry (3) groups. The HLS
         * entries match the `file:`/`sources` JSON these players embed, which is the form that
         * also carries a quality label. The mp4 entries are the fallback and are deliberately
         * last so a stray ad mp4 cannot win over a real manifest.
         */
        private val PATTERNS = listOf(
            // {"file":"https://.../master.m3u8?exp=1699999999","label":"1080p","type":"hls"}
            Pattern(
                regex = Regex(
                    """["']file["']\s*:\s*["'](https?://[^"']+\.m3u8[^"']*)["']""" +
                        """(?:[^}]*?["']label["']\s*:\s*["']([^"']{1,20})["'])?""" +
                        """(?:[^}]*?["'](?:exp|expires|expire)[^"']*["']\s*:\s*["']?(\d{10,13})["']?)?""",
                    RegexOption.IGNORE_CASE
                ),
                urlGroup = 1, qualityGroup = 2, expiresGroup = 3
            ),
            // source: "https://.../playlist.m3u8"  /  hls: "https://..."
            Pattern(
                regex = Regex(
                    """(?:src|source|sources|hls|stream|file|playlist|url)["']?\s*[:=]\s*["'](https?://[^"'\s]+\.m3u8[^"'\s]*)["']""",
                    RegexOption.IGNORE_CASE
                ),
                urlGroup = 1
            ),
            // Bare quoted hls manifest
            Pattern(
                regex = Regex("""["'](https?://[^"'\s]+\.m3u8[^"'\s]*)["']""", RegexOption.IGNORE_CASE),
                urlGroup = 1
            ),
            // DASH manifest
            Pattern(
                regex = Regex("""["'](https?://[^"'\s]+\.mpd[^"'\s]*)["']""", RegexOption.IGNORE_CASE),
                urlGroup = 1
            ),
            // mp4, last resort. Quality is not advertised in this form so it stays null.
            Pattern(
                regex = Regex("""["'](https?://[^"'\s]+\.(?:mp4|m4v|webm)(?:\?[^"'\s]*)?)["']""", RegexOption.IGNORE_CASE),
                urlGroup = 1
            )
        )
    }
}
