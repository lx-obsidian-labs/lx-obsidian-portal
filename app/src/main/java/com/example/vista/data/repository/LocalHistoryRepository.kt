package com.example.vista.data.repository

import com.example.vista.data.local.dao.PlaybackHistoryDao
import com.example.vista.data.local.entity.PlaybackHistoryEntity
import com.example.vista.domain.model.*
import com.example.vista.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flatMapLatest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalHistoryRepository @Inject constructor(
    private val historyDao: PlaybackHistoryDao,
    private val profile: com.example.vista.data.local.ProfileScope
) : HistoryRepository {

    override fun getContinueWatching(): Flow<List<MediaItem.ContinueWatchingItem>> {
        return profile.owners.flatMapLatest { historyDao.getContinueWatching(profile.claimLegacy(it)) }.map { entities ->
            entities.map { entity ->
                MediaItem.ContinueWatchingItem(
                    id = entity.mediaId,
                    title = entity.title,
                    posterUrl = entity.posterUrl,
                    rating = entity.rating,
                    progress = entity.progress,
                    currentTime = entity.currentPosition,
                    duration = entity.duration,
                    backdropUrl = null,
                    isSeries = entity.mediaType == "series",
                    seasonNumber = entity.seasonNumber,
                    episodeNumber = entity.episodeNumber,
                    episodeTitle = if (entity.seasonNumber != null) "S${entity.seasonNumber} E${entity.episodeNumber}" else null
                )
            }
        }
    }

    override suspend fun saveProgress(progress: PlaybackProgress) {
        historyDao.insert(
            PlaybackHistoryEntity(
                mediaId = progress.mediaId,
                ownerId = profile.claimLegacy(),
                mediaType = progress.mediaType.name.lowercase(),
                title = progress.title,
                posterUrl = progress.posterUrl,
                rating = progress.rating,
                currentPosition = progress.currentPosition,
                duration = progress.duration,
                lastPlayedTimestamp = progress.lastPlayedTimestamp,
                seasonNumber = progress.seasonNumber,
                episodeNumber = progress.episodeNumber,
                isCompleted = progress.isCompleted
            )
        )
    }

    override suspend fun removeProgress(mediaId: String, mediaType: MediaType) {
        historyDao.delete(mediaId, mediaType.name.lowercase(), profile.claimLegacy())
    }

    override suspend fun markAsCompleted(mediaId: String, mediaType: MediaType) {
        val entity = historyDao.getByMediaIdAndType(mediaId, mediaType.name.lowercase(), profile.claimLegacy()) ?: return
        historyDao.insert(entity.copy(isCompleted = true))
    }

    override suspend fun getProgress(mediaId: String, mediaType: MediaType): PlaybackProgress? {
        return historyDao.getByMediaIdAndType(mediaId, mediaType.name.lowercase(), profile.claimLegacy())?.toDomain()
    }

    override fun getWatchHistory(): Flow<List<PlaybackProgress>> {
        return profile.owners.flatMapLatest { historyDao.getAllHistory(profile.claimLegacy(it)) }.map { entities -> entities.map { it.toDomain() } }
    }

    override suspend fun clearHistory() {
        historyDao.deleteAll(profile.claimLegacy())
    }

    override suspend fun clearCompleted() {
        historyDao.deleteCompleted(profile.claimLegacy())
    }

    private fun PlaybackHistoryEntity.toDomain(): PlaybackProgress {
        return PlaybackProgress(
            mediaId = mediaId,
            mediaType = if (mediaType == "series") MediaType.SERIES else MediaType.MOVIE,
            title = title,
            posterUrl = posterUrl,
            rating = rating,
            currentPosition = currentPosition,
            duration = duration,
            lastPlayedTimestamp = lastPlayedTimestamp,
            seasonNumber = seasonNumber,
            episodeNumber = episodeNumber,
            isCompleted = isCompleted
        )
    }
}
