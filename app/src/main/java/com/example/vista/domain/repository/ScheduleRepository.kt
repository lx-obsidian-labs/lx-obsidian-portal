package com.example.vista.domain.repository

import com.example.vista.domain.model.Episode
import kotlinx.coroutines.flow.Flow

interface ScheduleRepository {
    /**
     * Get TV show schedule for a specific date and country
     * @param country 2-letter country code (e.g., "US", "GB")
     * @param date ISO 8601 date string (e.g., "2024-01-15"), null for today
     */
    suspend fun getSchedule(country: String = "US", date: String? = null): List<ScheduleItem>
    
    /**
     * Get today's schedule
     */
    suspend fun getTodaySchedule(country: String = "US"): List<ScheduleItem>
    
    /**
     * Get schedule for the next 7 days
     */
    suspend fun getWeekSchedule(country: String = "US"): List<ScheduleItem>
}

data class ScheduleItem(
    val showId: Int,
    val showName: String,
    val showImage: String?,
    val season: Int,
    val episodeNumber: Int,
    val episodeTitle: String?,
    val airDate: String?,
    val airTime: String?,
    val runtime: Int?,
    val rating: Double?,
    val summary: String?
) {
    fun toEpisode(): Episode {
        return Episode(
            seriesId = showId.toString(),
            seasonNumber = season,
            episodeNumber = episodeNumber,
            title = episodeTitle ?: "S${season}E$episodeNumber",
            overview = summary?.replace(Regex("<[^>]*>"), ""),
            thumbnailUrl = showImage,
            runtimeMinutes = runtime,
            rating = rating,
            airDate = airDate,
            stillUrl = showImage
        )
    }
}
