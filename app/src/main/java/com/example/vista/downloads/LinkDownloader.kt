package com.example.vista.downloads

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import com.example.vista.domain.model.StreamSource
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Direct Link Downloader helper using system DownloadManager for direct media links (.mp4, .mkv, .m3u8).
 */
@Singleton
class LinkDownloader @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    /**
     * Enqueues a direct link download using system DownloadManager.
     */
    fun downloadLink(
        url: String,
        title: String,
        headers: Map<String, String> = emptyMap(),
        destinationFileName: String? = null,
    ): Long {
        val sanitizedTitle = title.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val fileName = destinationFileName ?: "$sanitizedTitle.mp4"

        val request = DownloadManager.Request(Uri.parse(url)).apply {
            setTitle(title)
            setDescription("Downloading $title")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "VistaWatch/$fileName")
            headers.forEach { (key, value) ->
                addRequestHeader(key, value)
            }
            setAllowedOverMetered(true)
            setAllowedOverRoaming(false)
        }

        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        return manager.enqueue(request)
    }

    /**
     * Enqueues a StreamSource using direct link downloading.
     */
    fun downloadSource(source: StreamSource, title: String): Long {
        return downloadLink(
            url = source.url,
            title = title,
            headers = source.headers,
        )
    }
}
