package com.eyalm.adns.ui.screens.settings

import android.util.Patterns
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eyalm.adns.ui.components.ExpressiveListItem
import com.eyalm.adns.ui.theme.pageTitle
import com.eyalm.adns.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GenericListScreen(onBack: () -> Unit) {
    val viewModel: SettingsViewModel = viewModel()
    val listSetting = viewModel.currentListSetting ?: return
    val activeIds by viewModel.activeListIds.collectAsState()
    val availableItems by viewModel.availableItems.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var searchQuery by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }
    val errorMessage by viewModel.errorMessage.collectAsState(initial = null)

    val openAddDialog = remember { mutableStateOf(false) }
    val openRemoveDialog = remember { mutableStateOf(false) }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    when {
        openAddDialog.value -> {
            AddDialog(
                onDismissRequest = { openAddDialog.value = false },
                onConfirmation = { domain ->
                    viewModel.addCustomDomain(domain)
                    openAddDialog.value = false
                }
            )
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            if (listSetting.allowsCustomInput) {
                ExtendedFloatingActionButton(
                    icon = { Icon(Icons.Filled.Add, "add") },
                    text = { Text(text = "Add Item") },
                    onClick = {
                        openAddDialog.value = true
                    }
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        if (isLoading && availableItems.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularWavyProgressIndicator(modifier = Modifier.size(64.dp))
            }
        } else {
            val filteredItems = remember(searchQuery, availableItems) {
                availableItems.filter { item ->
                    item.name.contains(searchQuery, ignoreCase = true) ||
                            item.id.contains(searchQuery, ignoreCase = true) ||
                            (item.description?.contains(searchQuery, ignoreCase = true) == true)
                }
            }

            val checkboxItem = remember {
                @Composable { selected: Boolean, onClick: () -> Unit ->
                    Checkbox(
                        checked = selected,
                        onCheckedChange = { onClick() }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
            ) {
                item {
                    Text(
                        text = listSetting.title(),
                        style = MaterialTheme.typography.pageTitle,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
                    )
                    Text(text = listSetting.description() , fontSize = 16.sp)
                    if (listSetting.allowsCustomInput) {
                        Text(text = "Hint: To remove items from the list, swipe to the left.", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
                if (availableItems.count() > 10 ) {
                    item {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search...") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)

                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                itemsIndexed(filteredItems, key = { _, item -> item.id }) { index, item ->

                    val dismissState = rememberSwipeToDismissBoxState()

                    LaunchedEffect(dismissState.currentValue) {
                        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
                            viewModel.deleteCustomDomain(item.id)
                        }
                    }

                    val onItemClick = remember(item.id) {
                        { viewModel.toggleListItem(item.id) }
                    }
                    if (listSetting.allowsCustomInput) {
                        SwipeToDismissBox(
                            state = dismissState,
                            enableDismissFromStartToEnd = false,
                            enableDismissFromEndToStart = true,
                            backgroundContent = {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Transparent)
                                        .padding(horizontal = 20.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Delete,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        ) {

                            ExpressiveListItem(
                                title = item.name,
                                description = item.description,
                                isSelected = activeIds.contains(item.id),
                                onClick = onItemClick,
                                /* TODO
                                leadingIcon = {
                                    ListIconView(icon = item.icon)
                                }
                                 */
                                interactiveItem = checkboxItem,
                                isLast = index == filteredItems.lastIndex,
                                isFirst = index == 0,
                            )

                        }
                    } else {
                        ExpressiveListItem(
                            title = item.name,
                            description = item.description,
                            isSelected = activeIds.contains(item.id),
                            onClick = onItemClick,
                            /* TODO
                            leadingIcon = {
                                ListIconView(icon = item.icon)
                            }
                             */
                            interactiveItem = checkboxItem,
                            isLast = index == filteredItems.lastIndex,
                            isFirst = index == 0,
                        )

                    }
                    if (index != filteredItems.lastIndex) {
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
fun AddDialog(
    onDismissRequest: () -> Unit,
    onConfirmation: (domain: String) -> Unit,
) {
    var domain by remember { mutableStateOf("") }

    AlertDialog(
        icon = {
            Icon(imageVector=Icons.Default.Add, contentDescription = "Add")
        },
        title = {
            Text(text = "Add Item")
        },
        text = {
            Text(text = "")
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                value = domain,
                placeholder = { Text("Enter a domain...") },
                singleLine = true,
                onValueChange = {
                    domain = it
                },
                isError = !Patterns.DOMAIN_NAME.matcher(domain).matches() && domain != "",
                supportingText = {
                    if (!Patterns.DOMAIN_NAME.matcher(domain).matches() && domain != "") {
                        Text("Invalid Domain")
                    }
                },
                shape = RoundedCornerShape(12.dp)
            )
        },
        onDismissRequest = {
            onDismissRequest()
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (Patterns.DOMAIN_NAME.matcher(domain).matches()) {
                        onConfirmation(domain)
                    }
                }
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onDismissRequest()
                }
            ) {
                Text("Dismiss")
            }
        }
    )
}