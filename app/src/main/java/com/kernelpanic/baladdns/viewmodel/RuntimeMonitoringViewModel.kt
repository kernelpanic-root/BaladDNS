package com.kernelpanic.baladdns.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kernelpanic.baladdns.data.AppRuntimeRepositories
import com.kernelpanic.baladdns.data.runtime.AndroidRuntimeMonitoringSystemStateReader
import com.kernelpanic.baladdns.data.runtime.RuntimeMonitoringRepositories
import com.kernelpanic.baladdns.data.runtime.RuntimeMonitoringState
import com.kernelpanic.baladdns.data.runtime.RuntimeNotificationFactory
import com.kernelpanic.baladdns.data.runtime.RuntimeServiceController
import com.kernelpanic.baladdns.data.runtime.RuntimeServiceFailure
import com.kernelpanic.baladdns.data.runtime.deriveRuntimeMonitoringState
import com.kernelpanic.baladdns.data.wifi.ConnectedWifiIdentity
import com.kernelpanic.baladdns.data.wifi.ConnectedWifiObservers
import com.kernelpanic.baladdns.data.wifi.WifiRuleCoordinators
import com.kernelpanic.baladdns.data.wifi.WifiRuleStatus
import com.kernelpanic.baladdns.data.wifi.WifiRulesRepositories
import com.kernelpanic.baladdns.data.wifi.WifiRulesState
import com.kernelpanic.baladdns.data.wifi.WifiSsid
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class RuntimeMonitoringViewModel(application: Application) : AndroidViewModel(application) {
    private val app: Application
        get() = getApplication()
    private val runtimeRepository = RuntimeMonitoringRepositories.getInstance(app)
    private val rulesRepository = WifiRulesRepositories.getInstance(app)
    private val capabilitiesRepository = AppRuntimeRepositories.capabilities(app)
    private val wifiCoordinator = WifiRuleCoordinators.getInstance(app)
    private val wifiObserver = ConnectedWifiObservers.getInstance(app)
    private val systemStateReader = AndroidRuntimeMonitoringSystemStateReader(app)
    private val notificationFactory = RuntimeNotificationFactory(app)
    private val _systemState = MutableStateFlow(systemStateReader.read())
    private val _wifiIdentity = MutableStateFlow(wifiObserver.current())
    private var wifiIdentityJob: Job? = null
    private val serviceRuntime = combine(
        runtimeRepository.serviceRunning,
        runtimeRepository.serviceFailure,
    ) { running, failure -> ServiceRuntime(running, failure) }

    val wifiIdentity: StateFlow<ConnectedWifiIdentity> = _wifiIdentity.asStateFlow()
    val wifiRulesState: StateFlow<WifiRulesState> = rulesRepository.state
    val wifiStatus: StateFlow<WifiRuleStatus> = wifiCoordinator.status

    val runtimeState: StateFlow<RuntimeMonitoringState> = combine(
        runtimeRepository.preferences,
        serviceRuntime,
        capabilitiesRepository.state,
        _systemState,
        _wifiIdentity,
    ) { preferences, service, capabilities, system, identity ->
        deriveRuntimeMonitoringState(
            preferences = preferences,
            system = system.copy(
                serviceRunning = service.running,
                serviceFailure = service.failure,
            ),
            canRunRuntimeMonitor = capabilities.canRunRuntimeMonitor,
            canUseWifiRules = capabilities.canUseWifiRules &&
                identity != ConnectedWifiIdentity.PermissionRequired &&
                identity != ConnectedWifiIdentity.LocationServicesDisabled,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = deriveRuntimeMonitoringState(
            preferences = runtimeRepository.preferences.value,
            system = _systemState.value.copy(
                serviceRunning = runtimeRepository.serviceRunning.value,
                serviceFailure = runtimeRepository.serviceFailure.value,
            ),
            canRunRuntimeMonitor = capabilitiesRepository.current().canRunRuntimeMonitor,
            canUseWifiRules = capabilitiesRepository.current().canUseWifiRules &&
                _wifiIdentity.value != ConnectedWifiIdentity.PermissionRequired &&
                _wifiIdentity.value != ConnectedWifiIdentity.LocationServicesDisabled,
        ),
    )

    fun refreshSystemState() {
        _systemState.value = systemStateReader.read(runtimeRepository.serviceRunning.value)
        restartWifiIdentityObservationIfActive()
        refreshWifiIdentity()
        RuntimeServiceController.sync(app)
    }

    fun refreshWifiIdentity(): ConnectedWifiIdentity = wifiObserver.current().also {
        _wifiIdentity.value = it
    }

    fun startWifiIdentityObservation() {
        if (wifiIdentityJob?.isActive == true) return
        wifiIdentityJob = viewModelScope.launch {
            wifiObserver.identity.collect { identity ->
                _wifiIdentity.value = identity
            }
        }
    }

    fun stopWifiIdentityObservation() {
        wifiIdentityJob?.cancel()
        wifiIdentityJob = null
    }

    fun setStateNotificationEnabled(enabled: Boolean) {
        if (enabled) notificationFactory.ensureChannel()
        runtimeRepository.setStateNotificationEnabled(enabled)
        RuntimeServiceController.sync(app)
        refreshSystemStateWithoutSync()
    }

    fun onNotificationPermissionResult(granted: Boolean) {
        setStateNotificationEnabled(granted)
    }

    fun setWifiRulesEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val capabilities = capabilitiesRepository.current()
            if (!enabled) {
                runtimeRepository.setWifiRulesEnabled(false)
                wifiCoordinator.reconcile(
                    enabled = false,
                    canControl = capabilities.canUseWifiRules,
                    identity = _wifiIdentity.value,
                )
            } else {
                notificationFactory.ensureChannel()
                runtimeRepository.setWifiRulesEnabled(true)
                RuntimeServiceController.sync(app)
                wifiCoordinator.reconcile(
                    enabled = true,
                    canControl = capabilities.canUseWifiRules,
                    identity = _wifiIdentity.value,
                )
            }
            RuntimeServiceController.sync(app)
            refreshSystemStateWithoutSync()
        }
    }

    fun onWifiPermissionResult(granted: Boolean) {
        if (granted) {
            restartWifiIdentityObservationIfActive()
            refreshWifiIdentity()
            setWifiRulesEnabled(true)
        } else {
            RuntimeServiceController.sync(app)
            refreshSystemStateWithoutSync()
        }
    }

    fun addManualSsid(value: String): Boolean {
        val ssid = WifiSsid.fromUserInput(value) ?: return false
        val added = rulesRepository.add(ssid)
        if (added) reconcileRules()
        return added
    }

    fun captureCurrentSsid() {
        viewModelScope.launch {
            val identity = withTimeoutOrNull(CURRENT_WIFI_TIMEOUT_MILLIS) {
                wifiObserver.identity.first {
                    it is ConnectedWifiIdentity.Known ||
                        it == ConnectedWifiIdentity.PermissionRequired ||
                        it == ConnectedWifiIdentity.LocationServicesDisabled
                }
            } ?: wifiObserver.current()
            _wifiIdentity.value = identity
            (identity as? ConnectedWifiIdentity.Known)?.ssid?.let { ssid ->
                if (rulesRepository.add(ssid)) reconcileRules()
            }
        }
    }

    fun removeSsid(ssid: WifiSsid) {
        if (rulesRepository.remove(ssid)) reconcileRules()
    }

    private fun reconcileRules() {
        viewModelScope.launch {
            wifiCoordinator.reconcile(
                enabled = runtimeRepository.preferences.value.wifiRulesEnabled,
                canControl = capabilitiesRepository.current().canUseWifiRules,
                identity = _wifiIdentity.value,
            )
            RuntimeServiceController.sync(app)
        }
    }

    private fun refreshSystemStateWithoutSync() {
        _systemState.value = systemStateReader.read(runtimeRepository.serviceRunning.value)
    }

    private fun restartWifiIdentityObservationIfActive() {
        if (wifiIdentityJob?.isActive != true) return
        stopWifiIdentityObservation()
        startWifiIdentityObservation()
    }

    private data class ServiceRuntime(
        val running: Boolean,
        val failure: RuntimeServiceFailure?,
    )

    companion object {
        private const val CURRENT_WIFI_TIMEOUT_MILLIS = 2_000L
    }
}
