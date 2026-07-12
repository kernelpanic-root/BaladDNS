package com.eyalm.adns.data.runtime

import com.eyalm.adns.data.wifi.WifiRuleStatus
import com.eyalm.adns.data.wifi.WifiSsid
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RuntimeNotificationPresentationTest {

    @Test
    fun `suspension takes priority over generic DNS state`() {
        val presentation = deriveRuntimeNotificationPresentation(
            reasons = setOf(
                RuntimeMonitorReason.StateNotification,
                RuntimeMonitorReason.WifiRules,
            ),
            selectedDnsActive = false,
            wifiStatus = WifiRuleStatus.Suspended(WifiSsid.fromUserInput("Home")!!),
            canToggle = true,
        )

        assertEquals(RuntimeNotificationMessage.WifiSuspended, presentation.message)
        assertTrue(presentation.showToggleAction)
    }

    @Test
    fun `wifi only service explains its required notification`() {
        val presentation = deriveRuntimeNotificationPresentation(
            reasons = setOf(RuntimeMonitorReason.WifiRules),
            selectedDnsActive = true,
            wifiStatus = WifiRuleStatus.Monitoring,
            canToggle = true,
        )

        assertEquals(RuntimeNotificationMessage.WifiMonitoring, presentation.message)
    }

    @Test
    fun `state presentation reflects selected DNS and never exposes blocked action`() {
        val enabled = deriveRuntimeNotificationPresentation(
            reasons = setOf(RuntimeMonitorReason.StateNotification),
            selectedDnsActive = true,
            wifiStatus = WifiRuleStatus.Disabled,
            canToggle = true,
        )
        val blocked = deriveRuntimeNotificationPresentation(
            reasons = setOf(RuntimeMonitorReason.StateNotification),
            selectedDnsActive = false,
            wifiStatus = WifiRuleStatus.Disabled,
            canToggle = false,
        )

        assertEquals(RuntimeNotificationMessage.DnsEnabled, enabled.message)
        assertTrue(enabled.showToggleAction)
        assertEquals(RuntimeNotificationMessage.DnsDisabled, blocked.message)
        assertFalse(blocked.showToggleAction)
    }

    @Test
    fun `external DNS override is not described as active monitoring`() {
        val presentation = deriveRuntimeNotificationPresentation(
            reasons = setOf(RuntimeMonitorReason.WifiRules),
            selectedDnsActive = true,
            wifiStatus = WifiRuleStatus.ExternalChangeDetected,
            canToggle = true,
        )

        assertEquals(RuntimeNotificationMessage.WifiExternalChange, presentation.message)
    }
}
