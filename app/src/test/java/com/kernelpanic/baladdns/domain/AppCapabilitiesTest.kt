package com.kernelpanic.baladdns.domain

import com.kernelpanic.baladdns.data.activation.ActivationMode
import com.kernelpanic.baladdns.data.activation.ActivationState
import com.kernelpanic.baladdns.data.activation.PermissionState
import com.kernelpanic.baladdns.data.provider.DnsProviderSelection
import com.kernelpanic.baladdns.data.provider.ProviderId
import com.kernelpanic.baladdns.data.provider.ResolverPresetId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppCapabilitiesTest {
    @Test
    fun `privileged mode exposes dns controls and home`() {
        val capabilities = deriveAppCapabilities(
            AppCapabilityInputs(
                activation = ActivationState(
                    onboardingComplete = true,
                    mode = ActivationMode.PrivilegedDnsControl,
                    permission = PermissionState.Granted,
                ),
                provider = standardProvider,
                nextDnsSessionActive = false,
                nextDnsProfileSelected = false,
            )
        )

        assertTrue(capabilities.canControlPrivateDns)
        assertTrue(capabilities.canUseDnsToggleSurfaces)
        assertTrue(capabilities.showHome)
        assertFalse(capabilities.showStats)
        assertEquals(MainTab.Home, capabilities.defaultTab)
        assertEquals(AppDestination.Main(MainTab.Home), capabilities.startupDestination)
    }

    @Test
    fun `permission loss keeps privileged intent and routes to activation`() {
        val capabilities = deriveAppCapabilities(
            AppCapabilityInputs(
                activation = ActivationState(
                    onboardingComplete = true,
                    mode = ActivationMode.PrivilegedDnsControl,
                    permission = PermissionState.Missing,
                ),
                provider = standardProvider,
                nextDnsSessionActive = false,
                nextDnsProfileSelected = false,
            )
        )

        assertFalse(capabilities.canControlPrivateDns)
        assertTrue(capabilities.showHome)
        assertTrue(capabilities.showActivationWarning)
        assertEquals(AppDestination.Activation, capabilities.startupDestination)
    }

    @Test
    fun `nextdns control-only hides home and defaults to settings`() {
        val capabilities = deriveAppCapabilities(
            AppCapabilityInputs(
                activation = ActivationState(
                    onboardingComplete = true,
                    mode = ActivationMode.NextDnsControlOnly,
                    permission = PermissionState.Missing,
                ),
                provider = DnsProviderSelection.Enhanced(ProviderId("nextdns")),
                nextDnsSessionActive = true,
                nextDnsProfileSelected = true,
            )
        )

        assertFalse(capabilities.canControlPrivateDns)
        assertFalse(capabilities.canUseDnsToggleSurfaces)
        assertFalse(capabilities.showHome)
        assertTrue(capabilities.showStats)
        assertTrue(capabilities.canManageNextDns)
        assertEquals(MainTab.Settings, capabilities.defaultTab)
        assertEquals(
            listOf(MainTab.Settings, MainTab.Stats),
            capabilities.visibleTabs,
        )
        assertEquals(AppDestination.Main(MainTab.Settings), capabilities.startupDestination)
    }

    @Test
    fun `unfinished onboarding always routes to onboarding`() {
        val capabilities = deriveAppCapabilities(
            AppCapabilityInputs(
                activation = ActivationState(
                    onboardingComplete = false,
                    mode = null,
                    permission = PermissionState.Missing,
                ),
                provider = standardProvider,
                nextDnsSessionActive = false,
                nextDnsProfileSelected = false,
            )
        )

        assertEquals(AppDestination.Onboarding, capabilities.startupDestination)
    }

    @Test
    fun `unavailable current tab falls back to capability default`() {
        val capabilities = deriveAppCapabilities(
            AppCapabilityInputs(
                activation = ActivationState(
                    onboardingComplete = true,
                    mode = ActivationMode.NextDnsControlOnly,
                    permission = PermissionState.Missing,
                ),
                provider = DnsProviderSelection.Enhanced(ProviderId("nextdns")),
                nextDnsSessionActive = true,
                nextDnsProfileSelected = true,
            )
        )

        assertEquals(
            MainTab.Settings,
            resolveAvailableMainTab(MainTab.Home, capabilities),
        )
        assertEquals(
            MainTab.Stats,
            resolveAvailableMainTab(MainTab.Stats, capabilities),
        )
    }

    private companion object {
        val standardProvider = DnsProviderSelection.Standard(
            ProviderId("adguard"),
            ResolverPresetId("default"),
        )
    }
}
