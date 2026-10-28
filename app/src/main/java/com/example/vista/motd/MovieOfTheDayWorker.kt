package com.example.vista.motd

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.vista.MainActivity
import com.example.vista.R
import com.example.vista.data.local.PreferencesManager
import com.example.vista.data.local.dao.PlaybackHistoryDao
import com.example.vista.data.analytics.VistaAnalytics
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/**
 * The daily reminder.
 *
 * Local rather than pushed: a daily server push would mean sending the whole catalogue a message
 * every day to describe a pick that the client could have derived on its own. The cost is a
 * notification the app can only schedule in approximate windows, which is a good trade for something
 * that is not urgent.
 */
@HiltWorker
class MovieOfTheDayWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val preferencesManager: PreferencesManager,
    private val historyRepository: com.example.vista.domain.repository.HistoryRepository,
    private val vistaAnalytics: VistaAnalytics
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        if (!preferencesManager.movieOfTheDayEnabled.first()) return Result.success()

        // The master switch has to be re-read here, not just honoured when the toggle is touched.
        // This worker was registered while notifications were on, so a later "pause all
        // notifications" would otherwise keep being delivered until the next scheduling pass.
        if (!preferencesManager.notificationsEnabled()) return Result.success()
        if (!NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()) {
            return Result.success()
        }

        // Same watch-history filter the in-app card uses, so the notification cannot recommend a
        // film the viewer has already finished. Room is a local read, not a network call.
        val watched = historyRepository.getWatchHistory().first()
            .filter { it.mediaType == com.example.vista.domain.model.MediaType.MOVIE }
            .map { it.mediaId }
            .toSet()

        val today = MovieOfTheDayPicker.pick(LocalDate.now(), watched)

        val channelManager = applicationContext.getSystemService(NotificationManager::class.java)
        if (channelManager == null) return Result.success()

        // A separate channel so this suggestion can be turned off on its own, or muted, without
        // silencing real announcements.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channelManager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Movie of the Day",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply { description = "A free film suggestion each day" }
            )
        }

        // Asks MainActivity to surface Home, where the suggestion card is waiting. Still no deep
        // link to the film itself: tapping through to a title is a purchase of attention, and the
        // card lets the viewer dismiss the suggestion without leaving Home.
        val launched = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_OPEN_HOME, true)
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            NOTIFICATION_ID,
            launched,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("${today.eventLabel}: ${today.movie.title}")
            .setContentText(today.movie.blurb)
            .setStyle(NotificationCompat.BigTextStyle().bigText(today.movie.blurb))
            .setSubText("Free on Vista Cinema")
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        if (Build.VERSION.SDK_INT < 33 || androidx.core.content.ContextCompat.checkSelfPermission(applicationContext, android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            try { NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_ID, notification) }
            catch (_: SecurityException) { return Result.success() }
        }

        // Only recorded after the system successfully accepts the notification, so the count is
        // delivery, not intent.
        vistaAnalytics.track(
            "motd_reminder_delivered",
            properties = mapOf(
                "label" to (today.eventLabel ?: ""),
                "seasonal" to today.isSeasonal.toString()
            )
        )
        return Result.success()
    }

    companion object {
        const val CHANNEL_ID = "vista_movie_of_day"
        private const val NOTIFICATION_ID = 4201
    }
}

/**
 * Registers the daily reminder.
 *
 * Kept as a thin object rather than injected everywhere because scheduling happens from two unrelated
 * places - app start and the settings toggle - and both only need the same one call.
 */
object MovieOfTheDayScheduler {

    private const val WORK_NAME = "vista_movie_of_day"

    fun schedule(context: Context, enabled: Boolean) {
        val workManager = WorkManager.getInstance(context)
        if (!enabled) {
            workManager.cancelUniqueWork(WORK_NAME)
            return
        }

        val request = PeriodicWorkRequestBuilder<MovieOfTheDayWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(2, TimeUnit.HOURS)
            .build()

        // KEEP, not UPDATE: rescheduling on every app start would push the daily run further out each
        // time, because the delay is measured from the call.
        workManager.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    /** Epoch day of [date], for comparing against the stored last-shown day. */
    fun epochDay(date: LocalDate): Long = date.toEpochDay()
}
