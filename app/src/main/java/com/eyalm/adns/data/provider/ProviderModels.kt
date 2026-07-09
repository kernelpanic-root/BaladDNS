package com.eyalm.adns.data.provider

import androidx.annotation.StringRes

@JvmInline
value class ProviderId(val value: String)

@JvmInline
value class ResolverPresetId(val value: String)

enum class ResolverFeature {
    Ads,
    Trackers,
    Malware,
    AdultContent,
    SafeSearch,
}

sealed interface ProviderDefinition {
    val id: ProviderId

    @get:StringRes
    val titleRes: Int

    @get:StringRes
    val descriptionRes: Int
}

data class ResolverPreset(
    val id: ResolverPresetId,
    val hostname: String,
    @param:StringRes val titleRes: Int,
    @param:StringRes val descriptionRes: Int,
    val features: Set<ResolverFeature> = emptySet(),
)

data class StandardProviderDefinition(
    override val id: ProviderId,
    @param:StringRes override val titleRes: Int,
    @param:StringRes override val descriptionRes: Int,
    val defaultPresetId: ResolverPresetId,
    val presets: List<ResolverPreset>,
) : ProviderDefinition

data class EnhancedProviderDefinition(
    override val id: ProviderId,
    @param:StringRes override val titleRes: Int,
    @param:StringRes override val descriptionRes: Int,
) : ProviderDefinition

sealed interface DnsProviderSelection {
    data class Standard(
        val providerId: ProviderId,
        val presetId: ResolverPresetId,
    ) : DnsProviderSelection

    data class Enhanced(
        val providerId: ProviderId,
    ) : DnsProviderSelection

    data class Custom(
        val hostname: String,
    ) : DnsProviderSelection
}

val DnsProviderSelection.providerId: ProviderId
    get() = when (this) {
        is DnsProviderSelection.Standard -> providerId
        is DnsProviderSelection.Enhanced -> providerId
        is DnsProviderSelection.Custom -> ProviderId(ProviderSelectionMigration.CUSTOM_PROVIDER_ID)
    }
