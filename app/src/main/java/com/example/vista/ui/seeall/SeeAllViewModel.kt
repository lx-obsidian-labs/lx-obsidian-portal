package com.example.vista.ui.seeall

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

data class SeeAllUiState(
    val isLoading: Boolean = true,
    val title: String = "",
    val items: List<MediaItem> = emptyList(),
    val currentPage: Int = 1,
    val hasMore: Boolean = true,
    val isLoadingMore: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class SeeAllViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val catalogRepository: CatalogRepository,
    private val watchlistRepository: WatchlistRepository
) : ViewModel() {

    private val sectionId: String = savedStateHandle["sectionId"] ?: ""
    private val title: String = savedStateHandle["title"] ?: ""

    private val _uiState = MutableStateFlow(SeeAllUiState(title = title))
    val uiState: StateFlow<SeeAllUiState> = _uiState.asStateFlow()

    private val _watchlistKeys = MutableStateFlow<Set<String>>(emptySet())
    val watchlistKeys: StateFlow<Set<String>> = _watchlistKeys.asStateFlow()

    init {
        loadContent(page = 1, isInitial = true)
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
                // Resolved from the shared section registry instead of a private id map, so a row
                // renamed on the home screen can never open an empty "See All" page here.
                val section = CatalogSection.fromId(sectionId)
                val content = section?.let { catalogRepository.getSection(it, page) }.orEmpty()
                if (section == null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isLoadingMore = false,
                            hasMore = false,
                            error = "This section is no longer available."
                        )
                    }
                    return@launch
                }
                _uiState.update {
                    if (isInitial) {
                        it.copy(
                            isLoading = false,
                            items = content.distinctBy { item -> item.id },
                            currentPage = page,
                            hasMore = content.isNotEmpty()
                        )
                    } else {
                        val combinedItems = (it.items + content).distinctBy { item -> item.id }
                        it.copy(
                            isLoadingMore = false,
                            items = combinedItems,
                            currentPage = page,
                            hasMore = content.isNotEmpty()
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        error = if (isInitial) e.message ?: "Failed to load content" else it.error
                    )
                }
            }
        }
    }

    fun loadMore() {
        if (_uiState.value.isLoadingMore || !_uiState.value.hasMore) return
        loadContent(page = _uiState.value.currentPage + 1, isInitial = false)
    }

    fun retry() {
        loadContent(page = 1, isInitial = true)
    }
}
