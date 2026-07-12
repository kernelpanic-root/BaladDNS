package com.eyalm.adns.data.wifi

import com.eyalm.adns.data.PrivateDnsObservation

sealed interface WifiRuleDecision {
    data class NoChange(val status: WifiRuleStatus) : WifiRuleDecision
    data class BeginSuspension(
        val suspension: WifiRuleSuspension,
        val previousSuspension: WifiRuleSuspension? = null,
    ) : WifiRuleDecision
    data class Restore(val suspension: WifiRuleSuspension) : WifiRuleDecision
    data class UpdateSuspension(val suspension: WifiRuleSuspension) : WifiRuleDecision
    data class RelinquishOwnership(val ssid: WifiSsid?) : WifiRuleDecision
    data object ClearSuspension : WifiRuleDecision
    data object ClearRelinquishedSsid : WifiRuleDecision
}

class WifiRuleEngine {
    fun evaluate(
        enabled: Boolean,
        canControl: Boolean,
        configuration: WifiRulesConfiguration,
        identity: ConnectedWifiIdentity,
        currentDns: PrivateDnsObservation,
        suspensionTarget: PrivateDnsObservation,
        suspension: WifiRuleSuspension?,
        relinquishedSsid: WifiSsid? = null,
    ): WifiRuleDecision {
        if (!canControl || currentDns == PrivateDnsObservation.PermissionMissing) {
            return WifiRuleDecision.NoChange(WifiRuleStatus.ActivationRequired)
        }

        reconcilePending(currentDns, suspension)?.let { return it }

        if (
            suspension?.phase == WifiSuspensionPhase.Active &&
            !sameDnsState(currentDns, suspension.stateAppliedByAdns)
        ) {
            val currentSsid = (identity as? ConnectedWifiIdentity.Known)
                ?.ssid
                ?.takeIf { it in configuration.ssids }
            val lastKnownSsid = suspension.matchedSsid.takeIf {
                identity !is ConnectedWifiIdentity.NotOnWifi && it in configuration.ssids
            }
            return WifiRuleDecision.RelinquishOwnership(currentSsid ?: lastKnownSsid)
        }

        if (!enabled) {
            return suspension?.let(::restoreDecision)
                ?: if (relinquishedSsid != null) {
                    WifiRuleDecision.ClearRelinquishedSsid
                } else {
                    WifiRuleDecision.NoChange(WifiRuleStatus.Disabled)
                }
        }

        if (relinquishedSsid != null) {
            if (relinquishedSsid !in configuration.ssids) {
                return WifiRuleDecision.ClearRelinquishedSsid
            }
            return when (identity) {
                ConnectedWifiIdentity.NotOnWifi -> WifiRuleDecision.ClearRelinquishedSsid
                ConnectedWifiIdentity.PermissionRequired -> WifiRuleDecision.NoChange(
                    WifiRuleStatus.PermissionRequired
                )
                ConnectedWifiIdentity.LocationServicesDisabled -> WifiRuleDecision.NoChange(
                    WifiRuleStatus.LocationServicesDisabled
                )
                ConnectedWifiIdentity.RedactedOrUnknown -> WifiRuleDecision.NoChange(
                    WifiRuleStatus.IdentityUnavailable
                )
                is ConnectedWifiIdentity.Known -> if (identity.ssid == relinquishedSsid) {
                    WifiRuleDecision.NoChange(WifiRuleStatus.ExternalChangeDetected)
                } else {
                    WifiRuleDecision.ClearRelinquishedSsid
                }
            }
        }

        when (identity) {
            ConnectedWifiIdentity.PermissionRequired -> return WifiRuleDecision.NoChange(
                WifiRuleStatus.PermissionRequired
            )
            ConnectedWifiIdentity.LocationServicesDisabled -> return WifiRuleDecision.NoChange(
                WifiRuleStatus.LocationServicesDisabled
            )
            ConnectedWifiIdentity.RedactedOrUnknown -> return WifiRuleDecision.NoChange(
                WifiRuleStatus.IdentityUnavailable
            )
            ConnectedWifiIdentity.NotOnWifi -> return suspension?.let(::restoreDecision)
                ?: WifiRuleDecision.NoChange(WifiRuleStatus.Monitoring)
            is ConnectedWifiIdentity.Known -> {
                val matches = identity.ssid in configuration.ssids
                if (!matches) {
                    return suspension?.let(::restoreDecision)
                        ?: WifiRuleDecision.NoChange(WifiRuleStatus.Monitoring)
                }
                if (suspension != null) {
                    return if (suspension.matchedSsid == identity.ssid) {
                        val desiredState = suspensionTarget
                        if (!sameDnsState(suspension.stateAppliedByAdns, desiredState)) {
                            WifiRuleDecision.BeginSuspension(
                                suspension.copy(
                                    stateAppliedByAdns = desiredState,
                                    phase = WifiSuspensionPhase.Applying,
                                    previousStateAppliedByAdns =
                                        suspension.stateAppliedByAdns,
                                ),
                                previousSuspension = suspension,
                            )
                        } else {
                            WifiRuleDecision.NoChange(
                                WifiRuleStatus.Suspended(identity.ssid)
                            )
                        }
                    } else {
                        WifiRuleDecision.UpdateSuspension(
                            suspension.copy(matchedSsid = identity.ssid)
                        )
                    }
                }
                val applied = suspensionTarget
                if (sameDnsState(currentDns, applied)) {
                    return WifiRuleDecision.NoChange(
                        WifiRuleStatus.MatchedAlreadyDisabled(identity.ssid)
                    )
                }
                return WifiRuleDecision.BeginSuspension(
                    WifiRuleSuspension(
                        matchedSsid = identity.ssid,
                        restoreTarget = currentDns,
                        stateAppliedByAdns = applied,
                        phase = WifiSuspensionPhase.Applying,
                    )
                )
            }
        }
    }

    private fun reconcilePending(
        currentDns: PrivateDnsObservation,
        suspension: WifiRuleSuspension?,
    ): WifiRuleDecision? = when (suspension?.phase) {
        null,
        WifiSuspensionPhase.Active,
        -> null

        WifiSuspensionPhase.Applying -> when {
            sameDnsState(currentDns, suspension.stateAppliedByAdns) ->
                WifiRuleDecision.UpdateSuspension(
                    suspension.copy(
                        phase = WifiSuspensionPhase.Active,
                        previousStateAppliedByAdns = null,
                    )
                )
            suspension.previousStateAppliedByAdns?.let { previous ->
                sameDnsState(currentDns, previous)
            } == true -> WifiRuleDecision.UpdateSuspension(
                suspension.copy(
                    stateAppliedByAdns = requireNotNull(
                        suspension.previousStateAppliedByAdns
                    ),
                    phase = WifiSuspensionPhase.Active,
                    previousStateAppliedByAdns = null,
                )
            )
            else -> WifiRuleDecision.ClearSuspension
        }

        WifiSuspensionPhase.Restoring -> when {
            sameDnsState(currentDns, suspension.restoreTarget) ->
                WifiRuleDecision.ClearSuspension
            sameDnsState(currentDns, suspension.stateAppliedByAdns) ->
                WifiRuleDecision.Restore(suspension)
            else -> WifiRuleDecision.ClearSuspension
        }
    }

    private fun restoreDecision(suspension: WifiRuleSuspension) =
        WifiRuleDecision.Restore(
            suspension.copy(phase = WifiSuspensionPhase.Restoring)
        )

    private fun sameDnsState(
        first: PrivateDnsObservation,
        second: PrivateDnsObservation,
    ): Boolean = when {
        first is PrivateDnsObservation.Hostname && second is PrivateDnsObservation.Hostname ->
            first.value.equals(second.value, ignoreCase = true)
        else -> first == second
    }
}
