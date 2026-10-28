package com.example.vista.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

// MARK: - Settings Models

data class AppSettings(
    val autoplayNext: Boolean = true,
    val preferredQuality: String = "Auto",
    val subtitleLanguage: String = "Off",
    val subtitleEnabled: Boolean = false,
    val subtitleSize: Float = 16f,
    val subtitleColor: String = "White",
    val playbackSpeed: Float = 1f,
    val cellularStreaming: CellularStreamingSetting = CellularStreamingSetting.ALWAYS_ASK,
    val appTheme: AppTheme = AppTheme.DARK,
    val notificationsEnabled: Boolean = true,
    val notificationNewEpisodes: Boolean = true,
    val notificationNewSeasons: Boolean = true,
    val notificationReleases: Boolean = true,
    val notificationTvGuideReminders: Boolean = true,
    val shareDiagnostics: Boolean = true,
    val audioLanguage: String = "Original",
    val doubleTapToSeekSeconds: Int = 10,
    val showAdultContent: Boolean = false,
    val dataSaverMode: Boolean = false,
    val autoResume: Boolean = true,
    val cacheLimitMb: Int = 500
)

data class Viewer(
    val uid: String = "",
    val alias: String = "",
    val avatarId: String = "",
    val email: String? = null,
    val phoneNumber: String? = null,
    val createdAt: Long = 0,
    val lastSeenAt: Long = 0,
    val appVersion: String = "",
    val personalizationEnabled: Boolean = true,
    val moderationStatus: String = "approved"
)

enum class AppTheme(val displayName: String) {
    DARK("Dark"),
    LIGHT("Light"),
    SYSTEM("System Default"),
    OLED("OLED Black")
}

enum class CellularStreamingSetting(val displayName: String) {
    ALWAYS_ASK("Always Ask"),
    WIFI_ONLY("Wi-Fi Only"),
    ALWAYS_ALLOW("Allow on Cellular")
}

// MARK: - Core Media Models

data class Movie(
    val id: String,
    val title: String,
    val overview: String,
    val posterUrl: String?,
    val backdropUrl: String?,
    val releaseYear: Int?,
    val runtimeMinutes: Int?,
    val rating: Double?,
    val voteCount: Int?,
    val genres: List<Genre>,
    val cast: List<Person>,
    val director: String?,
    val ageRating: String?,
    val isAdult: Boolean = false
)

data class Series(
    val id: String,
    val title: String,
    val overview: String,
    val posterUrl: String?,
    val backdropUrl: String?,
    val firstAirYear: Int?,
    val lastAirYear: Int?,
    val rating: Double?,
    val voteCount: Int?,
    val genres: List<Genre>,
    val cast: List<Person>,
    val creators: List<String>,
    val totalSeasons: Int,
    val totalEpisodes: Int,
    val status: String?,
    val ageRating: String?,
    val isInProduction: Boolean = true
)

data class Season(
    val seriesId: String,
    val seasonNumber: Int,
    val title: String,
    val overview: String?,
    val posterUrl: String?,
    val episodeCount: Int,
    val airYear: Int?
)

data class Episode(
    val seriesId: String,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val title: String,
    val overview: String?,
    val thumbnailUrl: String?,
    val runtimeMinutes: Int?,
    val rating: Double?,
    val airDate: String?,
    val stillUrl: String?
)// MARK: - Video/Trailer Models
data class Video(
    val id: String,
    val key: String,
    val name: String,
    val site: String,
    val type: String,
    val official: Boolean
) {
    fun isYouTubeTrailer(): Boolean {
        return site == "YouTube" && (type == "Trailer" || type == "Teaser")
    }
    
    fun getYouTubeUrl(): String? {
        return if (site == "YouTube") "https://www.youtube.com/watch?v=$key" else null
    }
    
    fun getYouTubeThumbnailUrl(): String? {
        return if (site == "YouTube") "https://img.youtube.com/vi/$key/mqdefault.jpg" else null
    }
}

// MARK: - Supporting Models
data class Genre(
    val id: String,
    val name: String
)

data class Person(
    val id: String,
    val name: String,
    val profileUrl: String?,
    val character: String?,
    val department: String?
)

data class PersonDetails(
    val id: String,
    val name: String,
    val profileUrl: String?,
    val biography: String?,
    val birthday: String?,
    val deathday: String?,
    val placeOfBirth: String?,
    val popularity: Double?,
    val knownForDepartment: String?,
    val alsoKnownAs: List<String>,
    val movieCredits: List<PersonMovieCredit>,
    val tvCredits: List<PersonTvCredit>
)

data class PersonMovieCredit(
    val id: String,
    val title: String,
    val character: String?,
    val posterUrl: String?,
    val releaseYear: Int?,
    val rating: Double?
)

data class PersonTvCredit(
    val id: String,
    val name: String,
    val character: String?,
    val posterUrl: String?,
    val firstAirYear: Int?,
    val rating: Double?
)

data class CastMember(
    val id: String,
    val name: String,
    val profileUrl: String?,
    val character: String,
    val order: Int
)

// MARK: - Stream Models

data class StreamSource(
    val url: String,
    val type: StreamType,
    val quality: String?,
    val bitrate: Int?,
    val name: String = "Primary Source",
    val headers: Map<String, String> = emptyMap(),
    val language: String? = null,
    val expiresAt: Long? = null,
    val drmInfo: DrmInfo? = null
)

enum class StreamType {
    MP4, HLS, DASH, OTHER
}

data class DrmInfo(
    val licenseUrl: String?,
    val drmScheme: String?,
    val drmHeaders: Map<String, String> = emptyMap()
)

data class SubtitleTrack(
    val id: String,
    val language: String,
    val label: String,
    val url: String,
    val mimeType: String = "application/x-subrip"
)

data class AudioTrack(
    val id: String,
    val language: String,
    val label: String,
    val mimeType: String = "audio/mp4"
)

// MARK: - Home Screen Models

data class HomeSection(
    val id: String,
    val title: String,
    val type: HomeSectionType,
    val items: List<MediaItem>
)

enum class HomeSectionType {
    FEATURED_CAROUSEL,
    CONTINUE_WATCHING,
    TRENDING,
    NEW_RELEASES,
    POPULAR_MOVIES,
    POPULAR_SERIES,
    TVMAZE_TRENDING,
    RECOMMENDED,
    GENRE_BASED,
    SOCIAL_BUZZ
}

sealed class MediaItem {
    abstract val id: String
    abstract val title: String
    abstract val posterUrl: String?
    abstract val rating: Double?

    data class MovieItem(
        override val id: String,
        override val title: String,
        override val posterUrl: String?,
        override val rating: Double?,
        val year: Int?,
        val duration: String?,
        val backdropUrl: String?,
        val ageRating: String? = null
    ) : MediaItem()

    data class SeriesItem(
        override val id: String,
        override val title: String,
        override val posterUrl: String?,
        override val rating: Double?,
        val year: Int?,
        val totalSeasons: Int?,
        val backdropUrl: String?,
        val ageRating: String? = null
    ) : MediaItem()

    data class ContinueWatchingItem(
        override val id: String,
        override val title: String,
        override val posterUrl: String?,
        override val rating: Double?,
        val progress: Float,
        val currentTime: Long,
        val duration: Long,
        val backdropUrl: String?,
        val isSeries: Boolean,
        val seasonNumber: Int? = null,
        val episodeNumber: Int? = null,
        val episodeTitle: String? = null
    ) : MediaItem()

    data class SocialBuzzItem(
        override val id: String,
        override val title: String,
        override val posterUrl: String?,
        override val rating: Double? = null,
        val platform: String, // e.g., "Reddit", "Twitter"
        val discussionTopic: String,
        val hotnessScore: Int, // 1-100
        val mediaId: String? = null
    ) : MediaItem()
}

/**
 * Stable composite key for watchlist lookups. The type is part of the key because provider id
 * namespaces overlap (e.g. a TMDB movie and a Shoofaflam series can share a numeric id), so the
 * key must include the type to be unambiguous.
 */
fun MediaItem.watchlistKey(): String {
    val type = when (this) {
        is MediaItem.MovieItem -> "movie"
        is MediaItem.SeriesItem -> "series"
        is MediaItem.ContinueWatchingItem -> if (isSeries) "series" else "movie"
        is MediaItem.SocialBuzzItem -> "movie"
    }
    return "$type:${id}"
}

fun Movie.toMediaItem(): MediaItem.MovieItem {
    return MediaItem.MovieItem(
        id = id,
        title = title,
        posterUrl = posterUrl,
        rating = rating,
        year = releaseYear,
        duration = runtimeMinutes?.let { "${it}m" },
        backdropUrl = backdropUrl,
        ageRating = ageRating
    )
}

fun Series.toMediaItem(): MediaItem.SeriesItem {
    return MediaItem.SeriesItem(
        id = id,
        title = title,
        posterUrl = posterUrl,
        rating = rating,
        year = firstAirYear,
        totalSeasons = totalSeasons,
        backdropUrl = backdropUrl,
        ageRating = ageRating
    )
}

fun PersonMovieCredit.toMediaItem(): MediaItem.MovieItem {
    return MediaItem.MovieItem(
        id = id,
        title = title,
        posterUrl = posterUrl,
        rating = rating,
        year = releaseYear,
        duration = null,
        backdropUrl = null
    )
}

fun PersonTvCredit.toMediaItem(): MediaItem.SeriesItem {
    return MediaItem.SeriesItem(
        id = id,
        title = name,
        posterUrl = posterUrl,
        rating = rating,
        year = firstAirYear,
        totalSeasons = null,
        backdropUrl = null
    )
}

// MARK: - Search Models

data class SearchResult(
    val movies: List<Movie>,
    val series: List<Series>,
    val people: List<Person>,
    val totalCount: Int
)

data class SearchFilters(
    val query: String = "",
    val type: SearchType = SearchType.ALL,
    val genres: List<String> = emptyList(),
    val yearFrom: Int? = null,
    val yearTo: Int? = null,
    val ratingFrom: Double? = null,
    val sortBy: SortBy = SortBy.POPULARITY,
    val page: Int? = 1
)

enum class SearchType {
    ALL, MOVIES, SERIES, PEOPLE
}

enum class SortBy(val displayName: String) {
    POPULARITY("Popularity"),
    RATING("Rating"),
    YEAR("Year"),
    TITLE("Title")
}
