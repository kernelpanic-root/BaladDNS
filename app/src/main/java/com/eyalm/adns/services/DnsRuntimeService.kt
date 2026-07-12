package com.eyalm.adns.services

import android.Manifest
import android.app.Notification
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.annotation.RequiresApi
import com.eyalm.adns.data.AppRuntimeRepositories
import com.eyalm.adns.data.DnsRepository
import com.eyalm.adns.data.PrivateDnsObservation
import com.eyalm.adns.data.activation.ActivationRepositories
import com.eyalm.adns.data.dns.DnsDisableBehaviorRepositories
import com.eyalm.adns.data.runtime.RuntimeMonitorReason
import com.eyalm.adns.data.runtime.RuntimeMonitoringRepositories
import com.eyalm.adns.data.runtime.RuntimeNotificationFactory
import com.eyalm.adns.data.runtime.RuntimeNotifications
import com.eyalm.adns.data.runtime.RuntimeServicePlan
import com.eyalm.adns.data.runtime.deriveRuntimeServicePlan
import com.eyalm.adns.data.runtime.classifyRuntimeServiceFailure
import com.eyalm.adns.data.wifi.ConnectedWifiIdentity
import com.eyalm.adns.data.wifi.ConnectedWifiObservers
import com.eyalm.adns.data.wifi.WifiRuleCoordinators
import com.eyalm.adns.data.wifi.WifiRuleStatus
import com.eyalm.adns.data.wifi.WifiRulesRepositories
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class DnsRuntimeService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val wifiReasonAllowed = MutableStateFlow(false)
    private val dnsObservation = MutableStateFlow<PrivateDnsObservation>(
        PrivateDnsObservation.PermissionMissing
    )
    private var collectorsStarted = false
    private var stopping = false
    private var wifiWorkMode = WifiWorkMode.None
    private var wifiJob: Job? = null
    private var currentPlan = RuntimeServicePlan(emptySet(), recoveryRequired = false)
    private var selectedDnsActive = false

    private val runtimeRepository by lazy {
        RuntimeMonitoringRepositories.getInstance(applicationContext)
    }
    private val rulesRepository by lazy {
        WifiRulesRepositories.getInstance(applicationContext)
    }
    private val disableBehaviorRepository by lazy {
        DnsDisableBehaviorRepositories.getInstance(applicationContext)
    }
    private val capabilitiesRepository by lazy {
        AppRuntimeRepositories.capabilities(applicationContext)
    }
    private val wifiCoordinator by lazy {
        WifiRuleCoordinators.getInstance(applicationContext)
    }
    private val wifiObserver by lazy {
        ConnectedWifiObservers.getInstance(applicationContext)
    }
    private val dnsRepository by lazy { DnsRepository(applicationContext) }
    private val notificationFactory by lazy { RuntimeNotificationFactory(applicationContext) }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        runtimeRepository.setServiceRunning(true)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        stopping = false
        val explicitSync = intent?.action == ACTION_SYNC
        if (explicitSync) {
            wifiReasonAllowed.value = intent.getBooleanExtra(
                EXTRA_WIFI_REASON_ALLOWED,
                false,
            )
        }
        currentPlan = calculatePlan()
        if (!currentPlan.shouldRun) {
            stopRuntime()
            return START_NOT_STICKY
        }
        if (!startAsForegroundSafely(currentPlan)) return START_NOT_STICKY
        startCollectors()
        updateWifiWork(currentPlan, forceRefresh = explicitSync)
        return START_REDELIVER_INTENT
    }

    override fun onDestroy() {
        runtimeRepository.setServiceRunning(false)
        wifiJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun startCollectors() {
        if (collectorsStarted) return
        collectorsStarted = true

        serviceScope.launch {
            combine(
                runtimeRepository.preferences,
                capabilitiesRepository.state,
                rulesRepository.state,
                wifiReasonAllowed,
            ) { preferences, capabilities, rules, allowWifi ->
                deriveRuntimeServicePlan(
                    preferences = preferences,
                    canRunRuntimeMonitor = capabilities.canRunRuntimeMonitor,
                    canUseWifiRules = capabilities.canUseWifiRules && canUseLocationRuntime(),
                    wifiReasonAllowed = allowWifi,
                    hasPendingWifiSuspension = rules.suspension != null,
                )
            }.collect { plan ->
                currentPlan = plan
                if (!plan.shouldRun) {
                    stopRuntime()
                } else {
                    if (!startAsForegroundSafely(plan)) return@collect
                    updateWifiWork(plan)
                }
            }
        }

        serviceScope.launch {
            dnsRepository.getDnsRuntimeStateFlow().collect { state ->
                dnsObservation.value = state.observation
                selectedDnsActive = state.selectedResolverActive
                wifiCoordinator.updateRestoreHostname(state.selectedHostname)
                refreshNotification()
            }
        }

        serviceScope.launch {
            wifiCoordinator.status.collect {
                refreshNotification()
            }
        }
    }

    private fun updateWifiWork(
        plan: RuntimeServicePlan,
        forceRefresh: Boolean = false,
    ) {
        val wantedMode = when {
            RuntimeMonitorReason.WifiRules in plan.activeReasons -> WifiWorkMode.Monitoring
            plan.recoveryRequired -> WifiWorkMode.Recovery
            else -> WifiWorkMode.None
        }
        if (wifiWorkMode == wantedMode && !forceRefresh) return
        wifiWorkMode = wantedMode
        wifiJob?.cancel()
        wifiJob = when (wantedMode) {
            WifiWorkMode.None -> null
            WifiWorkMode.Recovery -> serviceScope.launch {
                val status = wifiCoordinator.reconcile(
                    enabled = false,
                    canControl = capabilitiesRepository.current().canUseWifiRules,
                    identity = ConnectedWifiIdentity.RedactedOrUnknown,
                )
                refreshActivationIfNeeded(status)
            }
            WifiWorkMode.Monitoring -> serviceScope.launch wifiMonitoring@ {
                val ruleChanges = combine(
                    rulesRepository.state.map { it.configuration }.distinctUntilChanged(),
                    disableBehaviorRepository.behavior,
                ) { _, _ -> Unit }
                combine(
                    wifiObserver.identity,
                    ruleChanges,
                    runtimeRepository.preferences,
                    capabilitiesRepository.state,
                    dnsObservation,
                ) { identity, _, preferences, capabilities, _ ->
                    WifiInput(
                        identity = identity,
                        enabled = preferences.wifiRulesEnabled,
                        canControl = capabilities.canUseWifiRules,
                    )
                }.collect { input ->
                    val status = wifiCoordinator.reconcile(
                        enabled = input.enabled,
                        canControl = input.canControl,
                        identity = input.identity,
                    )
                    refreshActivationIfNeeded(status)
                    if (!hasFineLocation()) {
                        val planAfterRevocation = calculatePlan()
                        currentPlan = planAfterRevocation
                        wifiWorkMode = WifiWorkMode.None
                        if (planAfterRevocation.shouldRun) {
                            if (!startAsForegroundSafely(planAfterRevocation)) {
                                return@collect
                            }
                        } else {
                            stopRuntime()
                        }
                        this@wifiMonitoring.cancel()
                    }
                }
            }
        }
    }

    private fun calculatePlan(): RuntimeServicePlan {
        val capabilities = capabilitiesRepository.current()
        return deriveRuntimeServicePlan(
            preferences = runtimeRepository.preferences.value,
            canRunRuntimeMonitor = capabilities.canRunRuntimeMonitor,
            canUseWifiRules = capabilities.canUseWifiRules && canUseLocationRuntime(),
            wifiReasonAllowed = wifiReasonAllowed.value,
            hasPendingWifiSuspension = rulesRepository.state.value.suspension != null,
        )
    }

    private fun startAsForeground(plan: RuntimeServicePlan) {
        val notification = buildNotification(plan)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                RuntimeNotifications.NOTIFICATION_ID,
                notification,
                foregroundServiceType(plan),
            )
        } else {
            startForeground(RuntimeNotifications.NOTIFICATION_ID, notification)
        }
    }

    private fun startAsForegroundSafely(plan: RuntimeServicePlan): Boolean = try {
        startAsForeground(plan)
        runtimeRepository.setServiceFailure(null)
        true
    } catch (error: RuntimeException) {
        runtimeRepository.setServiceFailure(classifyRuntimeServiceFailure(error))
        android.util.Log.e(TAG, "Unable to enter foreground runtime mode", error)
        stopRuntime()
        false
    }

    private fun refreshNotification() {
        if (!currentPlan.shouldRun || stopping) return
        val notification = buildNotification(currentPlan)
        getSystemService(android.app.NotificationManager::class.java)?.notify(
            RuntimeNotifications.NOTIFICATION_ID,
            notification,
        )
    }

    private fun buildNotification(plan: RuntimeServicePlan): Notification =
        notificationFactory.build(
            reasons = plan.activeReasons,
            selectedDnsActive = selectedDnsActive,
            wifiStatus = wifiCoordinator.status.value,
            canToggle = capabilitiesRepository.current().canUseDnsToggleSurfaces,
        )

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun foregroundServiceType(plan: RuntimeServicePlan): Int {
        var type = 0
        val needsLocation = (
            RuntimeMonitorReason.WifiRules in plan.activeReasons || plan.recoveryRequired
        ) && hasFineLocation()
        if (needsLocation) {
            type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        }
        return type
    }

    private fun hasFineLocation(): Boolean = ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.ACCESS_FINE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED

    private fun canUseLocationRuntime(): Boolean {
        if (!hasFineLocation()) return false
        val locationManager = getSystemService(LocationManager::class.java)
        return locationManager == null || LocationManagerCompat.isLocationEnabled(locationManager)
    }

    private fun refreshActivationIfNeeded(status: WifiRuleStatus) {
        if (status == WifiRuleStatus.ActivationRequired) {
            ActivationRepositories.getInstance(applicationContext).refreshPermission()
        }
    }

    private fun stopRuntime() {
        if (stopping) return
        stopping = true
        runtimeRepository.setServiceRunning(false)
        stopForeground(STOP_FOREGROUND_REMOVE)
        notificationFactory.cancel()
        stopSelf()
    }

    private enum class WifiWorkMode {
        None,
        Monitoring,
        Recovery,
    }

    private data class WifiInput(
        val identity: ConnectedWifiIdentity,
        val enabled: Boolean,
        val canControl: Boolean,
    )

    companion object {
        private const val TAG = "DnsRuntimeService"
        const val ACTION_SYNC = "com.eyalm.adns.action.SYNC_RUNTIME_MONITOR"
        const val EXTRA_WIFI_REASON_ALLOWED = "wifi_reason_allowed"
    }
}
