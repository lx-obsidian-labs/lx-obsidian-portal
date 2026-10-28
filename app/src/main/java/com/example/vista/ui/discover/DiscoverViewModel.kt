package com.example.vista.ui.discover

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vista.domain.model.*
import com.example.vista.domain.repository.CatalogRepository
import com.example.vista.domain.repository.WatchlistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DiscoverUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val genres: List<Genre> = emptyList(),
    val items: List<MediaItem> = emptyList(),
    val selectedTab: DiscoverTab = DiscoverTab.MOVIES,
    val selectedGenre: Genre? = null,
    val currentPage: Int = 1,
    val hasMore: Boolean = true,
    val isLoadingMore: Boolean = false,
    val error: String? = null
)

enum class DiscoverTab(val displayName: String, val genreId: String? = null) {
    MOVIES("Movies"),
    TV_SHOWS("TV Shows"),
    ANIME("Anime", "16"),
    ACTION("Action", "28"),
    COMEDY("Comedy", "35"),
    HORROR("Horror", "27"),
    SCIFI("Sci-Fi", "878"),
    DRAMA("Drama", "18"),
    THRILLER("Thriller", "53"),
    ROMANCE("Romance", "10749"),
    DOCUMENTARIES("Documentaries", "99");

    companion object {
        fun fromName(name: String?): DiscoverTab? =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) }
    }
}

@HiltViewModel
class DiscoverViewModel @Inject constructor(
    private val catalogRepository: CatalogRepository,
    private val watchlistRepository: WatchlistRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiscoverUiState())
    val uiState: StateFlow<DiscoverUiState> = _uiState.asStateFlow()

    private val _watchlistKeys = MutableStateFlow<Set<String>>(emptySet())
    val watchlistKeys: StateFlow<Set<String>> = _watchlistKeys.asStateFlow()

    init {
        // Home category chips route here with a pre-selected tab, e.g. "discover?tab=ACTION".
        val requestedTab = DiscoverTab.fromName(savedStateHandle.get<String>("tab"))
        if (requestedTab != null) {
            _uiState.update { it.copy(selectedTab = requestedTab) }
        }
        loadGenres()
        loadContent()
        observeWatchlist()
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

    private fun loadGenres() {
        viewModelScope.launch {
            try {
                val genres = catalogRepository.getGenres()
                _uiState.update { it.copy(genres = genres) }
            } catch (e: Exception) {
                // Genres are optional
            }
        }
    }

    private fun loadContent(page: Int = 1, isInitial: Boolean = true) {
        viewModelScope.launch {
            if (isInitial) {
                _uiState.update { it.copy(
                    isLoading = true,
                    error = null,
                    items = emptyList(),
                    currentPage = 1,
                    hasMore = true
                ) }
            } else {
                _uiState.update { it.copy(isLoadingMore = true) }
            }
            try {
                val selectedGenre = _uiState.value.selectedGenre
                val tab = _uiState.value.selectedTab
                val genreId = selectedGenre?.id ?: tab.genreId
                val items = when {
                    tab == DiscoverTab.MOVIES -> {
                        if (genreId != null) {
                            catalogRepository.getMoviesByGenre(genreId, page).map { it.toMediaItem() }
                        } else {
                            catalogRepository.getPopularMovies(page).map { it.toMediaItem() }
                        }
                    }
                    tab == DiscoverTab.TV_SHOWS -> {
                        if (genreId != null) {
                            catalogRepository.getSeriesByGenre(genreId, page).map { it.toMediaItem() }
                        } else {
                            catalogRepository.getPopularSeries(page).map { it.toMediaItem() }
                        }
                    }
                    // Genre-specific tabs (Anime, Action, Comedy, etc.) use the genreId
                    genreId != null -> {
                        catalogRepository.getMoviesByGenre(genreId, page).map { it.toMediaItem() }
                    }
                    else -> emptyList()
                }

                _uiState.update {
                    if (isInitial) {
                        it.copy(
                            isLoading = false,
                            items = items.distinctBy { item -> item.id },
                            currentPage = page,
                            hasMore = items.isNotEmpty()
                        )
                    } else {
                        val combinedItems = (it.items + items).distinctBy { item -> item.id }
                        it.copy(
                            isLoadingMore = false,
                            items = combinedItems,
                            currentPage = page,
                            hasMore = items.isNotEmpty()
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        error = e.message ?: "Failed to load content"
                    )
                }
            }
        }
    }

    fun loadMore() {
        if (_uiState.value.isLoadingMore || !_uiState.value.hasMore || _uiState.value.isLoading) return
        loadContent(page = _uiState.value.currentPage + 1, isInitial = false)
    }

    fun selectTab(tab: DiscoverTab) {
        if (_uiState.value.selectedTab == tab) return
        _uiState.update { it.copy(selectedTab = tab, selectedGenre = null) }
        loadContent(page = 1, isInitial = true)
    }

    fun selectGenre(genre: Genre?) {
        if (_uiState.value.selectedGenre == genre) return
        _uiState.update { it.copy(selectedGenre = genre) }
        loadContent(page = 1, isInitial = true)
    }

    fun retry() {
        loadContent(page = 1, isInitial = true)
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, error = null) }
            try {
                loadGenres()
                val selectedGenre = _uiState.value.selectedGenre
                val tab = _uiState.value.selectedTab
                val genreId = selectedGenre?.id ?: tab.genreId
                val items = when {
                    tab == DiscoverTab.MOVIES -> {
                        if (genreId != null) {
                            catalogRepository.getMoviesByGenre(genreId, 1).map { it.toMediaItem() }
                        } else {
                            catalogRepository.getPopularMovies(1).map { it.toMediaItem() }
                        }
                    }
                    tab == DiscoverTab.TV_SHOWS -> {
                        if (genreId != null) {
                            catalogRepository.getSeriesByGenre(genreId, 1).map { it.toMediaItem() }
                        } else {
                            catalogRepository.getPopularSeries(1).map { it.toMediaItem() }
                        }
                    }
                    genreId != null -> {
                        catalogRepository.getMoviesByGenre(genreId, 1).map { it.toMediaItem() }
                    }
                    else -> emptyList()
                }
                _uiState.update { it.copy(
                    isRefreshing = false,
                    items = items.distinctBy { item -> item.id },
                    currentPage = 1,
                    hasMore = items.isNotEmpty()
                ) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isRefreshing = false, error = e.message ?: "Failed to refresh content")
                }
            }
        }
    }
}
