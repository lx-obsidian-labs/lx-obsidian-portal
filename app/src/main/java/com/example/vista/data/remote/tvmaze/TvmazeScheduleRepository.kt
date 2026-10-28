package com.example.vista.data.remote.tvmaze

import com.example.vista.domain.repository.ScheduleRepository
import com.example.vista.domain.repository.ScheduleItem
import com.example.vista.util.DateUtils
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TvmazeScheduleRepository @Inject constructor(
    private val apiService: TvmazeApiService
) : ScheduleRepository {

    override suspend fun getSchedule(country: String, date: String?): List<ScheduleItem> {
        return try {
            val episodes = apiService.getSchedule(country, date)
            episodes.map { episode ->
                ScheduleItem(
                    showId = episode._links?.show?.href?.substringAfterLast("/")?.toIntOrNull() ?: 0,
                    showName = episode.name ?: "",
                    showImage = episode.image?.medium,
                    season = episode.season ?: 0,
                    episodeNumber = episode.number ?: 0,
                    episodeTitle = episode.name,
                    airDate = episode.airDate,
                    airTime = episode.airTime,
                    runtime = episode.runtime,
                    rating = episode.rating?.average,
                    summary = episode.summary?.replace(Regex("<[^>]*>"), "")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getTodaySchedule(country: String): List<ScheduleItem> {
        return getSchedule(country, DateUtils.todayIso())
    }

    override suspend fun getWeekSchedule(country: String): List<ScheduleItem> {
        return try {
            val allEpisodes = mutableListOf<ScheduleItem>()
            for (dayOffset in 0..6) {
                allEpisodes.addAll(getSchedule(country, DateUtils.isoDaysFromToday(dayOffset)))
            }
            allEpisodes
        } catch (e: Exception) {
            emptyList()
        }
    }
}
