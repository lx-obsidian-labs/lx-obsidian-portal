package com.example.vista.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vista.domain.model.*
import com.example.vista.domain.repository.SearchRepository
import com.example.vista.domain.repository.WatchlistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val isSearching: Boolean = false,
    val isLoadingMore: Boolean = false,
    val movies: List<Movie> = emptyList(),
    val series: List<Series> = emptyList(),
    val people: List<Person> = emptyList(),
    val totalCount: Int = 0,
    val currentPage: Int = 1,
    val hasMore: Boolean = true,
    val recentSearches: List<String> = emptyList(),
    val trendingSearches: List<String> = emptyList(),
    val selectedTab: SearchType = SearchType.ALL,
    val error: String? = null
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchRepository: SearchRepository,
    private val watchlistRepository: WatchlistRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _watchlistKeys = MutableStateFlow<Set<String>>(emptySet())
    val watchlistKeys: StateFlow<Set<String>> = _watchlistKeys.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadRecentSearches()
        loadTrendingSearches()
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

    private fun loadRecentSearches() {
        viewModelScope.launch {
            searchRepository.getRecentSearches().collect { searches ->
                _uiState.update { it.copy(recentSearches = searches) }
            }
        }
    }

    private fun loadTrendingSearches() {
        viewModelScope.launch {
            searchRepository.getTrendingSearches().collect { searches ->
                _uiState.update { it.copy(trendingSearches = searches) }
            }
        }
    }

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
        searchJob?.cancel()
        if (query.isBlank()) {
            _uiState.update {
                it.copy(
                    movies = emptyList(),
                    series = emptyList(),
                    people = emptyList(),
                    isSearching = false,
                    currentPage = 1,
                    hasMore = true
                )
            }
            return
        }
        searchJob = viewModelScope.launch {
            delay(300)
            performSearch(query, page = 1)
        }
    }

    fun search(query: String) {
        _uiState.update { it.copy(query = query) }
        viewModelScope.launch {
            searchRepository.addRecentSearch(query)
            performSearch(query, page = 1)
        }
    }

    private suspend fun performSearch(query: String, page: Int) {
        val isInitial = page == 1
        if (isInitial) {
            _uiState.update { it.copy(
                isSearching = true,
                isLoadingMore = false,
                error = null,
                movies = emptyList(),
                series = emptyList(),
                people = emptyList(),
                currentPage = 1,
                hasMore = true
            ) }
        } else {
            _uiState.update { it.copy(isLoadingMore = true) }
        }
        try {
            val filters = SearchFilters(
                query = query,
                type = _uiState.value.selectedTab,
                page = page
            )
            val results = searchRepository.search(query, filters)
            // People are only requested on the All and People tabs.
            val includesPeople = _uiState.value.selectedTab == SearchType.ALL ||
                _uiState.value.selectedTab == SearchType.PEOPLE
            val hasResults = results.movies.isNotEmpty() || results.series.isNotEmpty() ||
                (includesPeople && results.people.isNotEmpty())
            _uiState.update {
                if (isInitial) {
                    it.copy(
                        isSearching = false,
                        movies = results.movies.distinctBy { m -> m.id },
                        series = results.series.distinctBy { s -> s.id },
                        people = results.people.distinctBy { p -> p.id },
                        totalCount = results.totalCount,
                        currentPage = page,
                        hasMore = hasResults
                    )
                } else {
                    it.copy(
                        isLoadingMore = false,
                        movies = (it.movies + results.movies).distinctBy { m -> m.id },
                        series = (it.series + results.series).distinctBy { s -> s.id },
                        people = (it.people + results.people).distinctBy { p -> p.id },
                        totalCount = it.totalCount + results.totalCount,
                        currentPage = page,
                        hasMore = hasResults
                    )
                }
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    isSearching = false,
                    isLoadingMore = false,
                    error = e.message ?: "Search failed"
                )
            }
        }
    }

    fun loadMore() {
        if (_uiState.value.isLoadingMore || !_uiState.value.hasMore || _uiState.value.query.isBlank()) return
        viewModelScope.launch {
            performSearch(_uiState.value.query, page = _uiState.value.currentPage + 1)
        }
    }

    fun selectTab(tab: SearchType) {
        _uiState.update { it.copy(selectedTab = tab) }
        if (_uiState.value.query.isNotBlank()) {
            viewModelScope.launch {
                performSearch(_uiState.value.query, page = 1)
            }
        }
    }

    fun removeRecentSearch(query: String) {
        viewModelScope.launch {
            searchRepository.removeRecentSearch(query)
        }
    }

    fun clearRecentSearches() {
        viewModelScope.launch {
            searchRepository.clearRecentSearches()
        }
    }

    fun clearQuery() {
        _uiState.update {
            it.copy(
                query = "",
                movies = emptyList(),
                series = emptyList(),
                people = emptyList(),
                isSearching = false,
                currentPage = 1,
                hasMore = true
            )
        }
    }
}
