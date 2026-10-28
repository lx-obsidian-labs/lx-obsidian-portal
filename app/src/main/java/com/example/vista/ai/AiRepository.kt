package com.example.vista.ai

import com.example.vista.BuildConfig
import com.example.vista.data.local.PreferencesManager
import com.example.vista.domain.model.*
import com.example.vista.domain.repository.*
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

data class AssistantPlan(val tool: String = "help", val query: String = "", val type: String = "all")
data class AssistantReply(val text: String, val picks: List<AssistantPick> = emptyList(), val pendingTool: String? = null)

@Singleton
class AiRepository @Inject constructor(
    private val catalog: CatalogRepository, private val watchlist: WatchlistRepository,
    private val recommendations: TasteRecommendations, private val preferences: PreferencesManager,
    private val auth: FirebaseAuth
) {
    private val client = OkHttpClient.Builder().callTimeout(30, TimeUnit.SECONDS).build()
    private val gson = Gson()

    suspend fun answer(message: String, contextId: String? = null, contextType: String? = null, spoilers: Boolean = false): AssistantReply {
        check(preferences.aiFeaturesEnabled.first() && preferences.aiChatEnabled.first()) { "AI Assistant is disabled in preferences." }
        val plan = plan(message)
        check(plan.tool in setOf("help", "search", "recommend", "cast", "plot", "save", "remove")) { "Unsupported assistant action." }
        check(plan.type in setOf("movie", "series", "all") && plan.query.length <= 200) { "Invalid assistant action." }
        if (plan.tool == "help") return AssistantReply("Ask for a title, a genre, a short movie, cast details, or recommendations. I only use catalog metadata. Opening a title does not guarantee playback availability.")
        if (plan.tool == "recommend") return AssistantReply("Suggestions from verified catalog metadata:", recommendations.suggestions(plan.query))
        val query = plan.query.trim()
        val movies = if (plan.type != "series" && query.isNotBlank()) catalog.searchMovies(query).take(8) else emptyList()
        val series = if (plan.type != "movie" && query.isNotBlank()) catalog.searchSeries(query).take(8) else emptyList()
        val allowAdult = preferences.settings.first().showAdultContent
        val picks = movies.filter { allowAdult || !it.isAdult }.map { AssistantPick(it.toMediaItem(), "TMDB catalog • Availability not verified") } +
            series.filter { allowAdult || it.ageRating !in setOf("TV-MA", "18", "NC-17") }.map { AssistantPick(it.toMediaItem(), "Catalog metadata • Availability not verified") }
        if (plan.tool in setOf("cast", "plot")) {
            val selected = if (contextId != null) null else picks.singleOrNull()
            val id = contextId ?: selected?.item?.id
            val type = contextType ?: selected?.item?.let { if (it is MediaItem.SeriesItem) "series" else "movie" }
            if (id == null) return AssistantReply("Choose the exact title first, then ask from its companion screen.", picks)
            if (plan.tool == "plot" && !spoilers) return AssistantReply("Plot summaries may reveal events after your current position. Enable spoilers explicitly to read the verified synopsis. Cast questions are available without spoilers.")
            val metadata = if (type == "series") {
                val item = catalog.getSeriesDetails(id) ?: error("Verified metadata is unavailable.")
                if (plan.tool == "cast") item.cast.joinToString("\n") { "${it.name}${it.character?.let { role -> " as $role" } ?: ""}" } else item.overview
            } else {
                val item = catalog.getMovieDetails(id) ?: error("Verified metadata is unavailable.")
                check(allowAdult || !item.isAdult) { "This title is restricted by content settings." }
                if (plan.tool == "cast") item.cast.joinToString("\n") { "${it.name}${it.character?.let { role -> " as $role" } ?: ""}" } else item.overview
            }
            return AssistantReply((metadata.ifBlank { "No verified information is available." }) + "\n\nSource: catalog title metadata. Scene-level context is unavailable.")
        }
        return AssistantReply(if (picks.isEmpty()) "No verified catalog matches. Try the title and year." else if (plan.tool in setOf("save", "remove")) "Choose a title and confirm the watch list action." else "Catalog matches. Availability is checked by the playback flow, not inferred by AI.", picks, plan.tool.takeIf { it == "save" || it == "remove" })
    }

    suspend fun confirmWatchlist(item: MediaItem, remove: Boolean) {
        check(auth.currentUser != null || preferences.isGuest.first()) { "Sign in or start a guest session first." }
        if (remove) watchlist.removeFromWatchlist(item) else watchlist.addToWatchlist(item)
    }

    private suspend fun plan(message: String): AssistantPlan {
        require(message.isNotBlank() && message.length <= 1000) { "Use between 1 and 1000 characters." }
        // Explicit local commands remain usable without a cloud service or an AI account.
        val lower = message.lowercase()
        if (lower.startsWith("recommend") || lower.startsWith("suggest")) return AssistantPlan("recommend", message.take(200))
        if (lower.startsWith("cast")) return AssistantPlan("cast", message.substringAfter(' ', "").take(200))
        if (lower.startsWith("plot")) return AssistantPlan("plot", message.substringAfter(' ', "").take(200))
        if (lower.startsWith("search ")) return AssistantPlan("search", message.substringAfter(' ').take(200))
        val base = BuildConfig.VISTA_AI_BASE_URL.trimEnd('/')
        if (base.isBlank()) return AssistantPlan("search", message.take(200))
        require(base.startsWith("https://")) { "Assistant requires HTTPS." }
        val user = auth.currentUser ?: return AssistantPlan("search", message.take(200))
        val token = user.getIdToken(false).await().token ?: error("Sign in again to use AI.")
        return withContext(Dispatchers.IO) {
            val request = Request.Builder().url("$base/api/assistant").header("Authorization", "Bearer $token")
                .post(gson.toJson(mapOf("message" to message)).toRequestBody("application/json".toMediaType())).build()
            client.newCall(request).execute().use {
                check(it.isSuccessful) { "Assistant is temporarily unavailable (${it.code}). Try ‘search title’ or ‘recommend comedy’." }
                gson.fromJson(it.body?.string() ?: error("Empty assistant response"), AssistantPlan::class.java)
            }
        }
    }
}
