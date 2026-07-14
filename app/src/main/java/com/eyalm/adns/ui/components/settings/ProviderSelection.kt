package com.eyalm.adns.ui.components.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.eyalm.adns.R
import com.eyalm.adns.data.provider.DnsProviderCatalog
import com.eyalm.adns.data.provider.DnsProviderSelection
import com.eyalm.adns.data.provider.ProviderDefinition
import com.eyalm.adns.data.provider.ResolverPresetId
import com.eyalm.adns.data.provider.StandardProviderDefinition
import com.eyalm.adns.data.provider.providerId
import com.eyalm.adns.ui.components.RadioSettingRow
import com.eyalm.adns.ui.components.ResourceSettingRow
import com.eyalm.adns.ui.components.SegmentPosition
import com.eyalm.adns.ui.components.segmentPosition

fun LazyListScope.ProviderSelection(
    catalog: DnsProviderCatalog,
    currentSelection: DnsProviderSelection?,
    isCustomSelected: Boolean,
    onProviderClick: (ProviderSelectionAction) -> Unit,
    customHostname: String,
    onCustomHostnameChange: (String) -> Unit,
    onCustomClick: () -> Unit,
    onCustomConfirm: () -> Unit,
    isCustomHostnameValid: Boolean,
    showCustomConfirmButton: Boolean = true,
) {

    // TODO - take care of ResourceSettingRow here

    catalog.providers.forEachIndexed { index, provider ->
        val selected = currentSelection?.providerId == provider.id && !isCustomSelected
        item(key = provider.id.value) {
            ResourceSettingRow(
                title = stringResource(provider.titleRes) +
                    if (provider.id == DnsProviderCatalog.NEXTDNS) {
                        " ${stringResource(R.string.recommended)}"
                    } else {
                        ""
                    },
                description = stringResource(provider.descriptionRes),
                selected = selected,
                onClick = {
                    onProviderClick(
                        resolveProviderSelectionAction(provider, currentSelection)
                    )
                },
                leading = {
                    RadioButton(
                        selected = selected,
                        onClick = null,
                    )
                },
                trailing = if (shouldShowProviderOptionsHint(provider, currentSelection)) {
                    {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                        )
                    }
                } else {
                    null
                },
                position = segmentPosition(index, catalog.providers.size + 1),
            )
        }
    }

    item(key = "custom") {
        ResourceSettingRow(
            supporting = {
                if (isCustomSelected) {
                    Column {
                        OutlinedTextField(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            value = customHostname,
                            onValueChange = onCustomHostnameChange,
                            isError = customHostname.isNotEmpty() && !isCustomHostnameValid,
                            placeholder = {
                                Text(stringResource(R.string.enter_a_hostname))
                            },
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                        )
                        if (showCustomConfirmButton) {
                            Button(
                                onClick = onCustomConfirm,
                                enabled = isCustomHostnameValid,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp, bottom = 8.dp)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Text(
                                    stringResource(R.string.confirm),
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }
            },
            selected = isCustomSelected,
            onClick = onCustomClick,
            leading = {
                RadioButton(
                    selected = isCustomSelected,
                    onClick = null,
                )
            },
            title = stringResource(R.string.custom_hostname_advanced),
            description = stringResource(R.string.use_a_custom_hostname_for_the_dns_server),
            position = SegmentPosition.Last,
        )
    }
}

fun LazyListScope.ProviderPresetSelection(
    provider: StandardProviderDefinition,
    selectedPresetId: ResolverPresetId?,
    onPresetClick: (ResolverPresetId) -> Unit,
) {
    provider.presets.forEachIndexed { index, preset ->
        val selected = selectedPresetId == preset.id
        item(key = preset.id.value) {
            RadioSettingRow(
                title = stringResource(preset.titleRes),
                description = stringResource(preset.descriptionRes),
                selected = selected,
                onClick = { onPresetClick(preset.id) },
                radio = { _, onClick ->
                    RadioButton(
                        selected = selected,
                        onClick = onClick,
                    )
                },
                position = segmentPosition(index, provider.presets.size),
            )
            Spacer(Modifier.height(4.dp))
        }
    }
}

sealed interface ProviderSelectionAction {
    data class Apply(
        val selection: DnsProviderSelection,
    ) : ProviderSelectionAction

    data class OpenPresets(
        val provider: StandardProviderDefinition,
        val selectedPresetId: ResolverPresetId?,
    ) : ProviderSelectionAction
}

fun resolveProviderSelectionAction(
    provider: ProviderDefinition,
    currentSelection: DnsProviderSelection?,
): ProviderSelectionAction = when (provider) {
    is StandardProviderDefinition -> {
        val selectedPresetId = (currentSelection as? DnsProviderSelection.Standard)
            ?.takeIf { it.providerId == provider.id }
            ?.presetId
            ?.takeIf { presetId -> provider.presets.any { it.id == presetId } }
        if (provider.presets.size > 1) {
            ProviderSelectionAction.OpenPresets(provider, selectedPresetId)
        } else {
            ProviderSelectionAction.Apply(
                DnsProviderSelection.Standard(
                    provider.id,
                    selectedPresetId ?: provider.defaultPresetId,
                )
            )
        }
    }

    is com.eyalm.adns.data.provider.EnhancedProviderDefinition ->
        ProviderSelectionAction.Apply(DnsProviderSelection.Enhanced(provider.id))
}

fun shouldShowProviderOptionsHint(
    provider: ProviderDefinition,
    currentSelection: DnsProviderSelection?,
): Boolean = provider is StandardProviderDefinition &&
    provider.presets.size > 1 &&
    (currentSelection as? DnsProviderSelection.Standard)?.providerId == provider.id
