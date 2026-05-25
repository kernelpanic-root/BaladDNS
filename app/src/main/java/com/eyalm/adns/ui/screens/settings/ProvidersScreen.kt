package com.eyalm.adns.ui.screens.settings

import android.util.Patterns
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eyalm.adns.data.models.DnsProvider
import com.eyalm.adns.data.models.DnsProviders
import com.eyalm.adns.ui.components.ExpressiveListItem
import com.eyalm.adns.ui.theme.pageTitle
import com.eyalm.adns.viewmodel.SettingsViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable

fun ProvidersScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = { },
    onEnhancedModeClick: (providerId: String) -> Unit = { },
) {

    val viewModel: SettingsViewModel = viewModel()
    val currentProvider by viewModel.selectedProvider.collectAsState()
    val focusManager = LocalFocusManager.current

    val listState = rememberLazyListState()
    val isKeyboardVisible = WindowInsets.isImeVisible
    var isCustomSelected by remember { mutableStateOf(false) }

    LaunchedEffect(isKeyboardVisible, isCustomSelected) {
        if (isKeyboardVisible || isCustomSelected) {
            delay(200)
            val lastIndex = listState.layoutInfo.totalItemsCount - 1
            if (lastIndex >= 0) {
                listState.animateScrollToItem(lastIndex)
            }
        }
    }

    val provider = currentProvider

    val customUrlText = remember {
        mutableStateOf(
            when (provider) {
                is DnsProvider.Custom -> provider.userUrl
                else -> ""
            }
        )
    }

    val isCustomValid = customUrlText.value.isNotEmpty() && Patterns.DOMAIN_NAME.matcher(customUrlText.value).matches()
    // val isConfirmEnabled = isAdGuard.value || isCustomValid

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {},
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
        contentWindowInsets = WindowInsets.systemBars
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = innerPadding.calculateTopPadding(),
                bottom = innerPadding.calculateBottomPadding() + 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item { Text(
                text = "Providers",
                style = MaterialTheme.typography.pageTitle,
                modifier = Modifier.padding(top = 24.dp),
            ) }

            item {
                Text(text = "Select your preferred DNS provider", modifier = Modifier.padding(vertical = 8.dp))
            }


            DnsProviders.getAllProviders.forEach { provider ->
                item {
                    ExpressiveListItem(
                        title = provider.name + if (provider.name == "NextDNS") " (Recommended)" else "",
                        description = provider.description,
                        isSelected = provider.id == currentProvider.id && !isCustomSelected,
                        onClick = {
                            if (!provider.isEnhanced) {
                                viewModel.setProvider(provider.id)
                                isCustomSelected = false
                            } else {
                                if (viewModel.isLoggedIn(provider)) {
                                    viewModel.setProvider(provider.id)
                                    isCustomSelected = false
                                }
                                else if (provider.id != currentProvider.id) {
                                    onEnhancedModeClick(provider.id)
                                }
                            }
                        },
                        altLeadingContent = {
                            RadioButton(provider.id == currentProvider.id && !isCustomSelected, {
                                if (!provider.isEnhanced) {
                                    viewModel.setProvider(provider.id)
                                    isCustomSelected = false
                                } else {
                                    if (viewModel.isLoggedIn(provider)) {
                                        viewModel.setProvider(provider.id)
                                        isCustomSelected = false
                                    }
                                    else if (provider.id != currentProvider.id) {
                                        onEnhancedModeClick(provider.id)
                                    }
                                }
                            })
                        },
                        isFirst = DnsProviders.getAllProviders.first() == provider,
                    )
                }
            }

            item {
                ExpressiveListItem(
                    altContent = @Composable {
                        if (currentProvider.id == "custom" || isCustomSelected) {
                            Column {
                                OutlinedTextField(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 16.dp),
                                    value = customUrlText.value,
                                    onValueChange = {
                                        customUrlText.value = it
                                        // isAdGuard.value = false
                                    },
                                    isError = !isCustomValid && customUrlText.value != "", // !isAdGuard.value && !isCustomValid,
                                    placeholder = { Text("Enter a hostname...") },
                                    shape = RoundedCornerShape(12.dp)
                                )
                                Button(
                                    onClick = {
                                        if (isCustomValid) {
                                            viewModel.setProvider("custom", url = customUrlText.value)
                                            onBack()
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp, bottom = 8.dp)
                                        .height(48.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isCustomValid) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                                        contentColor = if (isCustomValid) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Confirm", fontWeight = FontWeight.Bold)
                                }

                            }

                        }

                    },
                    isSelected = currentProvider.id == "custom" || isCustomSelected,
                    onClick = {
                        isCustomSelected = true
                    },
                    altLeadingContent = {
                        RadioButton(provider.id == "custom" || isCustomSelected, {isCustomSelected = true})
                    },
                    title = "Custom Hostname (advanced)",
                    description = "Use a custom hostname for the DNS server",
                    isLast = true,
                )
            }
        }
    }
}