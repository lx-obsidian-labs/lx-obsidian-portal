package com.example.vista.data.repository

import com.example.vista.data.remote.tvmaze.TvmazeApiService
import com.example.vista.domain.model.*
import com.example.vista.domain.repository.CatalogRepository
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Secondary catalog source using TVMaze. Used primarily for Series metadata fallback.
 */
@Singleton
class TvmazeCatalogRepository @Inject constructor(
    private val apiService: TvmazeApiService
) {
    // Two home sections both search "trending"; caching makes the second a no-op instead of a
    // duplicate network round-trip.
    private val searchCache = ConcurrentHashMap<String, Pair<Long, List<Series>>>()

    private companion object {
        const val SEARCH_TTL_MS = 5 * 60_000L
    }

    suspend fun searchSeries(query: String): List<Series> {
        val sanitized = query.replace("-", " ").trim()
        val now = System.currentTimeMillis()
        val cached = searchCache[sanitized]
        if (cached != null && now - cached.first < SEARCH_TTL_MS) return cached.second
        val result = try {
            apiService.searchShows(sanitized).mapNotNull { it.show?.toDomainSeries() }
        } catch (e: Exception) {
            emptyList()
        }
        searchCache[sanitized] = now to result
        return result
    }

    suspend fun getSeriesDetails(tvmazeId: Int): Series? {
        return try {
            apiService.getShow(tvmazeId).toDomainSeries()
        } catch (e: Exception) {
            null
        }
    }
}

// Extension to map TVMaze models to Domain
fun com.example.vista.data.remote.tvmaze.TvmazeShow.toDomainSeries(): Series {
    return Series(
        id = "tvmaze_$id",
        title = name ?: "Unknown",
        overview = summary ?: "",
        posterUrl = image?.original ?: image?.medium,
        backdropUrl = image?.original,
        firstAirYear = premiered?.take(4)?.toIntOrNull(),
        lastAirYear = null,
        rating = rating?.average,
        voteCount = null,
        genres = genres?.map { Genre(it.hashCode().toString(), it) } ?: emptyList(),
        cast = emptyList(),
        creators = emptyList(),
        totalSeasons = 1,
        totalEpisodes = 0,
        status = status,
        ageRating = null
    )
}
