package com.eyalm.adns.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.eyalm.adns.MainActivity
import com.eyalm.adns.R
import com.eyalm.adns.services.ToggleReceiver
import androidx.core.content.edit

class NotificationsManager(private val context: Context) {

    private val notificationManager = context.getSystemService(NotificationManager::class.java)
    private val sharedPrefs = context.getSharedPreferences("adns_settings", Context.MODE_PRIVATE)

    companion object {
        const val CHANNEL_ID = "dns_status_channel"
        const val NOTIFICATION_ID = 1
    }

    init {
        createNotificationChannel()
    }

    fun isNotificationEnabled(): Boolean {
        return sharedPrefs.getBoolean("state_notifications_enabled", true)
    }

    fun setNotificationEnabled(enabled: Boolean, isActive: Boolean) {
        sharedPrefs.edit { putBoolean("state_notifications_enabled", enabled) }
        updateNotification(isActive)
    }

    private fun createNotificationChannel() {
        val name = context.getString(R.string.ad_blocker_state)
        val importance = NotificationManager.IMPORTANCE_LOW

        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = context.getString(R.string.shows_the_state_of_the_ad_blocker)
        }

        notificationManager?.createNotificationChannel(channel)
    }

    fun updateNotification(isActive: Boolean) {
        if (notificationManager == null) return

        if (
            !isNotificationEnabled() ||
            !AppRuntimeRepositories.capabilities(context).current().canUseDnsToggleSurfaces
        ) {
            notificationManager.cancel(NOTIFICATION_ID)
            return
        }

        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        val buttonIntent = Intent(context, ToggleReceiver::class.java).apply {
            action = "TOGGLE_DNS"
        }

        val buttonPendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            buttonIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_qs_adns)
            .setContentTitle(context.getString(R.string.ad_blocker))
            .setContentText(if (isActive) context.getString(R.string.blocker_enabled) else context.getString(R.string.blocker_disabled))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(isActive)
            .setAutoCancel(false)
            .setContentIntent(pendingIntent)
            .setShowWhen(false)
            .addAction(
                R.drawable.ic_qs_adns,
                if (isActive) context.getString(R.string.disable_blocker) else context.getString(R.string.enable_blocker),
                buttonPendingIntent
            )
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
