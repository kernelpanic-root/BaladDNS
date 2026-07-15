package com.kernelpanic.baladdns.data.wifi

import com.kernelpanic.baladdns.data.PrivateDnsObservation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WifiRulesRepositoryTest {

    @Test
    fun `rules survive repository recreation without delimiter encoding`() {
        val store = FakeStore(StoredWifiRulesSnapshot())
        val first = WifiRulesRepository(store)
        val comma = WifiSsid.fromUserInput("Office, 5 GHz")!!
        val unicode = WifiSsid.fromUserInput("בית")!!

        assertTrue(first.add(comma))
        assertTrue(first.add(unicode))
        assertFalse(first.add(comma))

        val recreated = WifiRulesRepository(store)

        assertEquals(setOf(comma, unicode), recreated.state.value.configuration.ssids)
    }

    @Test
    fun `provider change updates hostname restore target only`() {
        val hostnameSuspension = suspension(
            PrivateDnsObservation.Hostname("old.example"),
        )
        val store = FakeStore(
            StoredWifiRulesSnapshot(suspension = hostnameSuspension),
        )
        val repository = WifiRulesRepository(store)

        repository.updateRestoreHostname("new.example")

        assertEquals(
            PrivateDnsObservation.Hostname("new.example"),
            repository.state.value.suspension?.restoreTarget,
        )

        repository.setSuspension(suspension(PrivateDnsObservation.Automatic))
        repository.updateRestoreHostname("ignored.example")
        assertEquals(
            PrivateDnsObservation.Automatic,
            repository.state.value.suspension?.restoreTarget,
        )
    }

    @Test
    fun `external change suppression survives repository recreation`() {
        val store = FakeStore(StoredWifiRulesSnapshot())
        val first = WifiRulesRepository(store)
        val ssid = WifiSsid.fromUserInput("Home")!!

        first.setRelinquishedSsid(ssid)

        assertEquals(ssid, WifiRulesRepository(store).state.value.relinquishedSsid)
    }

    private fun suspension(restore: PrivateDnsObservation) = WifiRuleSuspension(
        matchedSsid = WifiSsid.fromUserInput("Home")!!,
        restoreTarget = restore,
        stateAppliedByAdns = PrivateDnsObservation.Off,
        phase = WifiSuspensionPhase.Active,
    )

    private class FakeStore(
        var value: StoredWifiRulesSnapshot,
    ) : WifiRulesStore {
        override fun read(): StoredWifiRulesSnapshot = value

        override fun write(snapshot: StoredWifiRulesSnapshot) {
            value = snapshot
        }
    }
}
