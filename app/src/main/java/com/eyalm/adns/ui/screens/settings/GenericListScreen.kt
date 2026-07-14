package com.eyalm.adns.ui.screens.settings

import android.text.format.DateUtils
import android.util.Patterns
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eyalm.adns.R
import com.eyalm.adns.data.Locales
import com.eyalm.adns.data.nextdns.model.ListIcon
import com.eyalm.adns.data.nextdns.resources.BlocklistSort
import com.eyalm.adns.data.nextdns.resources.NextDnsResourceItem
import com.eyalm.adns.data.nextdns.resources.filterResourceItems
import com.eyalm.adns.data.nextdns.resources.orderResourceItems
import com.eyalm.adns.data.nextdns.resources.updatedInstantOrNull
import com.eyalm.adns.ui.components.ExpressiveIcon
import com.eyalm.adns.ui.components.ListIconView
import com.eyalm.adns.ui.components.ResourceSettingRow
import com.eyalm.adns.ui.components.SegmentPosition
import com.eyalm.adns.ui.components.ToggleSettingRow
import com.eyalm.adns.ui.components.dialogs.DestructiveConfirmationDialog
import com.eyalm.adns.ui.components.dialogs.FormDialog
import com.eyalm.adns.ui.components.settings.LoadingError
import com.eyalm.adns.ui.theme.pageTitle
import com.eyalm.adns.ui.theme.settingsLabel
import com.eyalm.adns.viewmodel.ProfileSessionState
import com.eyalm.adns.viewmodel.SettingsViewModel
import com.eyalm.adns.viewmodel.nextdns.ResourceListViewModel
import java.text.NumberFormat

private sealed interface ResourceRow {
    data class Header(val key: String, val title: String) : ResourceRow
    data class Item(val value: NextDnsResourceItem) : ResourceRow
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GenericListScreen(
    onBack: () -> Unit,
    profileState: ProfileSessionState,
) {
    val navigationViewModel: SettingsViewModel = viewModel()
    val listSetting = navigationViewModel.currentListSetting ?: return
    val profile = profileState.selected ?: return
    val listViewModel: ResourceListViewModel = viewModel()
    val listState by listViewModel.state.collectAsState()
    val context = LocalContext.current
    val activeIds = listState.activeIds
    val availableItems = listState.availableItems
    val isLoading =
        listState.profileId != profile.id ||
            listState.spec != listSetting ||
            listState.loading
    val refreshing = listState.refreshing
    val listError = listState.error
    val canEdit = profileState.capabilities.canEditSettings
    val snackbarHostState = remember { SnackbarHostState() }
    val errorMessage by listViewModel.messages.collectAsState(initial = null)

    var searchQuery by remember(listSetting) { mutableStateOf("") }
    var addDialogVisible by remember { mutableStateOf(false) }
    var pendingRemoval by remember { mutableStateOf<String?>(null) }
    var blocklistSort by remember(listSetting) { mutableStateOf(BlocklistSort.Popularity) }
    var sortMenuVisible by remember(listSetting) { mutableStateOf(false) }
    var showConfig by remember(listSetting) { mutableStateOf(false) }
    var showEnabledOnly by remember(listSetting) { mutableStateOf(false) }

    LaunchedEffect(profile.id, listSetting, canEdit) {
        listViewModel.load(
            profileId = profile.id,
            spec = listSetting,
            editable = canEdit,
        )
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let { snackbarHostState.showSnackbar(it) }
    }

    if (addDialogVisible) {
        AddDialog(
            onDismissRequest = { addDialogVisible = false },
            onConfirmation = { domain ->
                listViewModel.addCustomDomain(domain)
                addDialogVisible = false
            },
        )
    }

    pendingRemoval?.let { domain ->
        DestructiveConfirmationDialog(
            title = stringResource(R.string.remove, domain),
            body = stringResource(R.string.remove_list_item_confirmation),
            confirmLabel = Locales.getString("global", "remove"),
            onConfirm = {
                pendingRemoval = null
                listViewModel.deleteCustomDomain(domain)
            },
            onDismiss = { pendingRemoval = null },
        )
    }

    SettingsScreenLayout(
        title = listSetting.title(context),
        onBack = onBack,
        showAppBarTitle = false,
        refreshing = refreshing,
        onRefresh = listViewModel::refresh,
        snackbarHostState = snackbarHostState,
        floatingActionButton = {
            if (listSetting.allowsCustomInput && canEdit) {
                ExtendedFloatingActionButton(
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.add_item)) },
                    onClick = { addDialogVisible = true },
                )
            }
        },
    ) { innerPadding ->
        if (isLoading && availableItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularWavyProgressIndicator(modifier = Modifier.size(64.dp))
            }
        } else {
            val orderedItems = remember(availableItems, listSetting, blocklistSort) {
                orderResourceItems(
                    items = availableItems,
                    feature = listSetting.apiFeature,
                    blocklistSort = blocklistSort,
                )
            }
            val filteredItems = remember(
                searchQuery,
                orderedItems,
                showEnabledOnly,
                activeIds,
            ) {
                filterResourceItems(
                    items = orderedItems,
                    query = searchQuery,
                    enabledOnly = showEnabledOnly && availableItems.size > 10,
                    activeIds = activeIds,
                )
            }
            val rows = remember(filteredItems, listSetting) {
                if (listSetting.apiFeature == "tlds") {
                    buildList<ResourceRow> {
                        val spamhaus = filteredItems.filter { it.spamhausRank > 0 }
                        val regular = filteredItems.filterNot { it.spamhausRank > 0 }
                        if (spamhaus.isNotEmpty()) {
                            add(
                                ResourceRow.Header(
                                    "spamhaus",
                                    Locales.getString("security", "tld", "sections", "spamhaus"),
                                )
                            )
                            addAll(spamhaus.map(ResourceRow::Item))
                        }
                        if (regular.isNotEmpty()) {
                            add(
                                ResourceRow.Header(
                                    "all",
                                    Locales.getString("security", "tld", "sections", "all"),
                                )
                            )
                            addAll(regular.map(ResourceRow::Item))
                        }
                    }
                } else {
                    filteredItems.map(ResourceRow::Item)
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
            ) {
                item {
                    Text(
                        text = listSetting.title(context),
                        style = MaterialTheme.typography.pageTitle,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
                    )
                    Text(text = listSetting.description(context), fontSize = 16.sp)
                    if (listSetting.allowsCustomInput && canEdit) {
                        Text(
                            text = stringResource(
                                R.string.hint_to_remove_items_from_the_list_swipe_to_the_left
                            ),
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                val hasListControls = availableItems.size > 10
                if (hasListControls) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min),
                        ) {
                            TextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text(stringResource(R.string.search)) },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                                    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    disabledIndicatorColor = Color.Transparent,
                                ),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            ExpressiveIcon(
                                icon = Icons.Default.Settings,
                                selected = showConfig,
                                bgcolor = MaterialTheme.colorScheme.surfaceContainer,
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .aspectRatio(1f)
                                    .clickable(
                                        onClick = { showConfig = !showConfig },
                                        role = Role.Button,
                                    ),
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                if (hasListControls && showConfig) {
                    if (listSetting.apiFeature == "blocklists") {
                        item {
                            ResourceSettingRow(
                                onClick = { sortMenuVisible = true },
                                title = stringResource(R.string.sort_by),
                                description = stringResource(R.string.change_blocklist_sort_order),
                                leading = { ExpressiveIcon(Icons.AutoMirrored.Filled.Sort) },
                                trailing = {
                                    Box {
                                        FilterChip(
                                            selected = true,
                                            onClick = { sortMenuVisible = true },
                                            label = { Text(blocklistSort.label()) },
                                        )
                                        DropdownMenu(
                                            expanded = sortMenuVisible,
                                            onDismissRequest = { sortMenuVisible = false },
                                        ) {
                                            BlocklistSort.entries.forEach { sort ->
                                                DropdownMenuItem(
                                                    text = { Text(sort.label()) },
                                                    onClick = {
                                                        blocklistSort = sort
                                                        sortMenuVisible = false
                                                    },
                                                )
                                            }
                                        }
                                    }
                                },
                                position = SegmentPosition.First,
                                alignment = Alignment.CenterVertically
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                    item {
                        ToggleSettingRow(
                            title = stringResource(R.string.enabled_only),
                            description = stringResource(R.string.show_only_enabled_items),
                            checked = showEnabledOnly,
                            leading = { ExpressiveIcon(Icons.Filled.Check) },
                            toggle = { checked, onCheckedChange ->
                                Switch(
                                    checked = checked,
                                    onCheckedChange = onCheckedChange,
                                )
                            },
                            onCheckedChange = { showEnabledOnly = it },
                            position = if (listSetting.apiFeature == "blocklists") {
                                SegmentPosition.Last
                            } else {
                                SegmentPosition.Single
                            },
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                listError?.let { error ->
                    item {
                        LoadingError(
                            error = error,
                            onRetry = listViewModel::refresh,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                itemsIndexed(
                    items = rows,
                    key = { _, row ->
                        when (row) {
                            is ResourceRow.Header -> "header-${row.key}"
                            is ResourceRow.Item -> "item-${row.value.id}"
                        }
                    },
                ) { index, row ->
                    when (row) {
                        is ResourceRow.Header -> {
                            Text(
                                text = row.title,
                                style = MaterialTheme.typography.settingsLabel,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 8.dp, top = 12.dp, bottom = 8.dp),
                            )
                        }

                        is ResourceRow.Item -> {
                            val previousIsHeader = index == 0 || rows[index - 1] is ResourceRow.Header
                            val nextIsHeader = index == rows.lastIndex || rows[index + 1] is ResourceRow.Header
                            ResourceItemRow(
                                item = row.value,
                                selected = row.value.id in activeIds,
                                canEdit = canEdit,
                                allowsRemoval = listSetting.allowsCustomInput,
                                showWebsite = listSetting.apiFeature != "blocklists",
                                position = when {
                                    previousIsHeader && nextIsHeader -> SegmentPosition.Single
                                    previousIsHeader -> SegmentPosition.First
                                    nextIsHeader -> SegmentPosition.Last
                                    else -> SegmentPosition.Middle
                                },
                                onToggle = { listViewModel.toggle(row.value.id) },
                                onRemove = { pendingRemoval = row.value.id },
                            )
                            if (!nextIsHeader) Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }

                    item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun ResourceItemRow(
    item: NextDnsResourceItem,
    selected: Boolean,
    canEdit: Boolean,
    allowsRemoval: Boolean,
    showWebsite: Boolean,
    position: SegmentPosition,
    onToggle: () -> Unit,
    onRemove: () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState()
    val content: @Composable () -> Unit = {
        ResourceSettingRow(
            title = item.name,
            description = item.description,
            selected = selected,
            onClick = { if (canEdit) onToggle() },
            leading = if (item.icon !is ListIcon.None) {
                { ListIconView(item.icon, modifier = Modifier.size(36.dp)) } // todo Same story here with 4dp - cortical
            } else null,
            trailing = {
                Checkbox(
                    checked = selected,
                    enabled = canEdit,
                    onCheckedChange = { if (canEdit) onToggle() },
                )
            },
            supporting = {
                ResourceMetadata(item, showWebsite)
            },
            position = position,
            alignment = Alignment.CenterVertically
        )
    }

    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
            onRemove()
            dismissState.reset()
        }
    }

    if (allowsRemoval && canEdit) {
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
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.remove_item),
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            },
            content = { content() },
        )
    } else {
        content()
    }
}

@Composable
private fun ResourceMetadata(item: NextDnsResourceItem, showWebsite: Boolean) {
    val metadata = buildList {
        item.entries?.let { count ->
            add(
                Locales.getPlainString(
                    path = arrayOf(
                        "privacy",
                        "blocklists",
                        if (count == 1) "entries" else "entries_plural",
                    ),
                    values = mapOf(
                        "count, number" to NumberFormat.getIntegerInstance().format(count)
                    ),
                )
            )
        }
        item.updatedInstantOrNull()?.let { instant ->
            val relative = DateUtils.getRelativeTimeSpanString(
                    instant.toEpochMilli(),
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS,
                ).toString()
            add(
                Locales.getPlainString(
                    path = arrayOf("privacy", "blocklists", "updated"),
                    values = mapOf("ago" to relative),
                )
            )
        }
        if (showWebsite) item.website?.let(::add)
    }
    if (metadata.isNotEmpty()) {
        Text(
            text = metadata.joinToString(" • "),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun BlocklistSort.label(): String = Locales.getString(
    "privacy",
    "blocklists",
    "sortBy",
    when (this) {
        BlocklistSort.Popularity -> "popularity"
        BlocklistSort.Name -> "name"
        BlocklistSort.Entries -> "entries"
        BlocklistSort.Recent -> "updated"
    },
)

@Composable
fun AddDialog(
    onDismissRequest: () -> Unit,
    onConfirmation: (domain: String) -> Unit,
) {
    var domain by remember { mutableStateOf("") }
    val valid = domain.isNotBlank() && Patterns.DOMAIN_NAME.matcher(domain).matches()

    FormDialog(
        title = stringResource(R.string.add_item),
        confirmLabel = stringResource(R.string.confirm),
        confirmEnabled = valid,
        onConfirm = { if (valid) onConfirmation(domain) },
        onDismiss = onDismissRequest,
    ) {
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            value = domain,
            placeholder = { Text(stringResource(R.string.enter_a_domain)) },
            singleLine = true,
            onValueChange = { domain = it },
            isError = domain.isNotEmpty() && !valid,
            supportingText = {
                if (domain.isNotEmpty() && !valid) {
                    Text(stringResource(R.string.invalid_domain))
                }
            },
            shape = RoundedCornerShape(12.dp),
        )
    }
}
