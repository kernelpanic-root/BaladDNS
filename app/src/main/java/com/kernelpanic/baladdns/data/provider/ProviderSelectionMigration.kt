package com.kernelpanic.baladdns.data.provider

data class LegacyProviderSnapshot(
    val migrationVersion: Int = 0,
    val selectedProviderId: String? = null,
    val selectedPresetId: String? = null,
    val customHostname: String? = null,
    val enhancedHostname: String? = null,
)

data class ProviderSelectionMigrationResult(
    val selection: DnsProviderSelection,
    val customHostname: String?,
    val enhancedHostname: String?,
    val migrationVersion: Int,
)

class ProviderSelectionMigration(
    private val catalog: DnsProviderCatalog,
) {
    fun migrate(snapshot: LegacyProviderSnapshot): ProviderSelectionMigrationResult {
        val selection = if (snapshot.migrationVersion >= CURRENT_VERSION) {
            readTypedSelection(snapshot)
        } else {
            readLegacySelection(snapshot)
        } ?: defaultSelection()

        return ProviderSelectionMigrationResult(
            selection = selection,
            customHostname = (selection as? DnsProviderSelection.Custom)?.hostname
                ?: PrivateDnsHostname.parse(snapshot.customHostname)?.ascii,
            enhancedHostname = PrivateDnsHostname.parsePreservingCase(
                snapshot.enhancedHostname
            )?.ascii,
            migrationVersion = CURRENT_VERSION,
        )
    }

    private fun readTypedSelection(snapshot: LegacyProviderSnapshot): DnsProviderSelection? {
        val providerId = snapshot.selectedProviderId?.let(::ProviderId) ?: return null
        if (providerId.value == CUSTOM_PROVIDER_ID) {
            return PrivateDnsHostname.parse(snapshot.customHostname)?.let {
                DnsProviderSelection.Custom(it.ascii)
            }
        }

        catalog.enhancedProviders.firstOrNull { it.id == providerId }?.let {
            return DnsProviderSelection.Enhanced(providerId)
        }

        val provider = catalog.standardProvider(providerId) ?: return null
        val presetId = snapshot.selectedPresetId?.let(::ResolverPresetId)
            ?: provider.defaultPresetId
        return provider.presets
            .firstOrNull { it.id == presetId }
            ?.let { DnsProviderSelection.Standard(providerId, presetId) }
    }

    private fun readLegacySelection(snapshot: LegacyProviderSnapshot): DnsProviderSelection? =
        when (snapshot.selectedProviderId) {
            "adguard_default", "adguard" -> standardSelection("adguard", "default")
            "google" -> standardSelection("google", "default")
            "cloudflare" -> DnsProviderSelection.Custom(
                PrivateDnsHostname.parse(snapshot.customHostname)?.ascii
                    ?: LEGACY_CLOUDFLARE_HOSTNAME
            )

            "nextdns" -> DnsProviderSelection.Enhanced(DnsProviderCatalog.NEXTDNS)
            CUSTOM_PROVIDER_ID -> customSelection(snapshot.customHostname)
            null -> knownSelectionOrCustom(snapshot.customHostname)
            else -> customSelection(snapshot.customHostname)
        }

    private fun knownSelectionOrCustom(hostname: String?): DnsProviderSelection? {
        val normalized = PrivateDnsHostname.parse(hostname)?.ascii ?: return null
        catalog.standardProviders.forEach { provider ->
            provider.presets.firstOrNull {
                PrivateDnsHostname.parse(it.hostname)?.ascii
                    ?.equals(normalized, ignoreCase = true) == true
            }?.let { preset ->
                return DnsProviderSelection.Standard(provider.id, preset.id)
            }
        }
        return DnsProviderSelection.Custom(normalized)
    }

    private fun customSelection(hostname: String?): DnsProviderSelection.Custom? =
        PrivateDnsHostname.parse(hostname)?.let { DnsProviderSelection.Custom(it.ascii) }

    private fun standardSelection(
        providerId: String,
        presetId: String,
    ): DnsProviderSelection.Standard? {
        val provider = catalog.standardProvider(ProviderId(providerId)) ?: return null
        val typedPresetId = ResolverPresetId(presetId)
        return provider.presets
            .firstOrNull { it.id == typedPresetId }
            ?.let { DnsProviderSelection.Standard(provider.id, typedPresetId) }
    }

    private fun defaultSelection(): DnsProviderSelection.Standard {
        val provider = catalog.standardProviders.first()
        return DnsProviderSelection.Standard(provider.id, provider.defaultPresetId)
    }

    companion object {
        const val CURRENT_VERSION = 1
        const val CUSTOM_PROVIDER_ID = "custom"
        const val LEGACY_CLOUDFLARE_HOSTNAME = "cloudflare-dns.com"
    }
}
