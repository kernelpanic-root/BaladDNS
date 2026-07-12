package com.eyalm.adns.data.runtime

import com.eyalm.adns.data.wifi.WifiRuleStatus

enum class RuntimeNotificationMessage {
    DnsEnabled,
    DnsDisabled,
    WifiMonitoring,
    WifiSuspended,
    WifiExternalChange,
}

data class RuntimeNotificationPresentation(
    val message: RuntimeNotificationMessage,
    val showToggleAction: Boolean,
)

fun deriveRuntimeNotificationPresentation(
    reasons: Set<RuntimeMonitorReason>,
    selectedDnsActive: Boolean,
    wifiStatus: WifiRuleStatus,
    canToggle: Boolean,
): RuntimeNotificationPresentation {
    val message = when {
        wifiStatus is WifiRuleStatus.Suspended -> RuntimeNotificationMessage.WifiSuspended
        wifiStatus == WifiRuleStatus.ExternalChangeDetected ->
            RuntimeNotificationMessage.WifiExternalChange
        RuntimeMonitorReason.StateNotification in reasons -> {
            if (selectedDnsActive) {
                RuntimeNotificationMessage.DnsEnabled
            } else {
                RuntimeNotificationMessage.DnsDisabled
            }
        }
        else -> RuntimeNotificationMessage.WifiMonitoring
    }
    return RuntimeNotificationPresentation(
        message = message,
        showToggleAction = canToggle,
    )
}
