package com.kernelpanic.baladdns.ui.screens.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kernelpanic.baladdns.R
import com.kernelpanic.baladdns.ui.components.OnboardingTemplate
import com.kernelpanic.baladdns.ui.theme.pageTitle
import com.kernelpanic.baladdns.viewmodel.PermissionAcquisitionState

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ShizukuActivationScreen(
    state: PermissionAcquisitionState,
    onBack: () -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
) {
    DisposableEffect(Unit) {
        onStart()
        onDispose(onStop)
    }

    OnboardingTemplate(
        onBackClick = onBack,
        bottomBarContent = {
            if (
                state == PermissionAcquisitionState.RequestingShizuku ||
                state == PermissionAcquisitionState.Granting
            ) {
                LinearWavyProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                stringResource(R.string.shizuku),
                style = MaterialTheme.typography.pageTitle,
                modifier = Modifier.padding(top = 16.dp),
            )
            when (state) {
                PermissionAcquisitionState.ShizukuUnavailable -> Text(
                    stringResource(R.string.shizuku_unavailable),
                    color = MaterialTheme.colorScheme.error,
                )

                PermissionAcquisitionState.Denied -> Text(
                    stringResource(R.string.shizuku_permission_denied),
                    color = MaterialTheme.colorScheme.error,
                )

                is PermissionAcquisitionState.Error -> Text(
                    state.message ?: stringResource(R.string.login_failed_try_again),
                    color = MaterialTheme.colorScheme.error,
                )

                else -> Text(
                    stringResource(R.string.waiting_for_permission),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
