package com.eyalm.adns.data.runtime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RuntimeServicePlanTest {

    @Test
    fun `service runs for each independently enabled capable reason`() {
        val stateOnly = deriveRuntimeServicePlan(
            preferences = RuntimeMonitoringPreferences(stateNotificationEnabled = true),
            canRunRuntimeMonitor = true,
            canUseWifiRules = true,
            wifiReasonAllowed = true,
            hasPendingWifiSuspension = false,
        )
        val wifiOnly = deriveRuntimeServicePlan(
            preferences = RuntimeMonitoringPreferences(wifiRulesEnabled = true),
            canRunRuntimeMonitor = true,
            canUseWifiRules = true,
            wifiReasonAllowed = true,
            hasPendingWifiSuspension = false,
        )

        assertEquals(setOf(RuntimeMonitorReason.StateNotification), stateOnly.activeReasons)
        assertEquals(setOf(RuntimeMonitorReason.WifiRules), wifiOnly.activeReasons)
        assertTrue(stateOnly.shouldRun)
        assertTrue(wifiOnly.shouldRun)
    }

    @Test
    fun `boot policy omits wifi when permission policy disallows it`() {
        val wifiOnly = deriveRuntimeServicePlan(
            preferences = RuntimeMonitoringPreferences(wifiRulesEnabled = true),
            canRunRuntimeMonitor = true,
            canUseWifiRules = true,
            wifiReasonAllowed = false,
            hasPendingWifiSuspension = false,
        )
        val both = deriveRuntimeServicePlan(
            preferences = RuntimeMonitoringPreferences(
                stateNotificationEnabled = true,
                wifiRulesEnabled = true,
            ),
            canRunRuntimeMonitor = true,
            canUseWifiRules = true,
            wifiReasonAllowed = false,
            hasPendingWifiSuspension = false,
        )

        assertFalse(wifiOnly.shouldRun)
        assertEquals(setOf(RuntimeMonitorReason.StateNotification), both.activeReasons)
    }

    @Test
    fun `wifi boot resume requires background location only from API 29`() {
        assertTrue(
            canResumeWifiRulesFromBoot(
                sdkInt = 28,
                hasFineLocation = true,
                hasBackgroundLocation = false,
                locationServicesEnabled = true,
            )
        )
        assertFalse(
            canResumeWifiRulesFromBoot(
                sdkInt = 29,
                hasFineLocation = true,
                hasBackgroundLocation = false,
                locationServicesEnabled = true,
            )
        )
        assertTrue(
            canResumeWifiRulesFromBoot(
                sdkInt = 29,
                hasFineLocation = true,
                hasBackgroundLocation = true,
                locationServicesEnabled = true,
            )
        )
        assertFalse(
            canResumeWifiRulesFromBoot(
                sdkInt = 36,
                hasFineLocation = false,
                hasBackgroundLocation = true,
                locationServicesEnabled = true,
            )
        )
        assertFalse(
            canResumeWifiRulesFromBoot(
                sdkInt = 36,
                hasFineLocation = true,
                hasBackgroundLocation = true,
                locationServicesEnabled = false,
            )
        )
    }

    @Test
    fun `pending owned suspension permits one shot recovery when wifi preference is off`() {
        val plan = deriveRuntimeServicePlan(
            preferences = RuntimeMonitoringPreferences(),
            canRunRuntimeMonitor = true,
            canUseWifiRules = true,
            wifiReasonAllowed = true,
            hasPendingWifiSuspension = true,
        )

        assertTrue(plan.shouldRun)
        assertTrue(plan.recoveryRequired)
        assertEquals(emptySet<RuntimeMonitorReason>(), plan.activeReasons)
    }

    @Test
    fun `control only mode cannot run requested or recovery work`() {
        val plan = deriveRuntimeServicePlan(
            preferences = RuntimeMonitoringPreferences(
                stateNotificationEnabled = true,
                wifiRulesEnabled = true,
            ),
            canRunRuntimeMonitor = false,
            canUseWifiRules = false,
            wifiReasonAllowed = true,
            hasPendingWifiSuspension = true,
        )

        assertFalse(plan.shouldRun)
        assertFalse(plan.recoveryRequired)
        assertEquals(emptySet<RuntimeMonitorReason>(), plan.activeReasons)
    }
}
