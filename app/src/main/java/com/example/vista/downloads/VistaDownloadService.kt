package com.example.vista.downloads

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.scheduler.Scheduler
import com.example.vista.MainActivity
import com.example.vista.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class VistaDownloadService : DownloadService(7301, 1_000, "vista_downloads", R.string.download_channel_name, 0) {
    @Inject lateinit var downloads: OfflineDownloads
    override fun getDownloadManager(): DownloadManager = downloads.manager
    override fun getScheduler(): Scheduler? = null
    override fun getForegroundNotification(current: MutableList<Download>, notMetRequirements: Int): Notification {
        val active = current.firstOrNull { it.state == Download.STATE_DOWNLOADING } ?: current.firstOrNull()
        val click = PendingIntent.getActivity(this, 7301, Intent(this, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val builder = NotificationCompat.Builder(this, "vista_downloads")
            .setSmallIcon(R.mipmap.ic_launcher).setContentTitle("Vista downloads")
            .setContentText(if (notMetRequirements != 0) "Waiting for an unmetered, non-roaming connection" else "Open Library → Downloads to manage your titles")
            .setContentIntent(click).setOngoing(true).setOnlyAlertOnce(true)
        val progress = active?.percentDownloaded ?: -1f
        builder.setProgress(100, progress.toInt().coerceIn(0, 100), progress < 0)
        active?.let {
            val pauseIntent = buildSetStopReasonIntent(this, VistaDownloadService::class.java, it.request.id, 1, true)
            val pause = if (Build.VERSION.SDK_INT >= 26) PendingIntent.getForegroundService(this, 7302, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                else PendingIntent.getService(this, 7302, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            builder.addAction(android.R.drawable.ic_media_pause, "Pause", pause)
        }
        return builder.build()
    }
}
