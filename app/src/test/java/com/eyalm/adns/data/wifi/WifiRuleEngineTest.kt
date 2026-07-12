package com.eyalm.adns.data.wifi

import com.eyalm.adns.data.PrivateDnsObservation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WifiRuleEngineTest {
    private val engine = WifiRuleEngine()
    private val home = WifiSsid.fromUserInput("Home")!!

    @Test
    fun `android SSID normalization preserves case unicode and hex form`() {
        assertEquals("Home WiFi", WifiSsid.fromAndroid("\"Home WiFi\"")?.value)
        assertEquals("机构", WifiSsid.fromAndroid("\"机构\"")?.value)
        assertEquals("e69cbae69e84", WifiSsid.fromAndroid("e69cbae69e84")?.value)
        assertEquals("UPPER", WifiSsid.fromAndroid("\"UPPER\"")?.value)
        assertEquals(null, WifiSsid.fromAndroid("<unknown ssid>"))
        assertEquals(null, WifiSsid.fromAndroid("\"\""))
    }

    @Test
    fun `matching is exact and case sensitive`() {
        val matching = evaluate(identity = ConnectedWifiIdentity.Known(home))
        val differentCase = evaluate(
            identity = ConnectedWifiIdentity.Known(WifiSsid.fromUserInput("home")!!),
        )

        assertTrue(matching is WifiRuleDecision.BeginSuspension)
        assertEquals(
            WifiRuleStatus.Monitoring,
            (differentCase as WifiRuleDecision.NoChange).status,
        )
    }

    @Test
    fun `matching transition records the exact restore state and applying phase`() {
        val current = PrivateDnsObservation.Hostname("Device.Profile.dns.nextdns.io")

        val decision = evaluate(current = current)

        val suspension = (decision as WifiRuleDecision.BeginSuspension).suspension
        assertEquals(home, suspension.matchedSsid)
        assertEquals(current, suspension.restoreTarget)
        assertEquals(PrivateDnsObservation.Off, suspension.stateAppliedByAdns)
        assertEquals(WifiSuspensionPhase.Applying, suspension.phase)
    }

    @Test
    fun `matching an already disabled state does not claim ownership`() {
        val decision = evaluate(current = PrivateDnsObservation.Off)

        assertEquals(
            WifiRuleStatus.MatchedAlreadyDisabled(home),
            (decision as WifiRuleDecision.NoChange).status,
        )
    }

    @Test
    fun `unknown identity never exits or enters a suspension`() {
        val withoutSuspension = evaluate(identity = ConnectedWifiIdentity.RedactedOrUnknown)
        val active = activeSuspension()
        val withSuspension = evaluate(
            identity = ConnectedWifiIdentity.RedactedOrUnknown,
            current = PrivateDnsObservation.Off,
            suspension = active,
        )

        assertEquals(
            WifiRuleStatus.IdentityUnavailable,
            (withoutSuspension as WifiRuleDecision.NoChange).status,
        )
        assertEquals(
            WifiRuleStatus.IdentityUnavailable,
            (withSuspension as WifiRuleDecision.NoChange).status,
        )
    }

    @Test
    fun `leaving restores only while current state is still ADNS applied state`() {
        val active = activeSuspension()
        val restore = evaluate(
            identity = ConnectedWifiIdentity.NotOnWifi,
            current = PrivateDnsObservation.Off,
            suspension = active,
        )
        val externalChange = evaluate(
            identity = ConnectedWifiIdentity.NotOnWifi,
            current = PrivateDnsObservation.Automatic,
            suspension = active,
        )

        assertEquals(
            WifiSuspensionPhase.Restoring,
            (restore as WifiRuleDecision.Restore).suspension.phase,
        )
        assertEquals(
            null,
            (externalChange as WifiRuleDecision.RelinquishOwnership).ssid,
        )
    }

    @Test
    fun `disabling rules uses the same guarded restore path`() {
        val decision = evaluate(
            enabled = false,
            current = PrivateDnsObservation.Off,
            suspension = activeSuspension(),
        )

        assertTrue(decision is WifiRuleDecision.Restore)
    }

    @Test
    fun `pending phases reconcile after process death`() {
        val applying = activeSuspension().copy(phase = WifiSuspensionPhase.Applying)
        val restoring = activeSuspension().copy(phase = WifiSuspensionPhase.Restoring)

        val promote = evaluate(current = PrivateDnsObservation.Off, suspension = applying)
        val finishRestore = evaluate(
            current = applying.restoreTarget,
            suspension = restoring,
        )

        assertEquals(
            WifiSuspensionPhase.Active,
            (promote as WifiRuleDecision.UpdateSuspension).suspension.phase,
        )
        assertTrue(finishRestore is WifiRuleDecision.ClearSuspension)
    }

    @Test
    fun `permission loss retains ownership without a write`() {
        val decision = evaluate(
            canControl = false,
            current = PrivateDnsObservation.PermissionMissing,
            identity = ConnectedWifiIdentity.NotOnWifi,
            suspension = activeSuspension(),
        )

        assertEquals(
            WifiRuleStatus.ActivationRequired,
            (decision as WifiRuleDecision.NoChange).status,
        )
    }

    private fun evaluate(
        enabled: Boolean = true,
        canControl: Boolean = true,
        identity: ConnectedWifiIdentity = ConnectedWifiIdentity.Known(home),
        current: PrivateDnsObservation = PrivateDnsObservation.Hostname("dns.example"),
        suspension: WifiRuleSuspension? = null,
    ): WifiRuleDecision = engine.evaluate(
        enabled = enabled,
        canControl = canControl,
        configuration = WifiRulesConfiguration(
            ssids = setOf(home),
        ),
        identity = identity,
        currentDns = current,
        suspensionTarget = PrivateDnsObservation.Off,
        suspension = suspension,
    )

    private fun activeSuspension() = WifiRuleSuspension(
        matchedSsid = home,
        restoreTarget = PrivateDnsObservation.Hostname("dns.example"),
        stateAppliedByAdns = PrivateDnsObservation.Off,
        phase = WifiSuspensionPhase.Active,
    )
}
