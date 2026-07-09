package com.eyalm.adns.data.dns

import com.eyalm.adns.data.PrivateDnsObservation
import com.eyalm.adns.data.isSelectedPrivateDnsActive
import com.eyalm.adns.data.provider.DnsProviderSelection
import com.eyalm.adns.data.provider.PrivateDnsHostname
import com.eyalm.adns.data.provider.ProviderId
import com.eyalm.adns.data.provider.ProviderSelectionRepository
import com.eyalm.adns.data.provider.ProviderSelectionUpdateResult

sealed interface DnsConfigurationResult {
    data class Changed(
        val selection: DnsProviderSelection,
        val appliedToDevice: Boolean,
    ) : DnsConfigurationResult

    data class StateChanged(
        val observation: PrivateDnsObservation,
    ) : DnsConfigurationResult

    data object PermissionMissing : DnsConfigurationResult

    data object MissingHostname : DnsConfigurationResult

    data object InvalidSelection : DnsConfigurationResult

    data class WriteFailed(val result: DnsWriteResult) : DnsConfigurationResult
}

class DnsConfigurationRepository(
    private val selectionRepository: ProviderSelectionRepository,
    private val privateDnsControl: PrivateDnsControl,
    private val disableBehavior: () -> DnsDisableBehavior,
) {
    fun observation(): PrivateDnsObservation = privateDnsControl.observe()

    fun isSelectedResolverActive(): Boolean = isSelectedPrivateDnsActive(
        observation = observation(),
        selectedHostname = selectionRepository.resolveHostname(),
    )

    suspend fun changeSelection(
        requested: DnsProviderSelection,
    ): DnsConfigurationResult {
        val selection = selectionRepository.validateSelection(requested)
            ?: return DnsConfigurationResult.InvalidSelection
        val targetHostname = selectionRepository.resolveHostname(selection)
            ?: return DnsConfigurationResult.MissingHostname
        val observed = observation()
        if (observed == PrivateDnsObservation.PermissionMissing) {
            return DnsConfigurationResult.PermissionMissing
        }

        val currentlyActive = isSelectedPrivateDnsActive(
            observation = observed,
            selectedHostname = selectionRepository.resolveHostname(),
        )
        if (currentlyActive) {
            return when (val write = privateDnsControl.enable(targetHostname)) {
                is DnsWriteResult.Success -> persistSelection(selection, appliedToDevice = true)
                DnsWriteResult.PermissionMissing -> DnsConfigurationResult.PermissionMissing
                DnsWriteResult.MissingHostname -> DnsConfigurationResult.MissingHostname
                is DnsWriteResult.Failure,
                is DnsWriteResult.Rejected,
                -> DnsConfigurationResult.WriteFailed(write)
            }
        }

        return persistSelection(selection, appliedToDevice = false)
    }

    suspend fun changeEnhancedSelection(
        providerId: ProviderId,
        hostname: String,
    ): DnsConfigurationResult {
        val selection = DnsProviderSelection.Enhanced(providerId)
        if (selectionRepository.validateSelection(selection) == null) {
            return DnsConfigurationResult.InvalidSelection
        }
        val normalizedHostname = PrivateDnsHostname.parsePreservingCase(hostname)?.ascii
            ?: return DnsConfigurationResult.MissingHostname
        val observed = observation()
        if (observed == PrivateDnsObservation.PermissionMissing) {
            return DnsConfigurationResult.PermissionMissing
        }
        val currentlyActive = isSelectedPrivateDnsActive(
            observation = observed,
            selectedHostname = selectionRepository.resolveHostname(),
        )
        if (currentlyActive) {
            return when (val write = privateDnsControl.enable(normalizedHostname)) {
                is DnsWriteResult.Success -> persistEnhancedSelection(
                    selection = selection,
                    hostname = normalizedHostname,
                    appliedToDevice = true,
                )

                DnsWriteResult.PermissionMissing -> DnsConfigurationResult.PermissionMissing
                DnsWriteResult.MissingHostname -> DnsConfigurationResult.MissingHostname
                is DnsWriteResult.Failure,
                is DnsWriteResult.Rejected,
                -> DnsConfigurationResult.WriteFailed(write)
            }
        }
        return persistEnhancedSelection(
            selection = selection,
            hostname = normalizedHostname,
            appliedToDevice = false,
        )
    }

    suspend fun toggle(): DnsConfigurationResult {
        val observed = observation()
        if (observed == PrivateDnsObservation.PermissionMissing) {
            return DnsConfigurationResult.PermissionMissing
        }

        val write = if (
            isSelectedPrivateDnsActive(observed, selectionRepository.resolveHostname())
        ) {
            privateDnsControl.disable(disableBehavior())
        } else {
            val hostname = selectionRepository.resolveHostname()
                ?: return DnsConfigurationResult.MissingHostname
            privateDnsControl.enable(hostname)
        }

        return when (write) {
            is DnsWriteResult.Success -> DnsConfigurationResult.StateChanged(write.observation)
            DnsWriteResult.PermissionMissing -> DnsConfigurationResult.PermissionMissing
            DnsWriteResult.MissingHostname -> DnsConfigurationResult.MissingHostname
            is DnsWriteResult.Failure,
            is DnsWriteResult.Rejected,
            -> DnsConfigurationResult.WriteFailed(write)
        }
    }

    private fun persistSelection(
        selection: DnsProviderSelection,
        appliedToDevice: Boolean,
    ): DnsConfigurationResult = when (selectionRepository.save(selection)) {
        is ProviderSelectionUpdateResult.Saved -> DnsConfigurationResult.Changed(
            selection = selection,
            appliedToDevice = appliedToDevice,
        )

        is ProviderSelectionUpdateResult.Invalid -> DnsConfigurationResult.InvalidSelection
    }

    private fun persistEnhancedSelection(
        selection: DnsProviderSelection.Enhanced,
        hostname: String,
        appliedToDevice: Boolean,
    ): DnsConfigurationResult {
        if (
            selectionRepository.setEnhancedHostname(hostname) !is
            ProviderSelectionUpdateResult.Saved
        ) {
            return DnsConfigurationResult.MissingHostname
        }
        return persistSelection(selection, appliedToDevice)
    }
}
