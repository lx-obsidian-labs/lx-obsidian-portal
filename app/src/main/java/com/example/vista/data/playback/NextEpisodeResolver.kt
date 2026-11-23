package com.example.vista.data.playback

import com.example.vista.domain.model.Episode
import com.example.vista.domain.repository.CatalogRepository
import kotlinx.coroutines.CancellationException

/** Advance only from confirmed episode metadata, including a confirmed next season. */
object NextEpisodeResolver {
    suspend fun resolve(catalog: CatalogRepository, seriesId: String, season: Int, episode: Int): Episode? {
        if (season < 0 || episode < 1) return null
        return try {
            val current = catalog.getEpisodes(seriesId, season).filter { it.seasonNumber == season && it.episodeNumber > 0 }
            if (current.none { it.episodeNumber == episode }) return null
            current.filter { it.episodeNumber > episode }.minByOrNull { it.episodeNumber }?.let { return it }
            // Specials are not assumed to be chronologically followed by the regular first season.
            if (season == 0) return null
            val series = catalog.getSeriesDetails(seriesId) ?: return null
            if (season >= series.totalSeasons) return null
            catalog.getEpisodes(seriesId, season + 1)
                .filter { it.seasonNumber == season + 1 && it.episodeNumber > 0 }
                .minByOrNull { it.episodeNumber }
        } catch (e: CancellationException) { throw e }
        catch (_: Exception) { null }
    }
}
