package com.example.vista.data.remote.shoofaflam

import com.example.vista.domain.model.*
import com.example.vista.domain.repository.CatalogRepository
import android.util.Log
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShoofaflamRepository @Inject constructor(
    private val apiService: ShoofaflamApiService
) {
    companion object {
        private const val TAG = "ShoofaflamRepo"
    }

    private var cachedTitles: List<ShoofaflamTitle>? = null
    private val mutex = kotlinx.coroutines.sync.Mutex()

    suspend fun getAllTitles(): List<ShoofaflamTitle> {
        return mutex.withLock {
            cachedTitles ?: try {
                val titles = apiService.getAllTitles().filter { it.posterPath != null }
                cachedTitles = titles
                titles
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                Log.e(TAG, "Error fetching titles: ${e.message}")
                emptyList()
            }
        }
    }

    suspend fun getMovieTitles(): List<ShoofaflamTitle> {
        return getAllTitles().filter { it.type?.lowercase() == "movie" }
    }

    suspend fun getSeriesTitles(): List<ShoofaflamTitle> {
        return getAllTitles().filter { it.type?.lowercase() == "series" }
    }

    suspend fun getPopularMovies(limit: Int = 20): List<Movie> {
        return getMovieTitles()
            .filter { it.rating != null && it.rating > 0 }
            .sortedByDescending { it.rating }
            .take(limit)
            .map { it.toDomainMovie() }
    }

    suspend fun getPopularSeries(limit: Int = 20): List<Series> {
        return getSeriesTitles()
            .filter { it.rating != null && it.rating > 0 }
            .sortedByDescending { it.rating }
            .take(limit)
            .map { it.toDomainSeries() }
    }

    suspend fun getPopularMediaItems(limit: Int = 20): List<MediaItem> {
        return getMovieTitles()
            .filter { it.rating != null && it.rating > 0 }
            .sortedByDescending { it.rating }
            .take(limit)
            .map { it.toMediaItem() }
    }

    suspend fun getPopularSeriesMediaItems(limit: Int = 20): List<MediaItem> {
        return getSeriesTitles()
            .filter { it.rating != null && it.rating > 0 }
            .sortedByDescending { it.rating }
            .take(limit)
            .map { it.toSeriesMediaItem() }
    }

    suspend fun getTitlesByGenre(genre: String, limit: Int = 20): List<Movie> {
        return getMovieTitles()
            .filter { it.genres?.any { g -> g.equals(genre, ignoreCase = true) } == true }
            .sortedByDescending { it.rating ?: 0.0 }
            .take(limit)
            .map { it.toDomainMovie() }
    }

    suspend fun searchTitles(query: String, limit: Int = 20): List<Movie> {
        return getMovieTitles()
            .filter { 
                it.titleEn?.contains(query, ignoreCase = true) == true ||
                it.titleAr?.contains(query, ignoreCase = true) == true
            }
            .take(limit)
            .map { it.toDomainMovie() }
    }

    suspend fun getStats(): ShoofaflamStats? {
        return try {
            apiService.getStats()
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching stats: ${e.message}")
            null
        }
    }

    suspend fun getTitleBySlug(slug: String): ShoofaflamTitle? {
        return try {
            getAllTitles().find { it.slug == slug }
        } catch (e: Exception) {
            Log.e(TAG, "Error finding title by slug: ${e.message}")
            null
        }
    }
}
