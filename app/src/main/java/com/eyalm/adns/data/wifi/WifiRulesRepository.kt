package com.eyalm.adns.data.wifi

import com.eyalm.adns.data.PrivateDnsObservation
import com.eyalm.adns.data.provider.PrivateDnsHostname
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class StoredWifiRulesSnapshot(
    val configuration: WifiRulesConfiguration = WifiRulesConfiguration(),
    val suspension: WifiRuleSuspension? = null,
    val relinquishedSsid: WifiSsid? = null,
)

interface WifiRulesStore {
    fun read(): StoredWifiRulesSnapshot

    fun write(snapshot: StoredWifiRulesSnapshot)
}

class WifiRulesRepository(
    private val store: WifiRulesStore,
) {
    private val _state = MutableStateFlow(store.read().toState())
    val state: StateFlow<WifiRulesState> = _state.asStateFlow()

    @Synchronized
    fun add(ssid: WifiSsid): Boolean {
        if (ssid in _state.value.configuration.ssids) return false
        publish(
            _state.value.copy(
                configuration = _state.value.configuration.copy(
                    ssids = _state.value.configuration.ssids + ssid
                )
            )
        )
        return true
    }

    @Synchronized
    fun remove(ssid: WifiSsid): Boolean {
        if (ssid !in _state.value.configuration.ssids) return false
        publish(
            _state.value.copy(
                configuration = _state.value.configuration.copy(
                    ssids = _state.value.configuration.ssids - ssid
                )
            )
        )
        return true
    }

    @Synchronized
    fun setSuspension(suspension: WifiRuleSuspension?) {
        publish(_state.value.copy(suspension = suspension))
    }

    @Synchronized
    fun setRelinquishedSsid(ssid: WifiSsid?) {
        publish(_state.value.copy(relinquishedSsid = ssid))
    }

    @Synchronized
    fun updateRestoreHostname(hostname: String?) {
        val normalized = PrivateDnsHostname.parsePreservingCase(hostname)?.ascii ?: return
        val suspension = _state.value.suspension ?: return
        if (suspension.restoreTarget !is PrivateDnsObservation.Hostname) return
        setSuspension(
            suspension.copy(restoreTarget = PrivateDnsObservation.Hostname(normalized))
        )
    }

    private fun publish(value: WifiRulesState) {
        store.write(
            StoredWifiRulesSnapshot(
                configuration = value.configuration,
                suspension = value.suspension,
                relinquishedSsid = value.relinquishedSsid,
            )
        )
        _state.value = value
    }

    private fun StoredWifiRulesSnapshot.toState() = WifiRulesState(
        configuration = configuration,
        suspension = suspension,
        relinquishedSsid = relinquishedSsid,
    )
}
