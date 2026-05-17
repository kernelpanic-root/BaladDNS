package com.eyalm.adns.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eyalm.adns.data.models.DnsProvider
import com.eyalm.adns.ui.components.SelectableCard
import com.eyalm.adns.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AccountSettingsScreen(
    onBack: () -> Unit = {},
    provider: DnsProvider
) {
    val viewModel: SettingsViewModel = viewModel()
    var email by remember { mutableStateOf<String?>(null) }
    val blocklists = viewModel.blocklists ?: emptyList()


    LaunchedEffect(Unit) {
        viewModel.getBlocklists()
        email = viewModel.getEmail()
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
            item {
                Text(
                    text = "${provider.name} Settings",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 48.dp, bottom = 16.dp),
                    fontSize = 32.sp,
                )
            }

            item {
                Text(
                    "Currently logged in to ${email}"
                )
            }

            item {
                Button(
                    onClick = {
                    }
                ) {
                    Text("Logout")
                }
            }

            blocklists.forEach { Blocklist -> // TODO move this to an independent page and implement logic
                item {
                    SelectableCard(
                        title = Blocklist.name.toString(),
                        description = Blocklist.description.toString(),
                        selected = Blocklist.isEnabled,
                        onClick = {
                            when {
                                Blocklist.isEnabled -> {
                                    viewModel.removeBlocklists(Blocklist.id)
                                }
                                else -> {
                                    viewModel.updateBlocklists(Blocklist.id)
                                }
                            }
                            viewModel.getBlocklists()
                        }
                    )
                }
            }


        }
    }
}