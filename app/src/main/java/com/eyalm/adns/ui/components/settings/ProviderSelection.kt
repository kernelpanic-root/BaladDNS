package com.eyalm.adns.ui.components.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
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
import com.eyalm.adns.ui.components.ExpressiveListItem

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
    catalog.providers.forEachIndexed { index, provider ->
        val selected = currentSelection?.providerId == provider.id && !isCustomSelected
        item(key = provider.id.value) {
            ExpressiveListItem(
                title = stringResource(provider.titleRes) +
                    if (provider.id == DnsProviderCatalog.NEXTDNS) {
                        " ${stringResource(R.string.recommended)}"
                    } else {
                        ""
                    },
                description = stringResource(provider.descriptionRes),
                isSelected = selected,
                onClick = {
                    onProviderClick(
                        resolveProviderSelectionAction(provider, currentSelection)
                    )
                },
                altLeadingContent = {
                    RadioButton(
                        selected = selected,
                        onClick = null,
                    )
                },
                secondIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight.takeIf {
                    shouldShowProviderOptionsHint(provider, currentSelection)
                },
                isFirst = index == 0,
            )
        }
    }

    item(key = "custom") {
        ExpressiveListItem(
            altContent = {
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
            isSelected = isCustomSelected,
            onClick = onCustomClick,
            altLeadingContent = {
                RadioButton(
                    selected = isCustomSelected,
                    onClick = null,
                )
            },
            title = stringResource(R.string.custom_hostname_advanced),
            description = stringResource(R.string.use_a_custom_hostname_for_the_dns_server),
            isLast = true,
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
            ExpressiveListItem(
                title = stringResource(preset.titleRes),
                description = stringResource(preset.descriptionRes),
                isSelected = selected,
                onClick = { onPresetClick(preset.id) },
                altLeadingContent = {
                    RadioButton(
                        selected = selected,
                        onClick = null,
                    )
                },
                isFirst = index == 0,
                isLast = index == provider.presets.lastIndex,
            )
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
