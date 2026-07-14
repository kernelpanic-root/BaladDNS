package com.eyalm.adns.ui.screens.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CopyAll
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.RawOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eyalm.adns.R
import com.eyalm.adns.data.nextdns.logs.DomainRuleList
import com.eyalm.adns.data.nextdns.model.ListIcon
import com.eyalm.adns.data.nextdns.model.nextDnsFaviconUrl
import com.eyalm.adns.ui.components.ExpandableResourceSettingRow
import com.eyalm.adns.ui.components.ExpressiveIcon
import com.eyalm.adns.ui.components.ListIconView
import com.eyalm.adns.ui.components.NavigationSettingRow
import com.eyalm.adns.ui.components.SegmentPosition
import com.eyalm.adns.ui.components.ToggleSettingRow
import com.eyalm.adns.ui.components.segmentPosition
import com.eyalm.adns.viewmodel.ProfileSessionState
import com.eyalm.adns.viewmodel.nextdns.LogsEffect
import com.eyalm.adns.viewmodel.nextdns.LogsViewModel
import com.eyalm.adns.viewmodel.nextdns.PendingLogAction
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LogsScreen(
    onBack: () -> Unit,
    profileState: ProfileSessionState,
) {
    val profileId = profileState.selectedProfileId ?: return
    val viewModel: LogsViewModel = viewModel(key = "logs-$profileId")
    val context = LocalContext.current
    val copiedToClipboardMessage = stringResource(R.string.copied_to_clipboard)
    val state by viewModel.state.collectAsState()
    val items = state.items
    val devices = state.devices

    var showConfig by remember(profileId) { mutableStateOf(true) }
    var expandedId by remember(profileId) { mutableStateOf<Int?>(null) }

    LaunchedEffect(profileId, profileState.logsRevision) {
        viewModel.load(profileId, force = profileState.logsRevision > 0)
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is LogsEffect.CopyDomain -> {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE)
                        as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("domain", effect.domain))
                    Toast.makeText(
                        context,
                        copiedToClipboardMessage,
                        Toast.LENGTH_SHORT,
                    ).show()
                }

                is LogsEffect.Message -> {
                    Toast.makeText(context, effect.value, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    SettingsScreenScaffold(
        onBack = onBack,
        title = stringResource(R.string.logs),
        refreshing = state.refreshing,
        onRefresh = viewModel::refresh,
    ) {
            item {
                Row(
                    verticalAlignment = CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                ) {
                    TextField(
                        value = state.query.search,
                        onValueChange = {
                            viewModel.updateSearchQuery(it)
                        },
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
                        )
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
                                role = Role.Button
                            )
                    )

                }
            }

            if (showConfig) {
                item {
                    Spacer(Modifier.height(8.dp))
                    ToggleSettingRow(
                        title = stringResource(R.string.blocked_only),
                        description = stringResource(R.string.show_only_blocked_items),
                        checked = state.query.blockedOnly,
                        leading = { ExpressiveIcon(Icons.Filled.Block) },
                        toggle = { checked, onCheckedChange ->
                            Switch(
                                checked = checked,
                                onCheckedChange = onCheckedChange,
                            )
                        },
                        onCheckedChange = viewModel::setBlocked,
                        position = SegmentPosition.First,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                item {
                    ToggleSettingRow(
                        title = stringResource(R.string.raw_mode),
                        description = stringResource(R.string.show_raw_dns_logs),
                        checked = state.query.raw,
                        leading = { ExpressiveIcon(Icons.Filled.RawOn) },
                        toggle = { checked, onCheckedChange ->
                            Switch(
                                checked = checked,
                                onCheckedChange = onCheckedChange,
                            )
                        },
                        onCheckedChange = viewModel::setRaw,
                        position = SegmentPosition.Middle,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                item {
                    var expanded by remember { mutableStateOf(false) }
                    val selectedDeviceName = when (state.query.deviceId) {
                        null -> stringResource(R.string.all_devices)
                        "__UNIDENTIFIED__" -> stringResource(R.string.unknown_devices)
                        else -> devices.find { it.id == state.query.deviceId }?.name ?: state.query.deviceId!!
                    }

                    NavigationSettingRow(
                        onClick = {
                            expanded = true
                        },
                        title = selectedDeviceName,
                        description = stringResource(R.string.filter_the_logs_to_a_certain_device),
                        leading = { ExpressiveIcon(Icons.Filled.Devices) },
                        trailing = {
                            Box(
                                modifier = Modifier
                                    .wrapContentSize(Alignment.TopEnd)
                            ) {
                                IconButton(onClick = { expanded = true }) {
                                    Icon(Icons.Default.Edit, contentDescription = null)
                                }

                                DropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.all_devices)) },
                                        onClick = {
                                            expanded = false
                                            viewModel.setDevice(null)
                                        }
                                    )
                                    devices.filterNot { it.id == "__UNIDENTIFIED__" }.forEach { deviceItem ->
                                        DropdownMenuItem(
                                            text = { Text(deviceItem.name ?: deviceItem.id) },
                                            onClick = {
                                                expanded = false
                                                viewModel.setDevice(deviceItem.id)
                                            }
                                        )
                                    }
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.unknown_devices)) },
                                        onClick = {
                                            expanded = false
                                            viewModel.setDevice("__UNIDENTIFIED__")
                                        }
                                    )
                                }
                            }
                        },
                        position = SegmentPosition.Last,
                    )
                }
            }

            item { Spacer(Modifier.height(8.dp)) }

            if (state.initialLoading && items.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else if (items.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.no_logs_found),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(items.size) { index ->
                    val log = items[index]
                    LaunchedEffect(state.query, index, items.size) {
                        if (index >= items.size - 5) {
                            viewModel.fetchNextPage()
                        }
                    }
                    ExpandableResourceSettingRow(
                        title = log.domain,
                        onClick = { expandedId = if (expandedId == index) null else index },
                        expanded = expandedId == index,
                        selected = false,
                        indicatorColor = if (log.status == "blocked") MaterialTheme.colorScheme.error else null,
                        leading = {
                            ListIconView(
                                icon = nextDnsFaviconUrl(log.domain)
                                    ?.let(ListIcon::Url)
                                    ?: ListIcon.None,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        position = segmentPosition(index, items.size),
                        content = {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                //    .padding(vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                log.device?.let { dev ->
                                    val devName = dev.name ?: ""
                                    val devModel = dev.model?.let { " ($it)" } ?: ""
                                    if (devName.isNotEmpty() || devModel.isNotEmpty()) {
                                        DetailRow(label = stringResource(R.string.device), value = "$devName$devModel")
                                    }
                                }

                                DetailRow(
                                    label = stringResource(R.string.time),
                                    value = formatLogTimestamp(log.timestamp),
                                )

                                val encryptionStr = if (log.encrypted) stringResource(R.string.encrypted) else stringResource(
                                    R.string.unencrypted
                                )
                                DetailRow(
                                    label = stringResource(R.string.protocol),
                                    value = "${log.protocol} · $encryptionStr",
                                )

                                log.clientIp?.let { ip ->
                                    DetailRow(label = stringResource(R.string.client_ip), value = ip)
                                }

                                if (state.query.raw) {
                                    log.type?.let {
                                        DetailRow(label = stringResource(R.string.type), value = it)

                                    }
                                }
                                if (log.status == "blocked" && log.reasons.isNotEmpty()) {
                                    val reasonsStr = log.reasons.joinToString(", ") { it.name }
                                    DetailRow(label = stringResource(R.string.blocked_by), value = reasonsStr, isErrorColor = true)
                                }

                                Spacer(Modifier.height(8.dp))


                                val canEdit = profileState.capabilities.canEditSettings
                                val actionLabels = buildList {
                                    if (canEdit) {
                                        add(stringResource(R.string.allow))
                                        add(stringResource(R.string.deny))
                                    }
                                    add(stringResource(R.string.copy))
                                }
                                val actionIcons = buildList {
                                    if (canEdit) {
                                        add(Icons.Filled.Check)
                                        add(Icons.Filled.Block)
                                    }
                                    add(Icons.Filled.CopyAll)
                                }
                                val actionRules = buildList {
                                    if (canEdit) {
                                        add(DomainRuleList.Allow)
                                        add(DomainRuleList.Deny)
                                    }
                                    add(null)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    actionLabels.forEachIndexed { index, label ->
                                        val shapes = when (index) {
                                            0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                            actionLabels.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                                        }
                                        val rule = actionRules[index]
                                        val pending = rule?.let {
                                            PendingLogAction(log.domain, it) in state.pendingActions
                                        } ?: false

                                        ToggleButton(
                                            checked = true,
                                            onCheckedChange = {
                                                if (rule != null) {
                                                    viewModel.applyRule(
                                                        rule,
                                                        log.domain,
                                                        canEdit = true,
                                                    )
                                                } else {
                                                    viewModel.copyDomain(log.domain)
                                                }
                                            },
                                            enabled = !pending,
                                            modifier = Modifier.weight(1f),
                                            shapes = shapes.copy(checkedShape = shapes.shape),
                                            colors = ToggleButtonDefaults.toggleButtonColors(
                                                checkedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                                checkedContentColor = MaterialTheme.colorScheme.onSurface
                                            )
                                        ) {
                                            if (pending) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(18.dp),
                                                    strokeWidth = 2.dp,
                                                )
                                            } else {
                                                Icon(actionIcons[index], contentDescription = null)
                                            }
                                            Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
                                            Text(label)
                                        }
                                    }
                                }

                            }
                        },
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                if (state.loadingNextPage) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    }
                }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String, isErrorColor: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(90.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isErrorColor) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}

fun formatLogTimestamp(timestamp: String): String {
    return try {
        val parsed = ZonedDateTime.parse(timestamp)
        val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
        parsed.format(formatter)
    } catch (_: Exception) {
        timestamp
    }
}
