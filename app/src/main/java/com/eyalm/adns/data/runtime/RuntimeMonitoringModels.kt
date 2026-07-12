package com.eyalm.adns.data.runtime

data class RuntimeMonitoringPreferences(
    val stateNotificationEnabled: Boolean = false,
    val wifiRulesEnabled: Boolean = false,
)

enum class RuntimeMonitorReason {
    StateNotification,
    WifiRules,
}

enum class NotificationPermissionState {
    NotRequired,
    Granted,
    Denied,
}

enum class NotificationChannelState {
    Missing,
    Enabled,
    Disabled,
}

enum class RuntimeServiceFailure {
    StartNotAllowed,
    MissingPermission,
    Unknown,
}

data class RuntimeMonitoringSystemState(
    val notificationPermission: NotificationPermissionState =
        NotificationPermissionState.NotRequired,
    val appNotificationsEnabled: Boolean = true,
    val stateChannel: NotificationChannelState = NotificationChannelState.Missing,
    val batteryOptimizationIgnored: Boolean = false,
    val serviceRunning: Boolean = false,
    val serviceFailure: RuntimeServiceFailure? = null,
)

data class RuntimeMonitoringState(
    val stateNotificationEnabled: Boolean,
    val wifiRulesEnabled: Boolean,
    val notificationPermission: NotificationPermissionState,
    val appNotificationsEnabled: Boolean,
    val stateChannel: NotificationChannelState,
    val batteryOptimizationIgnored: Boolean,
    val requestedReasons: Set<RuntimeMonitorReason>,
    val activeReasons: Set<RuntimeMonitorReason>,
    val serviceRunning: Boolean,
    val serviceFailure: RuntimeServiceFailure?,
)

fun deriveRuntimeMonitoringState(
    preferences: RuntimeMonitoringPreferences,
    system: RuntimeMonitoringSystemState,
    canRunRuntimeMonitor: Boolean,
    canUseWifiRules: Boolean,
): RuntimeMonitoringState {
    val requestedReasons = buildSet {
        if (preferences.stateNotificationEnabled) {
            add(RuntimeMonitorReason.StateNotification)
        }
        if (preferences.wifiRulesEnabled) {
            add(RuntimeMonitorReason.WifiRules)
        }
    }
    val activeReasons = buildSet {
        if (
            RuntimeMonitorReason.StateNotification in requestedReasons &&
            canRunRuntimeMonitor
        ) {
            add(RuntimeMonitorReason.StateNotification)
        }
        if (RuntimeMonitorReason.WifiRules in requestedReasons && canUseWifiRules) {
            add(RuntimeMonitorReason.WifiRules)
        }
    }
    return RuntimeMonitoringState(
        stateNotificationEnabled = preferences.stateNotificationEnabled,
        wifiRulesEnabled = preferences.wifiRulesEnabled,
        notificationPermission = system.notificationPermission,
        appNotificationsEnabled = system.appNotificationsEnabled,
        stateChannel = system.stateChannel,
        batteryOptimizationIgnored = system.batteryOptimizationIgnored,
        requestedReasons = requestedReasons,
        activeReasons = activeReasons,
        serviceRunning = system.serviceRunning,
        serviceFailure = system.serviceFailure,
    )
}

data class RuntimeServicePlan(
    val activeReasons: Set<RuntimeMonitorReason>,
    val recoveryRequired: Boolean,
) {
    val shouldRun: Boolean
        get() = activeReasons.isNotEmpty() || recoveryRequired
}

fun deriveRuntimeServicePlan(
    preferences: RuntimeMonitoringPreferences,
    canRunRuntimeMonitor: Boolean,
    canUseWifiRules: Boolean,
    wifiReasonAllowed: Boolean,
    hasPendingWifiSuspension: Boolean,
): RuntimeServicePlan {
    val activeReasons = buildSet {
        if (preferences.stateNotificationEnabled && canRunRuntimeMonitor) {
            add(RuntimeMonitorReason.StateNotification)
        }
        if (
            preferences.wifiRulesEnabled &&
            canUseWifiRules &&
            wifiReasonAllowed
        ) {
            add(RuntimeMonitorReason.WifiRules)
        }
    }
    return RuntimeServicePlan(
        activeReasons = activeReasons,
        recoveryRequired = hasPendingWifiSuspension &&
            canUseWifiRules &&
            wifiReasonAllowed,
    )
}

fun canResumeWifiRulesFromBoot(
    sdkInt: Int,
    hasFineLocation: Boolean,
    hasBackgroundLocation: Boolean,
    locationServicesEnabled: Boolean,
): Boolean = hasFineLocation && locationServicesEnabled && (
    sdkInt < BACKGROUND_LOCATION_MIN_SDK || hasBackgroundLocation
)

private const val BACKGROUND_LOCATION_MIN_SDK = 29
