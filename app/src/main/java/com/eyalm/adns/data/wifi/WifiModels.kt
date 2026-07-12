package com.eyalm.adns.data.wifi

import com.eyalm.adns.data.PrivateDnsObservation

@JvmInline
value class WifiSsid private constructor(val value: String) {
    companion object {
        private const val UNKNOWN_SSID = "<unknown ssid>"

        fun fromUserInput(value: String?): WifiSsid? = value
            ?.takeUnless(String::isBlank)
            ?.let(::WifiSsid)

        fun fromAndroid(value: String?): WifiSsid? {
            val raw = value ?: return null
            if (raw.equals(UNKNOWN_SSID, ignoreCase = true)) return null
            val normalized = if (
                raw.length >= 2 && raw.first() == '"' && raw.last() == '"'
            ) {
                raw.substring(1, raw.lastIndex)
            } else {
                raw
            }
            return fromUserInput(normalized)
        }
    }
}

sealed interface ConnectedWifiIdentity {
    data object NotOnWifi : ConnectedWifiIdentity
    data object PermissionRequired : ConnectedWifiIdentity
    data object LocationServicesDisabled : ConnectedWifiIdentity
    data object RedactedOrUnknown : ConnectedWifiIdentity
    data class Known(val ssid: WifiSsid) : ConnectedWifiIdentity
}

data class WifiRulesConfiguration(
    val ssids: Set<WifiSsid> = emptySet(),
)

enum class WifiSuspensionPhase {
    Applying,
    Active,
    Restoring,
}

data class WifiRuleSuspension(
    val matchedSsid: WifiSsid,
    val restoreTarget: PrivateDnsObservation,
    val stateAppliedByAdns: PrivateDnsObservation,
    val phase: WifiSuspensionPhase,
    val previousStateAppliedByAdns: PrivateDnsObservation? = null,
)

data class WifiRulesState(
    val configuration: WifiRulesConfiguration = WifiRulesConfiguration(),
    val suspension: WifiRuleSuspension? = null,
    val relinquishedSsid: WifiSsid? = null,
)

sealed interface WifiRuleStatus {
    data object Disabled : WifiRuleStatus
    data object ActivationRequired : WifiRuleStatus
    data object PermissionRequired : WifiRuleStatus
    data object LocationServicesDisabled : WifiRuleStatus
    data object IdentityUnavailable : WifiRuleStatus
    data object Monitoring : WifiRuleStatus
    data object WriteFailed : WifiRuleStatus
    data object ExternalChangeDetected : WifiRuleStatus
    data class MatchedAlreadyDisabled(val ssid: WifiSsid) : WifiRuleStatus
    data class Suspended(val ssid: WifiSsid) : WifiRuleStatus
}
