package com.eyalm.adns.ui.screens.settings

import androidx.compose.material3.RadioButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.eyalm.adns.R
import com.eyalm.adns.data.localization.AppLocaleRepository
import com.eyalm.adns.ui.components.RadioSettingRow
import com.eyalm.adns.ui.components.segmentPosition

@Composable
fun LanguageScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repository = remember(context) { AppLocaleRepository(context.applicationContext) }
    val currentTag = repository.selectedTag()
    val locales = remember { repository.supportedLocales() }

    SettingsScreenScaffold(
        onBack = onBack,
        title = stringResource(R.string.language),
        description = stringResource(R.string.language_description),
    ) {
        locales.forEachIndexed { index, locale ->
            item(key = locale.tag) {
                RadioSettingRow(
                    title = locale.nativeDisplayName,
                    selected = locale.tag == currentTag,
                    position = segmentPosition(index, locales.size),
                    radio = { selected, onClick ->
                        RadioButton(selected = selected, onClick = onClick)
                    },
                    onClick = { repository.select(locale.tag) },
                )
            }
        }
    }
}
