package com.kernelpanic.baladdns.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kernelpanic.baladdns.BuildConfig
import com.kernelpanic.baladdns.R
import com.kernelpanic.baladdns.data.activation.ActivationMode
import com.kernelpanic.baladdns.data.activation.ActivationState
import com.kernelpanic.baladdns.data.activation.PermissionState
import com.kernelpanic.baladdns.ui.components.ExpressiveIcon
import com.kernelpanic.baladdns.ui.components.ResourceSettingRow
import com.kernelpanic.baladdns.ui.components.SegmentPosition
import com.kernelpanic.baladdns.ui.screens.onboarding.ActivationMethodScreen
import com.kernelpanic.baladdns.ui.screens.onboarding.AdbActivationScreen
import com.kernelpanic.baladdns.ui.screens.onboarding.ShizukuActivationScreen
import com.kernelpanic.baladdns.ui.screens.onboarding.SuccessScreen
import com.kernelpanic.baladdns.ui.screens.settings.SettingsScreenScaffold
import com.kernelpanic.baladdns.viewmodel.PermissionAcquisitionState

enum class ActivationScreenPage {
    Overview,
    Method,
    Adb,
    Shizuku,
    Success,
}

enum class ActivationExitPolicy {
    Allowed,
    SwitchToControlOnly,
    Blocked,
}

sealed interface ActivationScreenIntent {
    data object Reactivate : ActivationScreenIntent
    data object UseAdb : ActivationScreenIntent
    data object UseShizuku : ActivationScreenIntent
    data object PermissionGranted : ActivationScreenIntent
    data object Back : ActivationScreenIntent
}

fun reduceActivationScreenPage(
    current: ActivationScreenPage,
    intent: ActivationScreenIntent,
): ActivationScreenPage = when (intent) {
    ActivationScreenIntent.Reactivate -> ActivationScreenPage.Method
    ActivationScreenIntent.UseAdb ->
        if (current == ActivationScreenPage.Method) ActivationScreenPage.Adb else current

    ActivationScreenIntent.UseShizuku ->
        if (current == ActivationScreenPage.Method) ActivationScreenPage.Shizuku else current

    ActivationScreenIntent.PermissionGranted -> when (current) {
        ActivationScreenPage.Adb,
        ActivationScreenPage.Shizuku,
        -> ActivationScreenPage.Success

        else -> current
    }
    ActivationScreenIntent.Back -> when (current) {
        ActivationScreenPage.Overview -> ActivationScreenPage.Overview
        ActivationScreenPage.Method -> ActivationScreenPage.Overview
        ActivationScreenPage.Adb,
        ActivationScreenPage.Shizuku,
        -> ActivationScreenPage.Method

        ActivationScreenPage.Success -> ActivationScreenPage.Success
    }
}

fun forcedActivationExitPolicy(
    controlOnlyEligible: Boolean,
): ActivationExitPolicy = if (controlOnlyEligible) {
    ActivationExitPolicy.SwitchToControlOnly
} else {
    ActivationExitPolicy.Blocked
}

fun updatedForcedActivationVisibility(
    currentlyVisible: Boolean,
    previouslyRequired: Boolean,
    currentlyRequired: Boolean,
): Boolean = currentlyVisible || (currentlyRequired && !previouslyRequired)

fun shouldSwitchToPrivilegedModeOnGrant(
    page: ActivationScreenPage,
    mode: ActivationMode?,
): Boolean = mode == ActivationMode.NextDnsControlOnly &&
    page in setOf(ActivationScreenPage.Adb, ActivationScreenPage.Shizuku)

fun shouldOfferControlOnlySwitch(
    mode: ActivationMode?,
    controlOnlyEligible: Boolean,
    canControlPrivateDns: Boolean,
    isDebugBuild: Boolean,
): Boolean = mode == ActivationMode.PrivilegedDnsControl &&
    controlOnlyEligible &&
    (!canControlPrivateDns || isDebugBuild)

@Composable
fun ActivationScreen(
    state: ActivationState,
    permissionAcquisitionState: PermissionAcquisitionState,
    controlOnlyEligible: Boolean,
    exitPolicy: ActivationExitPolicy,
    onStartPermissionMonitoring: () -> Unit,
    onRequestShizuku: () -> Unit,
    onExit: () -> Unit,
    onUseControlOnly: () -> Unit,
    onUsePrivileged: () -> Unit,
    onStopPermissionAcquisition: () -> Unit,
) {
    var page by rememberSaveable { mutableStateOf(ActivationScreenPage.Overview) }

    fun exitOverview() {
        when (exitPolicy) {
            ActivationExitPolicy.Allowed -> onExit()
            ActivationExitPolicy.SwitchToControlOnly -> {
                onUseControlOnly()
                onExit()
            }
            ActivationExitPolicy.Blocked -> Unit
        }
    }

    LaunchedEffect(state.permission) {
        if (state.permission == PermissionState.Granted) {
            if (shouldSwitchToPrivilegedModeOnGrant(page, state.mode)) {
                onUsePrivileged()
            }
            page = reduceActivationScreenPage(
                page,
                ActivationScreenIntent.PermissionGranted,
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose(onStopPermissionAcquisition)
    }

    BackHandler {
        when (page) {
            ActivationScreenPage.Overview -> exitOverview()
            ActivationScreenPage.Success -> onExit()
            else -> {
                page = reduceActivationScreenPage(page, ActivationScreenIntent.Back)
            }
        }
    }

    when (page) {
        ActivationScreenPage.Overview -> ActivationOverview(
            state = state,
            controlOnlyEligible = controlOnlyEligible,
            onBack = if (exitPolicy == ActivationExitPolicy.Blocked) {
                null
            } else {
                ::exitOverview
            },
            onActivate = {
                if (state.permission == PermissionState.Granted) {
                    onUsePrivileged()
                    page = ActivationScreenPage.Success
                } else {
                    page = reduceActivationScreenPage(
                        page,
                        ActivationScreenIntent.Reactivate,
                    )
                }
            },
            onUseControlOnly = onUseControlOnly,
        )

        ActivationScreenPage.Method -> ActivationMethodScreen(
            onBackClick = {
                page = reduceActivationScreenPage(page, ActivationScreenIntent.Back)
            },
            onNextClick = { shizuku, adb ->
                page = when {
                    adb -> reduceActivationScreenPage(
                        page,
                        ActivationScreenIntent.UseAdb,
                    )

                    shizuku -> reduceActivationScreenPage(
                        page,
                        ActivationScreenIntent.UseShizuku,
                    )

                    else -> page
                }
            },
        )

        ActivationScreenPage.Adb -> AdbActivationScreen(
            onBack = {
                page = reduceActivationScreenPage(page, ActivationScreenIntent.Back)
            },
            onStartMonitoring = onStartPermissionMonitoring,
            onStopMonitoring = onStopPermissionAcquisition,
        )

        ActivationScreenPage.Shizuku -> ShizukuActivationScreen(
            state = permissionAcquisitionState,
            onBack = {
                page = reduceActivationScreenPage(page, ActivationScreenIntent.Back)
            },
            onStart = onRequestShizuku,
            onStop = onStopPermissionAcquisition,
        )

        ActivationScreenPage.Success -> SuccessScreen(
            onFinishClicked = onExit,
        )
    }
}

@Composable
private fun ActivationOverview(
    state: ActivationState,
    controlOnlyEligible: Boolean,
    onBack: (() -> Unit)?,
    onActivate: () -> Unit,
    onUseControlOnly: () -> Unit,
) {
    SettingsScreenScaffold(
        onBack = onBack,
        title = stringResource(R.string.activation),
    ) {
        item {
            StatusCard(state)
            Spacer(modifier = Modifier.height(16.dp))
        }
        item {
            BenefitsSection()
            Spacer(modifier = Modifier.height(16.dp))
        }
        if (!state.canControlPrivateDns) {
            item {
                Button(
                    onClick = onActivate,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(
                        text = stringResource(
                            if (state.mode == ActivationMode.NextDnsControlOnly) {
                                R.string.switch_to_privileged_mode
                            } else {
                                R.string.reactivate
                            }
                        ),
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
        if (
            shouldOfferControlOnlySwitch(
                mode = state.mode,
                controlOnlyEligible = controlOnlyEligible,
                canControlPrivateDns = state.canControlPrivateDns,
                isDebugBuild = BuildConfig.DEBUG,
            )
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onUseControlOnly,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(
                        stringResource(R.string.use_control_only),
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusCard(state: ActivationState) {
    val isActivated = state.canControlPrivateDns
    val isControlOnly = state.mode == ActivationMode.NextDnsControlOnly
    val targetColor = when {
        isActivated -> MaterialTheme.colorScheme.primaryContainer
        isControlOnly -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
    }
    val containerColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(400),
        label = "activation_status_card",
    )
    val contentColor = when {
        isActivated -> MaterialTheme.colorScheme.onPrimaryContainer
        isControlOnly -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onErrorContainer
    }
    val title = stringResource(
        when {
            isActivated -> R.string.activation_status_active
            isControlOnly -> R.string.activation_status_control_only
            else -> R.string.activation_status_inactive
        }
    )
    val description = stringResource(
        when {
            isActivated -> R.string.activation_active_description
            isControlOnly -> R.string.activation_control_only_description
            else -> R.string.activation_missing_description
        }
    )

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.2.sp),
                color = contentColor.copy(alpha = 0.7f),
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor,
            )
        }
    }
}

@Composable
private fun BenefitsSection() {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        BenefitRow(
            icon = Icons.Filled.Shield,
            title = stringResource(R.string.system_level_dns_control),
            description = stringResource(R.string.system_level_dns_control_description),
            position = SegmentPosition.First,
        )
        BenefitRow(
            icon = Icons.Filled.Speed,
            title = stringResource(R.string.instant_toggle),
            description = stringResource(R.string.instant_toggle_description),
        )
        BenefitRow(
            icon = Icons.Filled.Lock,
            title = stringResource(R.string.private_dns_automation),
            description = stringResource(R.string.private_dns_automation_description),
            position = SegmentPosition.Last,
        )
    }
}

@Composable
private fun BenefitRow(
    icon: ImageVector,
    title: String,
    description: String,
    position: SegmentPosition = SegmentPosition.Middle,
) {
    ResourceSettingRow(
        leading = { ExpressiveIcon(icon) },
        title = title,
        description = description,
        position = position,
        alignment = Alignment.CenterVertically
    )
}
