package com.kernelpanic.baladdns.data.runtime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RuntimeMonitoringRepositoryTest {

    @Test
    fun `missing legacy notification choice migrates to disabled`() {
        val store = FakeRuntimeMonitoringStore(
            StoredRuntimeMonitoringSnapshot(),
        )

        val repository = RuntimeMonitoringRepository(store)

        assertFalse(repository.preferences.value.stateNotificationEnabled)
        assertFalse(repository.preferences.value.wifiRulesEnabled)
        assertFalse(store.value.stateNotificationChoiceRecorded)
        assertEquals(RuntimeMonitoringMigration.CURRENT_VERSION, store.value.migrationVersion)
    }

    @Test
    fun `explicit legacy notification choice is preserved`() {
        val enabledStore = FakeRuntimeMonitoringStore(
            StoredRuntimeMonitoringSnapshot(legacyStateNotificationEnabled = true),
        )
        val disabledStore = FakeRuntimeMonitoringStore(
            StoredRuntimeMonitoringSnapshot(legacyStateNotificationEnabled = false),
        )

        val enabledRepository = RuntimeMonitoringRepository(enabledStore)
        val disabledRepository = RuntimeMonitoringRepository(disabledStore)

        assertTrue(enabledRepository.preferences.value.stateNotificationEnabled)
        assertTrue(enabledStore.value.stateNotificationChoiceRecorded)
        assertFalse(disabledRepository.preferences.value.stateNotificationEnabled)
        assertTrue(disabledStore.value.stateNotificationChoiceRecorded)
    }

    @Test
    fun `current choices are preserved and setters update features independently`() {
        val store = FakeRuntimeMonitoringStore(
            StoredRuntimeMonitoringSnapshot(
                migrationVersion = RuntimeMonitoringMigration.CURRENT_VERSION,
                stateNotificationEnabled = true,
                stateNotificationChoiceRecorded = true,
                wifiRulesEnabled = false,
            )
        )
        val repository = RuntimeMonitoringRepository(store)

        repository.setWifiRulesEnabled(true)

        assertTrue(repository.preferences.value.stateNotificationEnabled)
        assertTrue(repository.preferences.value.wifiRulesEnabled)

        repository.setStateNotificationEnabled(false)

        assertFalse(repository.preferences.value.stateNotificationEnabled)
        assertTrue(repository.preferences.value.wifiRulesEnabled)
        assertTrue(store.value.stateNotificationChoiceRecorded)
    }

    @Test
    fun `runtime reasons are capability gated without merging user preferences`() {
        val preferences = RuntimeMonitoringPreferences(
            stateNotificationEnabled = true,
            wifiRulesEnabled = true,
        )
        val system = RuntimeMonitoringSystemState(
            notificationPermission = NotificationPermissionState.Denied,
            appNotificationsEnabled = false,
            stateChannel = NotificationChannelState.Disabled,
            batteryOptimizationIgnored = false,
            serviceRunning = true,
        )

        val privileged = deriveRuntimeMonitoringState(
            preferences = preferences,
            system = system,
            canRunRuntimeMonitor = true,
            canUseWifiRules = true,
        )
        val controlOnly = deriveRuntimeMonitoringState(
            preferences = preferences,
            system = system,
            canRunRuntimeMonitor = false,
            canUseWifiRules = false,
        )

        assertEquals(
            setOf(RuntimeMonitorReason.StateNotification, RuntimeMonitorReason.WifiRules),
            privileged.requestedReasons,
        )
        assertEquals(privileged.requestedReasons, privileged.activeReasons)
        assertEquals(emptySet<RuntimeMonitorReason>(), controlOnly.activeReasons)
        assertTrue(controlOnly.stateNotificationEnabled)
        assertTrue(controlOnly.wifiRulesEnabled)
        assertEquals(NotificationPermissionState.Denied, controlOnly.notificationPermission)
        assertEquals(NotificationChannelState.Disabled, controlOnly.stateChannel)
    }

    @Test
    fun `turning off state presentation keeps wifi runtime reason active`() {
        val state = deriveRuntimeMonitoringState(
            preferences = RuntimeMonitoringPreferences(
                stateNotificationEnabled = false,
                wifiRulesEnabled = true,
            ),
            system = RuntimeMonitoringSystemState(),
            canRunRuntimeMonitor = true,
            canUseWifiRules = true,
        )

        assertEquals(setOf(RuntimeMonitorReason.WifiRules), state.activeReasons)
    }

    @Test
    fun `service running state is process local and observable`() {
        val repository = RuntimeMonitoringRepository(
            FakeRuntimeMonitoringStore(StoredRuntimeMonitoringSnapshot())
        )

        assertFalse(repository.serviceRunning.value)
        repository.setServiceRunning(true)
        assertTrue(repository.serviceRunning.value)
        repository.setServiceFailure(RuntimeServiceFailure.StartNotAllowed)
        assertEquals(RuntimeServiceFailure.StartNotAllowed, repository.serviceFailure.value)
        repository.setServiceRunning(true)
        assertEquals(null, repository.serviceFailure.value)
    }

    private class FakeRuntimeMonitoringStore(
        initial: StoredRuntimeMonitoringSnapshot,
    ) : RuntimeMonitoringStore {
        var value: StoredRuntimeMonitoringSnapshot = initial

        override fun read(): StoredRuntimeMonitoringSnapshot = value

        override fun write(snapshot: StoredRuntimeMonitoringSnapshot) {
            value = snapshot
        }
    }
}
