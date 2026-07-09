package com.eyalm.adns.data

import android.content.Context
import com.eyalm.adns.data.activation.ActivationRepositories
import com.eyalm.adns.data.nextdns.auth.NextDnsSessionManager
import com.eyalm.adns.data.provider.ProviderSelectionRepositories
import com.eyalm.adns.domain.AppCapabilityRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

object AppRuntimeRepositories {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Volatile
    private var capabilities: AppCapabilityRepository? = null

    fun capabilities(context: Context): AppCapabilityRepository {
        val appContext = context.applicationContext
        return capabilities ?: synchronized(this) {
            capabilities ?: AppCapabilityRepository(
                activation = ActivationRepositories.getInstance(appContext).state,
                provider = ProviderSelectionRepositories.getInstance(appContext).selection,
                resolvedHostname = ProviderSelectionRepositories
                    .getInstance(appContext)
                    .resolvedHostname,
                nextDnsSession = NextDnsSessionManager.getInstance(appContext).state,
                scope = scope,
            ).also { capabilities = it }
        }
    }
}
