package com.eyalm.adns.data.wifi

import com.eyalm.adns.data.PrivateDnsObservation
import com.eyalm.adns.data.dns.DnsDisableBehavior
import com.eyalm.adns.data.dns.DnsWriteResult
import com.eyalm.adns.data.dns.PrivateDnsControl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class WifiRuleCoordinator(
    private val repository: WifiRulesRepository,
    private val privateDnsControl: PrivateDnsControl,
    private val disableBehavior: () -> DnsDisableBehavior,
    private val engine: WifiRuleEngine = WifiRuleEngine(),
) {
    private val mutex = Mutex()
    private val _status = MutableStateFlow<WifiRuleStatus>(WifiRuleStatus.Disabled)
    val status: StateFlow<WifiRuleStatus> = _status.asStateFlow()

    suspend fun reconcile(
        enabled: Boolean,
        canControl: Boolean,
        identity: ConnectedWifiIdentity,
    ): WifiRuleStatus = mutex.withLock {
        repeat(MAX_TRANSITIONS_PER_EVENT) {
            val current = privateDnsControl.observe()
            when (
                val decision = engine.evaluate(
                    enabled = enabled,
                    canControl = canControl,
                    configuration = repository.state.value.configuration,
                    identity = identity,
                    currentDns = current,
                    suspensionTarget = disabledState(),
                    suspension = repository.state.value.suspension,
                    relinquishedSsid = repository.state.value.relinquishedSsid,
                )
            ) {
                is WifiRuleDecision.NoChange -> return@withLock publish(decision.status)
                is WifiRuleDecision.UpdateSuspension -> {
                    repository.setSuspension(decision.suspension)
                }
                WifiRuleDecision.ClearSuspension -> {
                    repository.setSuspension(null)
                }
                WifiRuleDecision.ClearRelinquishedSsid -> {
                    repository.setRelinquishedSsid(null)
                }
                is WifiRuleDecision.RelinquishOwnership -> {
                    repository.setSuspension(null)
                    repository.setRelinquishedSsid(decision.ssid)
                    return@withLock publish(
                        if (decision.ssid != null) {
                            WifiRuleStatus.ExternalChangeDetected
                        } else {
                            WifiRuleStatus.Monitoring
                        }
                    )
                }
                is WifiRuleDecision.BeginSuspension -> {
                    repository.setSuspension(decision.suspension)
                    val result = write(decision.suspension.stateAppliedByAdns)
                    if (result is DnsWriteResult.Success) {
                        repository.setSuspension(
                            decision.suspension.copy(
                                phase = WifiSuspensionPhase.Active,
                                previousStateAppliedByAdns = null,
                            )
                        )
                    } else {
                        reconcileFailedWrite(
                            suspension = decision.suspension,
                            applying = true,
                            previousSuspension = decision.previousSuspension,
                        )
                        return@withLock publish(WifiRuleStatus.WriteFailed)
                    }
                }
                is WifiRuleDecision.Restore -> {
                    repository.setSuspension(decision.suspension)
                    val result = write(decision.suspension.restoreTarget)
                    if (result is DnsWriteResult.Success) {
                        repository.setSuspension(null)
                    } else {
                        reconcileFailedWrite(decision.suspension, applying = false)
                        return@withLock publish(WifiRuleStatus.WriteFailed)
                    }
                }
            }
        }
        publish(WifiRuleStatus.WriteFailed)
    }

    suspend fun updateRestoreHostname(hostname: String?) = mutex.withLock {
        repository.updateRestoreHostname(hostname)
    }

    private suspend fun write(target: PrivateDnsObservation): DnsWriteResult = when (target) {
        PrivateDnsObservation.Automatic ->
            privateDnsControl.disable(DnsDisableBehavior.Automatic)
        PrivateDnsObservation.Off -> privateDnsControl.disable(DnsDisableBehavior.Off)
        PrivateDnsObservation.PermissionMissing -> DnsWriteResult.PermissionMissing
        is PrivateDnsObservation.Hostname -> privateDnsControl.enable(target.value)
    }

    private fun disabledState(): PrivateDnsObservation = when (disableBehavior()) {
        DnsDisableBehavior.Automatic -> PrivateDnsObservation.Automatic
        DnsDisableBehavior.Off -> PrivateDnsObservation.Off
    }

    private fun reconcileFailedWrite(
        suspension: WifiRuleSuspension,
        applying: Boolean,
        previousSuspension: WifiRuleSuspension? = null,
    ) {
        val observed = privateDnsControl.observe()
        when {
            sameDnsState(observed, suspension.stateAppliedByAdns) -> repository.setSuspension(
                suspension.copy(
                    phase = if (applying) {
                        WifiSuspensionPhase.Active
                    } else {
                        WifiSuspensionPhase.Restoring
                    },
                    previousStateAppliedByAdns = null,
                )
            )
            applying &&
                previousSuspension != null &&
                sameDnsState(observed, previousSuspension.stateAppliedByAdns) ->
                repository.setSuspension(previousSuspension)
            else -> repository.setSuspension(null)
        }
    }

    private fun publish(value: WifiRuleStatus): WifiRuleStatus {
        _status.value = value
        return value
    }

    private fun sameDnsState(
        first: PrivateDnsObservation,
        second: PrivateDnsObservation,
    ): Boolean = when {
        first is PrivateDnsObservation.Hostname && second is PrivateDnsObservation.Hostname ->
            first.value.equals(second.value, ignoreCase = true)
        else -> first == second
    }

    companion object {
        private const val MAX_TRANSITIONS_PER_EVENT = 5
    }
}
