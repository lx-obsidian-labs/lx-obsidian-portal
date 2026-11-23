package com.example.vista.data.remote.playback

import com.example.vista.BuildConfig
import com.example.vista.data.remote.tmdb.TmdbApiService
import com.example.vista.domain.model.*
import com.example.vista.domain.repository.PlaybackRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Builds embed sources for the streaming providers we support.
 *
 * Two rules keep this correct, and both were broken before:
 *
 *  1. The media type is never guessed. TMDB stores movies and TV in two independent id namespaces
 *     that overlap numerically, so a TV id asked of a movie endpoint returns a different title.
 *     Playback therefore receives [MediaType] explicitly, and passes it to every provider.
 *
 *  2. A provider only receives an id it actually understands. Some providers take TMDB ids, some
 *     take IMDb ids. Previously every provider was handed whatever id the caller had, which is why
 *     the fallback list produced 404s and wrong films.
 */
@Singleton
class VidsrcPlaybackRepository @Inject constructor(
    private val tmdbApiService: TmdbApiService,
    private val tvmazeApiService: com.example.vista.data.remote.tvmaze.TvmazeApiService,
    private val streamResolver: StreamResolver
) : PlaybackRepository {
    private val probePermits = Semaphore(2)

    /** The identifiers we managed to resolve for a title, out of the namespaces providers accept. */
    private data class MediaRef(
        val tmdbId: String? = null,
        val imdbId: String? = null
    ) {
        val hasTmdb: Boolean get() = !tmdbId.isNullOrBlank()
        val hasImdb: Boolean get() = !imdbId.isNullOrBlank()
        val bestId: String? get() = tmdbId ?: imdbId
    }

    private data class Provider(
        val name: String,
        val build: (MediaRef, MediaType, Int, Int) -> String?
    )

    /**
     * Ordered best-first. `build` returns null when the provider cannot serve the ids we hold, in
     * which case it is skipped entirely instead of being given a URL that will 404.
     */
    private val providers: List<Provider> = listOf(
        // VidSrc accepts either namespace (IMDb ids must carry the `tt` prefix).
        Provider("VidSrc") { ref, type, season, episode ->
            val id = ref.bestId ?: return@Provider null
            if (type == MediaType.MOVIE) {
                "${EmbedOrigins.VIDSRUC}/embed/movie/$id"
            } else {
                "${EmbedOrigins.VIDSRUC}/embed/tv/$id/$season/$episode"
            }
        },
        // TMDB-id providers.
        Provider("VidSrc PRO") { ref, type, season, episode ->
            val id = ref.tmdbId ?: return@Provider null
            if (type == MediaType.MOVIE) {
                "${EmbedOrigins.VIDSRCPRO}/embed/movie/$id"
            } else {
                "${EmbedOrigins.VIDSRCPRO}/embed/tv/$id/$season/$episode"
            }
        },
        Provider("2Embed") { ref, type, season, episode ->
            val id = ref.tmdbId ?: return@Provider null
            if (type == MediaType.MOVIE) {
                "${EmbedOrigins.TWOE}/embed/$id"
            } else {
                "${EmbedOrigins.TWOE}/embedtv/$id?s=$season&e=$episode"
            }
        },
        Provider("VidSrc.me") { ref, type, season, episode ->
            val id = ref.imdbId ?: ref.tmdbId ?: return@Provider null
            if (type == MediaType.MOVIE) {
                "${EmbedOrigins.VIDSRCME}/embed/movie?$id"
            } else {
                "${EmbedOrigins.VIDSRCME}/embed/tv?$id/$season/$episode"
            }
        },
        // MultiEmbed. Previously listed twice, as "SuperEmbed" and "MultiEmbed", both pointing at
        // multiembed.mov: the "SuperEmbed" entry ignored the `tmdb` flag and so handed a TMDB id to
        // a host that treats a bare id as an IMDb id, while the "MultiEmbed" entry ignored season
        // and episode and so fetched series premieres regardless of what was asked for. One entry
        // now carries both behaviours, which also saves a redundant page fetch per title.
        Provider("MultiEmbed") { ref, type, season, episode ->
            val id = when {
                ref.hasTmdb -> "${ref.tmdbId}&tmdb=1"
                ref.hasImdb -> ref.imdbId
                else -> return@Provider null
            }
            if (type == MediaType.MOVIE) {
                "${EmbedOrigins.MULTIEMBED}/?video_id=$id"
            } else {
                "${EmbedOrigins.MULTIEMBED}/?video_id=$id&s=$season&e=$episode"
            }
        }
    )

    override suspend fun getStreamSources(
        mediaId: String,
        mediaType: MediaType,
        season: Int?,
        episode: Int?,
        titleHint: String?
    ): List<StreamSource> {
        val ref = withTimeoutOrNull(6_000) { resolveMediaRef(mediaId, mediaType, titleHint) }
            ?: when {
                mediaId.toIntOrNull() != null -> MediaRef(tmdbId = mediaId)
                mediaId.startsWith("tt") -> MediaRef(imdbId = mediaId)
                else -> return emptyList()
            }
        val seasonNumber = season ?: 1
        val episodeNumber = episode ?: 1

        val embeds = providers.mapNotNull { provider ->
            val url = provider.build(ref, mediaType, seasonNumber, episodeNumber) ?: return@mapNotNull null
            provider to url
        }

        // Bound fan-out on mobile data. Each probe's deadline includes permit wait, so a slow
        // provider cannot hold all the others (or the foreground screen) indefinitely.
        val resolved = coroutineScope {
            embeds.map { (provider, url) -> async {
                withTimeoutOrNull(12_000) {
                    probePermits.withPermit { streamResolver.resolveAll(url, provider.name) }
                }.orEmpty()
            } }
                .awaitAll()
                .flatten()
        }

        // No HLS variant probing here. It existed only to find a progressive rendition for the
        // download worker, which no longer exists; leaving it would add a network round trip per
        // HLS source on every title for no consumer.
        // Keep an explicitly labelled embedded option even when extraction succeeds: a CDN can
        // reject a direct request while the provider's own player still works with its cookies.
        val fallbacks = embeds
            .distinctBy { (_, url) -> url }
            .map { (provider, url) ->
                StreamSource(
                    url = url,
                    type = StreamType.OTHER,
                    quality = "Auto",
                    bitrate = null,
                    name = "${provider.name} (embedded)"
                )
            }

        // Playback wants the manifest first: it is the better source for streaming, and it is what
        // the player has always landed on. Unresolved embeds follow as WebView fallbacks.
        // Keep the authenticated embed last, outside extraction probes and their HTTP logging.
        val backup = BackupMovieSource.create(ref.tmdbId, mediaType, BuildConfig.BACKUP_MOVIES_API_KEY)
        return resolved + fallbacks + listOfNotNull(backup)
    }

    /**
     * Normalises whatever id the catalogue handed us into the namespaces providers understand.
     * Accepts a numeric TMDB id, an IMDb id (`tt…`) or a slug from the local catalogue.
     */
    private suspend fun resolveMediaRef(mediaId: String, mediaType: MediaType, titleHint: String? = null): MediaRef {
        val id = mediaId.trim()

        // Handle TVMaze specific IDs by looking up their externals (IMDb ID)
        if (id.startsWith("tvmaze_")) {
            val tvmazeId = id.removePrefix("tvmaze_").toIntOrNull()
            if (tvmazeId != null) {
                try {
                    val show = tvmazeApiService.getShow(tvmazeId)
                    val imdbId = show.externals?.imdb
                    if (imdbId != null) {
                        // Recurse to resolve the IMDb ID to TMDB ID as well
                        return resolveMediaRef(imdbId, mediaType, titleHint)
                    }
                } catch (e: CancellationException) { throw e }
                catch (_: Exception) {}
            }
        }

        // Already an IMDb id: also try to get the TMDB id so the TMDB-only providers stay usable.
        if (id.startsWith("tt")) {
            val tmdbId = try {
                val found = tmdbApiService.findByExternalId(id)
                if (mediaType == MediaType.MOVIE) {
                    found.movieResults.firstOrNull()?.id
                } else {
                    found.tvResults.firstOrNull()?.id
                }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) {
                null
            }
            return MediaRef(tmdbId = tmdbId?.toString(), imdbId = id)
        }

        // A numeric id is a TMDB id in the matching namespace.
        if (id.toIntOrNull() != null) {
            val imdbId = try {
                if (mediaType == MediaType.MOVIE) {
                    tmdbApiService.getMovieDetails(id.toInt()).imdbId
                } else {
                    tmdbApiService.getTvShowDetails(id.toInt()).externalIds?.imdbId
                }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) {
                null
            }
            return MediaRef(tmdbId = id, imdbId = imdbId)
        }

        // Slug from the local catalogue or a title hint — look the title up in TMDB.
        val query = titleHint ?: id.replace('-', ' ')
        var tmdbId = try {
            if (mediaType == MediaType.MOVIE) {
                tmdbApiService.searchMovies(query, 1).results?.firstOrNull()?.id
            } else {
                tmdbApiService.searchTvShows(query, 1).results?.firstOrNull()?.id
            }
        } catch (e: CancellationException) { throw e }
        catch (_: Exception) {
            null
        }

        // If TMDB search failed for a series, try TVMaze search to find an IMDb ID
        var imdbId: String? = null
        if (tmdbId == null && mediaType == MediaType.SERIES) {
            try {
                val tvmazeResults = tvmazeApiService.searchShows(query)
                val bestMatch = tvmazeResults.find { it.show?.name?.equals(query, ignoreCase = true) == true }
                    ?: tvmazeResults.firstOrNull()
                imdbId = bestMatch?.show?.externals?.imdb
                
                // If we found an IMDb ID, try to get the TMDB ID for it
                if (imdbId != null) {
                    tmdbId = try {
                        tmdbApiService.findByExternalId(imdbId).tvResults.firstOrNull()?.id
                    } catch (e: CancellationException) { throw e }
                    catch (_: Exception) { null }
                }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) {}
        }

        return MediaRef(tmdbId = tmdbId?.toString(), imdbId = imdbId)
    }

    override suspend fun getSubtitles(mediaId: String): List<SubtitleTrack> = emptyList()

    override suspend fun getAudioTracks(mediaId: String): List<AudioTrack> = emptyList()
}
