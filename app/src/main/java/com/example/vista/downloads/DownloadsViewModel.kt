package com.example.vista.downloads

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vista.domain.model.MediaType
import com.example.vista.domain.model.StreamSource
import com.example.vista.domain.repository.PlaybackRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class DownloadsUiState(
    val entries: List<OfflineEntry> = emptyList(), val loading: Boolean = true,
    val busy: Boolean = false, val sources: List<StreamSource> = emptyList(),
    val defaultHeight: Int = 720,
    val message: String? = null, val target: DownloadTarget? = null
)

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val downloads: OfflineDownloads,
    private val playback: PlaybackRepository,
    private val captures: PlaybackCaptureStore,
    private val preferences: com.example.vista.data.local.PreferencesManager
) : ViewModel() {
    private val targetId = savedStateHandle.get<String>("downloadMediaId").orEmpty()
    private val _state = MutableStateFlow(DownloadsUiState(target = if (targetId.isBlank()) null else DownloadTarget(
        targetId, savedStateHandle["downloadTitle"] ?: "Title", savedStateHandle["downloadType"] ?: "movie",
        savedStateHandle.get<String>("downloadSeason")?.toIntOrNull() ?: 0,
        savedStateHandle.get<String>("downloadEpisode")?.toIntOrNull() ?: 0
    )))
    val state = _state.asStateFlow()
    init {
        viewModelScope.launch { preferences.downloadMaxHeight.collect { height -> _state.update { it.copy(defaultHeight = height) } } }
        viewModelScope.launch {
            while (isActive) {
                try {
                    val entries = downloads.list()
                    _state.update { it.copy(entries = entries, loading = false) }
                }
                catch (e: CancellationException) { throw e }
                catch (_: Exception) { _state.update { it.copy(loading = false, message = "Couldn't read the download library. Try reopening it.") } }
                delay(1_000)
            }
        }
    }

    private fun operation(block: suspend () -> Unit) {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true, message = null) }
        viewModelScope.launch {
            try { block() }
            catch (_: TimeoutCancellationException) { _state.update { it.copy(message = "The provider did not respond in time. Try again or choose another source.") } }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { _state.update { it.copy(message = e.message?.takeUnless { message -> message.contains("http") } ?: "This source couldn't complete the download. Try a different source.") } }
            finally { _state.update { it.copy(busy = false) } }
        }
    }

    private suspend fun sources(target: DownloadTarget): List<StreamSource> = withTimeout(22_000) {
        val captured = captures.candidates(target)
        if (captured.isNotEmpty()) return@withTimeout captured
        playback.getStreamSources(target.mediaId, if (target.type == "series") MediaType.SERIES else MediaType.MOVIE,
            target.season.takeIf { it > 0 }, target.episode.takeIf { it > 0 }, target.title)
            .filter { DownloadPolicy.unavailableReason(it, System.currentTimeMillis()) == null }.distinctBy { it.url }
    }

    fun checkSources() = operation {
        val target = _state.value.target ?: return@operation
        val sources = sources(target)
        _state.update { it.copy(sources = sources, message = if (sources.isEmpty())
            "No downloadable media source was resolved. If the title plays in an embedded player, start it there and use the player's Download button. Some sources remain streaming-only." else null) }
    }

    fun start(source: StreamSource, maxHeight: Int) = operation {
        val target = _state.value.target ?: return@operation
        downloads.enqueue(target, source, maxHeight)
        _state.update { it.copy(target = null, sources = emptyList(), message = "Added to Downloads. Transfers wait for an unmetered, non-roaming connection.") }
    }
    fun pause(id: String) = operation { downloads.pause(id) }
    fun resume(id: String) = operation { downloads.resume(id) }
    fun remove(id: String) = operation { downloads.remove(id) }
    fun retry(entry: OfflineEntry) = operation {
        check(downloads.owner() == entry.metadata.owner) { "Your profile changed. Reopen Downloads." }
        val refreshed = sources(entry.metadata.target).let { list -> list.firstOrNull { it.name == entry.metadata.provider } ?: list.firstOrNull() }
            ?: error("No fresh downloadable source is available. Your partial download was kept.")
        downloads.remove(entry.id)
        withTimeout(15_000) { while (downloads.list().any { it.id == entry.id }) delay(200) }
        check(downloads.owner() == entry.metadata.owner) { "Your profile changed. Start this title again from its details." }
        downloads.enqueue(entry.metadata.target, refreshed, entry.metadata.quality.removeSuffix("p").toIntOrNull() ?: 720)
    }
}
