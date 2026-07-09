package com.eyalm.adns.domain

import com.eyalm.adns.data.activation.ActivationMode
import com.eyalm.adns.data.activation.ActivationState
import com.eyalm.adns.data.activation.PermissionState
import com.eyalm.adns.data.nextdns.auth.NextDnsManagementSession
import com.eyalm.adns.data.provider.DnsProviderSelection
import com.eyalm.adns.data.provider.ProviderId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppCapabilityRepositoryTest {
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `repository reacts to permission and management-session changes`() = runTest {
        val activation = MutableStateFlow(
            ActivationState(
                onboardingComplete = true,
                mode = ActivationMode.NextDnsControlOnly,
                permission = PermissionState.Missing,
            )
        )
        val provider = MutableStateFlow<DnsProviderSelection>(
            DnsProviderSelection.Enhanced(ProviderId("nextdns"))
        )
        val hostname = MutableStateFlow<String?>("profile.dns.nextdns.io")
        val session = MutableStateFlow<NextDnsManagementSession>(
            NextDnsManagementSession.Active
        )
        val repository = AppCapabilityRepository(
            activation = activation,
            provider = provider,
            resolvedHostname = hostname,
            nextDnsSession = session,
            scope = backgroundScope,
        )

        runCurrent()
        assertTrue(repository.state.value.canManageNextDns)

        session.value = NextDnsManagementSession.Expired
        assertFalse(repository.current().canManageNextDns)
        runCurrent()

        assertFalse(repository.state.value.canManageNextDns)
        assertEquals(MainTab.Settings, repository.state.value.defaultTab)
    }
}
