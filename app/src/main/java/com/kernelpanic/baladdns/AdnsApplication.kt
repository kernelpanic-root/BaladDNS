package com.kernelpanic.baladdns

import android.app.Application
import com.kernelpanic.baladdns.data.Locales
import com.kernelpanic.baladdns.data.network.ApiClient
import com.kernelpanic.baladdns.data.runtime.RuntimeMonitoringRepositories
import com.kernelpanic.baladdns.data.wifi.WifiRulesRepositories

class AdnsApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ApiClient.init(this)
        Locales.init(this)
        RuntimeMonitoringRepositories.getInstance(this)
        WifiRulesRepositories.getInstance(this)
    }
}
