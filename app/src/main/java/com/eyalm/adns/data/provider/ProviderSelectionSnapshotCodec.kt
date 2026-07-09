package com.eyalm.adns.data.provider

object ProviderSelectionSnapshotCodec {
    const val SELECTED_PROVIDER_ID = "selected_provider_id"
    const val SELECTED_PRESET_ID = "selected_provider_preset_id"
    const val CUSTOM_HOSTNAME = "custom_url"
    const val ENHANCED_HOSTNAME = "enhanced_url"
    const val MIGRATION_VERSION = "provider_selection_migration_version"

    fun decode(values: Map<String, *>): LegacyProviderSnapshot = LegacyProviderSnapshot(
        migrationVersion = values[MIGRATION_VERSION] as? Int ?: 0,
        selectedProviderId = values[SELECTED_PROVIDER_ID] as? String,
        selectedPresetId = values[SELECTED_PRESET_ID] as? String,
        customHostname = values[CUSTOM_HOSTNAME] as? String,
        enhancedHostname = values[ENHANCED_HOSTNAME] as? String,
    )

    fun encode(result: ProviderSelectionMigrationResult): Map<String, Any?> = mapOf(
        SELECTED_PROVIDER_ID to when (val selection = result.selection) {
            is DnsProviderSelection.Standard -> selection.providerId.value
            is DnsProviderSelection.Enhanced -> selection.providerId.value
            is DnsProviderSelection.Custom -> ProviderSelectionMigration.CUSTOM_PROVIDER_ID
        },
        SELECTED_PRESET_ID to (result.selection as? DnsProviderSelection.Standard)
            ?.presetId
            ?.value,
        CUSTOM_HOSTNAME to result.customHostname,
        ENHANCED_HOSTNAME to result.enhancedHostname,
        MIGRATION_VERSION to result.migrationVersion,
    )
}
