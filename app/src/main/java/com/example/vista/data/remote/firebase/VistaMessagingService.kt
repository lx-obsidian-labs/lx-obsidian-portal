package com.example.vista.data.remote.firebase

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.vista.MainActivity
import com.example.vista.R
import com.example.vista.data.local.PreferencesManager
import com.example.vista.util.AnnouncementsStore
import com.example.vista.util.CountryResolver
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Handles Firebase Cloud Messaging for release announcements.
 *
 * Devices running 1.0.5+ subscribe to the `vista_updates` topic on token refresh and at app
 * start ([com.example.vista.VistaApp]), and 1.0.6+ additionally subscribes to a market topic
 * (`vista_updates_za` / `vista_updates_row`) chosen from the viewer's country. Sending from the
 * Firebase CLI (`firebase messaging:send --topic vista_updates ...`) reaches every installed copy;
 * targeting a market topic narrows that to one region. Never send the same message to both.
 *
 * Message formats understood (data payload keys, all optional):
 *  - `title` / `body`  - notification text (falls back to the `notification` payload, then defaults)
 *  - `check_update`    - `"true"` forces an in-app update check when the notification is tapped
 *  - `category`        - `"updates"` (default) always renders; other categories respect the
 *                         in-app master notification toggle so Settings > Notifications is honest
 */
@AndroidEntryPoint
class VistaMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var preferencesManager: PreferencesManager

    @Inject
    lateinit var announcementsStore: AnnouncementsStore

    override fun onNewToken(token: String) {
        // A refreshed token drops every topic subscription this install had, so both have to be
        // re-established or the viewer silently stops receiving announcements.
        FirebaseMessaging.getInstance().subscribeToTopic(TOPIC_GLOBAL)
        GlobalScope.launch {
            preferencesManager.setFcmToken(token)
            val market = CountryResolver.marketTopic(preferencesManager.ensureCountryCode())
            FirebaseMessaging.getInstance().subscribeToTopic(market)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val data = remoteMessage.data
        val category = data["category"] ?: CATEGORY_UPDATES
        val isUpdate = category == CATEGORY_UPDATES

        // Release announcements bypass the in-app toggle (the in-app updater is the safety net
        // anyway); every other category obeys Settings > Notifications. This runs on FCM's
        // background thread, so a blocking DataStore read is safe here.
        if (!isUpdate && !kotlinx.coroutines.runBlocking { preferencesManager.notificationsEnabled() }) return

        val title = data["title"] ?: remoteMessage.notification?.title ?: "Vista Cinema"
        val body = data["body"] ?: remoteMessage.notification?.body ?: return
        val checkUpdate = data["check_update"] == "true" || isUpdate

        // Keep the announcement in the in-app bell so it survives the notification shade.
        if (remoteMessage.sentTime != 0L) {
            announcementsStore.add(title, body, remoteMessage.sentTime)
        } else {
            announcementsStore.add(title, body)
        }

        showNotification(remoteMessage.messageId?.hashCode() ?: body.hashCode(), title, body, checkUpdate)
    }

    private fun showNotification(id: Int, title: String, body: String, checkUpdate: Boolean) {
        val manager = getSystemService(NotificationManager::class.java) ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Updates",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply { description = "New Vista Cinema releases and announcements" }
            )
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_CHECK_UPDATE, checkUpdate)
            putExtra(EXTRA_FROM_NOTIFICATION, true)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()

        // Mirrors the system behaviour for notification messages: no sound head when DND is on.
        manager.notify(id, notification)
    }

    companion object {
        /**
         * Every install on 1.0.5+ has always subscribed here. Send to this when the whole audience
         * should hear something.
         */
        const val TOPIC_GLOBAL = "vista_updates"
        const val CHANNEL_ID = "vista_updates"
        const val CATEGORY_UPDATES = "updates"
        const val EXTRA_CHECK_UPDATE = "check_update"
        const val EXTRA_FROM_NOTIFICATION = "from_notification"
    }
}
