package com.example.vista.ui.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vista.data.remote.watchmode.WatchmodeRepository
import com.example.vista.data.remote.watchmode.WatchmodeSource
import com.example.vista.domain.model.*
import com.example.vista.domain.repository.CatalogRepository
import com.example.vista.domain.repository.HistoryRepository
import com.example.vista.domain.repository.WatchlistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DetailsUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isEpisodesLoading: Boolean = false,
    val movie: Movie? = null,
    val series: Series? = null,
    val episodes: List<Episode> = emptyList(),
    val cast: List<Person> = emptyList(),
    val videos: List<Video> = emptyList(),
    val similarMovies: List<Movie> = emptyList(),
    val similarSeries: List<Series> = emptyList(),
    val isInWatchlist: Boolean = false,
    val playbackProgress: PlaybackProgress? = null,
    val streamingSources: List<WatchmodeSource> = emptyList(),
    val error: String? = null,
    val isSeries: Boolean = false,
    val selectedSeason: Int = 1
) {
    /** Episode the Watch button should resume from, falling back to the season premiere. */
    val resumeSeason: Int get() = playbackProgress?.seasonNumber ?: selectedSeason
    val resumeEpisode: Int get() = playbackProgress?.episodeNumber ?: 1
}

@HiltViewModel
class DetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val catalogRepository: CatalogRepository,
    private val watchlistRepository: WatchlistRepository,
    private val historyRepository: HistoryRepository,
    private val watchmodeRepository: WatchmodeRepository
) : ViewModel() {

    /**
     * The media type is taken from the route we were opened with, never inferred.
     *
     * The movie and series routes carry their id under different argument names, which is the only
     * reliable signal available. Previously this class asked TMDB for a *movie* first and only
     * tried TV if that returned nothing — but TMDB keeps movies and TV in two independent id
     * namespaces that overlap numerically, so a TV id (e.g. 1399) happily resolved to an unrelated
     * film and every series opened the wrong title.
     */
    private val mediaId: String =
        savedStateHandle.get<String>("seriesId") ?: savedStateHandle.get<String>("movieId") ?: ""

    private val requestedType: MediaType =
        if (savedStateHandle.get<String>("seriesId") != null || mediaId.startsWith("tvmaze_")) 
            MediaType.SERIES 
        else 
            MediaType.MOVIE

    private val _uiState = MutableStateFlow(DetailsUiState(isSeries = requestedType == MediaType.SERIES))
    val uiState: StateFlow<DetailsUiState> = _uiState.asStateFlow()

    init {
        if (mediaId.isNotEmpty()) {
            loadDetails()
        } else {
            _uiState.update { it.copy(isLoading = false, error = "No media ID provided") }
        }
    }

    private fun loadDetails() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                if (requestedType == MediaType.MOVIE) loadMovie() else loadSeries()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = e.message ?: "Failed to load details")
                }
            }
        }
    }

    private suspend fun loadMovie() {
        val movie = catalogRepository.getMovieDetails(mediaId)
        if (movie == null) {
            _uiState.update { it.copy(isLoading = false, error = "Content not found") }
            return
        }
        coroutineScope {
            val castDeferred = async { movie.cast.ifEmpty { catalogRepository.getCast(mediaId, MediaType.MOVIE) } }
            val videosDeferred = async { catalogRepository.getMovieVideos(mediaId) }
            val similarDeferred = async { catalogRepository.getSimilarMovies(mediaId) }
            val watchlistDeferred = async { watchlistRepository.isInWatchlist(mediaId, "movie") }
            val progressDeferred = async { historyRepository.getProgress(mediaId, MediaType.MOVIE) }
            val sourcesDeferred = async { watchmodeRepository.getStreamingSources(mediaId, "movie") }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    movie = movie,
                    cast = castDeferred.await(),
                    videos = videosDeferred.await(),
                    similarMovies = similarDeferred.await(),
                    isInWatchlist = watchlistDeferred.await(),
                    playbackProgress = progressDeferred.await(),
                    streamingSources = sourcesDeferred.await(),
                    isSeries = false,
                    error = null
                )
            }
        }
    }

    private suspend fun loadSeries() {
        val series = catalogRepository.getSeriesDetails(mediaId)
        if (series == null) {
            _uiState.update { it.copy(isLoading = false, error = "Content not found") }
            return
        }

        val initialSeason = firstAvailableSeason(series)
        coroutineScope {
            val castDeferred = async { series.cast.ifEmpty { catalogRepository.getCast(mediaId, MediaType.SERIES) } }
            val episodesDeferred = async { catalogRepository.getEpisodes(mediaId, initialSeason) }
            val videosDeferred = async { catalogRepository.getTvShowVideos(mediaId) }
            val similarDeferred = async { catalogRepository.getSimilarSeries(mediaId) }
            val watchlistDeferred = async { watchlistRepository.isInWatchlist(mediaId, "series") }
            val progressDeferred = async { historyRepository.getProgress(mediaId, MediaType.SERIES) }
            val sourcesDeferred = async { watchmodeRepository.getStreamingSources(mediaId, "tv") }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    series = series,
                    cast = castDeferred.await(),
                    episodes = episodesDeferred.await(),
                    videos = videosDeferred.await(),
                    similarSeries = similarDeferred.await(),
                    isInWatchlist = watchlistDeferred.await(),
                    playbackProgress = progressDeferred.await(),
                    streamingSources = sourcesDeferred.await(),
                    isSeries = true,
                    selectedSeason = initialSeason,
                    error = null
                )
            }
        }
    }

    /** Some shows have no season 1 (specials start at 0) or report a season that has no episodes. */
    private suspend fun firstAvailableSeason(series: Series): Int {
        val candidates = (1..series.totalSeasons.coerceAtLeast(1)).toList() + listOf(0)
        for (season in candidates) {
            if (catalogRepository.getEpisodes(mediaId, season).isNotEmpty()) return season
        }
        return 1
    }

    private var toggleInFlight = false

    fun toggleWatchlist() {
        if (toggleInFlight) return
        toggleInFlight = true
        viewModelScope.launch {
            try {
                val currentState = _uiState.value
                val item = currentState.movie?.let {
                    MediaItem.MovieItem(it.id, it.title, it.posterUrl, it.rating, it.releaseYear, null, it.backdropUrl)
                } ?: currentState.series?.let {
                    MediaItem.SeriesItem(it.id, it.title, it.posterUrl, it.rating, it.firstAirYear, it.totalSeasons, it.backdropUrl)
                } ?: return@launch

                if (currentState.isInWatchlist) {
                    watchlistRepository.removeFromWatchlist(item)
                } else {
                    watchlistRepository.addToWatchlist(item)
                }
                _uiState.update { it.copy(isInWatchlist = !it.isInWatchlist) }
            } finally {
                toggleInFlight = false
            }
        }
    }

    fun selectSeason(seasonNumber: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(selectedSeason = seasonNumber, isEpisodesLoading = true) }
            try {
                val episodes = catalogRepository.getEpisodes(mediaId, seasonNumber)
                _uiState.update {
                    it.copy(
                        episodes = episodes,
                        isEpisodesLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isEpisodesLoading = false, error = e.message) }
            }
        }
    }

    fun retry() {
        loadDetails()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, error = null) }
            try {
                if (requestedType == MediaType.MOVIE) {
                    refreshMovie()
                } else {
                    refreshSeries()
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isRefreshing = false, error = e.message ?: "Failed to refresh") }
            }
        }
    }

    private suspend fun refreshMovie() {
        val movie = catalogRepository.getMovieDetails(mediaId)
        if (movie == null) {
            _uiState.update { it.copy(isRefreshing = false, error = "Content not found") }
            return
        }
        coroutineScope {
            val castDeferred = async { movie.cast.ifEmpty { catalogRepository.getCast(mediaId, MediaType.MOVIE) } }
            val videosDeferred = async { catalogRepository.getMovieVideos(mediaId) }
            val similarDeferred = async { catalogRepository.getSimilarMovies(mediaId) }
            val watchlistDeferred = async { watchlistRepository.isInWatchlist(mediaId, "movie") }
            val progressDeferred = async { historyRepository.getProgress(mediaId, MediaType.MOVIE) }
            val sourcesDeferred = async { watchmodeRepository.getStreamingSources(mediaId, "movie") }

            _uiState.update {
                it.copy(
                    isRefreshing = false,
                    movie = movie,
                    cast = castDeferred.await(),
                    videos = videosDeferred.await(),
                    similarMovies = similarDeferred.await(),
                    isInWatchlist = watchlistDeferred.await(),
                    playbackProgress = progressDeferred.await(),
                    streamingSources = sourcesDeferred.await()
                )
            }
        }
    }

    private suspend fun refreshSeries() {
        val series = catalogRepository.getSeriesDetails(mediaId)
        if (series == null) {
            _uiState.update { it.copy(isRefreshing = false, error = "Content not found") }
            return
        }
        coroutineScope {
            val castDeferred = async { series.cast.ifEmpty { catalogRepository.getCast(mediaId, MediaType.SERIES) } }
            val episodesDeferred = async { catalogRepository.getEpisodes(mediaId, _uiState.value.selectedSeason) }
            val videosDeferred = async { catalogRepository.getTvShowVideos(mediaId) }
            val similarDeferred = async { catalogRepository.getSimilarSeries(mediaId) }
            val watchlistDeferred = async { watchlistRepository.isInWatchlist(mediaId, "series") }
            val progressDeferred = async { historyRepository.getProgress(mediaId, MediaType.SERIES) }
            val sourcesDeferred = async { watchmodeRepository.getStreamingSources(mediaId, "tv") }

            _uiState.update {
                it.copy(
                    isRefreshing = false,
                    series = series,
                    cast = castDeferred.await(),
                    episodes = episodesDeferred.await(),
                    videos = videosDeferred.await(),
                    similarSeries = similarDeferred.await(),
                    isInWatchlist = watchlistDeferred.await(),
                    playbackProgress = progressDeferred.await(),
                    streamingSources = sourcesDeferred.await()
                )
            }
        }
    }
}
