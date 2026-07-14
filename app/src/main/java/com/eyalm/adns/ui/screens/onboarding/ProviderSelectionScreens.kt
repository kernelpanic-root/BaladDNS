package com.eyalm.adns.ui.screens.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.eyalm.adns.R
import com.eyalm.adns.data.activation.ActivationMode
import com.eyalm.adns.data.provider.DnsProviderCatalog
import com.eyalm.adns.data.provider.DnsProviderSelection
import com.eyalm.adns.data.provider.PrivateDnsHostname
import com.eyalm.adns.data.provider.StandardProviderDefinition
import com.eyalm.adns.ui.components.RadioSettingRow
import com.eyalm.adns.ui.components.SegmentPosition
import com.eyalm.adns.ui.components.OnboardingTemplate
import com.eyalm.adns.ui.components.StandardBottomBar
import com.eyalm.adns.ui.components.settings.ProviderPresetSelection
import com.eyalm.adns.ui.components.settings.ProviderSelection
import com.eyalm.adns.ui.components.settings.ProviderSelectionAction
import com.eyalm.adns.ui.theme.pageTitle

@Composable
fun OnboardingProviderScreen(
    currentSelection: DnsProviderSelection?,
    onBack: () -> Unit,
    onSelected: (DnsProviderSelection) -> Unit,
) {
    val catalog = remember { DnsProviderCatalog.default }
    var isCustomSelected by remember(currentSelection) {
        mutableStateOf(currentSelection is DnsProviderSelection.Custom)
    }
    var customHostname by remember(currentSelection) {
        mutableStateOf(
            (currentSelection as? DnsProviderSelection.Custom)?.hostname.orEmpty()
        )
    }
    val normalizedCustom = remember(customHostname) {
        PrivateDnsHostname.parse(customHostname)?.ascii
    }
    OnboardingTemplate(
        onBackClick = onBack,
        bottomBarContent = {
            StandardBottomBar(
                message = stringResource(R.string.custom_hostname_advanced),
                buttonText = stringResource(R.string.confirm),
                enabled = isCustomSelected && normalizedCustom != null,
                onNextClick = {
                    normalizedCustom?.let {
                        onSelected(DnsProviderSelection.Custom(it))
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item {
                Text(
                    stringResource(R.string.choose_provider),
                    style = MaterialTheme.typography.pageTitle,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
            item {
                Text(
                    stringResource(R.string.choose_provider_description),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            ProviderSelection(
                catalog = catalog,
                currentSelection = currentSelection,
                isCustomSelected = isCustomSelected,
                onProviderClick = { action ->
                    isCustomSelected = false
                    onSelected(
                        when (action) {
                            is ProviderSelectionAction.Apply -> action.selection
                            is ProviderSelectionAction.OpenPresets ->
                                DnsProviderSelection.Standard(
                                    action.provider.id,
                                    action.selectedPresetId
                                        ?: action.provider.defaultPresetId,
                                )
                        }
                    )
                },
                customHostname = customHostname,
                onCustomHostnameChange = { customHostname = it },
                onCustomClick = { isCustomSelected = true },
                onCustomConfirm = {
                    normalizedCustom?.let {
                        onSelected(DnsProviderSelection.Custom(it))
                    }
                },
                isCustomHostnameValid = normalizedCustom != null,
                showCustomConfirmButton = false,
            )
        }
    }
}

@Composable
fun OnboardingPresetScreen(
    provider: StandardProviderDefinition,
    current: DnsProviderSelection.Standard,
    onBack: () -> Unit,
    onSelected: (DnsProviderSelection.Standard) -> Unit,
) {
    var selected by remember(current) { mutableStateOf(current.presetId) }
    OnboardingTemplate(
        onBackClick = onBack,
        bottomBarContent = {
            StandardBottomBar(
                message = stringResource(R.string.choose_preset),
                onNextClick = {
                    onSelected(DnsProviderSelection.Standard(provider.id, selected))
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item {
                Text(
                    stringResource(R.string.choose_preset),
                    style = MaterialTheme.typography.pageTitle,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                )
            }
            ProviderPresetSelection(
                provider = provider,
                selectedPresetId = selected,
                onPresetClick = { selected = it },
            )
        }
    }
}

@Composable
fun OnboardingActivationModeScreen(
    selectedMode: ActivationMode?,
    onBack: () -> Unit,
    onSelected: (ActivationMode) -> Unit,
) {
    var selected by remember(selectedMode) {
        mutableStateOf(selectedMode ?: ActivationMode.PrivilegedDnsControl)
    }
    OnboardingTemplate(
        onBackClick = onBack,
        bottomBarContent = {
            StandardBottomBar(
                message = stringResource(R.string.choose_activation_mode),
                onNextClick = { onSelected(selected) },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item {
                Text(
                    stringResource(R.string.choose_activation_mode),
                    style = MaterialTheme.typography.pageTitle,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                )
            }
            item {
                val isSelected = selected == ActivationMode.PrivilegedDnsControl
                RadioSettingRow(
                    title = stringResource(R.string.privileged_dns_control),
                    description = stringResource(R.string.privileged_dns_control_description),
                    selected = isSelected,
                    onClick = { selected = ActivationMode.PrivilegedDnsControl },
                    radio = { _, onClick ->
                        RadioButton(selected = isSelected, onClick = onClick)
                    },
                    position = SegmentPosition.First,
                )
            }
            item {
                val isSelected = selected == ActivationMode.NextDnsControlOnly
                RadioSettingRow(
                    title = stringResource(R.string.control_only_mode),
                    description = stringResource(R.string.control_only_mode_description),
                    selected = isSelected,
                    onClick = { selected = ActivationMode.NextDnsControlOnly },
                    radio = { _, onClick ->
                        RadioButton(selected = isSelected, onClick = onClick)
                    },
                    position = SegmentPosition.Last,
                )
            }
        }
    }
}
