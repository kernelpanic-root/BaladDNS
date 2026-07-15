package com.kernelpanic.baladdns.data.runtime

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.kernelpanic.baladdns.data.AppRuntimeRepositories
import com.kernelpanic.baladdns.data.activation.ActivationRepositories
import com.kernelpanic.baladdns.data.wifi.WifiRulesRepositories
import com.kernelpanic.baladdns.services.DnsRuntimeService

sealed interface RuntimeServiceControlResult {
    data class Started(val plan: RuntimeServicePlan) : RuntimeServiceControlResult
    data object Stopped : RuntimeServiceControlResult
    data class Failed(val error: RuntimeException) : RuntimeServiceControlResult
}

object RuntimeServiceController {
    fun sync(
        rawContext: Context,
        wifiReasonAllowed: Boolean = true,
    ): RuntimeServiceControlResult {
        val context = rawContext.applicationContext
        ActivationRepositories.getInstance(context).refreshPermission()
        val capabilities = AppRuntimeRepositories.capabilities(context).current()
        val runtime = RuntimeMonitoringRepositories.getInstance(context)
        val rules = WifiRulesRepositories.getInstance(context)
        val hasFineLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        val locationManager = context.getSystemService(LocationManager::class.java)
        val locationServicesEnabled = locationManager == null ||
            LocationManagerCompat.isLocationEnabled(locationManager)
        val plan = deriveRuntimeServicePlan(
            preferences = runtime.preferences.value,
            canRunRuntimeMonitor = capabilities.canRunRuntimeMonitor,
            canUseWifiRules = capabilities.canUseWifiRules &&
                hasFineLocation &&
                locationServicesEnabled,
            wifiReasonAllowed = wifiReasonAllowed,
            hasPendingWifiSuspension = rules.state.value.suspension != null,
        )
        if (!plan.shouldRun) {
            context.stopService(Intent(context, DnsRuntimeService::class.java))
            runtime.setServiceRunning(false)
            runtime.setServiceFailure(null)
            RuntimeNotificationFactory(context).cancel()
            return RuntimeServiceControlResult.Stopped
        }
        return try {
            val intent = Intent(context, DnsRuntimeService::class.java).apply {
                action = DnsRuntimeService.ACTION_SYNC
                putExtra(DnsRuntimeService.EXTRA_WIFI_REASON_ALLOWED, wifiReasonAllowed)
            }
            ContextCompat.startForegroundService(context, intent)
            runtime.setServiceFailure(null)
            RuntimeServiceControlResult.Started(plan)
        } catch (error: RuntimeException) {
            runtime.setServiceRunning(false)
            runtime.setServiceFailure(classifyRuntimeServiceFailure(error))
            Log.e(TAG, "Unable to start the DNS runtime service", error)
            RuntimeServiceControlResult.Failed(error)
        }
    }

    fun syncFromBoot(context: Context): RuntimeServiceControlResult {
        val hasFineLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        val hasBackgroundLocation = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED
        val locationManager = context.getSystemService(LocationManager::class.java)
        val locationServicesEnabled = locationManager == null ||
            LocationManagerCompat.isLocationEnabled(locationManager)
        return sync(
            rawContext = context,
            wifiReasonAllowed = canResumeWifiRulesFromBoot(
                sdkInt = Build.VERSION.SDK_INT,
                hasFineLocation = hasFineLocation,
                hasBackgroundLocation = hasBackgroundLocation,
                locationServicesEnabled = locationServicesEnabled,
            ),
        )
    }

    private const val TAG = "RuntimeService"
}
