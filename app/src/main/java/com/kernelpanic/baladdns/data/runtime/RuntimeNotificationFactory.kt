package com.kernelpanic.baladdns.data.runtime

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.kernelpanic.baladdns.MainActivity
import com.kernelpanic.baladdns.R
import com.kernelpanic.baladdns.data.wifi.WifiRuleStatus
import com.kernelpanic.baladdns.services.ToggleReceiver

class RuntimeNotificationFactory(
    rawContext: Context,
) {
    private val context = rawContext.applicationContext
    private val manager = context.getSystemService(NotificationManager::class.java)

    fun ensureChannel() {
        val channel = NotificationChannel(
            RuntimeNotifications.CHANNEL_ID,
            context.getString(R.string.runtime_notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.runtime_notification_channel_description)
            setShowBadge(false)
        }
        manager?.createNotificationChannel(channel)
    }

    fun build(
        reasons: Set<RuntimeMonitorReason>,
        selectedDnsActive: Boolean,
        wifiStatus: WifiRuleStatus,
        canToggle: Boolean,
    ): Notification {
        ensureChannel()
        val presentation = deriveRuntimeNotificationPresentation(
            reasons = reasons,
            selectedDnsActive = selectedDnsActive,
            wifiStatus = wifiStatus,
            canToggle = canToggle,
        )
        val contentIntent = PendingIntent.getActivity(
            context,
            REQUEST_OPEN_APP,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val builder = NotificationCompat.Builder(context, RuntimeNotifications.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_qs_adns)
            .setContentTitle(context.getString(R.string.runtime_notification_title))
            .setContentText(context.getString(presentation.message.stringResource))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setContentIntent(contentIntent)

        if (presentation.showToggleAction) {
            val toggleIntent = Intent(context, ToggleReceiver::class.java).apply {
                action = ToggleReceiver.ACTION_TOGGLE_DNS
            }
            val togglePendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_TOGGLE_DNS,
                toggleIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            builder.addAction(
                R.drawable.ic_qs_adns,
                context.getString(
                    if (selectedDnsActive) R.string.disable_blocker else R.string.enable_blocker
                ),
                togglePendingIntent,
            )
        }
        return builder.build()
    }

    fun cancel() {
        manager?.cancel(RuntimeNotifications.NOTIFICATION_ID)
    }

    private val RuntimeNotificationMessage.stringResource: Int
        get() = when (this) {
            RuntimeNotificationMessage.DnsEnabled -> R.string.runtime_notification_dns_enabled
            RuntimeNotificationMessage.DnsDisabled -> R.string.runtime_notification_dns_disabled
            RuntimeNotificationMessage.WifiMonitoring ->
                R.string.runtime_notification_wifi_monitoring
            RuntimeNotificationMessage.WifiSuspended ->
                R.string.runtime_notification_wifi_suspended
            RuntimeNotificationMessage.WifiExternalChange ->
                R.string.runtime_notification_wifi_external_change
        }

    companion object {
        private const val REQUEST_OPEN_APP = 7100
        private const val REQUEST_TOGGLE_DNS = 7101
    }
}
