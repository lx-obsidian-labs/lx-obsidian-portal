package com.example.vista.ai

import com.example.vista.BuildConfig
import com.example.vista.data.local.PreferencesManager
import com.example.vista.data.remote.firebase.FirebaseManager
import com.example.vista.navigation.Screen
import com.example.vista.domain.model.*
import com.example.vista.domain.model.MediaType
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class AssistantPlan(val tool: String = "help", val query: String = "", val type: String = "all", val selection: Int? = null)
data class AssistantSource(val title: String, val url: String, val publisher: String, val publishedAt: String, val id: String = url)
data class AssistantReply(val text: String, val picks: List<AssistantPick> = emptyList(), val pendingTool: String? = null, val destination: String? = null, val destinationLabel: String? = null,
    val sources: List<AssistantSource> = emptyList(), val followups: List<String> = emptyList(), val newsFeed: String = "global", val retrievedAt: String? = null)

@Singleton
class AiRepository @Inject constructor(
    private val catalog: CatalogRepository, private val watchlist: WatchlistRepository,
    private val recommendations: TasteRecommendations, private val preferences: PreferencesManager,
    private val auth: FirebaseAuth, private val firebase: FirebaseManager, private val tastes: TasteStore,
    private val historyRepository: HistoryRepository
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(18, TimeUnit.SECONDS)
        .callTimeout(20, TimeUnit.SECONDS)
        .build()
    private val gson = Gson()

    suspend fun answer(message: String, contextId: String? = null, contextType: String? = null, spoilers: Boolean = false, history: List<ChatMessage> = emptyList(), sourcePage: String = "AI assistant", previousPicks: List<AssistantPick> = emptyList(), newsSourceIds: List<String> = emptyList(), newsFeed: String = "global", onProgress: (String) -> Unit = {}): AssistantReply = withTimeout(45_000) {
        onProgress("Understanding your request")
        check(preferences.aiFeaturesEnabled.first() && preferences.aiChatEnabled.first()) { "AI Assistant is disabled in preferences." }
        require(message.isNotBlank() && message.length <= 1000) { "Use between 1 and 1000 characters." }
        val conversation = history.takeLast(20).map { it.copy(content = it.content.take(4000)) }
        val viewer = withTimeoutOrNull(2_000) { firebase.getViewerProfile() }
        val taste = tastes.read()
        val profile = gson.toJson(mapOf(
            "name" to (viewer?.alias?.takeIf { it.isNotBlank() } ?: auth.currentUser?.displayName ?: "Viewer"),
            "favoriteGenres" to if (taste.enabled && viewer?.personalizationEnabled != false) taste.genres else emptySet<String>(),
            "personalizationEnabled" to (taste.enabled && viewer?.personalizationEnabled != false),
            "currentPage" to sourcePage,
            "companionTitleId" to contextId,
            "companionType" to contextType,
            "recentResults" to previousPicks.take(8).mapIndexed { index, pick -> mapOf("position" to index + 1, "title" to pick.item.title,
                "type" to if (pick.item is MediaItem.SeriesItem) "series" else "movie") }
        )).take(2000)
        val newsRequest = newsSourceIds.isNotEmpty() || AgentActionPolicy.isNewsRequest(message, conversation)
        val plan = if (newsRequest) AssistantPlan("help") else plan(message, conversation, profile)
        onProgress(if (newsRequest) "Retrieving entertainment reporting" else "Checking your library and catalog")
        val result = if (newsRequest) AssistantReply("Current entertainment news requires the cloud assistant. Sign in and check your connection to retrieve sourced headlines.")
            else catalogAnswer(plan, contextId, contextType, spoilers, previousPicks, message)
        if (BuildConfig.VISTA_AI_BASE_URL.isBlank() || auth.currentUser == null) return@withTimeout result
        // Only verified catalog data is passed as grounding; the provider cannot run actions.
        val context = buildString {
            appendLine(result.text)
            result.picks.take(8).forEach { appendLine("Catalog title: ${it.item.title}. Metadata: ${gson.toJson(it.item)}. ${it.reason}") }
            appendLine("Pending watchlist action: ${result.pendingTool ?: "none"}; not executed.")
            result.destinationLabel?.let { appendLine("Offered page action: $it. The user must tap to open it.") }
        }.take(16000)
        onProgress("Preparing your answer and actions")
        val response = try { withTimeoutOrNull(15_000) { cloud(mapOf("mode" to "reply", "message" to message, "history" to conversation, "context" to context, "spoilers" to spoilers, "profile" to profile, "agentic" to true,
            "newsSourceIds" to newsSourceIds.take(5), "newsFeed" to newsFeed)) } }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { null }
        if (response == null) return@withTimeout result.copy(text = result.text + "\n\nCloud conversation is unavailable right now; these catalog-backed results and actions are still usable.")
        val text = response.get("text")?.asString?.takeIf { it.isNotBlank() } ?: error("The assistant returned an empty response. Try again.")
        AiReplyDecoder.enrich(result.copy(text = text), response)
    }

    private suspend fun catalogAnswer(plan: AssistantPlan, contextId: String?, contextType: String?, spoilers: Boolean,
        previousPicks: List<AssistantPick>, message: String): AssistantReply {
        check(plan.tool in AgentActionPolicy.tools) { "Unsupported assistant action." }
        check(plan.type in setOf("movie", "series", "all") && plan.query.length <= 200) { "Invalid assistant action." }
        if (plan.tool == "navigate") {
            val route = when (plan.query) {
                "home" -> Screen.Home.route
                "discover" -> Screen.Discover.route
                "search" -> Screen.Search.route
                "watchlist" -> Screen.Library.route
                "downloads" -> Screen.Downloads.route
                "profile" -> Screen.Profile.route
                "history" -> Screen.WatchHistory.route
                "stats" -> Screen.Stats.route
                "settings" -> Screen.Settings.createRoute("MAIN")
                "playback", "audio_subtitles", "notifications", "data_storage", "appearance", "privacy_history", "about", "ai_voice", "download_settings" -> Screen.Settings.createRoute(if (plan.query == "download_settings") "DOWNLOADS" else plan.query.uppercase())
                else -> error("Unknown app page.")
            }
            return AssistantReply("The requested page is available in Vista.", destination = route, destinationLabel = "Open ${plan.query.replace('_', ' ')}")
        }
        if (plan.tool == "help") return AssistantReply("I can find and compare titles, recommend something for your mood, show your watch list, prepare a download, and help you resume watching. Try ‘find a short comedy’, ‘show my watchlist’ or ‘download the second one’ after a search.")
        if (plan.tool == "watchlist") return AssistantReply("Here are your saved titles on this device.", watchlist.getWatchlist().first().take(8).map { AssistantPick(it, "Saved in your watch list") },
            destination = Screen.Library.route, destinationLabel = "Open Watch List")
        if (plan.tool == "continue") {
            val next = historyRepository.getContinueWatching().first().firstOrNull()
                ?: return AssistantReply("There is no resumable progress yet. Choose a title to begin.", destination = Screen.Library.route, destinationLabel = "Open Library")
            return AssistantReply("Ready to resume ${next.title}${if (next.isSeries) " · S${next.seasonNumber ?: 1} E${next.episodeNumber ?: 1}" else ""}. Tap Resume to open the player.",
                destination = Screen.Player.createRoute(next.id, next.title, if (next.isSeries) MediaType.SERIES else MediaType.MOVIE,
                    next.seasonNumber, next.episodeNumber, next.posterUrl), destinationLabel = "Resume ${next.title}")
        }
        if (plan.tool == "recommend") return AssistantReply("Suggestions from verified catalog metadata:", recommendations.suggestions(plan.query))
        if (plan.tool == "compare") {
            val queries = plan.query.split(Regex("\\s+(?:vs\\.?|versus|and)\\s+", RegexOption.IGNORE_CASE), limit = 2)
            if (queries.size != 2) return AssistantReply("Name two titles to compare, for example ‘compare Inception vs Interstellar’.")
            val matches = coroutineScope { queries.map { title -> async {
                catalog.searchMovies(title).firstOrNull { it.title.equals(title, ignoreCase = true) }
            } }.map { it.await() } }
            if (matches.any { it == null }) return AssistantReply("I couldn't identify both exact movie titles. Include the title and year, or search for each one first.")
            val details = coroutineScope { matches.filterNotNull().map { movie -> async { catalog.getMovieDetails(movie.id) ?: movie } }.map { it.await() } }
            val viewerSettings = preferences.settings.first()
            check(details.all { ContentAccessPolicy.allows(viewerSettings, it.ageRating, it.isAdult) }) { "One of those titles is restricted by your age or content settings." }
            return AssistantReply(details.joinToString("\n\n") { movie ->
                "${movie.title} (${movie.releaseYear ?: "year unknown"})\n" +
                    "Runtime: ${movie.runtimeMinutes?.let { "$it minutes" } ?: "unknown"}. Genres: ${movie.genres.joinToString { it.name }.ifBlank { "unknown" }}. " +
                    "Catalog rating: ${movie.rating ?: "unavailable"}."
            } + "\n\nComparison uses catalog metadata, not a guarantee of playback or personal preference.",
                details.map { AssistantPick(it.toMediaItem(), "Verified comparison title") })
        }
        val query = plan.query.trim()
        val referenced = when {
            plan.selection != null -> previousPicks.getOrNull(plan.selection - 1)?.let(::listOf).orEmpty()
            AgentActionPolicy.referencesPrevious(message) -> if (previousPicks.size == 1) previousPicks else previousPicks
            else -> emptyList()
        }
        if (plan.selection != null && referenced.isEmpty()) return AssistantReply("That numbered result isn't available in this conversation. Search again or choose one of the current titles.")
        val (movies, series) = if (referenced.isNotEmpty()) emptyList<Movie>() to emptyList<Series>() else coroutineScope {
            val movies = async { if (plan.type != "series" && query.isNotBlank()) catalog.searchMovies(query).take(8) else emptyList() }
            val series = async { if (plan.type != "movie" && query.isNotBlank()) catalog.searchSeries(query).take(8) else emptyList() }
            movies.await() to series.await()
        }
        val viewerSettings = preferences.settings.first()
        var picks = referenced.ifEmpty { movies.filter { ContentAccessPolicy.allows(viewerSettings, it.ageRating, it.isAdult) }.map { AssistantPick(it.toMediaItem(), "TMDB catalog • Availability not verified") } +
            series.filter { ContentAccessPolicy.allows(viewerSettings, it.ageRating, false) }.map { AssistantPick(it.toMediaItem(), "Catalog metadata • Availability not verified") }
        }.filter { ContentAccessPolicy.allows(viewerSettings, it.item) }.distinctBy { it.item.watchlistKey() }.take(8)
        if (picks.isEmpty() && query.isBlank() && contextId != null) {
            val contextItem = if (contextType == "series") catalog.getSeriesDetails(contextId)?.toMediaItem() else catalog.getMovieDetails(contextId)?.toMediaItem()
            picks = contextItem?.takeIf { ContentAccessPolicy.allows(viewerSettings, it) }?.let { listOf(AssistantPick(it, "Current companion title")) }.orEmpty()
        }
        if (plan.tool == "download") {
            val exact = picks.singleOrNull()?.item
            return AssistantReply(if (exact == null) "Choose the exact title below to review download sources and quality. No download has started."
                else "Ready to review ${exact.title}. Download sources, quality and network requirements are checked in Downloads; nothing has started yet.", picks,
                pendingTool = "download", destination = exact?.let { if (it is MediaItem.SeriesItem) Screen.SeriesDetails.createRoute(it.id)
                    else Screen.Downloads.createRoute(it.id, it.title, MediaType.MOVIE) },
                destinationLabel = exact?.let { if (it is MediaItem.SeriesItem) "Choose an episode" else "Review download" })
        }
        if (plan.tool == "details") {
            val exact = picks.singleOrNull()?.item
            return AssistantReply(if (exact == null) "Choose the exact title to open its details." else "I found ${exact.title}. Open its verified details below.", picks,
                destination = exact?.let { if (it is MediaItem.SeriesItem) Screen.SeriesDetails.createRoute(it.id) else Screen.MovieDetails.createRoute(it.id) },
                destinationLabel = exact?.let { "Open ${it.title}" })
        }
        if (plan.tool in setOf("cast", "plot")) {
            val selected = picks.singleOrNull()
            val useContext = query.isBlank() && referenced.isEmpty()
            val id = if (useContext) contextId else selected?.item?.id
            val type = if (useContext) contextType else selected?.item?.let { if (it is MediaItem.SeriesItem) "series" else "movie" }
            if (id == null) return AssistantReply("Choose the exact title first, then ask from its companion screen.", picks)
            if (plan.tool == "plot" && !spoilers) return AssistantReply("Plot summaries may reveal events after your current position. Enable spoilers explicitly to read the verified synopsis. Cast questions are available without spoilers.")
            val metadata = if (type == "series") {
                val item = catalog.getSeriesDetails(id) ?: error("Verified metadata is unavailable.")
                check(ContentAccessPolicy.allows(viewerSettings, item.ageRating, false)) { "This title is restricted by your age or content settings." }
                if (plan.tool == "cast") item.cast.joinToString("\n") { "${it.name}${it.character?.let { role -> " as $role" } ?: ""}" } else item.overview
            } else {
                val item = catalog.getMovieDetails(id) ?: error("Verified metadata is unavailable.")
                check(ContentAccessPolicy.allows(viewerSettings, item.ageRating, item.isAdult)) { "This title is restricted by your age or content settings." }
                if (plan.tool == "cast") item.cast.joinToString("\n") { "${it.name}${it.character?.let { role -> " as $role" } ?: ""}" } else item.overview
            }
            return AssistantReply((metadata.ifBlank { "No verified information is available." }) + "\n\nSource: catalog title metadata. Scene-level context is unavailable.")
        }
        return AssistantReply(if (picks.isEmpty()) "No verified catalog matches. Try the title and year." else if (plan.tool in setOf("save", "remove")) "Choose a title and confirm the watch list action." else "Catalog matches. Availability is checked by the playback flow, not inferred by AI.", picks, plan.tool.takeIf { it == "save" || it == "remove" }, destination = Screen.Search.createRoute(query), destinationLabel = "Open Search")
    }

    suspend fun confirmWatchlist(item: MediaItem, remove: Boolean) {
        check(auth.currentUser != null || preferences.isGuest.first()) { "Sign in or start a guest session first." }
        if (remove) watchlist.removeFromWatchlist(item) else watchlist.addToWatchlist(item)
    }

    private suspend fun plan(message: String, history: List<ChatMessage>, profile: String): AssistantPlan {
        val local = AgentActionPolicy.localPlan(message)
        if (local.selection != null && local.tool in setOf("save", "remove", "download", "details", "cast", "plot")) return local
        if (message.lowercase().trim() in setOf("open downloads", "my downloads", "show downloads")) return AssistantPlan("navigate", "downloads")
        if (local.tool in setOf("watchlist", "continue")) return local
        if (BuildConfig.VISTA_AI_BASE_URL.isNotBlank() && auth.currentUser != null) {
            try {
                val response = withTimeoutOrNull(10_000) { cloud(mapOf("mode" to "plan", "message" to message, "history" to history, "profile" to profile, "agentic" to true)) }
                if (response != null) return gson.fromJson(response, AssistantPlan::class.java)
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { /* Explicit local commands still work during a cloud outage. */ }
        }
        return local
    }

    private suspend fun cloud(payload: Map<String, Any>): com.google.gson.JsonObject {
        val base = BuildConfig.VISTA_AI_BASE_URL.trimEnd('/')
        require(base.startsWith("https://")) { "Assistant requires HTTPS." }
        val user = auth.currentUser ?: error("Sign in to use the cloud AI assistant. Catalog search remains available from Search.")
        val token = user.getIdToken(false).await().token ?: error("Sign in again to use AI.")
        val request = Request.Builder().url("$base/api/assistant").header("Authorization", "Bearer $token")
            .post(gson.toJson(payload).toRequestBody("application/json".toMediaType())).build()
        val body = suspendCancellableCoroutine<String> { continuation ->
            val call = client.newCall(request)
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) { if (continuation.isActive) continuation.resumeWithException(e) }
                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        try {
                            check(response.isSuccessful) {
                                when (response.code) {
                                    401 -> "Your AI session expired. Please sign in again."
                                    429 -> "You’re asking quickly. Please wait a minute and try again."
                                    else -> "Cloud AI is temporarily unavailable (${response.code}). Please try again."
                                }
                            }
                            val text = response.body?.string() ?: error("Empty assistant response")
                            check(text.length <= 65_536) { "Assistant response is too large." }
                            if (continuation.isActive) continuation.resume(text)
                        } catch (e: Exception) { if (continuation.isActive) continuation.resumeWithException(e) }
                    }
                }
            })
        }
        return gson.fromJson(body, com.google.gson.JsonObject::class.java)
    }
}
