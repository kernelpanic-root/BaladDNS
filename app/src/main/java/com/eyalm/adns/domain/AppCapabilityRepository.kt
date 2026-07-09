package com.eyalm.adns.domain

import com.eyalm.adns.data.activation.ActivationState
import com.eyalm.adns.data.nextdns.auth.NextDnsManagementSession
import com.eyalm.adns.data.provider.DnsProviderSelection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class AppCapabilityRepository(
    private val activation: StateFlow<ActivationState>,
    private val provider: StateFlow<DnsProviderSelection>,
    private val resolvedHostname: StateFlow<String?>,
    private val nextDnsSession: StateFlow<NextDnsManagementSession>,
    scope: CoroutineScope,
) {
    val state: StateFlow<AppCapabilities> = combine(
        activation,
        provider,
        resolvedHostname,
        nextDnsSession,
    ) { activationState, providerSelection, hostname, session ->
        deriveAppCapabilities(
            AppCapabilityInputs(
                activation = activationState,
                provider = providerSelection,
                nextDnsSessionActive = session == NextDnsManagementSession.Active,
                nextDnsProfileSelected = hostname != null &&
                    providerSelection is DnsProviderSelection.Enhanced,
            )
        )
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = current(),
    )

    fun current(): AppCapabilities = deriveAppCapabilities(
        AppCapabilityInputs(
            activation = activation.value,
            provider = provider.value,
            nextDnsSessionActive = nextDnsSession.value == NextDnsManagementSession.Active,
            nextDnsProfileSelected = resolvedHostname.value != null &&
                provider.value is DnsProviderSelection.Enhanced,
        )
    )
}
