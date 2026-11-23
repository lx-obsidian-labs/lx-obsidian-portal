package com.example.vista.ai

import com.example.vista.data.local.PreferencesManager
import com.example.vista.domain.model.*
import com.example.vista.domain.repository.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import javax.inject.Inject
import javax.inject.Singleton

data class AssistantPick(val item: MediaItem, val reason: String)

@Singleton
class TasteRecommendations @Inject constructor(
    private val catalog: CatalogRepository, private val history: HistoryRepository,
    private val taste: TasteStore, private val preferences: PreferencesManager,
    private val watchlist: WatchlistRepository
) {
    private val hydrated = java.util.concurrent.ConcurrentHashMap<String, Pair<Long, Movie>>()
    private suspend fun hydrate(movie: Movie): Movie {
        val now = System.currentTimeMillis()
        hydrated[movie.id]?.takeIf { now - it.first < 15 * 60_000 }?.let { return it.second }
        val result = withTimeoutOrNull(2_000) { catalog.getMovieDetails(movie.id) } ?: return movie
        if (hydrated.size >= 100) hydrated.clear()
        hydrated[movie.id] = now to result
        return result
    }
    suspend fun suggestions(hint: String = ""): List<AssistantPick> {
        val constraints = RecommendationConstraints.parse(hint)
        val prefs = taste.read()
        val settings = preferences.settings.first()
        val watched = if (prefs.enabled) history.getWatchHistory().first().filter {
            it.lastPlayedTimestamp > prefs.resetAt && (it.isCompleted || it.currentPosition >= 60_000)
        } else emptyList()
        val saved = if (prefs.enabled) watchlist.getWatchlist().first() else emptyList()
        val seen = watched.map { "${it.mediaType.name.lowercase()}:${it.mediaId}" }.toSet()
        val inferred = mutableMapOf<String, Int>()
        for (entry in watched.take(2)) {
            try {
                val genres = withTimeoutOrNull(2_500) {
                    if (entry.mediaType == MediaType.MOVIE) catalog.getMovieDetails(entry.mediaId)?.genres else catalog.getSeriesDetails(entry.mediaId)?.genres
                }
                genres.orEmpty().forEach { inferred[it.name] = (inferred[it.name] ?: 0) + if (entry.isCompleted) 2 else 1 }
            } catch (e: CancellationException) { throw e } catch (_: Exception) { }
        }
        val allGenres = withTimeoutOrNull(3_000) { catalog.getGenres() }.orEmpty()
        val moodGenres = when {
            hint.contains("relax", true) || hint.contains("funny", true) -> setOf("Comedy")
            hint.contains("thrill", true) || hint.contains("excited", true) -> setOf("Thriller", "Action")
            hint.contains("romantic", true) -> setOf("Romance")
            hint.contains("mind", true) -> setOf("Science Fiction", "Mystery")
            else -> emptySet()
        }
        val requested = allGenres.filter { hint.contains(it.name, true) }.map { it.name }.toSet() + moodGenres
        val explicit = if (prefs.enabled) prefs.genres else emptySet()
        val targets = allGenres.filter { it.name in requested + explicit }.take(2)
        // Bounded, independent candidate pools: a single genre or seed can no longer own the row.
        val pool = coroutineScope {
            val popular = async {
                try { withTimeoutOrNull(4_000) { catalog.getPopularMovies() }.orEmpty() }
                catch (e: CancellationException) { throw e }
                catch (_: Exception) { emptyList() }
            }
            val genres = targets.map { genre -> async { withTimeoutOrNull(4_000) { catalog.getMoviesByGenre(genre.id) }.orEmpty() } }
            val seed = watched.firstOrNull { it.mediaType == MediaType.MOVIE }?.mediaId
                ?: saved.filterIsInstance<MediaItem.MovieItem>().firstOrNull()?.id
            val related = async { if (seed == null) emptyList() else withTimeoutOrNull(4_000) { catalog.getRecommendedMovies(seed) }.orEmpty() }
            (related.await().take(10) + genres.awaitAll().flatten().take(12) + popular.await().take(12)).distinctBy { it.id }
        }
        val permits = Semaphore(4)
        val ranked = coroutineScope {
            pool.filter { TasteRanker.eligible("movie:${it.id}", seen, prefs.disliked) }
                .take(24).map { original -> async {
                    permits.withPermit {
                        // Search/list payloads often omit genres and runtime. Hydrate before ranking
                        // or applying a hard short-film filter; never invent unknown metadata.
                        val movie = if (original.genres.isEmpty() || (constraints.maxRuntimeMinutes != null && original.runtimeMinutes == null)) {
                            hydrate(original)
                        } else original
                        if (!ContentAccessPolicy.allows(settings, movie.ageRating, movie.isAdult) || !constraints.accepts(movie.runtimeMinutes, movie.releaseYear)) return@withPermit null
                        val names = movie.genres.map { it.name }.toSet()
                        if (requested.isNotEmpty() && names.intersect(requested).isEmpty()) return@withPermit null
                        val matched = names.intersect(explicit + requested).firstOrNull()
                        val isSaved = "movie:${movie.id}" in saved.map { it.watchlistKey() }.toSet()
                        val reason = when {
                            matched != null -> "Matches your $matched preference"
                            names.any { (inferred[it] ?: 0) > 0 } -> "Similar genres to your recent viewing"
                            isSaved -> "On your watchlist, ready to rediscover"
                            else -> "A discovery beyond your usual picks"
                        }
                        RankedPick(AssistantPick(movie.toMediaItem(), "$reason • Playback availability not verified"), names,
                            TasteRanker.score(names, explicit + requested, inferred) + if (isSaved) 3 else 0)
                    }
                } }.awaitAll().filterNotNull()
        }
        return diversify(ranked, 10)
    }

    private data class RankedPick(val pick: AssistantPick, val genres: Set<String>, val score: Int)
    private fun diversify(candidates: List<RankedPick>, limit: Int): List<AssistantPick> {
        val remaining = candidates.toMutableList()
        val counts = mutableMapOf<String, Int>()
        val result = mutableListOf<AssistantPick>()
        while (remaining.isNotEmpty() && result.size < limit) {
            val next = remaining.maxBy { it.score - it.genres.sumOf { genre -> (counts[genre] ?: 0) * 3 } }
            remaining.remove(next)
            next.genres.forEach { counts[it] = (counts[it] ?: 0) + 1 }
            result += next.pick
        }
        return result
    }
}
