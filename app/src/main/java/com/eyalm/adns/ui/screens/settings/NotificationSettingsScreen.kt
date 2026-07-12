package com.eyalm.adns.ui.screens.settings

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eyalm.adns.R
import com.eyalm.adns.data.runtime.NotificationChannelState
import com.eyalm.adns.data.runtime.NotificationPermissionState
import com.eyalm.adns.data.runtime.RuntimeMonitorReason
import com.eyalm.adns.data.runtime.RuntimeNotifications
import com.eyalm.adns.data.runtime.RuntimeServiceFailure
import com.eyalm.adns.ui.components.ExpressiveListItem
import com.eyalm.adns.ui.theme.settingsLabel
import com.eyalm.adns.viewmodel.RuntimeMonitoringViewModel

@Composable
fun NotificationSettingsScreen(
    onBack: () -> Unit,
    viewModel: RuntimeMonitoringViewModel = viewModel(),
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val state by viewModel.runtimeState.collectAsState()
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onNotificationPermissionResult(granted)
    }

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshSystemState()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val openAppNotificationSettings = remember(context) {
        {
            openSystemSettings(
                context = context,
                intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                },
            )
        }
    }
    val openChannelSettings = remember(context, state.stateChannel) {
        {
            if (state.stateChannel == NotificationChannelState.Missing) {
                openAppNotificationSettings()
            } else {
                openSystemSettings(
                    context,
                    Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        putExtra(Settings.EXTRA_CHANNEL_ID, RuntimeNotifications.CHANNEL_ID)
                    },
                )
            }
        }
    }
    val serviceStatusText = stringResource(
        when {
            state.serviceRunning -> R.string.runtime_service_running
            state.serviceFailure == RuntimeServiceFailure.StartNotAllowed ->
                R.string.runtime_service_start_not_allowed
            state.serviceFailure == RuntimeServiceFailure.MissingPermission ->
                R.string.runtime_service_missing_permission
            state.serviceFailure == RuntimeServiceFailure.Unknown ->
                R.string.runtime_service_unknown_error
            else -> R.string.runtime_service_stopped
        }
    )
    val wifiReasonText = stringResource(R.string.runtime_service_wifi_reason)
    val runtimeRequested = state.stateNotificationEnabled ||
        RuntimeMonitorReason.WifiRules in state.requestedReasons
    val activationMissing = state.stateNotificationEnabled &&
        RuntimeMonitorReason.StateNotification !in state.activeReasons

    SettingsCategoryScreenTemplate(
        onBack = onBack,
        title = stringResource(R.string.notification_settings),
        description = stringResource(R.string.notification_settings_description),
    ) {
        item {
            if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                state.notificationPermission == NotificationPermissionState.Denied
            ) {
                PermissionCard(
                    title = stringResource(R.string.notification_permission),
                    description = stringResource(
                        R.string.notification_permission_required_description
                    ),
                    buttonText = stringResource(R.string.settings),
                    onClick = openAppNotificationSettings,
                )
            } else if (!state.appNotificationsEnabled) {
                PermissionCard(
                    title = stringResource(R.string.app_notification_status),
                    description = stringResource(
                        R.string.app_notifications_disabled_description
                    ),
                    buttonText = stringResource(R.string.settings),
                    onClick = openAppNotificationSettings,
                )
            }
            if (state.stateChannel == NotificationChannelState.Disabled) {
                PermissionCard(
                    title = stringResource(R.string.notification_channel_status),
                    description = stringResource(
                        R.string.notification_channel_disabled_description
                    ),
                    buttonText = stringResource(R.string.settings),
                    onClick = openChannelSettings,
                )
            }
            if (!state.batteryOptimizationIgnored) {
                PermissionCard(
                    title = stringResource(R.string.battery_optimization),
                    description = stringResource(
                        R.string.notification_battery_optimization_description
                    ),
                    buttonText = stringResource(R.string.allow),
                    onClick = { requestBatteryOptimizationExemption(context) },
                )
            }

            ExpressiveListItem(
                title = stringResource(R.string.state_notifications),
                description = stringResource(R.string.state_notification_description),
                icon = Icons.Filled.Notifications,
                isSelected = state.stateNotificationEnabled,
                onClick = {
                    val enable = !state.stateNotificationEnabled
                    if (
                        enable &&
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        state.notificationPermission != NotificationPermissionState.Granted
                    ) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        viewModel.setStateNotificationEnabled(enable)
                    }
                },
                interactiveItem = { selected, onClick ->
                    Switch(checked = selected, onCheckedChange = { onClick() })
                },
                isFirst = true,
                isLast = true,
            )
            Spacer(Modifier.height(20.dp))
        }

        if (runtimeRequested) {
            item {
                SettingsSectionLabel(stringResource(R.string.settings_status_header))
                ExpressiveListItem(
                    title = stringResource(R.string.runtime_service_status),
                    description = buildString {
                        append(serviceStatusText)
                        if (
                            RuntimeMonitorReason.WifiRules in state.activeReasons &&
                            !state.stateNotificationEnabled
                        ) {
                            append("\n")
                            append(wifiReasonText)
                        }
                    },
                    icon = Icons.Filled.SettingsSuggest,
                    isFirst = true,
                    isLast = !activationMissing,
                )
                if (activationMissing) {
                    ExpressiveListItem(
                        title = stringResource(R.string.activation_required),
                        description = stringResource(R.string.activation_missing_description),
                        icon = Icons.Filled.Security,
                        isLast = true,
                    )
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun SettingsSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.settingsLabel,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 8.dp, bottom = 8.dp),
    )
}

private fun openSystemSettings(context: android.content.Context, intent: Intent) {
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}
