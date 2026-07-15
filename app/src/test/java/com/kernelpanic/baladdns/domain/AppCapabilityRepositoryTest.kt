package com.kernelpanic.baladdns.domain

import com.kernelpanic.baladdns.data.activation.ActivationMode
import com.kernelpanic.baladdns.data.activation.ActivationState
import com.kernelpanic.baladdns.data.activation.PermissionState
import com.kernelpanic.baladdns.data.nextdns.auth.NextDnsManagementSession
import com.kernelpanic.baladdns.data.provider.DnsProviderSelection
import com.kernelpanic.baladdns.data.provider.ProviderId
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
