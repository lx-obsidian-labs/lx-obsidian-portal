package com.example.vista.ai

import com.example.vista.data.local.PreferencesManager
import com.example.vista.domain.model.*
import com.example.vista.domain.repository.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

data class AssistantPick(val item: MediaItem, val reason: String)

@Singleton
class TasteRecommendations @Inject constructor(
    private val catalog: CatalogRepository, private val history: HistoryRepository,
    private val taste: TasteStore, private val preferences: PreferencesManager
) {
    suspend fun suggestions(hint: String = ""): List<AssistantPick> {
        val prefs = taste.read()
        val settings = preferences.settings.first()
        val watched = if (prefs.enabled) history.getWatchHistory().first().filter { it.lastPlayedTimestamp > prefs.resetAt } else emptyList()
        val seen = watched.map { "${it.mediaType.name.lowercase()}:${it.mediaId}" }.toSet()
        val inferred = mutableMapOf<String, Int>()
        for (entry in watched.take(3)) {
            try {
                val genres = if (entry.mediaType == MediaType.MOVIE) catalog.getMovieDetails(entry.mediaId)?.genres else catalog.getSeriesDetails(entry.mediaId)?.genres
                genres.orEmpty().forEach { inferred[it.name] = (inferred[it.name] ?: 0) + if (entry.isCompleted) 2 else 1 }
            } catch (e: CancellationException) { throw e } catch (_: Exception) { }
        }
        val allGenres = catalog.getGenres()
        val moodGenres = when {
            hint.contains("relax", true) || hint.contains("funny", true) -> setOf("Comedy")
            hint.contains("thrill", true) || hint.contains("excited", true) -> setOf("Thriller", "Action")
            hint.contains("romantic", true) -> setOf("Romance")
            hint.contains("mind", true) -> setOf("Science Fiction", "Mystery")
            else -> emptySet()
        }
        val requested = allGenres.filter { hint.contains(it.name, true) }.map { it.name }.toSet() + moodGenres
        val explicit = if (prefs.enabled) prefs.genres else emptySet()
        val target = allGenres.firstOrNull { it.name in requested } ?: allGenres.firstOrNull { it.name in explicit }
        val movies = if (target != null) catalog.getMoviesByGenre(target.id) else catalog.getPopularMovies()
        return movies.filter { settings.showAdultContent || !it.isAdult }.mapNotNull { movie ->
            val item = movie.toMediaItem()
            if (!TasteRanker.eligible(item.watchlistKey(), seen, if (prefs.enabled) prefs.disliked else emptySet())) return@mapNotNull null
            if (hint.contains("short", true) && (movie.runtimeMinutes == null || movie.runtimeMinutes > 100)) return@mapNotNull null
            val genres = movie.genres.map { it.name }.toSet()
            val score = TasteRanker.score(genres, explicit + requested, inferred)
            val matched = genres.intersect(explicit + requested).firstOrNull()
            val reason = when {
                matched != null -> "Matches your $matched preference"
                genres.any { (inferred[it] ?: 0) > 0 } -> "Similar genres to your recent viewing"
                else -> "Popular catalog discovery"
            }
            score to AssistantPick(item, "$reason • Playback availability not verified")
        }.sortedByDescending { it.first }.take(10).map { it.second }
    }
}
