package com.eyalm.adns

import android.app.Application
import com.eyalm.adns.data.Locales
import com.eyalm.adns.data.network.ApiClient
import com.eyalm.adns.data.runtime.RuntimeMonitoringRepositories
import com.eyalm.adns.data.wifi.WifiRulesRepositories

class AdnsApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ApiClient.init(this)
        Locales.init(this)
        RuntimeMonitoringRepositories.getInstance(this)
        WifiRulesRepositories.getInstance(this)
    }
}
