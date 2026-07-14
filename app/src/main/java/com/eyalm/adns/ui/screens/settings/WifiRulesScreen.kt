package com.eyalm.adns.ui.screens.settings

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NetworkWifi
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eyalm.adns.R
import com.eyalm.adns.data.runtime.RuntimeMonitorReason
import com.eyalm.adns.data.runtime.RuntimeServiceFailure
import com.eyalm.adns.data.wifi.ConnectedWifiIdentity
import com.eyalm.adns.data.wifi.WifiRuleStatus
import com.eyalm.adns.data.wifi.WifiSsid
import com.eyalm.adns.ui.components.ActionSettingRow
import com.eyalm.adns.ui.components.ExpressiveIcon
import com.eyalm.adns.ui.components.ResourceSettingRow
import com.eyalm.adns.ui.components.SegmentPosition
import com.eyalm.adns.ui.components.ToggleSettingRow
import com.eyalm.adns.ui.components.dialogs.DestructiveConfirmationDialog
import com.eyalm.adns.ui.components.dialogs.FormDialog
import com.eyalm.adns.ui.components.segmentPosition
import com.eyalm.adns.ui.theme.settingsLabel
import com.eyalm.adns.viewmodel.RuntimeMonitoringViewModel

@Composable
fun WifiRulesScreen(
    onBack: () -> Unit,
    viewModel: RuntimeMonitoringViewModel = viewModel(),
) {
    val context = LocalContext.current
    val runtime by viewModel.runtimeState.collectAsState()
    val rules by viewModel.wifiRulesState.collectAsState()
    val identity by viewModel.wifiIdentity.collectAsState()
    val coordinatorStatus by viewModel.wifiStatus.collectAsState()
    var addDialogVisible by remember { mutableStateOf(false) }
    var pendingRemoval by remember { mutableStateOf<WifiSsid?>(null) }
    var ssidInput by remember { mutableStateOf("") }
    var inputError by remember { mutableStateOf<Int?>(null) }
    var pendingPermissionAction by remember {
        mutableStateOf<WifiPermissionAction?>(null)
    }
    var pendingForegroundSettingsAction by remember {
        mutableStateOf<WifiPermissionAction?>(null)
    }
    var pendingBackgroundPermissionAction by remember {
        mutableStateOf<WifiPermissionAction?>(null)
    }
    var pendingBackgroundSettingsAction by remember {
        mutableStateOf<WifiPermissionAction?>(null)
    }
    var locationPermissionBlocked by remember { mutableStateOf(false) }

    fun hasLocationPermission(): Boolean = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED

    fun hasBackgroundLocationPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED

    fun completePermissionAction(action: WifiPermissionAction) {
        when (action) {
            WifiPermissionAction.EnableRules -> viewModel.onWifiPermissionResult(true)
            WifiPermissionAction.AddCurrent -> viewModel.captureCurrentSsid()
            WifiPermissionAction.RequestOnly -> viewModel.refreshSystemState()
        }
    }

    val backgroundPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val action = pendingBackgroundPermissionAction
        pendingBackgroundPermissionAction = null
        if (granted && action != null) completePermissionAction(action)
    }

    val requestBackgroundLocationPermission: (WifiPermissionAction) -> Unit = { action ->
        if (hasBackgroundLocationPermission()) {
            completePermissionAction(action)
        } else {
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                pendingBackgroundPermissionAction = action
                backgroundPermissionLauncher.launch(
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                )
            } else {
                pendingBackgroundSettingsAction = action
                context.startActivity(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null),
                    )
                )
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true
        if (!granted) {
            locationPermissionBlocked = (context as? Activity)?.let { activity ->
                !ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                )
            } == true
        }
        val action = pendingPermissionAction
        pendingPermissionAction = null
        if (granted && action != null) {
            if (action == WifiPermissionAction.AddCurrent) {
                completePermissionAction(action)
            } else {
                requestBackgroundLocationPermission(action)
            }
        } else if (action == WifiPermissionAction.EnableRules) {
            viewModel.onWifiPermissionResult(false)
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val backgroundAction = pendingBackgroundSettingsAction
                pendingBackgroundSettingsAction = null
                if (backgroundAction != null && hasBackgroundLocationPermission()) {
                    completePermissionAction(backgroundAction)
                }

                val foregroundAction = pendingForegroundSettingsAction
                pendingForegroundSettingsAction = null
                if (foregroundAction != null && hasLocationPermission()) {
                    if (foregroundAction == WifiPermissionAction.AddCurrent) {
                        completePermissionAction(foregroundAction)
                    } else {
                        requestBackgroundLocationPermission(foregroundAction)
                    }
                }
                viewModel.refreshSystemState()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        viewModel.startWifiIdentityObservation()
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.stopWifiIdentityObservation()
        }
    }

    fun requestLocationPermission(action: WifiPermissionAction) {
        if (hasLocationPermission()) {
            locationPermissionBlocked = false
            if (action == WifiPermissionAction.AddCurrent) {
                completePermissionAction(action)
            } else {
                requestBackgroundLocationPermission(action)
            }
        } else if (locationPermissionBlocked) {
            pendingForegroundSettingsAction = action
            context.startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", context.packageName, null),
                )
            )
        } else {
            pendingPermissionAction = action
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                )
            )
        }
    }

    if (addDialogVisible) {
        FormDialog(
            title = stringResource(R.string.wifi_rules_add_manual),
            confirmLabel = stringResource(R.string.add),
            confirmEnabled = ssidInput.isNotBlank(),
            errorMessage = inputError?.let { stringResource(it) },
            onConfirm = {
                if (ssidInput.isBlank()) {
                    inputError = R.string.wifi_rules_invalid_ssid
                } else if (!viewModel.addManualSsid(ssidInput)) {
                    inputError = R.string.wifi_rules_duplicate
                } else {
                    addDialogVisible = false
                    ssidInput = ""
                    inputError = null
                }
            },
            onDismiss = {
                addDialogVisible = false
                ssidInput = ""
                inputError = null
            },
        ) {
            OutlinedTextField(
                value = ssidInput,
                onValueChange = {
                    ssidInput = it
                    inputError = null
                },
                label = { Text(stringResource(R.string.wifi_rules_ssid_label)) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
        }
    }

    pendingRemoval?.let { ssid ->
        DestructiveConfirmationDialog(
            title = stringResource(R.string.wifi_rules_remove),
            body = stringResource(R.string.wifi_rules_remove_message),
            confirmLabel = stringResource(R.string.remove_item),
            onConfirm = {
                viewModel.removeSsid(ssid)
                pendingRemoval = null
            },
            onDismiss = { pendingRemoval = null },
        )
    }

    val statusText = stringResource(
        wifiStatusResource(
            enabled = runtime.wifiRulesEnabled,
            active = RuntimeMonitorReason.WifiRules in runtime.activeReasons,
            serviceRunning = runtime.serviceRunning,
            serviceFailure = runtime.serviceFailure,
            identity = identity,
            status = coordinatorStatus,
        )
    )
    val currentSsid = (identity as? ConnectedWifiIdentity.Known)?.ssid?.value
    val addedCurrentSsidMessage = stringResource(
        R.string.wifi_added_ssid,
        currentSsid.orEmpty(),
    )
    val currentSsidUnavailableMessage = stringResource(R.string.wifi_err_detect_ssid)
    val duplicateSsidMessage = stringResource(R.string.wifi_rules_duplicate)
    SettingsScreenScaffold(
        onBack = onBack,
        title = stringResource(R.string.wifi_rules),
        description = stringResource(R.string.wifi_rules_description),
    ) {
        item {
            if (identity == ConnectedWifiIdentity.PermissionRequired) {
                PermissionCard(
                    title = stringResource(R.string.wifi_location_permission_title),
                    description = stringResource(
                        R.string.wifi_location_permission_description
                    ),
                    onClick = { requestLocationPermission(WifiPermissionAction.RequestOnly) }
                )
            }
            if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                hasLocationPermission() &&
                !hasBackgroundLocationPermission()
            ) {
                PermissionCard(
                    title = stringResource(R.string.wifi_background_location_title),
                    description = stringResource(
                        R.string.wifi_background_location_description
                    ),
                    buttonText = stringResource(
                        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                            R.string.allow
                        } else {
                            R.string.settings
                        }
                    ),
                    onClick = {
                        requestBackgroundLocationPermission(
                            WifiPermissionAction.RequestOnly
                        )
                    },
                )
            }
            if (!runtime.batteryOptimizationIgnored) {
                PermissionCard(
                    title = stringResource(R.string.battery_optimization),
                    description = stringResource(R.string.wifi_battery_optimization_description),
                    buttonText = stringResource(R.string.allow),
                    onClick = { requestBatteryOptimizationExemption(context) },
                )
            }
            Spacer(Modifier.height(8.dp))

            ToggleSettingRow(
                title = stringResource(R.string.wifi_rules_enable),
                description = stringResource(R.string.wifi_rules_enable_description),
                checked = runtime.wifiRulesEnabled,
                toggle = { checked, onCheckedChange ->
                    Switch(checked = checked, onCheckedChange = onCheckedChange)
                },
                onCheckedChange = { enabled ->
                    if (!enabled) viewModel.setWifiRulesEnabled(false)
                    else requestLocationPermission(WifiPermissionAction.EnableRules)
                },
                position = SegmentPosition.Single,
            )
            Spacer(Modifier.height(20.dp))
        }

        if (runtime.wifiRulesEnabled) {
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.settings_status_header),
                        style = MaterialTheme.typography.settingsLabel,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
                    )
                    ResourceSettingRow(
                        title = stringResource(R.string.runtime_service_status),
                        description = statusText,
                        leading = { ExpressiveIcon(Icons.Filled.NetworkWifi) },
                        trailing = if (identity == ConnectedWifiIdentity.PermissionRequired) {
                            {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                            }
                        } else null,
                        onClick = {
                            if (identity == ConnectedWifiIdentity.PermissionRequired) {
                                requestLocationPermission(WifiPermissionAction.EnableRules)
                            }
                        },
                        position = SegmentPosition.Single,
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (currentSsid != null) {
                                    val added = viewModel.addManualSsid(currentSsid)
                                    Toast.makeText(
                                        context,
                                        if (added) addedCurrentSsidMessage else duplicateSsidMessage,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    Toast.makeText(
                                        context,
                                        currentSsidUnavailableMessage,
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = hasLocationPermission(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Wifi, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.wifi_add_current))
                        }

                        OutlinedButton(
                            onClick = { addDialogVisible = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.wifi_add_manually))
                        }
                    }
                }
            }

            item {
                Text(
                    text = stringResource(R.string.wifi_networks_header),
                    style = MaterialTheme.typography.settingsLabel,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 8.dp, top = 20.dp, bottom = 8.dp)
                )
            }

            if (rules.configuration.ssids.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.wifi_no_networks),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                val sortedSsids = rules.configuration.ssids.sortedBy { it.value }
                itemsIndexed(sortedSsids) { index, ssid ->
                    ActionSettingRow(
                        onClick = { pendingRemoval = ssid },
                        title = ssid.value,
                        description = null,
                        leading = { ExpressiveIcon(Icons.Default.Wifi) },
                        trailing = { Icon(Icons.Default.Delete, contentDescription = null) },
                        position = segmentPosition(index, sortedSsids.size),
                    )
                    Spacer(Modifier.height(4.dp))
                }
            }
        }


    }
}

private fun wifiStatusResource(
    enabled: Boolean,
    active: Boolean,
    serviceRunning: Boolean,
    serviceFailure: RuntimeServiceFailure?,
    identity: ConnectedWifiIdentity,
    status: WifiRuleStatus,
): Int = when {
    !enabled -> R.string.wifi_rules_inactive
    identity == ConnectedWifiIdentity.PermissionRequired -> R.string.wifi_rules_permission_needed
    identity == ConnectedWifiIdentity.LocationServicesDisabled ->
        R.string.wifi_rules_location_disabled
    !active -> R.string.wifi_rules_activation_required
    !serviceRunning && serviceFailure == RuntimeServiceFailure.StartNotAllowed ->
        R.string.runtime_service_start_not_allowed
    !serviceRunning && serviceFailure == RuntimeServiceFailure.MissingPermission ->
        R.string.runtime_service_missing_permission
    !serviceRunning && serviceFailure != null -> R.string.runtime_service_unknown_error
    !serviceRunning -> R.string.runtime_service_stopped
    status == WifiRuleStatus.IdentityUnavailable -> R.string.wifi_rules_identity_unavailable
    status is WifiRuleStatus.Suspended -> R.string.wifi_rules_suspended
    status is WifiRuleStatus.MatchedAlreadyDisabled -> R.string.wifi_rules_already_disabled
    status == WifiRuleStatus.WriteFailed -> R.string.wifi_rules_write_failed
    status == WifiRuleStatus.ExternalChangeDetected -> R.string.wifi_rules_external_change
    else -> R.string.wifi_rules_monitoring
}

private enum class WifiPermissionAction {
    EnableRules,
    AddCurrent,
    RequestOnly,
}
