package com.eyalm.adns.data.activation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface PermissionObserver {
    fun current(): PermissionState
}

interface ActivationStore {
    fun read(): StoredActivationSnapshot

    fun write(value: StoredActivationSnapshot)
}

class ActivationRepository(
    private val store: ActivationStore,
    private val permissionObserver: PermissionObserver,
    migrationContext: () -> ActivationMigrationContext,
) {
    private val _state: MutableStateFlow<ActivationState>
    val state: StateFlow<ActivationState>
        get() = _state.asStateFlow()

    init {
        val snapshot = store.read()
        val migrated = ActivationMigration().migrate(
            snapshot = snapshot,
            permission = permissionObserver.current(),
            context = migrationContext(),
        )
        if (snapshot.migrationVersion < ActivationMigration.CURRENT_VERSION) {
            store.write(migrated.stored)
        }
        _state = MutableStateFlow(migrated.state)
    }

    fun refreshPermission() {
        _state.value = _state.value.copy(permission = permissionObserver.current())
    }

    fun completeOnboarding(mode: ActivationMode) {
        publish(
            _state.value.copy(
                onboardingComplete = true,
                mode = mode,
                needsModeChoice = false,
            )
        )
    }

    fun changeMode(mode: ActivationMode) {
        publish(
            _state.value.copy(
                mode = mode,
                needsModeChoice = false,
            )
        )
    }

    private fun publish(value: ActivationState) {
        store.write(
            StoredActivationSnapshot(
                migrationVersion = ActivationMigration.CURRENT_VERSION,
                onboardingComplete = value.onboardingComplete,
                mode = value.mode?.storageValue,
                needsModeChoice = value.needsModeChoice,
            )
        )
        _state.value = value
    }
}
