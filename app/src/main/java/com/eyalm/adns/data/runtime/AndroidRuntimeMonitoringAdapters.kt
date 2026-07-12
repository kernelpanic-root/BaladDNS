package com.eyalm.adns.data.runtime

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.edit

object RuntimeNotifications {
    const val CHANNEL_ID = "dns_status_channel"
    const val NOTIFICATION_ID = 1
}

class SharedPreferencesRuntimeMonitoringStore(
    private val preferences: SharedPreferences,
) : RuntimeMonitoringStore {
    override fun read(): StoredRuntimeMonitoringSnapshot = StoredRuntimeMonitoringSnapshot(
        migrationVersion = preferences.getInt(KEY_MIGRATION_VERSION, 0),
        legacyStateNotificationEnabled = LEGACY_STATE_NOTIFICATION_KEY
            .takeIf(preferences::contains)
            ?.let { preferences.getBoolean(it, false) },
        stateNotificationEnabled = preferences.getBoolean(
            KEY_STATE_NOTIFICATION_ENABLED,
            false,
        ),
        stateNotificationChoiceRecorded = preferences.getBoolean(
            KEY_STATE_NOTIFICATION_CHOICE_RECORDED,
            false,
        ),
        wifiRulesEnabled = preferences.getBoolean(KEY_WIFI_RULES_ENABLED, false),
    )

    override fun write(snapshot: StoredRuntimeMonitoringSnapshot) {
        preferences.edit {
            putInt(KEY_MIGRATION_VERSION, snapshot.migrationVersion)
            putBoolean(KEY_STATE_NOTIFICATION_ENABLED, snapshot.stateNotificationEnabled)
            putBoolean(
                KEY_STATE_NOTIFICATION_CHOICE_RECORDED,
                snapshot.stateNotificationChoiceRecorded,
            )
            putBoolean(KEY_WIFI_RULES_ENABLED, snapshot.wifiRulesEnabled)
            remove(LEGACY_STATE_NOTIFICATION_KEY)
        }
    }

    companion object {
        const val LEGACY_STATE_NOTIFICATION_KEY = "state_notifications_enabled"
        private const val KEY_MIGRATION_VERSION = "runtime_monitoring.migration_version"
        private const val KEY_STATE_NOTIFICATION_ENABLED = "notifications.state_enabled"
        private const val KEY_STATE_NOTIFICATION_CHOICE_RECORDED =
            "notifications.state_choice_recorded"
        private const val KEY_WIFI_RULES_ENABLED = "wifi_rules.enabled"
    }
}

class AndroidRuntimeMonitoringSystemStateReader(
    rawContext: Context,
) {
    private val context = rawContext.applicationContext
    private val notificationManager = context.getSystemService(NotificationManager::class.java)
    private val powerManager = context.getSystemService(PowerManager::class.java)

    fun read(serviceRunning: Boolean = false): RuntimeMonitoringSystemState =
        RuntimeMonitoringSystemState(
            notificationPermission = notificationPermission(),
            appNotificationsEnabled = NotificationManagerCompat
                .from(context)
                .areNotificationsEnabled(),
            stateChannel = channelState(),
            batteryOptimizationIgnored = powerManager?.isIgnoringBatteryOptimizations(
                context.packageName
            ) == true,
            serviceRunning = serviceRunning,
        )

    private fun notificationPermission(): NotificationPermissionState = when {
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ->
            NotificationPermissionState.NotRequired
        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED -> NotificationPermissionState.Granted
        else -> NotificationPermissionState.Denied
    }

    private fun channelState(): NotificationChannelState {
        val channel = notificationManager?.getNotificationChannel(RuntimeNotifications.CHANNEL_ID)
            ?: return NotificationChannelState.Missing
        return if (channel.importance == NotificationManager.IMPORTANCE_NONE) {
            NotificationChannelState.Disabled
        } else {
            NotificationChannelState.Enabled
        }
    }
}

object RuntimeMonitoringRepositories {
    @Volatile
    private var instance: RuntimeMonitoringRepository? = null

    fun getInstance(context: Context): RuntimeMonitoringRepository =
        instance ?: synchronized(this) {
            instance ?: RuntimeMonitoringRepository(
                SharedPreferencesRuntimeMonitoringStore(
                    context.applicationContext.getSharedPreferences(
                        "adns_settings",
                        Context.MODE_PRIVATE,
                    )
                )
            ).also { repository ->
                instance = repository
                if (!repository.preferences.value.stateNotificationEnabled) {
                    context.getSystemService(NotificationManager::class.java)
                        ?.cancel(RuntimeNotifications.NOTIFICATION_ID)
                }
            }
        }
}
