package com.kernelpanic.baladdns.data.wifi

import com.kernelpanic.baladdns.data.PrivateDnsObservation
import com.kernelpanic.baladdns.data.dns.DnsDisableBehavior
import com.kernelpanic.baladdns.data.dns.DnsWriteResult
import com.kernelpanic.baladdns.data.dns.PrivateDnsControl
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WifiRuleCoordinatorTest {
    private val home = WifiSsid.fromUserInput("Home")!!

    @Test
    fun `successful match writes disabled state then records active ownership`() = runTest {
        val fixture = fixture(PrivateDnsObservation.Hostname("dns.example"))

        val status = fixture.coordinator.reconcile(
            enabled = true,
            canControl = true,
            identity = ConnectedWifiIdentity.Known(home),
        )

        assertEquals(listOf("disable:off"), fixture.control.writes)
        assertEquals(WifiSuspensionPhase.Active, fixture.repository.state.value.suspension?.phase)
        assertEquals(WifiRuleStatus.Suspended(home), status)
    }

    @Test
    fun `failed suspension write does not claim ownership`() = runTest {
        val fixture = fixture(PrivateDnsObservation.Hostname("dns.example"))
        fixture.control.rejectWrites = true

        val status = fixture.coordinator.reconcile(
            enabled = true,
            canControl = true,
            identity = ConnectedWifiIdentity.Known(home),
        )

        assertNull(fixture.repository.state.value.suspension)
        assertEquals(WifiRuleStatus.WriteFailed, status)
    }

    @Test
    fun `leaving restores captured hostname and clears ownership`() = runTest {
        val fixture = fixture(PrivateDnsObservation.Off, suspension = activeSuspension())

        val status = fixture.coordinator.reconcile(
            enabled = true,
            canControl = true,
            identity = ConnectedWifiIdentity.NotOnWifi,
        )

        assertEquals(listOf("enable:dns.example"), fixture.control.writes)
        assertNull(fixture.repository.state.value.suspension)
        assertEquals(WifiRuleStatus.Monitoring, status)
    }

    @Test
    fun `external DNS change relinquishes ownership without writing`() = runTest {
        val fixture = fixture(
            PrivateDnsObservation.Hostname("external.example"),
            suspension = activeSuspension(),
        )

        fixture.coordinator.reconcile(
            enabled = true,
            canControl = true,
            identity = ConnectedWifiIdentity.NotOnWifi,
        )

        assertTrue(fixture.control.writes.isEmpty())
        assertNull(fixture.repository.state.value.suspension)
    }

    @Test
    fun `external change on a matched wifi is not reacquired until that wifi is left`() = runTest {
        val fixture = fixture(
            PrivateDnsObservation.Hostname("external.example"),
            suspension = activeSuspension(),
        )

        val first = fixture.coordinator.reconcile(
            enabled = true,
            canControl = true,
            identity = ConnectedWifiIdentity.Known(home),
        )
        fixture.coordinator.reconcile(
            enabled = true,
            canControl = true,
            identity = ConnectedWifiIdentity.Known(home),
        )

        assertEquals(WifiRuleStatus.ExternalChangeDetected, first)
        assertTrue(fixture.control.writes.isEmpty())

        fixture.coordinator.reconcile(
            enabled = true,
            canControl = true,
            identity = ConnectedWifiIdentity.NotOnWifi,
        )
        fixture.coordinator.reconcile(
            enabled = true,
            canControl = true,
            identity = ConnectedWifiIdentity.Known(home),
        )

        assertEquals(listOf("disable:off"), fixture.control.writes)
    }

    @Test
    fun `disabling rules performs guarded restoration`() = runTest {
        val fixture = fixture(PrivateDnsObservation.Off, suspension = activeSuspension())

        val status = fixture.coordinator.reconcile(
            enabled = false,
            canControl = true,
            identity = ConnectedWifiIdentity.RedactedOrUnknown,
        )

        assertEquals(listOf("enable:dns.example"), fixture.control.writes)
        assertNull(fixture.repository.state.value.suspension)
        assertEquals(WifiRuleStatus.Disabled, status)
    }

    @Test
    fun `changing disable behavior reapplies owned suspension and preserves restore target`() =
        runTest {
            val fixture = fixture(PrivateDnsObservation.Off, suspension = activeSuspension())
            fixture.behavior.value = DnsDisableBehavior.Automatic

            val status = fixture.coordinator.reconcile(
                enabled = true,
                canControl = true,
                identity = ConnectedWifiIdentity.Known(home),
            )

            assertEquals(listOf("disable:automatic"), fixture.control.writes)
            assertEquals(WifiRuleStatus.Suspended(home), status)
            assertEquals(
                PrivateDnsObservation.Hostname("dns.example"),
                fixture.repository.state.value.suspension?.restoreTarget,
            )
            assertEquals(
                PrivateDnsObservation.Automatic,
                fixture.repository.state.value.suspension?.stateAppliedByAdns,
            )
            assertEquals(
                WifiSuspensionPhase.Active,
                fixture.repository.state.value.suspension?.phase,
            )
        }

    @Test
    fun `failed behavior change keeps ownership of the previously applied state`() = runTest {
        val original = activeSuspension()
        val fixture = fixture(PrivateDnsObservation.Off, suspension = original)
        fixture.behavior.value = DnsDisableBehavior.Automatic
        fixture.control.rejectWrites = true

        val status = fixture.coordinator.reconcile(
            enabled = true,
            canControl = true,
            identity = ConnectedWifiIdentity.Known(home),
        )

        assertEquals(WifiRuleStatus.WriteFailed, status)
        assertEquals(original, fixture.repository.state.value.suspension)
    }

    @Test
    fun `process recreation during behavior change resumes without losing ownership`() =
        runTest {
            val applyingChange = activeSuspension().copy(
                stateAppliedByAdns = PrivateDnsObservation.Automatic,
                phase = WifiSuspensionPhase.Applying,
                previousStateAppliedByAdns = PrivateDnsObservation.Off,
            )
            val fixture = fixture(
                observation = PrivateDnsObservation.Off,
                suspension = applyingChange,
                disableBehavior = DnsDisableBehavior.Automatic,
            )

            val status = fixture.coordinator.reconcile(
                enabled = true,
                canControl = true,
                identity = ConnectedWifiIdentity.Known(home),
            )

            assertEquals(listOf("disable:automatic"), fixture.control.writes)
            assertEquals(WifiRuleStatus.Suspended(home), status)
            assertEquals(
                PrivateDnsObservation.Automatic,
                fixture.repository.state.value.suspension?.stateAppliedByAdns,
            )
            assertNull(
                fixture.repository.state.value.suspension?.previousStateAppliedByAdns,
            )
        }

    private fun fixture(
        observation: PrivateDnsObservation,
        suspension: WifiRuleSuspension? = null,
        disableBehavior: DnsDisableBehavior = DnsDisableBehavior.Off,
    ): Fixture {
        val repository = WifiRulesRepository(
            FakeStore(
                StoredWifiRulesSnapshot(
                    configuration = WifiRulesConfiguration(
                        ssids = setOf(home),
                    ),
                    suspension = suspension,
                )
            )
        )
        val control = FakePrivateDnsControl(observation)
        val behavior = DefaultBehavior(disableBehavior)
        return Fixture(
            repository = repository,
            control = control,
            behavior = behavior,
            coordinator = WifiRuleCoordinator(
                repository = repository,
                privateDnsControl = control,
                disableBehavior = { behavior.value },
            ),
        )
    }

    private fun activeSuspension() = WifiRuleSuspension(
        matchedSsid = home,
        restoreTarget = PrivateDnsObservation.Hostname("dns.example"),
        stateAppliedByAdns = PrivateDnsObservation.Off,
        phase = WifiSuspensionPhase.Active,
    )

    private data class Fixture(
        val repository: WifiRulesRepository,
        val control: FakePrivateDnsControl,
        val behavior: DefaultBehavior,
        val coordinator: WifiRuleCoordinator,
    )

    private data class DefaultBehavior(var value: DnsDisableBehavior)

    private class FakeStore(
        private var snapshot: StoredWifiRulesSnapshot,
    ) : WifiRulesStore {
        override fun read(): StoredWifiRulesSnapshot = snapshot

        override fun write(snapshot: StoredWifiRulesSnapshot) {
            this.snapshot = snapshot
        }
    }

    private class FakePrivateDnsControl(
        private var observation: PrivateDnsObservation,
    ) : PrivateDnsControl {
        val writes = mutableListOf<String>()
        var rejectWrites = false

        override fun observe(): PrivateDnsObservation = observation

        override suspend fun enable(hostname: String?): DnsWriteResult {
            writes += "enable:$hostname"
            if (rejectWrites) return DnsWriteResult.Rejected(observation)
            observation = PrivateDnsObservation.Hostname(requireNotNull(hostname))
            return DnsWriteResult.Success(observation)
        }

        override suspend fun disable(behavior: DnsDisableBehavior): DnsWriteResult {
            writes += "disable:${behavior.storageValue}"
            if (rejectWrites) return DnsWriteResult.Rejected(observation)
            observation = when (behavior) {
                DnsDisableBehavior.Automatic -> PrivateDnsObservation.Automatic
                DnsDisableBehavior.Off -> PrivateDnsObservation.Off
            }
            return DnsWriteResult.Success(observation)
        }
    }
}
