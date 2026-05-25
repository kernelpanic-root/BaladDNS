package com.eyalm.adns.ui.screens.settings

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.BroadcastOnPersonal
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material.icons.filled.Tune
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eyalm.adns.BuildConfig
import com.eyalm.adns.R
import com.eyalm.adns.data.Allowlist
import com.eyalm.adns.data.DenyList
import com.eyalm.adns.data.models.DnsProviders
import com.eyalm.adns.ui.components.ExpressiveListItem
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
    permissionLauncher: ActivityResultLauncher<String>? = null,
    currentPage: Page = Page.MAIN,
    onPageChange: (Page) -> Unit = {},
    innerPadding: PaddingValues
) {
    val viewModel: SettingsViewModel = viewModel()
    val provider by viewModel.selectedProvider.collectAsState()
    val context = LocalContext.current

    val onAccountSettingsClick = remember(onPageChange) { { onPageChange(Page.ACCOUNT_SETTINGS) } }
    val onBlocklistsClick = remember(onPageChange) { { onPageChange(Page.BLOCKLISTS) } }
    val onProvidersClick = remember(onPageChange) { { onPageChange(Page.PROVIDERS) } }
    val onSecurityClick = remember(onPageChange) { { onPageChange(Page.SECURITY) } }
    val onPrivacyClick = remember(onPageChange) { { onPageChange(Page.PRIVACY) } }
    val onDenylistClick = remember(onPageChange) {  { viewModel.openListScreen(DenyList.lists.first()) } }
    val onAllowlistClick = remember(onPageChange) {  { viewModel.openListScreen(Allowlist.lists.first()) } }
    val onParentalControlClick = remember(onPageChange) { { onPageChange(Page.PARENTAL_CONTROL) } }
    val onSettingsPageClick = remember(onPageChange) { { onPageChange(Page.SETTINGS_PAGE) } }
    val onNotificationsClick = remember(permissionLauncher) {
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissionLauncher?.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }


        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            val isNextDns = provider == DnsProviders.NEXTDNS
            item {
                Text(
                    text = "Settings",
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
                        text = "PROVIDER SETTINGS",
                        style = MaterialTheme.typography.settingsLabel,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
                    )
                    ExpressiveListItem(
                        onClick = onProvidersClick,
                        title = "Change Provider",
                        description = "Change the provider to use",
                        icon = Icons.Filled.BroadcastOnPersonal,
                        secondIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        isFirst = true,
                        isLast = !isNextDns
                    )
                    if (isNextDns) {
                        ExpressiveListItem(
                            onClick = onAccountSettingsClick,
                            icon = Icons.Filled.AccountCircle,
                            secondIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            title = "${provider.name} Settings",
                            description = "Change account settings for ${provider.name}",
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
                            text = "NEXTDNS PROFILE SETTINGS",
                            style = MaterialTheme.typography.settingsLabel,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
                        )
                        ExpressiveListItem(
                            title = "Security",
                            description = "Threat protection, DNS rebinding, TLDs",
                            onClick = onSecurityClick,
                            icon = Icons.Filled.Security,
                            secondIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            isFirst = true
                        )
                        ExpressiveListItem(
                            title = "Privacy",
                            description = "Blocklists, trackers, affiliate links",
                            onClick = onPrivacyClick,
                            icon = Icons.Filled.PrivacyTip,
                            secondIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        )
                        ExpressiveListItem(
                            title = "Parental Control",
                            description = "SafeSearch, blocked apps, categories",
                            onClick = onParentalControlClick,
                            icon = Icons.Filled.FamilyRestroom,
                            secondIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        )
                        ExpressiveListItem(
                            title = "Allowlist",
                            description = "Add specific domains to the Allowlist.",
                            onClick = onAllowlistClick,
                            icon = Icons.Filled.Check,
                            secondIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        )
                        ExpressiveListItem(
                            title = "Denylist",
                            description = "Add specific domains to the denylist.",
                            onClick = onDenylistClick,
                            icon = Icons.Filled.Block,
                            secondIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        )
                        ExpressiveListItem(
                            title = "General Profile Settings",
                            description = "Logs, performance, block page",
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
                        text = "APP SETTINGS",
                        style = MaterialTheme.typography.settingsLabel,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
                    )
                    ExpressiveListItem(
                        onClick = onNotificationsClick,
                        title = "State Notifications",
                        description = "Enable or disable blocker state notifications",
                        icon = Icons.Filled.Notifications,
                        secondIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        isFirst = true
                    )
                    ExpressiveListItem(
                        onClick = onAddQuickTile,
                        title = "Add the quick settings tile",
                        description = "Add the quick settings tile to your device",
                        icon = Icons.Filled.SettingsSuggest,
                        secondIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        isLast = true
                    )
                }
            }


            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
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
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                                .padding(top = 12.dp, bottom = 4.dp),
                            text = "ADNS",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Version ${BuildConfig.VERSION_NAME}\nCreated by Eyal Meirom",
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
