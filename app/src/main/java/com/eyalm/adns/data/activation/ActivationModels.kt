package com.eyalm.adns.data.activation

enum class ActivationMode(val storageValue: String) {
    PrivilegedDnsControl("privileged_dns_control"),
    NextDnsControlOnly("nextdns_control_only"),
}

enum class PermissionState {
    Granted,
    Missing,
}

data class ActivationState(
    val onboardingComplete: Boolean,
    val mode: ActivationMode?,
    val permission: PermissionState,
    val needsModeChoice: Boolean = false,
) {
    val canControlPrivateDns: Boolean
        get() = onboardingComplete &&
            mode == ActivationMode.PrivilegedDnsControl &&
            permission == PermissionState.Granted

    val needsReactivation: Boolean
        get() = onboardingComplete &&
            mode == ActivationMode.PrivilegedDnsControl &&
            permission == PermissionState.Missing
}

data class StoredActivationSnapshot(
    val migrationVersion: Int = 0,
    val onboardingComplete: Boolean = false,
    val mode: String? = null,
    val needsModeChoice: Boolean = false,
)

data class ActivationMigrationContext(
    val hasNextDnsSession: Boolean = false,
    val hasSelectedNextDnsProfile: Boolean = false,
)
