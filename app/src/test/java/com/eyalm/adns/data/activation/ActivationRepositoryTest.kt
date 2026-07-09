package com.eyalm.adns.data.activation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ActivationRepositoryTest {
    @Test
    fun `legacy install with permission migrates to completed privileged mode`() {
        val store = FakeActivationStore()

        val repository = ActivationRepository(
            store = store,
            permissionObserver = FakePermissionObserver(granted = true),
            migrationContext = { ActivationMigrationContext() },
        )

        assertEquals(
            ActivationState(
                onboardingComplete = true,
                mode = ActivationMode.PrivilegedDnsControl,
                permission = PermissionState.Granted,
                needsModeChoice = false,
            ),
            repository.state.value,
        )
        assertEquals(ActivationMigration.CURRENT_VERSION, store.snapshot.migrationVersion)
    }

    @Test
    fun `legacy nextdns install without permission requires explicit mode choice`() {
        val repository = ActivationRepository(
            store = FakeActivationStore(),
            permissionObserver = FakePermissionObserver(granted = false),
            migrationContext = {
                ActivationMigrationContext(
                    hasNextDnsSession = true,
                    hasSelectedNextDnsProfile = true,
                )
            },
        )

        assertFalse(repository.state.value.onboardingComplete)
        assertEquals(null, repository.state.value.mode)
        assertTrue(repository.state.value.needsModeChoice)
    }

    @Test
    fun `legacy install without permission or nextdns starts onboarding`() {
        val repository = ActivationRepository(
            store = FakeActivationStore(),
            permissionObserver = FakePermissionObserver(granted = false),
            migrationContext = { ActivationMigrationContext() },
        )

        assertFalse(repository.state.value.onboardingComplete)
        assertEquals(null, repository.state.value.mode)
        assertFalse(repository.state.value.needsModeChoice)
    }

    @Test
    fun `permission refresh never rewrites selected mode`() {
        val permission = FakePermissionObserver(granted = false)
        val repository = ActivationRepository(
            store = FakeActivationStore(
                StoredActivationSnapshot(
                    migrationVersion = ActivationMigration.CURRENT_VERSION,
                    onboardingComplete = true,
                    mode = ActivationMode.PrivilegedDnsControl.storageValue,
                )
            ),
            permissionObserver = permission,
            migrationContext = { ActivationMigrationContext() },
        )

        permission.granted = true
        repository.refreshPermission()
        permission.granted = false
        repository.refreshPermission()

        assertEquals(ActivationMode.PrivilegedDnsControl, repository.state.value.mode)
        assertEquals(PermissionState.Missing, repository.state.value.permission)
        assertTrue(repository.state.value.needsReactivation)
    }

    @Test
    fun `completing onboarding persists deliberate control-only mode`() {
        val store = FakeActivationStore()
        val repository = ActivationRepository(
            store = store,
            permissionObserver = FakePermissionObserver(granted = false),
            migrationContext = { ActivationMigrationContext() },
        )

        repository.completeOnboarding(ActivationMode.NextDnsControlOnly)

        assertTrue(repository.state.value.onboardingComplete)
        assertEquals(ActivationMode.NextDnsControlOnly, repository.state.value.mode)
        assertFalse(repository.state.value.needsModeChoice)
        assertEquals(
            ActivationMode.NextDnsControlOnly.storageValue,
            store.snapshot.mode,
        )
    }

    private class FakePermissionObserver(var granted: Boolean) : PermissionObserver {
        override fun current(): PermissionState =
            if (granted) PermissionState.Granted else PermissionState.Missing
    }

    private class FakeActivationStore(
        initial: StoredActivationSnapshot = StoredActivationSnapshot(),
    ) : ActivationStore {
        var snapshot = initial

        override fun read(): StoredActivationSnapshot = snapshot

        override fun write(value: StoredActivationSnapshot) {
            snapshot = value
        }
    }
}
