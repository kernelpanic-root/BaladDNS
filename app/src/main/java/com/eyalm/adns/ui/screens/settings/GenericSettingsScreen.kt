package com.eyalm.adns.ui.screens.settings

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eyalm.adns.R
import com.eyalm.adns.data.Locales
import com.eyalm.adns.data.nextdns.model.ListIcon
import com.eyalm.adns.data.nextdns.model.countryFlag
import com.eyalm.adns.data.nextdns.resources.NextDnsResourceSpec
import com.eyalm.adns.data.nextdns.settings.BooleanSettingSpec
import com.eyalm.adns.data.nextdns.settings.IntSelectSettingSpec
import com.eyalm.adns.data.nextdns.settings.LocaleBinding
import com.eyalm.adns.data.nextdns.settings.ProfileSettingSpec
import com.eyalm.adns.data.nextdns.settings.SelectOption
import com.eyalm.adns.data.nextdns.settings.SettingId
import com.eyalm.adns.data.nextdns.settings.SettingsPageSpec
import com.eyalm.adns.data.nextdns.settings.StringSelectSettingSpec
import com.eyalm.adns.ui.components.ExpressiveListItem
import com.eyalm.adns.ui.components.refresh.AdnsPullToRefresh
import com.eyalm.adns.ui.components.ListIconView
import com.eyalm.adns.ui.components.dialogs.BaseDialog
import com.eyalm.adns.ui.theme.pageTitle
import com.eyalm.adns.ui.theme.settingsLabel
import com.eyalm.adns.viewmodel.ProfileSessionState
import com.eyalm.adns.viewmodel.SettingsViewModel
import com.eyalm.adns.viewmodel.nextdns.ScalarSettingsViewModel
import com.google.gson.JsonElement

private sealed interface SettingSelector {
    data class IntSelector(val spec: IntSelectSettingSpec) : SettingSelector
    data class StringSelector(val spec: StringSelectSettingSpec) : SettingSelector
}


@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GenericCategoryScreen(
    title: String,
    settingsPage: SettingsPageSpec,
    profileState: ProfileSessionState,
    lists: List<NextDnsResourceSpec> = emptyList(),
    extraContent: (@Composable (profileState: ProfileSessionState, refreshRevision: Long) -> Unit)? = null,
    onBack: () -> Unit,
    onLogsCleared: () -> Unit = {},
) {
    val navigationViewModel: SettingsViewModel = viewModel()
    val scalarViewModel: ScalarSettingsViewModel = viewModel()
    val scalarSettings by scalarViewModel.state.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var selector by remember(settingsPage.page, profileState.selected?.id) {
        mutableStateOf<SettingSelector?>(null)
    }

    LaunchedEffect(
        settingsPage.page,
        profileState.selected?.id,
        profileState.capabilities.canEditSettings,
    ) {
        profileState.selected?.let { profile ->
            scalarViewModel.load(
                profileId = profile.id,
                pageSpec = settingsPage,
                editable = profileState.capabilities.canEditSettings,
            )
        }
    }

    LaunchedEffect(Unit) {
        scalarViewModel.messages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    when (val activeSelector = selector) {
        is SettingSelector.IntSelector -> {
            val spec = activeSelector.spec
            SettingSelectionDialog(
                title = spec.locale.title(context),
                options = spec.options,
                current = spec.options.find {
                    it.value == scalarSettings.values[spec.id]?.let(spec::decode)
                },
                onSelect = {
                    selector = null
                    scalarViewModel.changeInt(spec, it)
                },
                onDismiss = { selector = null },
            )
        }

        is SettingSelector.StringSelector -> {
            val spec = activeSelector.spec
            SettingSelectionDialog(
                title = spec.locale.title(context),
                options = spec.options,
                current = spec.options.find {
                    it.value == scalarSettings.values[spec.id]?.let(spec::decode)
                },
                onSelect = {
                    selector = null
                    scalarViewModel.changeString(spec, it)
                },
                onDismiss = { selector = null },
            )
        }

        null -> Unit
    }

    scalarSettings.pendingConfirmation?.let { pending ->
        val confirmation = pending.spec.confirmation ?: return@let
        BaseDialog(
            title = Locales.getString(*confirmation.titlePath.toTypedArray()),
            body = Locales.getString(*confirmation.bodyPath.toTypedArray()),
            confirmLabel = Locales.getString("global", "change"),
            destructive = confirmation.destructive,
            onConfirm = scalarViewModel::confirmPendingChange,
            onDismiss = scalarViewModel::cancelPendingChange
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            Locales.getString("global", "back"),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        val isLoading =
            profileState.loading ||
                    profileState.selected == null ||
                    scalarSettings.page != settingsPage.page ||
                    scalarSettings.profileId != profileState.selected.id ||
                    scalarSettings.loading
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularWavyProgressIndicator(modifier = Modifier.size(64.dp))
            }
        } else {
            AdnsPullToRefresh(
                refreshing = scalarSettings.refreshing,
                onRefresh = {
                    profileState.selected?.let { profile ->
                        scalarViewModel.load(
                            profileId = profile.id,
                            pageSpec = settingsPage,
                            editable = profileState.capabilities.canEditSettings,
                            force = true,
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                LazyColumn(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                item {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.pageTitle,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
                    )
                }

                if (lists.isNotEmpty()) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.lists),
                                style = MaterialTheme.typography.settingsLabel,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 8.dp, bottom = 8.dp),
                            )
                            lists.forEachIndexed { index, listSetting ->
                                ExpressiveListItem(
                                    title = listSetting.title(context),
                                    description = listSetting.description(context),
                                    onClick = { navigationViewModel.openListScreen(listSetting) },
                                    secondIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    isFirst = index == 0,
                                    isLast = index == lists.lastIndex,
                                )
                            }
                        }
                    }
                }

                val allGroups = settingsPage.settings.groupBy { it.api.path.first() }
                val visibleSettings = settingsPage.settings.filter { spec ->
                    spec.visibleWhen?.invoke(scalarSettings.values) ?: true
                }
                val visibleGroups = visibleSettings.groupBy { it.api.path.first() }
                val multiItemGroupKeys = allGroups
                    .filterValues { it.size > 1 }
                    .keys

                visibleGroups
                    .filterKeys { it in multiItemGroupKeys }
                    .forEach { (groupKey, settings) ->
                        item {
                            val logsActions: (@Composable () -> Unit)? =
                                if (settingsPage.page == "settings" && groupKey == "logs") {
                                    {
                                        profileState.selected?.let { profile ->
                                            LogsActionsSection(
                                                profile = profile,
                                                capabilities = profileState.capabilities,
                                                onLogsCleared = onLogsCleared,
                                            )
                                        }
                                    }
                                } else {
                                    null
                                }
                            ScalarSettingsGroup(
                                title = groupTitle(settingsPage.page, groupKey),
                                settings = settings,
                                values = scalarSettings.values,
                                saving = scalarSettings.saving,
                                editable = profileState.capabilities.canEditSettings,
                                onBooleanChange = scalarViewModel::changeBoolean,
                                onIntSelect = { selector = SettingSelector.IntSelector(it) },
                                onStringSelect = { selector = SettingSelector.StringSelector(it) },
                                footer = logsActions,
                            )
                        }
                    }

                val singleItemSettings = visibleGroups
                    .filterKeys { it !in multiItemGroupKeys }
                    .values
                    .flatten()
                if (singleItemSettings.isNotEmpty()) {
                    item {
                        ScalarSettingsGroup(
                            title = stringResource(
                                if (multiItemGroupKeys.isEmpty()) R.string.preferences
                                else R.string.other_preferences
                            ),
                            settings = singleItemSettings,
                            values = scalarSettings.values,
                            saving = scalarSettings.saving,
                            editable = profileState.capabilities.canEditSettings,
                            onBooleanChange = scalarViewModel::changeBoolean,
                            onIntSelect = { selector = SettingSelector.IntSelector(it) },
                            onStringSelect = { selector = SettingSelector.StringSelector(it) },
                        )
                    }
                }

                extraContent?.let { content ->
                    item { content(profileState, scalarSettings.refreshRevision) }
                }

                item {
                    Spacer(Modifier.height(12.dp))
                }

                }
            }
        }
    }
}

@Composable
private fun ScalarSettingsGroup(
    title: String,
    settings: List<ProfileSettingSpec<*>>,
    values: Map<SettingId, JsonElement>,
    saving: Set<SettingId>,
    editable: Boolean,
    onBooleanChange: (BooleanSettingSpec, Boolean) -> Unit,
    onIntSelect: (IntSelectSettingSpec) -> Unit,
    onStringSelect: (StringSelectSettingSpec) -> Unit,
    footer: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.settingsLabel,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp),
        )
        settings.forEachIndexed { index, spec ->
            ScalarSettingRow(
                spec = spec,
                rawValue = values[spec.id],
                saving = spec.id in saving,
                editable = editable,
                isFirst = index == 0,
                isLast = if (footer != null) false else index == settings.lastIndex,
                onBooleanChange = onBooleanChange,
                onIntSelect = onIntSelect,
                onStringSelect = onStringSelect,
            )
        }
        footer?.invoke()

    }
}

private fun groupTitle(page: String, group: String): String = when {
    page == "settings" && group == "blockPage" ->
        Locales.getString("settings", "blockpage", "name")

    else -> Locales.getString(page, group, "name")
}

@Composable
private fun ScalarSettingRow(
    spec: ProfileSettingSpec<*>,
    rawValue: JsonElement?,
    saving: Boolean,
    editable: Boolean,
    isFirst: Boolean,
    isLast: Boolean,
    onBooleanChange: (BooleanSettingSpec, Boolean) -> Unit,
    onIntSelect: (IntSelectSettingSpec) -> Unit,
    onStringSelect: (StringSelectSettingSpec) -> Unit,
) {
    when (spec) {
        is BooleanSettingSpec -> {
            val checked = rawValue?.let(spec::decode) ?: return
            ExpressiveListItem(
                title = spec.locale.title(LocalContext.current),
                description = spec.locale.description(LocalContext.current),
                isSelected = checked,
                onClick = {
                    if (editable && !saving) onBooleanChange(spec, !checked)
                },
                interactiveItem = { _, _ ->
                    Switch(
                        checked = checked,
                        enabled = editable,
                        onCheckedChange = { newValue ->
                            if (editable && !saving) onBooleanChange(spec, newValue)
                        },
                    )
                },
                isFirst = isFirst,
                isLast = isLast,
            )
        }

        is IntSelectSettingSpec -> {
            val value = rawValue?.let(spec::decode) ?: return
            ExpressiveListItem(
                title = spec.locale.title(LocalContext.current),
                description = spec.options.firstOrNull { it.value == value }
                    ?.label(LocalContext.current)
                    ?: value.toString(),
                onClick = { if (editable && !saving) onIntSelect(spec) },
                secondIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                isFirst = isFirst,
                isLast = isLast,
            )
        }

        is StringSelectSettingSpec -> {
            val value = rawValue?.let(spec::decode) ?: return
            ExpressiveListItem(
                title = spec.locale.title(LocalContext.current),
                description = spec.options.firstOrNull { it.value == value }
                    ?.label(LocalContext.current)
                    ?: value,
                onClick = { if (editable && !saving) onStringSelect(spec) },
                secondIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                isFirst = isFirst,
                isLast = isLast,
            )
        }
    }
}

@Composable
private fun <T : Any> SettingSelectionDialog(
    title: String,
    options: List<SelectOption<T>>,
    current: SelectOption<T>?,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var selected by remember(current) { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth(0.8f),
        containerColor = MaterialTheme.colorScheme.background,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                options.forEach { option ->
                    ExpressiveListItem(
                        title = option.label(context),
                        description = option.descriptionPath?.let { Locales.getString(*it.toTypedArray()) },
                        onClick = { selected = option },
                        isSelected = selected == option,
                        altLeadingContent = option.iconKey
                            ?.let(::locationFlagIcon)
                            ?.let { icon -> { _ -> ListIconView(icon) } },
                        interactiveItem = { isSelected, _ ->
                            RadioButton(
                                selected = isSelected,
                                onClick = { selected = option }
                            )
                        },
                        isFirst = option == options.first(),
                        isLast = option == options.last(),
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(Locales.getString("global", "cancel"))
            }
        },
        confirmButton = {
            Button(
                onClick = { selected?.value?.let(onSelect) },
                enabled = selected != null
            ) {
                Text(Locales.getString("global", "save"))
            }
        }
    )
}

private fun LocaleBinding.title(context: Context): String =
    titleRes?.let(context::getString)
        ?: Locales.getString(*titlePath.toTypedArray())

private fun LocaleBinding.description(context: Context): String? =
    descriptionRes?.let(context::getString)
        ?: descriptionPath?.let { Locales.getString(*it.toTypedArray()) }

private fun <T : Any> SelectOption<T>.label(context: Context): String =
    Locales.getString(*labelPath.toTypedArray())

private fun locationFlagIcon(code: String): ListIcon =
    ListIcon.Text(countryFlag(code))
