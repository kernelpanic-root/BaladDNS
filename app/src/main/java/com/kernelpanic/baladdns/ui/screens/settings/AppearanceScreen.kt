package com.kernelpanic.baladdns.ui.screens.settings

import android.os.Build
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kernelpanic.baladdns.R
import com.kernelpanic.baladdns.data.appearance.AppearanceRepository
import com.kernelpanic.baladdns.data.appearance.ColorSchemePreference
import com.kernelpanic.baladdns.data.appearance.DarkModePreference
import com.kernelpanic.baladdns.data.appearance.supportsDynamicColor
import com.kernelpanic.baladdns.ui.components.RadioSettingRow
import com.kernelpanic.baladdns.ui.components.SegmentPosition

@Composable
fun AppearanceScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repository = remember(context) {
        AppearanceRepository.getInstance(context.applicationContext)
    }
    val preferences by repository.state.collectAsState()

    SettingsScreenScaffold(
        onBack = onBack,
        title = stringResource(R.string.appearance),
        description = stringResource(R.string.appearance_description),
    ) {
        item {
            RadioSettingRow(
                title = stringResource(R.string.theme_system_default),
                selected = preferences.darkMode == DarkModePreference.System,
                position = SegmentPosition.First,
                radio = { selected, onClick ->
                    RadioButton(selected = selected, onClick = onClick)
                },
                onClick = { repository.setDarkMode(DarkModePreference.System) },
            )
        }
        item { Spacer(Modifier.height(4.dp)) }
        item {
            RadioSettingRow(
                title = stringResource(R.string.theme_light),
                selected = preferences.darkMode == DarkModePreference.Light,
                position = SegmentPosition.Middle,
                radio = { selected, onClick ->
                    RadioButton(selected = selected, onClick = onClick)
                },
                onClick = { repository.setDarkMode(DarkModePreference.Light) },
            )
        }
        item { Spacer(Modifier.height(4.dp)) }
        item {
            RadioSettingRow(
                title = stringResource(R.string.theme_dark),
                selected = preferences.darkMode == DarkModePreference.Dark,
                position = SegmentPosition.Last,
                radio = { selected, onClick ->
                    RadioButton(selected = selected, onClick = onClick)
                },
                onClick = { repository.setDarkMode(DarkModePreference.Dark) },
            )
        }

        if (supportsDynamicColor(Build.VERSION.SDK_INT)) {
            item { Spacer(Modifier.height(20.dp)) }
            item {
                RadioSettingRow(
                    title = stringResource(R.string.color_scheme_adns),
                    selected = preferences.colorScheme == ColorSchemePreference.Adns,
                    position = SegmentPosition.First,
                    radio = { selected, onClick ->
                        RadioButton(selected = selected, onClick = onClick)
                    },
                    onClick = { repository.setColorScheme(ColorSchemePreference.Adns) },
                )
            }
            item { Spacer(Modifier.height(4.dp)) }
            item {
                RadioSettingRow(
                    title = stringResource(R.string.color_scheme_system),
                    selected = preferences.colorScheme == ColorSchemePreference.SystemDynamic,
                    position = SegmentPosition.Last,
                    radio = { selected, onClick ->
                        RadioButton(selected = selected, onClick = onClick)
                    },
                    onClick = {
                        repository.setColorScheme(ColorSchemePreference.SystemDynamic)
                    },
                )
            }
        }
    }
}
