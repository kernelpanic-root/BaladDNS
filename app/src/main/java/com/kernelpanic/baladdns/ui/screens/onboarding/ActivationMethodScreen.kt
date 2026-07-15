package com.kernelpanic.baladdns.ui.screens.onboarding
import com.kernelpanic.baladdns.R
import androidx.compose.ui.res.stringResource


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kernelpanic.baladdns.ui.components.RadioSettingRow
import com.kernelpanic.baladdns.ui.components.SegmentPosition
import com.kernelpanic.baladdns.ui.components.OnboardingTemplate
import com.kernelpanic.baladdns.ui.components.StandardBottomBar
import com.kernelpanic.baladdns.ui.theme.pageTitle

@Preview
@Composable
fun ActivationMethodScreen(
    onBackClick: () -> Unit = {},
    onNextClick: (shizuku: Boolean, adb: Boolean) -> Unit = { _, _ -> }
) {
    var shizukuPressed by remember { mutableStateOf(false) }
    var adbPressed by remember { mutableStateOf(false) }

    OnboardingTemplate(
        onBackClick = onBackClick,
        bottomBarContent = {
            StandardBottomBar(
                message = stringResource(R.string.please_choose_one_option),
                enabled = shizukuPressed || adbPressed,
                onNextClick = { onNextClick(shizukuPressed, adbPressed) }
            )

        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.activation),
                    style = MaterialTheme.typography.pageTitle,
                    modifier = Modifier.padding(top = 16.dp)
                )

            }
            item {
                Text(
                    text = stringResource(R.string.please_choose_an_activation_method_the_activation_is_a_onetime_process_after_the_activation_yo),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            item {
                RadioSettingRow(
                    title = stringResource(R.string.shizuku),
                    description = stringResource(R.string.requires_shizuku_or_sui_installed_and_set_up),
                    selected = shizukuPressed,
                    onClick = {
                        shizukuPressed = !shizukuPressed
                        adbPressed = false
                    },
                    radio = { _, onClick ->
                        RadioButton(selected = shizukuPressed, onClick = onClick)
                    },
                    position = SegmentPosition.First,
                )
            }
            item {
                RadioSettingRow(
                    title = stringResource(R.string.adb_shell),
                    description = stringResource(R.string.requires_adb_shell_access_for_advanced_users),
                    selected = adbPressed,
                    onClick = {
                        adbPressed = !adbPressed
                        shizukuPressed = false
                    },
                    radio = { _, onClick ->
                        RadioButton(selected = adbPressed, onClick = onClick)
                    },
                    position = SegmentPosition.Last,
                )
            }
        }
    }
}
