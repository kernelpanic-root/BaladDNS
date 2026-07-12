package com.eyalm.adns.data.wifi

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.location.LocationManagerCompat
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

interface ConnectedWifiObserver {
    val identity: Flow<ConnectedWifiIdentity>

    fun current(): ConnectedWifiIdentity
}

class AndroidConnectedWifiObserver(
    rawContext: Context,
) : ConnectedWifiObserver {
    private val context = rawContext.applicationContext
    private val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
    private val wifiManager = context.getSystemService(WifiManager::class.java)
    private val locationManager = context.getSystemService(LocationManager::class.java)
    @Volatile
    private var lastIdentity: ConnectedWifiIdentity? = null

    override val identity: Flow<ConnectedWifiIdentity> = callbackFlow {
        val knownNetworks = mutableMapOf<Network, NetworkCapabilities>()
        var pendingLoss: Job? = null
        var pendingUnknownIdentity: Job? = null
        fun publish(value: ConnectedWifiIdentity) {
            lastIdentity = value
            trySend(value)
        }
        fun publishStabilized(value: ConnectedWifiIdentity) {
            if (value == ConnectedWifiIdentity.RedactedOrUnknown) {
                if (pendingUnknownIdentity?.isActive != true) {
                    pendingUnknownIdentity = launch {
                        delay(UNKNOWN_IDENTITY_GRACE_MILLIS.milliseconds)
                        publish(value)
                    }
                }
            } else {
                pendingUnknownIdentity?.cancel()
                pendingUnknownIdentity = null
                publish(value)
            }
        }
        val callback = createCallback(
            onCapabilities = { network, capabilities ->
                pendingLoss?.cancel()
                synchronized(knownNetworks) { knownNetworks[network] = capabilities }
                publishStabilized(fromCapabilities(capabilities))
            },
            onLost = { network ->
                val remaining = synchronized(knownNetworks) {
                    knownNetworks.remove(network)
                    knownNetworks.values.lastOrNull()
                }
                if (remaining != null) {
                    publishStabilized(fromCapabilities(remaining))
                } else {
                    pendingLoss?.cancel()
                    pendingLoss = launch {
                        delay(NETWORK_LOSS_GRACE_MILLIS.milliseconds)
                        publishStabilized(currentDefaultNetwork())
                    }
                }
            },
        )
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()
        try {
            connectivityManager?.registerNetworkCallback(request, callback)
            publishStabilized(current())
        } catch (_: SecurityException) {
            publish(ConnectedWifiIdentity.PermissionRequired)
        } catch (_: RuntimeException) {
            publish(ConnectedWifiIdentity.RedactedOrUnknown)
        }
        awaitClose {
            pendingLoss?.cancel()
            pendingUnknownIdentity?.cancel()
            runCatching { connectivityManager?.unregisterNetworkCallback(callback) }
            lastIdentity = null
        }
    }.distinctUntilChanged()

    override fun current(): ConnectedWifiIdentity {
        if (!hasFineLocationPermission()) {
            return ConnectedWifiIdentity.PermissionRequired
        }
        if (!isLocationEnabled()) {
            return ConnectedWifiIdentity.LocationServicesDisabled
        }
        val currentDefault = currentDefaultNetwork()
        return if (currentDefault == ConnectedWifiIdentity.NotOnWifi) {
            lastIdentity?.takeIf { it is ConnectedWifiIdentity.Known } ?: currentDefault
        } else {
            currentDefault
        }
    }

    private fun fromCapabilities(
        capabilities: NetworkCapabilities,
    ): ConnectedWifiIdentity {
        if (!capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            return ConnectedWifiIdentity.NotOnWifi
        }
        if (!hasFineLocationPermission()) {
            return ConnectedWifiIdentity.PermissionRequired
        }
        if (!isLocationEnabled()) {
            return ConnectedWifiIdentity.LocationServicesDisabled
        }
        val wifiInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            capabilities.transportInfo as? WifiInfo
        } else {
            @Suppress("DEPRECATION")
            wifiManager?.connectionInfo
        }
        val ssid = try {
            WifiSsid.fromAndroid(wifiInfo?.ssid)
        } catch (_: SecurityException) {
            return ConnectedWifiIdentity.PermissionRequired
        }
        return ssid?.let(ConnectedWifiIdentity::Known)
            ?: ConnectedWifiIdentity.RedactedOrUnknown
    }

    private fun createCallback(
        onCapabilities: (Network, NetworkCapabilities) -> Unit,
        onLost: (Network) -> Unit,
    ): ConnectivityManager.NetworkCallback = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        LocationAwareNetworkCallback(onCapabilities, onLost)
    } else {
        LegacyNetworkCallback(onCapabilities, onLost)
    }

    private fun currentDefaultNetwork(): ConnectedWifiIdentity {
        val manager = connectivityManager ?: return ConnectedWifiIdentity.NotOnWifi
        val network = manager.activeNetwork ?: return ConnectedWifiIdentity.NotOnWifi
        val capabilities = try {
            manager.getNetworkCapabilities(network)
        } catch (_: SecurityException) {
            return ConnectedWifiIdentity.PermissionRequired
        } ?: return ConnectedWifiIdentity.NotOnWifi
        return fromCapabilities(capabilities)
    }

    private fun hasFineLocationPermission(): Boolean =
        context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    private fun isLocationEnabled(): Boolean = locationManager == null ||
        LocationManagerCompat.isLocationEnabled(locationManager)

    private class LegacyNetworkCallback(
        private val onCapabilities: (Network, NetworkCapabilities) -> Unit,
        private val onLostCallback: (Network) -> Unit,
    ) : ConnectivityManager.NetworkCallback() {
        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            onCapabilities(network, capabilities)
        }

        override fun onLost(network: Network) {
            onLostCallback(network)
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private class LocationAwareNetworkCallback(
        private val onCapabilities: (Network, NetworkCapabilities) -> Unit,
        private val onLostCallback: (Network) -> Unit,
    ) : ConnectivityManager.NetworkCallback(FLAG_INCLUDE_LOCATION_INFO) {
        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            onCapabilities(network, capabilities)
        }

        override fun onLost(network: Network) {
            onLostCallback(network)
        }
    }

    companion object {
        private const val NETWORK_LOSS_GRACE_MILLIS = 1_500L
        private const val UNKNOWN_IDENTITY_GRACE_MILLIS = 750L
    }
}
