package com.eyalm.adns.data.activation

data class ActivationMigrationResult(
    val state: ActivationState,
    val stored: StoredActivationSnapshot,
)

class ActivationMigration {
    fun migrate(
        snapshot: StoredActivationSnapshot,
        permission: PermissionState,
        context: ActivationMigrationContext,
    ): ActivationMigrationResult {
        val stored = if (snapshot.migrationVersion >= CURRENT_VERSION) {
            snapshot.copy(migrationVersion = CURRENT_VERSION)
        } else {
            migrateLegacy(permission, context)
        }
        val state = ActivationState(
            onboardingComplete = stored.onboardingComplete,
            mode = stored.mode.toActivationMode(),
            permission = permission,
            needsModeChoice = stored.needsModeChoice,
        )
        return ActivationMigrationResult(state, stored)
    }

    private fun migrateLegacy(
        permission: PermissionState,
        context: ActivationMigrationContext,
    ): StoredActivationSnapshot = when {
        permission == PermissionState.Granted -> StoredActivationSnapshot(
            migrationVersion = CURRENT_VERSION,
            onboardingComplete = true,
            mode = ActivationMode.PrivilegedDnsControl.storageValue,
        )

        context.hasNextDnsSession && context.hasSelectedNextDnsProfile ->
            StoredActivationSnapshot(
                migrationVersion = CURRENT_VERSION,
                onboardingComplete = false,
                mode = null,
                needsModeChoice = true,
            )

        else -> StoredActivationSnapshot(
            migrationVersion = CURRENT_VERSION,
            onboardingComplete = false,
            mode = null,
            needsModeChoice = false,
        )
    }

    companion object {
        const val CURRENT_VERSION = 1
    }
}

private fun String?.toActivationMode(): ActivationMode? =
    ActivationMode.entries.firstOrNull { it.storageValue == this }
