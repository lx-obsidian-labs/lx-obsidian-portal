package com.example.vista.data.remote.watchmode

import com.example.vista.BuildConfig
import com.example.vista.domain.model.StreamSource
import com.example.vista.domain.model.StreamType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WatchmodeRepository @Inject constructor(
    private val apiService: WatchmodeApiService
) {
    suspend fun getStreamingSources(tmdbId: String, type: String): List<WatchmodeSource> {
        return try {
            // Find Watchmode ID using TMDB ID
            // The search response for external IDs usually contains the title list directly
            val response = apiService.search(
                apiKey = BuildConfig.WATCHMODE_API_KEY,
                query = tmdbId,
                searchField = "tmdb_${type}_id"
            )
            
            // Map the generic Any response to a usable ID if possible
            // Note: In a production app, we'd define a proper search result model
            val watchmodeId = when (response) {
                is Map<*, *> -> {
                    val titles = response["title_results"] as? List<*>
                    (titles?.firstOrNull() as? Map<*, *>)?.get("id")?.toString()
                }
                else -> null
            }

            if (watchmodeId != null) {
                apiService.getSources(watchmodeId, BuildConfig.WATCHMODE_API_KEY)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
