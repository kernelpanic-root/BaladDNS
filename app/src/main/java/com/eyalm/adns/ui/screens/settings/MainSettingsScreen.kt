package com.eyalm.adns.ui.screens.settings


import android.content.ActivityNotFoundException
import android.content.Intent
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.BroadcastOnPersonal
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eyalm.adns.BuildConfig
import com.eyalm.adns.R
import com.eyalm.adns.data.AppRuntimeRepositories
import com.eyalm.adns.data.dns.DnsDisableBehavior
import com.eyalm.adns.data.nextdns.resources.NextDnsResourceRegistry
import com.eyalm.adns.data.provider.DnsProviderCatalog
import com.eyalm.adns.data.provider.DnsProviderSelection
import com.eyalm.adns.ui.components.ExpressiveListItem
import com.eyalm.adns.ui.components.dialogs.BaseDialog
import com.eyalm.adns.ui.theme.pageTitle
import com.eyalm.adns.ui.theme.settingsLabel
import com.eyalm.adns.viewmodel.SettingsViewModel
import com.eyalm.adns.viewmodel.SettingsViewModel.Page


@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MainSettingsScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onAddQuickTile: () -> Unit = {},
    currentPage: Page = Page.MAIN,
    onPageChange: (Page) -> Unit = {},
    innerPadding: PaddingValues
) {
    val viewModel: SettingsViewModel = viewModel()
    val provider by viewModel.selectedProvider.collectAsState()
    val disableBehavior by viewModel.disableBehavior.collectAsState()
    val context = LocalContext.current
    val capabilities by remember(context) {
        AppRuntimeRepositories.capabilities(context.applicationContext)
    }.state.collectAsState()
    var disableBehaviorDialogVisible by remember { mutableStateOf(false) }
    var pendingDisableBehavior by remember(disableBehavior) {
        mutableStateOf(disableBehavior)
    }

    if (disableBehaviorDialogVisible) {
        BaseDialog(
            title = stringResource(R.string.dns_toggling_mode),
            confirmLabel = stringResource(R.string.confirm),
            destructive = false,
            onConfirm = {
                viewModel.setDisableBehavior(pendingDisableBehavior)
                disableBehaviorDialogVisible = false
            },
            onDismiss = { disableBehaviorDialogVisible = false },
        ) {
            ExpressiveListItem(
                title = stringResource(R.string.dns_disable_off),
                description = stringResource(R.string.dns_disable_off_description),
                isSelected = pendingDisableBehavior == DnsDisableBehavior.Off,
                onClick = { pendingDisableBehavior = DnsDisableBehavior.Off },
                isFirst = true,
            )
            Spacer(Modifier.height(4.dp))
            ExpressiveListItem(
                title = stringResource(R.string.dns_disable_automatic),
                description = stringResource(R.string.dns_disable_automatic_description),
                isSelected = pendingDisableBehavior == DnsDisableBehavior.Automatic,
                onClick = { pendingDisableBehavior = DnsDisableBehavior.Automatic },
                isLast = true,
            )
        }
    }



    val onAccountSettingsClick = remember(onPageChange) { { onPageChange(Page.ACCOUNT_SETTINGS) } }
    val onSetupClick = remember(onPageChange) { { onPageChange(Page.SETUP) } }
    val onProvidersClick = remember(onPageChange) { { onPageChange(Page.PROVIDERS) } }
    val onActivationClick = remember(onPageChange) { { onPageChange(Page.ACTIVATION) } }
    val onNotificationSettingsClick = remember(onPageChange) { { onPageChange(Page.NOTIFICATIONS) } }
    val onWifiRulesClick = remember(onPageChange) { { onPageChange(Page.WIFI_RULES) } }
    val onSecurityClick = remember(onPageChange) { { onPageChange(Page.SECURITY) } }
    val onPrivacyClick = remember(onPageChange) { { onPageChange(Page.PRIVACY) } }
    val onDenylistClick = remember(onPageChange) {  { viewModel.openListScreen(NextDnsResourceRegistry.denylist) } }
    val onAllowlistClick = remember(onPageChange) {  { viewModel.openListScreen(NextDnsResourceRegistry.allowlist) } }
    val onParentalControlClick = remember(onPageChange) { { onPageChange(Page.PARENTAL_CONTROL) } }
    val onSettingsPageClick = remember(onPageChange) { { onPageChange(Page.SETTINGS_PAGE) } }
    val onLanguagePageClick = remember(onPageChange) { { onPageChange(Page.LANGUAGE) } }
    val onLogsClick =   remember(onPageChange) { { onPageChange(Page.LOGS)} }


        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            val isNextDns = (provider as? DnsProviderSelection.Enhanced)
                ?.providerId == DnsProviderCatalog.NEXTDNS
            val providerNameRes = DnsProviderCatalog.default.provider(provider)?.titleRes
                ?: R.string.custom_dns_hostname
            item {
                Text(
                    text = stringResource(R.string.settings),
                    style = MaterialTheme.typography.pageTitle,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 144.dp, bottom = 8.dp),
                )
            }

            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.provider_settings),
                        style = MaterialTheme.typography.settingsLabel,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
                    )
                    ExpressiveListItem(
                        onClick = onProvidersClick,
                        title = stringResource(R.string.change_provider),
                        description = stringResource(R.string.change_the_provider_to_use),
                        icon = Icons.Filled.BroadcastOnPersonal,
                        secondIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        isFirst = true,
                        isLast = !isNextDns,
                    )
                    if (isNextDns) {
                        ExpressiveListItem(
                            onClick = onAccountSettingsClick,
                            icon = Icons.Filled.AccountCircle,
                            secondIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                             title = stringResource(R.string.settings_1, stringResource(providerNameRes)),
                             description = stringResource(R.string.change_account_settings_for, stringResource(providerNameRes)),
                             isLast = true,
                        )
                        /*
                        ExpressiveListItem(
                            onClick = onBlocklistsClick,
                            icon = Icons.Filled.FilterList,
                            secondIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            title = "${provider.name} Blocklists",
                            description = "Change blocklists for ${provider.name}",
                        )
                         */
                    }
                }
            }



            if (isNextDns) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.nextdns_profile_settings),
                            style = MaterialTheme.typography.settingsLabel,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
                        )
                        ExpressiveListItem(
                            title = stringResource(R.string.setup_another_device),
                            description = stringResource(R.string.setup_nextdns_on_another_device),
                            onClick = onSetupClick,
                            icon = Icons.Filled.Devices,
                            secondIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            isFirst = true,
                        )
                        ExpressiveListItem(
                            title = stringResource(R.string.security),
                            description = stringResource(R.string.threat_protection_dns_rebinding_tlds),
                            onClick = onSecurityClick,
                            icon = Icons.Filled.Security,
                            secondIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        )
                        ExpressiveListItem(
                            title = stringResource(R.string.privacy),
                            description = stringResource(R.string.blocklists_trackers_affiliate_links),
                            onClick = onPrivacyClick,
                            icon = Icons.Filled.PrivacyTip,
                            secondIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        )
                        ExpressiveListItem(
                            title = stringResource(R.string.parental_control),
                            description = stringResource(R.string.safesearch_blocked_apps_categories),
                            onClick = onParentalControlClick,
                            icon = Icons.Filled.FamilyRestroom,
                            secondIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        )
                        ExpressiveListItem(
                            title = stringResource(R.string.allowlist),
                            description = stringResource(R.string.add_specific_domains_to_the_allowlist),
                            onClick = onAllowlistClick,
                            icon = Icons.Filled.Check,
                            secondIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        )
                        ExpressiveListItem(
                            title = stringResource(R.string.denylist),
                            description = stringResource(R.string.add_specific_domains_to_the_denylist),
                            onClick = onDenylistClick,
                            icon = Icons.Filled.Block,
                            secondIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        )
                        ExpressiveListItem(
                            title = stringResource(R.string.logs),
                            description = stringResource(R.string.view_your_dns_logs),
                            onClick = onLogsClick,
                            icon = Icons.AutoMirrored.Filled.List,
                            secondIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            )
                        ExpressiveListItem(
                            title = stringResource(R.string.general_profile_settings),
                            description = stringResource(R.string.logs_performance_block_page),
                            onClick = onSettingsPageClick,
                            icon = Icons.Filled.Tune,
                            secondIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            isLast = true
                        )
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.app_settings),
                        style = MaterialTheme.typography.settingsLabel,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
                    )
                    ExpressiveListItem(
                        onClick = onActivationClick,
                        icon = Icons.Filled.SettingsSuggest,
                        secondIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        title = stringResource(R.string.activation),
                        description = stringResource(R.string.manage_activation),
                        isFirst = true,
                    )
                    if (capabilities.canUseDnsToggleSurfaces) {
                        ExpressiveListItem(
                            title = stringResource(R.string.dns_toggling_mode),
                            description = stringResource(
                                if (disableBehavior == DnsDisableBehavior.Off) {
                                    R.string.dns_disable_off
                                } else {
                                    R.string.dns_disable_automatic
                                }
                            ),
                            icon = Icons.Filled.Tune,
                            secondIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            onClick = {
                                pendingDisableBehavior = disableBehavior
                                disableBehaviorDialogVisible = true
                            },
                        )
                        ExpressiveListItem(
                            onClick = onNotificationSettingsClick,
                            title = stringResource(R.string.state_notifications),
                            description = stringResource(R.string.enable_or_disable_blocker_state_notifications),
                            icon = Icons.Filled.Notifications,
                            secondIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        )
                        ExpressiveListItem(
                            onClick = onWifiRulesClick,
                            title = stringResource(R.string.wifi_rules),
                            description = stringResource(R.string.wifi_rules_enable_description),
                            icon = Icons.Filled.Wifi,
                            secondIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        )
                        ExpressiveListItem(
                            onClick = onAddQuickTile,
                            title = stringResource(R.string.add_the_quick_settings_tile),
                            description = stringResource(R.string.add_the_quick_settings_tile_to_your_device),
                            icon = Icons.Filled.SettingsSuggest,
                            secondIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight
                        )
                    }
                    ExpressiveListItem(
                        onClick = onLanguagePageClick,
                        title = stringResource(R.string.language),
                        description = stringResource(R.string.language_description),
                        icon = Icons.Filled.Language,
                        secondIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        isLast = true,
                    )
                }
            }


            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    onClick = {
                        val url = "https://github.com/eyalm2000/adns"
                        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                        try {
                            context.startActivity(intent)
                        } catch (e: ActivityNotFoundException) {
                            Log.e("Settings", "No browser found to open GitHub URL", e)
                        }
                    }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_adns_filled),
                            contentDescription = "App icon",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .size(64.dp)
                        )

                        Text(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(top = 12.dp, bottom = 4.dp),
                            text = "ADNS",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.version_ncreated_by_eyal_meirom, BuildConfig.VERSION_NAME),
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }

}
