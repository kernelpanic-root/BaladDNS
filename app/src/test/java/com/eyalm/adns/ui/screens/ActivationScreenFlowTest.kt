package com.eyalm.adns.ui.screens

import com.eyalm.adns.data.activation.ActivationMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ActivationScreenFlowTest {
    @Test
    fun `reactivation opens method selection and chosen method`() {
        assertEquals(
            ActivationScreenPage.Method,
            reduceActivationScreenPage(
                ActivationScreenPage.Overview,
                ActivationScreenIntent.Reactivate,
            ),
        )
        assertEquals(
            ActivationScreenPage.Adb,
            reduceActivationScreenPage(
                ActivationScreenPage.Method,
                ActivationScreenIntent.UseAdb,
            ),
        )
        assertEquals(
            ActivationScreenPage.Shizuku,
            reduceActivationScreenPage(
                ActivationScreenPage.Method,
                ActivationScreenIntent.UseShizuku,
            ),
        )
    }

    @Test
    fun `back follows the active activation branch`() {
        assertEquals(
            ActivationScreenPage.Method,
            reduceActivationScreenPage(
                ActivationScreenPage.Adb,
                ActivationScreenIntent.Back,
            ),
        )
        assertEquals(
            ActivationScreenPage.Method,
            reduceActivationScreenPage(
                ActivationScreenPage.Shizuku,
                ActivationScreenIntent.Back,
            ),
        )
        assertEquals(
            ActivationScreenPage.Overview,
            reduceActivationScreenPage(
                ActivationScreenPage.Method,
                ActivationScreenIntent.Back,
            ),
        )
    }

    @Test
    fun `permission grant after an activation attempt opens success`() {
        assertEquals(
            ActivationScreenPage.Success,
            reduceActivationScreenPage(
                ActivationScreenPage.Adb,
                ActivationScreenIntent.PermissionGranted,
            ),
        )
        assertEquals(
            ActivationScreenPage.Overview,
            reduceActivationScreenPage(
                ActivationScreenPage.Overview,
                ActivationScreenIntent.PermissionGranted,
            ),
        )
    }

    @Test
    fun `forced activation can fall back only for an eligible NextDNS session`() {
        assertEquals(
            ActivationExitPolicy.SwitchToControlOnly,
            forcedActivationExitPolicy(controlOnlyEligible = true),
        )
        assertEquals(
            ActivationExitPolicy.Blocked,
            forcedActivationExitPolicy(controlOnlyEligible = false),
        )
    }

    @Test
    fun `control-only switch is hidden after activation outside debug builds`() {
        assertFalse(
            shouldOfferControlOnlySwitch(
                mode = ActivationMode.PrivilegedDnsControl,
                controlOnlyEligible = true,
                canControlPrivateDns = true,
                isDebugBuild = false,
            )
        )
        assertTrue(
            shouldOfferControlOnlySwitch(
                mode = ActivationMode.PrivilegedDnsControl,
                controlOnlyEligible = true,
                canControlPrivateDns = true,
                isDebugBuild = true,
            )
        )
        assertTrue(
            shouldOfferControlOnlySwitch(
                mode = ActivationMode.PrivilegedDnsControl,
                controlOnlyEligible = true,
                canControlPrivateDns = false,
                isDebugBuild = false,
            )
        )
        assertFalse(
            shouldOfferControlOnlySwitch(
                mode = ActivationMode.PrivilegedDnsControl,
                controlOnlyEligible = false,
                canControlPrivateDns = false,
                isDebugBuild = true,
            )
        )
    }

    @Test
    fun `resolved warning keeps activation visible until success is acknowledged`() {
        assertTrue(
            updatedForcedActivationVisibility(
                currentlyVisible = true,
                previouslyRequired = true,
                currentlyRequired = false,
            )
        )
        assertTrue(
            updatedForcedActivationVisibility(
                currentlyVisible = false,
                previouslyRequired = false,
                currentlyRequired = true,
            )
        )
        assertFalse(
            updatedForcedActivationVisibility(
                currentlyVisible = false,
                previouslyRequired = false,
                currentlyRequired = false,
            )
        )
    }

    @Test
    fun `control-only mode switches to privileged only after an activation attempt`() {
        assertTrue(
            shouldSwitchToPrivilegedModeOnGrant(
                page = ActivationScreenPage.Shizuku,
                mode = ActivationMode.NextDnsControlOnly,
            )
        )
        assertFalse(
            shouldSwitchToPrivilegedModeOnGrant(
                page = ActivationScreenPage.Overview,
                mode = ActivationMode.NextDnsControlOnly,
            )
        )
        assertFalse(
            shouldSwitchToPrivilegedModeOnGrant(
                page = ActivationScreenPage.Adb,
                mode = ActivationMode.PrivilegedDnsControl,
            )
        )
    }
}
