package com.example.vista.sharing

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.vista.MainActivity
import com.example.vista.R
import com.example.vista.domain.model.SharingMilestone
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Posts the one notification a viewer ever gets for reaching a milestone.
 *
 * Its own channel, deliberately separate from the "Updates" channel used for announcements: someone
 * who is bored of being asked to share can silence this and still receive app news.
 */
@Singleton
class SharingMilestoneNotifier @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * @return true only if the system accepted the notification. Callers must not spend a share
     * allowance slot on a notification that was silently dropped - permission denied, channel muted,
     * or notifications switched off entirely.
     */
    fun notify(milestone: SharingMilestone): Boolean {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.getSystemService(NotificationManager::class.java)
                ?.createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_ID,
                        "Your wins",
                        // DEFAULT, not HIGH: an achievement is welcome but it should never vibrate
                        // the phone or interrupt something the viewer is doing.
                        NotificationManager.IMPORTANCE_DEFAULT
                    ).apply { description = "Occasional invitations to share a viewing milestone" }
                )
        }

        val launched = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_OPEN_SHARE_MILESTONE, milestone.id)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            // Distinct request code per milestone so a second prompt does not reuse - and overwrite -
            // the first one's pending intent, which would make the notification open the wrong win.
            milestone.id.hashCode(),
            launched,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("${milestone.emoji} ${milestone.headline}")
            .setContentText("Tap to share it - message already written")
            .setStyle(NotificationCompat.BigTextStyle().bigText(milestone.shareBody))
            .setSubText("Vista Cinema")
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        return try {
            NotificationManagerCompat.from(context).notify(milestone.id.hashCode(), notification)
            true
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS can be revoked between the check above and the notify call.
            false
        }
    }

    companion object {
        const val CHANNEL_ID = "vista_share_milestones"
    }
}