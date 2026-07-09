package com.eyalm.adns.data.provider

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface ProviderSelectionStore {
    fun read(): LegacyProviderSnapshot

    fun write(result: ProviderSelectionMigrationResult)
}

sealed interface ProviderSelectionUpdateResult {
    data class Saved(val selection: DnsProviderSelection) : ProviderSelectionUpdateResult

    data class Invalid(val reason: Reason) : ProviderSelectionUpdateResult {
        enum class Reason {
            Provider,
            Preset,
            Hostname,
        }
    }
}

class ProviderSelectionRepository(
    private val store: ProviderSelectionStore,
    private val catalog: DnsProviderCatalog = DnsProviderCatalog.default,
) {
    private val migration = ProviderSelectionMigration(catalog)
    private var customHostname: String?
    private var enhancedHostname: String?
    private val _selection: MutableStateFlow<DnsProviderSelection>
    private val _resolvedHostname: MutableStateFlow<String?>

    val selection: StateFlow<DnsProviderSelection>
        get() = _selection.asStateFlow()

    val resolvedHostname: StateFlow<String?>
        get() = _resolvedHostname.asStateFlow()

    init {
        val snapshot = store.read()
        val migrated = migration.migrate(snapshot)
        customHostname = migrated.customHostname
        enhancedHostname = migrated.enhancedHostname
        _selection = MutableStateFlow(migrated.selection)
        _resolvedHostname = MutableStateFlow(resolveHostname(migrated.selection))
        if (snapshot.migrationVersion < ProviderSelectionMigration.CURRENT_VERSION) {
            store.write(migrated)
        }
    }

    fun resolveHostname(
        value: DnsProviderSelection = selection.value,
    ): String? = when (value) {
        is DnsProviderSelection.Standard -> catalog.resolveHostname(value)
        is DnsProviderSelection.Custom -> PrivateDnsHostname.parse(value.hostname)?.ascii
        is DnsProviderSelection.Enhanced -> enhancedHostname
    }

    fun save(value: DnsProviderSelection): ProviderSelectionUpdateResult {
        val normalized = validateSelection(value) ?: return invalidResult(value)
        if (normalized is DnsProviderSelection.Custom) {
            customHostname = normalized.hostname
        }
        persist(normalized)
        return ProviderSelectionUpdateResult.Saved(normalized)
    }

    fun setEnhancedHostname(hostname: String?): ProviderSelectionUpdateResult {
        val normalized = if (hostname == null) {
            null
        } else {
            PrivateDnsHostname.parsePreservingCase(hostname)?.ascii
                ?: return ProviderSelectionUpdateResult.Invalid(
                    ProviderSelectionUpdateResult.Invalid.Reason.Hostname
                )
        }
        enhancedHostname = normalized
        persist(selection.value)
        return ProviderSelectionUpdateResult.Saved(selection.value)
    }

    fun validateSelection(value: DnsProviderSelection): DnsProviderSelection? = when (value) {
        is DnsProviderSelection.Standard -> {
            val provider = catalog.standardProvider(value.providerId) ?: return null
            provider.presets
                .firstOrNull { it.id == value.presetId }
                ?.let { value }
        }

        is DnsProviderSelection.Enhanced -> catalog.enhancedProviders
            .firstOrNull { it.id == value.providerId }
            ?.let { value }

        is DnsProviderSelection.Custom -> PrivateDnsHostname.parse(value.hostname)
            ?.let { DnsProviderSelection.Custom(it.ascii) }
    }

    private fun invalidResult(
        value: DnsProviderSelection,
    ): ProviderSelectionUpdateResult.Invalid = ProviderSelectionUpdateResult.Invalid(
        when (value) {
            is DnsProviderSelection.Custom -> ProviderSelectionUpdateResult.Invalid.Reason.Hostname
            is DnsProviderSelection.Enhanced -> ProviderSelectionUpdateResult.Invalid.Reason.Provider
            is DnsProviderSelection.Standard -> {
                if (catalog.standardProvider(value.providerId) == null) {
                    ProviderSelectionUpdateResult.Invalid.Reason.Provider
                } else {
                    ProviderSelectionUpdateResult.Invalid.Reason.Preset
                }
            }
        }
    )

    private fun persist(value: DnsProviderSelection) {
        store.write(
            ProviderSelectionMigrationResult(
                selection = value,
                customHostname = customHostname,
                enhancedHostname = enhancedHostname,
                migrationVersion = ProviderSelectionMigration.CURRENT_VERSION,
            )
        )
        _selection.value = value
        _resolvedHostname.value = resolveHostname(value)
    }
}
