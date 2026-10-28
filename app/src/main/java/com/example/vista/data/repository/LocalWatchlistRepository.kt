package com.example.vista.data.repository

import com.example.vista.data.local.dao.WatchlistDao
import com.example.vista.data.local.entity.WatchlistEntity
import com.example.vista.domain.model.*
import com.example.vista.domain.repository.WatchlistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flatMapLatest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalWatchlistRepository @Inject constructor(
    private val watchlistDao: WatchlistDao,
    private val profile: com.example.vista.data.local.ProfileScope
) : WatchlistRepository {

    override fun getWatchlist(): Flow<List<MediaItem>> {
        return profile.owners.flatMapLatest { watchlistDao.getAll(profile.claimLegacy(it)) }.map { entities ->
            entities.map { entity ->
                when (entity.type) {
                    "movie" -> MediaItem.MovieItem(
                        id = entity.id,
                        title = entity.title,
                        posterUrl = entity.posterUrl,
                        rating = entity.rating,
                        year = entity.year,
                        duration = null,
                        backdropUrl = entity.backdropUrl
                    )
                    "series" -> MediaItem.SeriesItem(
                        id = entity.id,
                        title = entity.title,
                        posterUrl = entity.posterUrl,
                        rating = entity.rating,
                        year = entity.year,
                        totalSeasons = null,
                        backdropUrl = entity.backdropUrl
                    )
                    else -> MediaItem.MovieItem(
                        id = entity.id,
                        title = entity.title,
                        posterUrl = entity.posterUrl,
                        rating = entity.rating,
                        year = entity.year,
                        duration = null,
                        backdropUrl = entity.backdropUrl
                    )
                }
            }
        }
    }

    override suspend fun addToWatchlist(item: MediaItem) {
        watchlistDao.insert(
            WatchlistEntity(
                id = item.id,
                ownerId = profile.claimLegacy(),
                title = item.title,
                posterUrl = item.posterUrl,
                backdropUrl = when (item) {
                    is MediaItem.MovieItem -> item.backdropUrl
                    is MediaItem.SeriesItem -> item.backdropUrl
                    is MediaItem.ContinueWatchingItem -> item.backdropUrl
                    is MediaItem.SocialBuzzItem -> item.posterUrl
                },
                rating = item.rating,
                year = when (item) {
                    is MediaItem.MovieItem -> item.year
                    is MediaItem.SeriesItem -> item.year
                    is MediaItem.ContinueWatchingItem -> null
                    is MediaItem.SocialBuzzItem -> null
                },
                type = mediaTypeOf(item)
            )
        )
    }

    override suspend fun removeFromWatchlist(item: MediaItem) {
        watchlistDao.delete(item.id, mediaTypeOf(item), profile.claimLegacy())
    }

    override suspend fun isInWatchlist(id: String, type: String): Boolean {
        return watchlistDao.exists(id, type, profile.claimLegacy())
    }

    private fun mediaTypeOf(item: MediaItem): String {
        return when (item) {
            is MediaItem.MovieItem -> "movie"
            is MediaItem.SeriesItem -> "series"
            is MediaItem.ContinueWatchingItem -> if (item.isSeries) "series" else "movie"
            is MediaItem.SocialBuzzItem -> "movie"
        }
    }
}
