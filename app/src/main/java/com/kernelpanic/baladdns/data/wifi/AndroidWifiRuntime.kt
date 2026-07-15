package com.kernelpanic.baladdns.data.wifi

import android.content.Context
import com.kernelpanic.baladdns.data.dns.AndroidPrivateDnsSettings
import com.kernelpanic.baladdns.data.dns.DnsDisableBehaviorRepositories
import com.kernelpanic.baladdns.data.dns.PrivateDnsController

object WifiRuleCoordinators {
    @Volatile
    private var instance: WifiRuleCoordinator? = null

    fun getInstance(context: Context): WifiRuleCoordinator = instance ?: synchronized(this) {
        val disableBehaviorRepository = DnsDisableBehaviorRepositories.getInstance(context)
        instance ?: WifiRuleCoordinator(
            repository = WifiRulesRepositories.getInstance(context),
            privateDnsControl = PrivateDnsController(
                AndroidPrivateDnsSettings(context.applicationContext.contentResolver)
            ),
            disableBehavior = { disableBehaviorRepository.behavior.value },
        ).also { instance = it }
    }
}

object ConnectedWifiObservers {
    @Volatile
    private var instance: ConnectedWifiObserver? = null

    fun getInstance(context: Context): ConnectedWifiObserver = instance ?: synchronized(this) {
        instance ?: AndroidConnectedWifiObserver(context.applicationContext)
            .also { instance = it }
    }
}
