package com.eyalm.adns.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eyalm.adns.data.models.DnsProvider
import com.eyalm.adns.ui.components.SelectableCard
import com.eyalm.adns.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BlocklistsScreen(
    onBack: () -> Unit = {},
    provider: DnsProvider
) {
    val viewModel: SettingsViewModel = viewModel()
    val blocklists = viewModel.blocklists ?: emptyList()


    LaunchedEffect(Unit) {
        viewModel.getBlocklists()
    }


    Scaffold(
        modifier = Modifier,
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = {
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->


        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            if (blocklists.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillParentMaxHeight()
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        LoadingIndicator(modifier = Modifier.size(100.dp))
                    }
                }
            } else {
                item {
                    Text(
                        text = "${provider.name} Blocklists",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(top = 48.dp, bottom = 16.dp),
                        fontSize = 32.sp,
                    )
                }

                items(blocklists) { blocklist ->
                    SelectableCard(
                        title = blocklist.name.toString(),
                        description = blocklist.description.toString(),
                        selected = blocklist.isEnabled,
                        onClick = {
                            if (blocklist.isEnabled) {
                                viewModel.removeBlocklists(blocklist.id)
                            } else {
                                viewModel.updateBlocklists(blocklist.id)
                            }
                            viewModel.getBlocklists()
                        }
                    )
                }
            }
        }
    }
}