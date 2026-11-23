package com.example.vista.downloads

import android.content.Context
import android.net.Uri
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.offline.*
import androidx.media3.exoplayer.scheduler.Requirements
import com.example.vista.domain.model.StreamSource
import com.example.vista.domain.model.StreamType
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.io.IOException
import java.util.concurrent.Executor
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** One persistent engine/index. Request data owns metadata; no second conflicting Room state. */
@Singleton
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class OfflineDownloads @Inject constructor(
    @ApplicationContext private val context: Context,
    private val auth: FirebaseAuth
) {
    private val gson = Gson()
    private val commands = Mutex()
    private val readers = mutableSetOf<String>()
    private val database by lazy { StandaloneDatabaseProvider(context) }
    private val mediaDir by lazy { File(context.noBackupFilesDir, "offline_media").apply { mkdirs() } }
    private val cache by lazy { SimpleCache(mediaDir, NoOpCacheEvictor(), database) }
    private val index by lazy { DefaultDownloadIndex(database) }
    private val executor = Executor { it.run() }
    fun owner(): String = auth.currentUser?.uid ?: "local-guest"
    private fun metadata(request: DownloadRequest): OfflineMetadata? =
        runCatching { gson.fromJson(request.data.toString(Charsets.UTF_8), OfflineMetadata::class.java) }
            .getOrNull()?.takeIf { it.target != null && it.owner != null && it.headers != null }

    val manager: DownloadManager by lazy {
        DownloadManager(context, index, DownloaderFactory { request ->
            val data = metadata(request)
            val upstream = httpFactory(data?.headers.orEmpty())
            val guarded = DataSource.Factory {
                val source = upstream.createDataSource()
                object : DataSource by source {
                    override fun open(dataSpec: DataSpec): Long {
                        if (mediaDir.usableSpace < 100L * 1024 * 1024) throw IOException("Not enough free storage for this download.")
                        if (isRoaming()) throw IOException("Downloads are paused while roaming.")
                        return source.open(dataSpec)
                    }
                }
            }
            DefaultDownloaderFactory(cacheFactory(request.id, guarded), executor).createDownloader(request)
        }).apply {
            maxParallelDownloads = 1
            minRetryCount = 2
            requirements = Requirements(Requirements.NETWORK_UNMETERED)
            addListener(object : DownloadManager.Listener {
                override fun onInitialized(downloadManager: DownloadManager) { isolateOwners(); applyRoamingPolicy() }
                override fun onDownloadChanged(downloadManager: DownloadManager, download: Download, finalException: Exception?) { applyRoamingPolicy() }
            })
            auth.addAuthStateListener { isolateOwners() }
            context.getSystemService(ConnectivityManager::class.java).registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                    Handler(Looper.getMainLooper()).post { applyRoamingPolicy() }
                }
            })
        }
    }

    private fun isRoaming(): Boolean {
        val manager = context.getSystemService(ConnectivityManager::class.java)
        val caps = manager.getNetworkCapabilities(manager.activeNetwork) ?: return false
        if (!caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) return false
        return if (Build.VERSION.SDK_INT >= 28) !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_ROAMING)
        else caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) &&
            context.getSystemService(android.telephony.TelephonyManager::class.java).isNetworkRoaming
    }

    private fun applyRoamingPolicy() {
        val roaming = isRoaming()
        manager.currentDownloads.forEach { download ->
            if (metadata(download.request)?.owner == owner()) {
                if (roaming && download.stopReason == 0) manager.setStopReason(download.request.id, 3)
                else if (!roaming && download.stopReason == 3) manager.setStopReason(download.request.id, 0)
            }
        }
    }

    private fun isolateOwners() {
        // Called on Main by Firebase/Media3; user pause is never cleared automatically.
        val activeOwner = owner()
        manager.currentDownloads.forEach { download ->
            if (metadata(download.request)?.owner != activeOwner) manager.setStopReason(download.request.id, 2)
        }
    }

    private fun httpFactory(headers: Map<String, String>) = DefaultHttpDataSource.Factory()
        .setDefaultRequestProperties(headers.filterKeys { it.lowercase() in setOf("user-agent", "referer", "origin") })
        .setConnectTimeoutMs(10_000).setReadTimeoutMs(20_000)
        .setContentTypePredicate { type -> type == null || (!type.contains("html", true) && !type.contains("json", true)) }

    private fun cacheFactory(id: String, upstream: DataSource.Factory?) = CacheDataSource.Factory()
        .setCache(cache).setUpstreamDataSourceFactory(upstream)
        // Isolate owners/items: removing one download must never remove another item's segments.
        .setCacheKeyFactory { spec -> "$id:${spec.key ?: spec.uri}" }

    suspend fun list(): List<OfflineEntry> {
        val (live, waiting) = withContext(Dispatchers.Main.immediate) {
            manager.currentDownloads.associateBy { it.request.id } to (manager.notMetRequirements != 0)
        }
        return withContext(Dispatchers.IO) {
            index.getDownloads().use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        val stored = cursor.download
                        val download = live[stored.request.id] ?: stored
                        val data = metadata(download.request) ?: continue
                        if (data.owner == owner()) add(OfflineEntry(download.request.id, data, download.state,
                            download.bytesDownloaded, download.contentLength, download.percentDownloaded, waiting, download.stopReason))
                    }
                }
            }
        }
    }

    suspend fun enqueue(target: DownloadTarget, source: StreamSource, maxHeight: Int): String = commands.withLock {
        val capturedOwner = owner()
        val id = DownloadPolicy.id(capturedOwner, target.mediaId, target.type, target.season, target.episode)
        check(DownloadPolicy.unavailableReason(source, System.currentTimeMillis()) == null) {
            DownloadPolicy.unavailableReason(source, System.currentTimeMillis()).orEmpty()
        }
        withContext(Dispatchers.IO) {
            check(mediaDir.usableSpace >= 300L * 1024 * 1024) { "Free at least 300 MB before starting. The complete title may need considerably more space." }
            val existing = index.getDownload(id)
            check(existing == null) { "This title is already in Downloads. Resume or delete it there before starting another copy." }
        }
        withContext(Dispatchers.Main.immediate) {
            val mime = when (source.type) { StreamType.HLS -> MimeTypes.APPLICATION_M3U8; StreamType.DASH -> MimeTypes.APPLICATION_MPD; else -> null }
            val item = MediaItem.Builder().setUri(source.url).setMimeType(mime).build()
            val parameters = DownloadHelper.getDefaultTrackSelectorParameters(context).buildUpon()
                .setMaxVideoSize(Int.MAX_VALUE, maxHeight).setExceedVideoConstraintsIfNecessary(false)
                .setPreferredTextLanguage("en").setSelectUndeterminedTextLanguage(true).build()
            val helper = DownloadHelper.forMediaItem(item, parameters, DefaultRenderersFactory(context), httpFactory(source.headers))
            try {
                withTimeout(15_000) { suspendCancellableCoroutine<Unit> { continuation ->
                    helper.prepare(object : DownloadHelper.Callback {
                        override fun onPrepared(helper: DownloadHelper) { if (continuation.isActive) continuation.resume(Unit) }
                        override fun onPrepareError(helper: DownloadHelper, e: IOException) { if (continuation.isActive) continuation.resumeWithException(e) }
                    })
                } }
                var selectedHeight = 0
                for (period in 0 until helper.periodCount) {
                    val info = helper.getMappedTrackInfo(period)
                    var hasVideo = false
                    for (renderer in 0 until info.rendererCount) {
                        helper.getTrackSelections(period, renderer).forEach { selection ->
                            for (track in 0 until selection.length()) {
                                val format = selection.getFormat(track)
                                check(format.drmInitData == null) { "This source needs an unsupported offline license." }
                                if (info.getRendererType(renderer) == C.TRACK_TYPE_VIDEO) {
                                    hasVideo = true
                                    selectedHeight = maxOf(selectedHeight, format.height)
                                }
                            }
                        }
                    }
                    check(hasVideo) { "No compatible video track at that quality. Choose Best available or another source." }
                }
                check(owner() == capturedOwner) { "Your account changed. Start the download again." }
                val data = OfflineMetadata(capturedOwner, target, source.headers, source.name,
                    if (selectedHeight > 0) "${selectedHeight}p" else source.quality ?: "Original file",
                    source.expiresAt?.let(DownloadPolicy::expiresAtMs))
                val request = helper.getDownloadRequest(id, gson.toJson(data).toByteArray())
                DownloadService.sendAddDownload(context, VistaDownloadService::class.java, request, true)
                id
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                // Direct link downloader fallback for direct URLs or when helper preparation fails.
                enqueueDirectLinkInternal(capturedOwner, id, target, source)
            } finally { helper.release() }
        }
    }

    /** Direct link downloader method for Media3 DownloadService. Bypasses track preparation requirements. */
    private fun enqueueDirectLinkInternal(capturedOwner: String, id: String, target: DownloadTarget, source: StreamSource): String {
        val mime = when (source.type) { StreamType.HLS -> MimeTypes.APPLICATION_M3U8; StreamType.DASH -> MimeTypes.APPLICATION_MPD; else -> MimeTypes.VIDEO_MP4 }
        val data = OfflineMetadata(capturedOwner, target, source.headers, source.name, source.quality ?: "Direct link", source.expiresAt?.let(DownloadPolicy::expiresAtMs))
        val request = DownloadRequest.Builder(id, Uri.parse(source.url))
            .setMimeType(mime)
            .setData(gson.toJson(data).toByteArray())
            .build()
        DownloadService.sendAddDownload(context, VistaDownloadService::class.java, request, true)
        return id
    }

    suspend fun pause(id: String) = command(id) { DownloadService.sendSetStopReason(context, VistaDownloadService::class.java, id, 1, true) }
    suspend fun resume(id: String) = command(id) {
        DownloadService.sendSetStopReason(context, VistaDownloadService::class.java, id, 0, true)
    }
    suspend fun remove(id: String) = command(id) {
        check(id !in readers) { "Close offline playback before deleting this title." }
        DownloadService.sendRemoveDownload(context, VistaDownloadService::class.java, id, true)
    }
    private suspend fun command(id: String, action: () -> Unit) = withContext(Dispatchers.Main.immediate) {
        val data = withContext(Dispatchers.IO) { index.getDownload(id)?.request?.let(::metadata) }
        check(data?.owner == owner()) { "This download belongs to a different profile or was removed." }
        action()
    }

    suspend fun offlineItem(id: String): Pair<MediaItem, DataSource.Factory> {
        val download = withContext(Dispatchers.IO) { index.getDownload(id) } ?: error("This download was removed.")
        check(download.state == Download.STATE_COMPLETED) { "This download is not complete." }
        check(metadata(download.request)?.owner == owner()) { "Sign in to the profile that downloaded this title." }
        return download.request.toMediaItem() to cacheFactory(id, null).setCacheWriteDataSinkFactory(null)
    }
    fun beginReading(id: String) { readers.add(id) }
    fun endReading(id: String) { readers.remove(id) }
}
