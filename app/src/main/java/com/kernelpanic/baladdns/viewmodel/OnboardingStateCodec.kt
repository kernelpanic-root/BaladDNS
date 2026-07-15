package com.kernelpanic.baladdns.viewmodel

import com.kernelpanic.baladdns.data.activation.ActivationMode
import com.kernelpanic.baladdns.data.provider.DnsProviderSelection
import com.kernelpanic.baladdns.data.provider.ProviderId
import com.kernelpanic.baladdns.data.provider.ResolverPresetId

object OnboardingStateCodec {
    private const val STEP = "step"
    private const val PROVIDER_KIND = "provider_kind"
    private const val PROVIDER_ID = "provider_id"
    private const val PRESET_ID = "preset_id"
    private const val CUSTOM_HOSTNAME = "custom_hostname"
    private const val MODE = "mode"
    private const val METHOD = "method"
    private const val PROFILE_ID = "profile_id"

    fun encode(state: OnboardingFlowState): Map<String, String?> {
        val selection = state.draft.providerSelection
        return mapOf(
            STEP to state.step.name,
            PROVIDER_KIND to when (selection) {
                is DnsProviderSelection.Standard -> "standard"
                is DnsProviderSelection.Enhanced -> "enhanced"
                is DnsProviderSelection.Custom -> "custom"
                null -> null
            },
            PROVIDER_ID to when (selection) {
                is DnsProviderSelection.Standard -> selection.providerId.value
                is DnsProviderSelection.Enhanced -> selection.providerId.value
                is DnsProviderSelection.Custom,
                null,
                -> null
            },
            PRESET_ID to (selection as? DnsProviderSelection.Standard)?.presetId?.value,
            CUSTOM_HOSTNAME to (selection as? DnsProviderSelection.Custom)?.hostname,
            MODE to state.draft.mode?.storageValue,
            METHOD to state.draft.privilegedMethod?.name,
            PROFILE_ID to state.draft.nextDnsProfileId,
        )
    }

    fun decode(values: Map<String, String?>): OnboardingFlowState {
        val selection = when (values[PROVIDER_KIND]) {
            "standard" -> {
                val providerId = values[PROVIDER_ID] ?: return OnboardingFlowState()
                val presetId = values[PRESET_ID] ?: return OnboardingFlowState()
                DnsProviderSelection.Standard(
                    ProviderId(providerId),
                    ResolverPresetId(presetId),
                )
            }

            "enhanced" -> values[PROVIDER_ID]
                ?.let(::ProviderId)
                ?.let(DnsProviderSelection::Enhanced)

            "custom" -> values[CUSTOM_HOSTNAME]?.let(DnsProviderSelection::Custom)
            else -> null
        }
        return OnboardingFlowState(
            step = OnboardingStep.entries.firstOrNull { it.name == values[STEP] }
                ?: OnboardingStep.Welcome,
            draft = OnboardingDraft(
                providerSelection = selection,
                mode = ActivationMode.entries.firstOrNull {
                    it.storageValue == values[MODE]
                },
                privilegedMethod = PrivilegedMethod.entries.firstOrNull {
                    it.name == values[METHOD]
                },
                nextDnsProfileId = values[PROFILE_ID],
            ),
        )
    }
}
