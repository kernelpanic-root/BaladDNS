package com.eyalm.adns.data.runtime

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class StoredRuntimeMonitoringSnapshot(
    val migrationVersion: Int = 0,
    val legacyStateNotificationEnabled: Boolean? = null,
    val stateNotificationEnabled: Boolean = false,
    val stateNotificationChoiceRecorded: Boolean = false,
    val wifiRulesEnabled: Boolean = false,
)

interface RuntimeMonitoringStore {
    fun read(): StoredRuntimeMonitoringSnapshot

    fun write(snapshot: StoredRuntimeMonitoringSnapshot)
}

data class RuntimeMonitoringMigrationResult(
    val stored: StoredRuntimeMonitoringSnapshot,
    val preferences: RuntimeMonitoringPreferences,
)

class RuntimeMonitoringMigration {
    fun migrate(snapshot: StoredRuntimeMonitoringSnapshot): RuntimeMonitoringMigrationResult {
        val alreadyMigrated = snapshot.migrationVersion >= CURRENT_VERSION
        val stateEnabled = if (alreadyMigrated) {
            snapshot.stateNotificationEnabled
        } else {
            snapshot.legacyStateNotificationEnabled ?: false
        }
        val choiceRecorded = if (alreadyMigrated) {
            snapshot.stateNotificationChoiceRecorded
        } else {
            snapshot.legacyStateNotificationEnabled != null
        }
        val wifiRulesEnabled = if (alreadyMigrated) snapshot.wifiRulesEnabled else false
        val stored = snapshot.copy(
            migrationVersion = CURRENT_VERSION,
            stateNotificationEnabled = stateEnabled,
            stateNotificationChoiceRecorded = choiceRecorded,
            wifiRulesEnabled = wifiRulesEnabled,
        )
        return RuntimeMonitoringMigrationResult(
            stored = stored,
            preferences = RuntimeMonitoringPreferences(
                stateNotificationEnabled = stateEnabled,
                wifiRulesEnabled = wifiRulesEnabled,
            ),
        )
    }

    companion object {
        const val CURRENT_VERSION = 1
    }
}

class RuntimeMonitoringRepository(
    private val store: RuntimeMonitoringStore,
) {
    private var stored: StoredRuntimeMonitoringSnapshot
    private val _preferences: MutableStateFlow<RuntimeMonitoringPreferences>
    val preferences: StateFlow<RuntimeMonitoringPreferences>
        get() = _preferences.asStateFlow()
    private val _serviceRunning = MutableStateFlow(false)
    val serviceRunning: StateFlow<Boolean> = _serviceRunning.asStateFlow()
    private val _serviceFailure = MutableStateFlow<RuntimeServiceFailure?>(null)
    val serviceFailure: StateFlow<RuntimeServiceFailure?> = _serviceFailure.asStateFlow()

    init {
        val source = store.read()
        val migrated = RuntimeMonitoringMigration().migrate(source)
        stored = migrated.stored
        _preferences = MutableStateFlow(migrated.preferences)
        if (source != migrated.stored) {
            store.write(migrated.stored)
        }
    }

    fun setStateNotificationEnabled(enabled: Boolean) {
        stored = stored.copy(
            stateNotificationEnabled = enabled,
            stateNotificationChoiceRecorded = true,
        )
        store.write(stored)
        _preferences.value = _preferences.value.copy(stateNotificationEnabled = enabled)
    }

    fun setWifiRulesEnabled(enabled: Boolean) {
        stored = stored.copy(wifiRulesEnabled = enabled)
        store.write(stored)
        _preferences.value = _preferences.value.copy(wifiRulesEnabled = enabled)
    }

    fun setServiceRunning(running: Boolean) {
        _serviceRunning.value = running
        if (running) _serviceFailure.value = null
    }

    fun setServiceFailure(failure: RuntimeServiceFailure?) {
        _serviceFailure.value = failure
    }
}
