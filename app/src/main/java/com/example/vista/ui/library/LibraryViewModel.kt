package com.example.vista.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vista.domain.model.MediaItem
import com.example.vista.domain.model.MediaType
import com.example.vista.domain.model.PlaybackProgress
import com.example.vista.domain.repository.HistoryRepository
import com.example.vista.domain.repository.WatchlistRepository
import com.example.vista.domain.model.watchlistKey
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Which slice of the library is on screen.
 *
 * Kept as a separate type rather than a bare string because [ALL] has to mean "union of the other
 * two" in a way that cannot be expressed as another enum constant, and because the empty-state copy
 * differs per filter.
 */
enum class LibraryFilter(val label: String) {
    ALL("All"),
    WATCHLIST("Watchlist"),
    WATCHED("Watched")
}

/**
 * How the merged list is ordered.
 *
 * RECENT_FIRST is the default because the overwhelmingly common question in a library is "what did
 * I leave halfway through", and that wants the same ordering as the underlying history.
 */
enum class LibrarySort(val label: String) {
    RECENT_FIRST("Recent"),
    TITLE("A-Z"),
    PROGRESS("Most watched")
}

/**
 * A row in the library, unified across the two underlying tables.
 *
 * The watchlist and watch history are stored separately and have no id in common beyond
 * (mediaId, mediaType), so they cannot be concatenated as one list of MediaItem without losing the
 * distinction the UI needs: only a watchlist entry can be un-saved, only a history entry carries a
 * resume position, and a title can be in both and must not appear twice.
 */
data class LibraryEntry(
    val id: String,
    val title: String,
    val posterUrl: String?,
    val rating: Double?,
    val mediaType: MediaType,
    val inWatchlist: Boolean,
    val hasProgress: Boolean,
    val isCompleted: Boolean = false,
    val progressFraction: Float = 0f,
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
    val lastPlayedTimestamp: Long = 0L
) {
    /** The list key, because a movie and a series can share a provider id. */
    val watchlistKey: String get() = "${mediaType.name}:$id"

    val isResumeable: Boolean get() = hasProgress && !isCompleted && progressFraction > 0f
}

/**
 * Union of the saved list and watch history, de-duplicated and filterable.
 *
 * Both sources are exposed as flows rather than fetched once, so removing a title or finishing an
 * episode updates the screen without a manual refresh - the previous behaviour on the saved screen
 * required leaving and re-entering.
 */
data class LibraryUiState(
    val isLoading: Boolean = true,
    val filter: LibraryFilter = LibraryFilter.ALL,
    val sort: LibrarySort = LibrarySort.RECENT_FIRST,
    val entries: List<LibraryEntry> = emptyList(),
    val watchlistCount: Int = 0,
    val watchedCount: Int = 0,
    val error: String? = null,
    /** Finished titles in the whole library, not just the filtered view. */
    val completedCount: Int = 0
) {
    /** True only once loading finished, so an empty filter result does not show the empty state. */
    val isEmpty: Boolean get() = !isLoading && entries.isEmpty()

    /** The action is only worth showing when there is something for it to do. */
    val canClearWatched: Boolean get() = completedCount > 0
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val watchlistRepository: WatchlistRepository,
    private val historyRepository: HistoryRepository
) : ViewModel() {

    private val filter = MutableStateFlow(LibraryFilter.ALL)
    private val sort = MutableStateFlow(LibrarySort.RECENT_FIRST)

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    /**
     * The unfiltered, unsorted merge of both tables.
     *
     * Held separately from the visible list so changing a filter or the sort is pure in-memory work.
     * Filtering off the already-filtered list - the obvious shortcut - silently loses rows: switching
     * Watched -> All could never bring the watchlist back.
     */
    private var merged: List<LibraryEntry> = emptyList()

    init {
        viewModelScope.launch {
            // combine, not two independent collects: the merge below needs both halves of the
            // library to build each row, and an independent pair would emit inconsistent states.
            combine(
                watchlistRepository.getWatchlist(),
                historyRepository.getWatchHistory()
            ) { saved, history -> merge(saved, history) }
                .catch { e ->
                    _uiState.update {
                        it.copy(isLoading = false, error = e.message ?: "Could not load your library")
                    }
                }
                .collect { result ->
                    merged = result
                    publish()
                }
        }
    }

    /** Recomputes the visible list and its counters from [merged] and the current filter/sort. */
    private fun publish() {
        _uiState.update { state ->
            state.copy(
                isLoading = false,
                entries = applySort(applyFilter(merged, filter.value), sort.value),
                watchlistCount = merged.count { it.inWatchlist },
                watchedCount = merged.count { it.hasProgress },
                completedCount = merged.count { it.isCompleted },
                error = null
            )
        }
    }

    fun selectFilter(value: LibraryFilter) {
        if (filter.value == value) return
        filter.value = value
        publish()
    }

    fun selectSort(value: LibrarySort) {
        if (sort.value == value) return
        sort.value = value
        publish()
    }

    fun removeFromWatchlist(entry: LibraryEntry) {
        viewModelScope.launch {
            // Only the id and the type actually reach the database - WatchlistRepository keys the
            // delete on (id, type) and the row being deleted is the one we just tapped - so this
            // stub carries no artwork or year. Passing the row's own metadata through would imply
            // it is used to find the row, and it is not.
            val stub = if (entry.mediaType == MediaType.SERIES) {
                MediaItem.SeriesItem(
                    id = entry.id,
                    title = entry.title,
                    posterUrl = null,
                    rating = null,
                    year = null,
                    totalSeasons = null,
                    backdropUrl = null
                )
            } else {
                MediaItem.MovieItem(
                    id = entry.id,
                    title = entry.title,
                    posterUrl = null,
                    rating = null,
                    year = null,
                    duration = null,
                    backdropUrl = null
                )
            }
            watchlistRepository.removeFromWatchlist(stub)
        }
    }

    fun removeFromHistory(entry: LibraryEntry) {
        viewModelScope.launch {
            historyRepository.removeProgress(entry.id, entry.mediaType)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            historyRepository.clearHistory()
        }
    }

    /** Tidy-up action: drops finished titles only, so resume positions survive. */
    fun clearWatched() {
        viewModelScope.launch {
            historyRepository.clearCompleted()
        }
    }

    private fun applyFilter(entries: List<LibraryEntry>, value: LibraryFilter): List<LibraryEntry> =
        when (value) {
            LibraryFilter.ALL -> entries
            LibraryFilter.WATCHLIST -> entries.filter { it.inWatchlist }
            LibraryFilter.WATCHED -> entries.filter { it.hasProgress }
        }

    /**
     * Orders the visible list.
     *
     * RECENT_FIRST sorts on `lastPlayedTimestamp`, which is 0 for a watchlist-only title that has
     * never been played. Those therefore sink to the bottom instead of interleaving by an arbitrary
     * position, which is what a raw descending sort on a zero timestamp would do.
     */
    private fun applySort(entries: List<LibraryEntry>, value: LibrarySort): List<LibraryEntry> =
        when (value) {
            LibrarySort.RECENT_FIRST ->
                entries.sortedWith(
                    compareByDescending<LibraryEntry> { it.lastPlayedTimestamp > 0 }
                        .thenByDescending { it.lastPlayedTimestamp }
                )
            // Locale-aware, and case-insensitive: an A-Z list that sorts "apple" after "Zebra"
            // because the keys are compared by code point is not an A-Z list.
            LibrarySort.TITLE ->
                entries.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })
            LibrarySort.PROGRESS ->
                entries.sortedByDescending { it.progressFraction }
        }

    /**
     * Joins the two lists into one, most recently touched first.
     *
     * A title present in both becomes a single row carrying both flags, which is what stops the
     * library showing the same film twice just because the viewer saved it and then watched it.
     */
    private fun merge(
        saved: List<MediaItem>,
        history: List<PlaybackProgress>
    ): List<LibraryEntry> {
        val merged = LinkedHashMap<String, LibraryEntry>()

        saved.forEach { item ->
            val type = when (item) {
                is MediaItem.SeriesItem -> MediaType.SERIES
                else -> MediaType.MOVIE
            }
            merged[item.watchlistKey()] = LibraryEntry(
                id = item.id,
                title = item.title,
                posterUrl = item.posterUrl,
                rating = item.rating,
                mediaType = type,
                inWatchlist = true,
                hasProgress = false
            )
        }

        history.forEach { progress ->
            val key = "${progress.mediaType.name}:${progress.mediaId}"
            val existing = merged[key]
            merged[key] = (existing ?: LibraryEntry(
                id = progress.mediaId,
                title = progress.title,
                posterUrl = progress.posterUrl,
                rating = progress.rating,
                mediaType = progress.mediaType,
                inWatchlist = false,
                hasProgress = false
            )).copy(
                hasProgress = true,
                isCompleted = progress.isCompleted,
                progressFraction = progress.progress,
                seasonNumber = progress.seasonNumber,
                episodeNumber = progress.episodeNumber,
                lastPlayedTimestamp = progress.lastPlayedTimestamp
            )
        }

        // Ordering is left to applySort: returning a pre-sorted list here would make the RECENT_FIRST
        // option silently do nothing, since a sort applied later overwrites whatever we chose.
        return merged.values.toList()
    }
}