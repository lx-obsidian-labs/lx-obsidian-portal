package com.example.vista.domain.repository

import com.example.vista.domain.model.*
import kotlinx.coroutines.flow.Flow

// MARK: - Repository Interfaces

interface CatalogRepository {
    suspend fun getHomeSections(): List<HomeSection>

    /**
     * One page of any declared [CatalogSection]. Home rows, "See All" and load-more all resolve
     * their content through this so they can never disagree about what a row contains.
     */
    suspend fun getSection(section: CatalogSection, page: Int = 1): List<MediaItem>
    suspend fun getFeaturedMovies(): List<Movie>
    suspend fun getTrendingMovies(page: Int = 1): List<Movie>
    suspend fun getNewReleases(page: Int = 1): List<Movie>
    suspend fun getPopularMovies(page: Int = 1): List<Movie>
    suspend fun getPopularSeries(page: Int = 1): List<Series>
    suspend fun getMovieDetails(id: String): Movie?
    suspend fun getSeriesDetails(id: String): Series?
    suspend fun getSeasonDetails(seriesId: String, seasonNumber: Int): Season?
    suspend fun getEpisodes(seriesId: String, seasonNumber: Int): List<Episode>
    suspend fun getSimilarMovies(id: String): List<Movie>
    suspend fun getSimilarSeries(id: String): List<Series>
    suspend fun getMoviesByGenre(genreId: String, page: Int = 1): List<Movie>
    suspend fun getSeriesByGenre(genreId: String, page: Int = 1): List<Series>
    suspend fun searchMovies(query: String, page: Int = 1): List<Movie>
    suspend fun searchSeries(query: String, page: Int = 1): List<Series>
    suspend fun getGenres(): List<Genre>
    /** Cast for a title. [mediaType] is explicit because TMDB movie/TV ids are not interchangeable. */
    suspend fun getCast(mediaId: String, mediaType: MediaType): List<Person>
    suspend fun getRecommendedMovies(movieId: String): List<Movie>
    suspend fun getRecommendedSeries(seriesId: String): List<Series>
    suspend fun getMovieVideos(movieId: String): List<Video>
    suspend fun getTvShowVideos(tvShowId: String): List<Video>
    suspend fun getPersonDetails(personId: String): PersonDetails?
}

interface PlaybackRepository {
    suspend fun getStreamSources(
        mediaId: String, 
        mediaType: MediaType,
        season: Int? = null,
        episode: Int? = null,
        titleHint: String? = null
    ): List<StreamSource>
    suspend fun getSubtitles(mediaId: String): List<SubtitleTrack>
    suspend fun getAudioTracks(mediaId: String): List<AudioTrack>
}

interface WatchlistRepository {
    fun getWatchlist(): Flow<List<MediaItem>>
    suspend fun addToWatchlist(item: MediaItem)

    /**
     * Removes a saved title. Uses the full item (not just its id) because the saved list is keyed
     * by (id, type): a movie and a series can share a provider id.
     */
    suspend fun removeFromWatchlist(item: MediaItem)

    /** @param type "movie" or "series" — the watchlist key includes the type, never inferred. */
    suspend fun isInWatchlist(id: String, type: String): Boolean
}

interface HistoryRepository {
    fun getContinueWatching(): Flow<List<MediaItem.ContinueWatchingItem>>

    /**
     * Full playback history, most recently played first. Unlike [getContinueWatching] this includes
     * finished titles and carries the season/episode needed to resume a specific episode.
     */
    fun getWatchHistory(): Flow<List<PlaybackProgress>>

    suspend fun saveProgress(progress: PlaybackProgress)
    suspend fun removeProgress(mediaId: String, mediaType: MediaType)
    suspend fun markAsCompleted(mediaId: String, mediaType: MediaType)
    suspend fun getProgress(mediaId: String, mediaType: MediaType): PlaybackProgress?
    suspend fun clearHistory()

    /**
     * Removes finished titles from history, keeping anything still in progress.
     *
     * Distinct from [clearHistory] so the viewer can tidy up without discarding resume positions
     * for shows they are part-way through.
     */
    suspend fun clearCompleted()
}

interface SearchRepository {
    suspend fun search(query: String, filters: SearchFilters): SearchResult
    fun getRecentSearches(): Flow<List<String>>
    suspend fun addRecentSearch(query: String)
    suspend fun removeRecentSearch(query: String)
    suspend fun clearRecentSearches()
    fun getTrendingSearches(): Flow<List<String>>
}interface SettingsRepository {
    fun getSettings(): Flow<AppSettings>
    suspend fun updateSettings(settings: AppSettings)
    suspend fun updateAutoplayNext(enabled: Boolean)
    suspend fun updatePreferredQuality(quality: String)
    suspend fun updateSubtitleLanguage(language: String)
    suspend fun updateSubtitleEnabled(enabled: Boolean)
    suspend fun updateSubtitleSize(size: Float)
    suspend fun updateSubtitleColor(color: String)
    suspend fun updatePlaybackSpeed(speed: Float)
    suspend fun updateCellularStreaming(setting: CellularStreamingSetting)
    suspend fun updateAppTheme(theme: AppTheme)
    suspend fun updateDataSaverMode(enabled: Boolean)
    suspend fun updateAutoResume(enabled: Boolean)
    suspend fun updateNotificationsEnabled(enabled: Boolean)
    suspend fun updateNotificationNewEpisodes(enabled: Boolean)
    suspend fun updateNotificationNewSeasons(enabled: Boolean)
    suspend fun updateNotificationReleases(enabled: Boolean)
    suspend fun updateNotificationTvGuideReminders(enabled: Boolean)
    suspend fun updateShareDiagnostics(enabled: Boolean)
    suspend fun updateAudioLanguage(language: String)
    suspend fun updateDoubleTapToSeek(seconds: Int)
    suspend fun updateShowAdultContent(show: Boolean)
    suspend fun updateCacheLimitMb(limitMb: Int)
    suspend fun clearHistory()
    suspend fun clearSearchHistory()
    suspend fun clearCache()
    suspend fun repairPlaybackCache()
}
