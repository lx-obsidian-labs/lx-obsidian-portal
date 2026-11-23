package com.example.vista.data.remote.firebase

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.vista.MainActivity
import com.example.vista.R
import com.example.vista.data.local.PreferencesManager
import com.example.vista.util.AnnouncementsStore
import com.example.vista.util.CountryResolver
import com.example.vista.util.NotificationLinks
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Handles Firebase Cloud Messaging for release announcements and system notifications.
 */
@AndroidEntryPoint
class VistaMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var preferencesManager: PreferencesManager

    @Inject
    lateinit var announcementsStore: AnnouncementsStore

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onNewToken(token: String) {
        FirebaseMessaging.getInstance().subscribeToTopic(TOPIC_GLOBAL)
        serviceScope.launch {
            runCatching {
                preferencesManager.setFcmToken(token)
                val market = CountryResolver.marketTopic(preferencesManager.ensureCountryCode())
                FirebaseMessaging.getInstance().subscribeToTopic(market)
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        serviceScope.launch {
            val data = remoteMessage.data
            val category = data["category"] ?: CATEGORY_UPDATES
            val isUpdate = category == CATEGORY_UPDATES

            // Non-update categories (promotions, recommendations, features) strictly obey Settings > Notifications toggle.
            if (!isUpdate) {
                val settings = runCatching { preferencesManager.settings.first() }.getOrNull() ?: return@launch
                if (!settings.notificationsEnabled || (category == CATEGORY_PROMOTIONS && !settings.notificationReleases)) return@launch
            }

            val title = com.example.vista.util.VistaBrand.normalize(data["title"] ?: remoteMessage.notification?.title ?: com.example.vista.util.VistaBrand.NAME)
            val body = com.example.vista.util.VistaBrand.normalize(data["body"] ?: remoteMessage.notification?.body ?: return@launch)
            val checkUpdate = data["check_update"] == "true" || isUpdate

            // Keep the announcement in the in-app bell store so it survives notification shade dismissal.
            if (remoteMessage.sentTime != 0L) {
                announcementsStore.add(title, body, remoteMessage.sentTime)
            } else {
                announcementsStore.add(title, body)
            }

            showNotification(remoteMessage.messageId?.hashCode() ?: body.hashCode(), title, body, checkUpdate, category, NotificationLinks.resolve(data["action_url"]))
        }
    }

    private fun showNotification(id: Int, title: String, body: String, checkUpdate: Boolean, category: String, actionUrl: String?) {
        val manager = getSystemService(NotificationManager::class.java) ?: return

        val isUpdate = category == CATEGORY_UPDATES
        val channelId = if (isUpdate) CHANNEL_ID_UPDATES else CHANNEL_ID_PROMOTIONS

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val updatesChannel = NotificationChannel(
                CHANNEL_ID_UPDATES,
                "App Updates & Security",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Essential Vista Watch app releases and critical security updates" }

            val promotionsChannel = NotificationChannel(
                CHANNEL_ID_PROMOTIONS,
                "Promotions & Recommendations",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "New content recommendations, feature announcements, and milestone achievements" }

            manager.createNotificationChannel(updatesChannel)
            manager.createNotificationChannel(promotionsChannel)
        }

        val linkIntent = actionUrl?.let { url ->
            Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val intent = if (linkIntent != null && linkIntent.resolveActivity(packageManager) != null) {
            linkIntent
        } else {
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(EXTRA_CHECK_UPDATE, checkUpdate)
                putExtra(EXTRA_FROM_NOTIFICATION, true)
            }
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setPriority(if (isUpdate) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .build()

        manager.notify(id, notification)
    }

    companion object {
        const val TOPIC_GLOBAL = "vista_updates"
        const val CHANNEL_ID_UPDATES = "vista_updates"
        const val CHANNEL_ID_PROMOTIONS = "vista_promotions"
        const val CATEGORY_UPDATES = "updates"
        const val CATEGORY_PROMOTIONS = "promotions"
        const val EXTRA_CHECK_UPDATE = "check_update"
        const val EXTRA_FROM_NOTIFICATION = "from_notification"
    }
}
