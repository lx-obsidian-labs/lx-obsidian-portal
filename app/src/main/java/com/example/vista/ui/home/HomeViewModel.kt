package com.example.vista.ui.home

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vista.data.local.PreferencesManager
import com.example.vista.domain.model.*
import com.example.vista.domain.repository.CatalogRepository
import com.example.vista.domain.repository.HistoryRepository
import com.example.vista.domain.repository.WatchlistRepository
import com.example.vista.motd.MovieOfTheDay
import com.example.vista.motd.MovieOfTheDayPicker
import com.example.vista.util.AnnouncementsStore
import com.example.vista.util.NetworkUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val sections: List<HomeSection> = emptyList(),
    val sectionPages: Map<String, Int> = emptyMap(),
    val sectionHasMore: Map<String, Boolean> = emptyMap(),
    val sectionLoadMoreLoading: Map<String, Boolean> = emptyMap(),
    val featuredMovies: List<MediaItem> = emptyList(),
    val becauseYouWatched: List<MediaItem> = emptyList(),
    val continueWatching: List<MediaItem.ContinueWatchingItem> = emptyList(),
    val error: String? = null,
    val isOffline: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val catalogRepository: CatalogRepository,
    private val historyRepository: HistoryRepository,
    private val watchlistRepository: com.example.vista.domain.repository.WatchlistRepository,
    private val preferencesManager: PreferencesManager,
    private val announcementsStore: AnnouncementsStore,
    private val playbackHistoryDao: com.example.vista.data.local.dao.PlaybackHistoryDao,
    private val vistaAnalytics: com.example.vista.data.analytics.VistaAnalytics,
    private val taste: com.example.vista.ai.TasteStore,
    private val recommendations: com.example.vista.ai.TasteRecommendations,
    application: Application
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _movieOfTheDay = MutableStateFlow<MovieOfTheDay?>(null)
    val movieOfTheDay: StateFlow<MovieOfTheDay?> = _movieOfTheDay.asStateFlow()

    /**
     * Works out today's suggestion, once per app start.
     *
     * Skipped entirely when the feature is off or the card has already been seen today. The stored
     * day is only advanced when the card is actually surfaced, so a viewer who dismisses it does not
     * lose the suggestion until tomorrow.
     */
    private fun resolveMovieOfTheDay() {
        viewModelScope.launch {
            if (!preferencesManager.movieOfTheDayEnabled.first()) return@launch
            if (preferencesManager.movieOfTheDayNotInterested.first()) return@launch

            val today = LocalDate.now().toEpochDay()
            if (preferencesManager.movieOfTheDayLastCardDay.first() == today) return@launch
            if (preferencesManager.movieOfTheDayLastDismissedDay.first() == today) return@launch

            val watched = historyRepository.getWatchHistory().first()
                .filter { it.mediaType == MediaType.MOVIE }
                .map { it.mediaId }
                .toSet()

            val pick = MovieOfTheDayPicker.pick(LocalDate.now(), watched)
            _movieOfTheDay.value = pick
            vistaAnalytics.track(
                "motd_card_shown",
                properties = mapOf(
                    "label" to (pick.eventLabel ?: ""),
                    "seasonal" to pick.isSeasonal.toString()
                )
            )
        }
    }

    /** Called when the card is dismissed or acted on, so it does not reappear the same day. */
    fun onMovieOfTheDaySeen() {
        viewModelScope.launch {
            markMovieOfTheDaySeen()
        }
    }

    /** Called when the viewer taps through to the suggested film. */
    fun onMovieOfTheDayOpened() {
        vistaAnalytics.track("motd_opened")
        viewModelScope.launch {
            markMovieOfTheDaySeen()
        }
    }

    /** Called when the viewer explicitly dismisses the card. */
    fun onMovieOfTheDayDismissed(notInterested: Boolean = false) {
        vistaAnalytics.track(
            "motd_dismissed",
            properties = mapOf("notInterested" to notInterested.toString())
        )
        viewModelScope.launch {
            val today = LocalDate.now().toEpochDay()
            if (notInterested) {
                preferencesManager.setMovieOfTheDayNotInterested(true)
            } else {
                preferencesManager.markMovieOfTheDayDismissedDay(today)
            }
            preferencesManager.markMovieOfTheDayCardShown(today)
            _movieOfTheDay.value = null
        }
    }

    private suspend fun markMovieOfTheDaySeen() {
        val today = LocalDate.now().toEpochDay()
        preferencesManager.markMovieOfTheDayCardShown(today)
        _movieOfTheDay.value = null
    }

    private val _watchlistKeys = MutableStateFlow<Set<String>>(emptySet())
    val watchlistKeys: StateFlow<Set<String>> = _watchlistKeys.asStateFlow()

    /** Name shown in the time-based greeting ("Good evening, {name}"). */
    private val _displayName = MutableStateFlow("")
    val displayName: StateFlow<String> = _displayName.asStateFlow()

    /** Announcements inbox for the notification bell. Newest first. */
    private val _announcements = MutableStateFlow<List<AnnouncementsStore.Announcement>>(emptyList())
    val announcements: StateFlow<List<AnnouncementsStore.Announcement>> = _announcements.asStateFlow()

    /** True while there is anything the viewer has not seen yet (badge dot on the bell). */
    private val _bellUnread = MutableStateFlow(false)
    val bellUnread: StateFlow<Boolean> = _bellUnread.asStateFlow()

    private val _premiumAcked = MutableStateFlow(false)
    val premiumAcked: StateFlow<Boolean> = _premiumAcked.asStateFlow()

    // The seed for the "Because You Watched" row. Keyed so a new history entry triggers exactly one
    // recommendation fetch (never per-recomposition or per-observe).
    private val becauseYouWatchedSource = MutableStateFlow<String?>(null)

    init {
        observeNetworkConnectivity(application)
        observeIdentity()
        observeBellStates()
        loadHomeContent()
        observeContinueWatching()
        observeWatchlist()
        observeBecauseYouWatched()
        resolveMovieOfTheDay()
    }

    private fun observeIdentity() {
        viewModelScope.launch {
            preferencesManager.displayName.collect { name ->
                _displayName.value = name
            }
        }
    }

    private fun observeBellStates() {
        viewModelScope.launch {
            combine(
                preferencesManager.premiumAcked,
                preferencesManager.bellLastSeenAt
            ) { acked, lastSeen -> acked to lastSeen }.collect { (acked, lastSeen) ->
                _premiumAcked.value = acked
                refreshBellUnread(acked, lastSeen)
            }
        }
    }

    private fun refreshBellUnread(acked: Boolean, lastSeen: Long) {
        val hasAnnouncement = announcementsStore.latestTimestamp() > lastSeen
        val premiumPending = !acked
        _bellUnread.value = hasAnnouncement || premiumPending
        _announcements.value = announcementsStore.all()
    }

    /** Ran when the bell sheet opens: clears the badge and records "all seen". */
    fun markBellSeen() {
        viewModelScope.launch {
            preferencesManager.markBellSeen()
            // Premium stays flagged until explicitly acknowledged; badge only drops after ack.
        }
    }

    fun acknowledgePremium() {
        viewModelScope.launch {
            preferencesManager.markPremiumAcked()
        }
    }

    fun refreshAnnouncements() {
        _announcements.value = announcementsStore.all()
        viewModelScope.launch {
            combine(
                preferencesManager.premiumAcked,
                preferencesManager.bellLastSeenAt
            ) { acked, lastSeen -> acked to lastSeen }.first().let { (acked, lastSeen) ->
                refreshBellUnread(acked, lastSeen)
            }
        }
    }

    private fun observeNetworkConnectivity(application: Application) {
        viewModelScope.launch {
            NetworkUtils.observeConnectivity(application).collect { isConnected ->
                val wasOffline = _uiState.value.isOffline
                _uiState.update { it.copy(isOffline = !isConnected) }
                // Auto-retry when connection is restored
                if (isConnected && wasOffline && _uiState.value.error != null) {
                    loadHomeContent()
                }
            }
        }
    }

    private fun observeWatchlist() {
        viewModelScope.launch {
            watchlistRepository.getWatchlist().collect { items ->
                _watchlistKeys.value = items.map { it.watchlistKey() }.toSet()
            }
        }
    }

    fun toggleWatchlist(item: MediaItem) {
        viewModelScope.launch {
            if (_watchlistKeys.value.contains(item.watchlistKey())) {
                watchlistRepository.removeFromWatchlist(item)
            } else {
                watchlistRepository.addToWatchlist(item)
            }
        }
    }

    /**
     * Loads the home content. Extracts featured movies from sections to avoid
     * a duplicate trending-movies API call.
     */
    private fun loadHomeContent() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val sections = catalogRepository.getHomeSections()
                // Reuse the featured section from getHomeSections instead of a separate API call
                val featured = sections
                    .firstOrNull { it.id == CatalogSection.FEATURED_ID }
                    ?.items
                    ?.ifEmpty { null }
                    ?: sections
                        .firstOrNull { it.id == CatalogSection.TRENDING_MOVIES.id }
                        ?.items
                        ?.take(5)
                val featuredMovies = featured ?: emptyList()
                val initialPages = sections.associate { it.id to 1 }
                val initialHasMore = sections.associate { it.id to (it.items.size >= 10) }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        sections = sections,
                        featuredMovies = featuredMovies,
                        sectionPages = initialPages,
                        sectionHasMore = initialHasMore,
                        error = if (sections.isEmpty()) "No content available. Please check your connection." else null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load content"
                    )
                }
            }
        }
    }

    fun loadMoreSection(sectionId: String) {
        val currentPages = _uiState.value.sectionPages
        val currentLoading = _uiState.value.sectionLoadMoreLoading
        if (currentLoading[sectionId] == true || _uiState.value.sectionHasMore[sectionId] != true) return

        val currentPage = currentPages[sectionId] ?: 1
        val nextPage = currentPage + 1

        _uiState.update { it.copy(sectionLoadMoreLoading = it.sectionLoadMoreLoading + (sectionId to true)) }

        viewModelScope.launch {
            try {
                val newItems = loadSectionPage(sectionId, nextPage)
                _uiState.update {
                    val existingSections = it.sections.toMutableList()
                    val sectionIndex = existingSections.indexOfFirst { s -> s.id == sectionId }
                    if (sectionIndex >= 0) {
                        val section = existingSections[sectionIndex]
                        existingSections[sectionIndex] = section.copy(
                            items = (section.items + newItems).distinctBy { m -> m.id }
                        )
                    }
                    it.copy(
                        sections = existingSections,
                        sectionPages = it.sectionPages + (sectionId to nextPage),
                        sectionHasMore = it.sectionHasMore + (sectionId to newItems.isNotEmpty()),
                        sectionLoadMoreLoading = it.sectionLoadMoreLoading - sectionId
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(sectionLoadMoreLoading = it.sectionLoadMoreLoading - sectionId) }
            }
        }
    }

    private suspend fun loadSectionPage(sectionId: String, page: Int): List<MediaItem> {
        // Resolved from the shared section registry. This used to be a private copy of the id map,
        // which is why "load more" silently returned nothing on most rows.
        val section = CatalogSection.fromId(sectionId) ?: return emptyList()
        return catalogRepository.getSection(section, page)
    }

    private fun observeContinueWatching() {
        viewModelScope.launch {
            historyRepository.getContinueWatching().collect { items ->
                _uiState.update { it.copy(continueWatching = items) }
            }
        }
    }

    private fun observeBecauseYouWatched() {
        viewModelScope.launch {
            combine(taste.changes, historyRepository.getWatchHistory()) { prefs, history ->
                "$prefs:${history.firstOrNull()?.mediaId}"
            }.distinctUntilChanged().collectLatest {
                try {
                    val picks = recommendations.suggestions()
                    _uiState.update { it.copy(becauseYouWatched = picks.map { pick -> pick.item }) }
                } catch (e: kotlinx.coroutines.CancellationException) { throw e }
                catch (_: Exception) { _uiState.update { it.copy(becauseYouWatched = emptyList()) } }
            }
        }
    }

    private suspend fun loadBecauseYouWatched(latest: PlaybackProgress) {
        val items = try {
            if (latest.mediaType == MediaType.MOVIE) {
                catalogRepository.getRecommendedMovies(latest.mediaId).map { it.toMediaItem() }
            } else {
                catalogRepository.getRecommendedSeries(latest.mediaId).map { it.toMediaItem() }
            }
        } catch (e: Exception) {
            emptyList()
        }
        _uiState.update {
            it.copy(becauseYouWatched = items.filterNot { item -> item.watchlistKey() == latest.mediaId })
        }
    }

    fun retry() {
        loadHomeContent()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, error = null) }
            try {
                val sections = catalogRepository.getHomeSections()
                val featured = sections
                    .firstOrNull { it.id == CatalogSection.FEATURED_ID }
                    ?.items
                    ?.ifEmpty { null }
                    ?: sections
                        .firstOrNull { it.id == CatalogSection.TRENDING_MOVIES.id }
                        ?.items
                        ?.take(5)
                val featuredMovies = featured ?: emptyList()
                val initialPages = sections.associate { it.id to 1 }
                val initialHasMore = sections.associate { it.id to (it.items.size >= 10) }

                _uiState.update {
                    it.copy(
                        isRefreshing = false,
                        sections = sections,
                        featuredMovies = featuredMovies,
                        sectionPages = initialPages,
                        sectionHasMore = initialHasMore
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isRefreshing = false,
                        error = e.message ?: "Failed to refresh content"
                    )
                }
            }
        }
    }
}
