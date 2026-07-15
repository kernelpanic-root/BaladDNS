package com.kernelpanic.baladdns.ui.screens.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kernelpanic.baladdns.R
import com.kernelpanic.baladdns.data.localization.AppLocaleRepository
import com.kernelpanic.baladdns.ui.components.ExpressiveCard
import com.kernelpanic.baladdns.ui.components.ExpressiveCardHeader
import com.kernelpanic.baladdns.ui.components.RadioSettingRow
import com.kernelpanic.baladdns.ui.components.segmentPosition

private const val WEBLATE_PROJECT_URL = "https://hosted.weblate.org/projects/adns/"

@Composable
fun LanguageScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repository = remember(context) { AppLocaleRepository(context.applicationContext) }
    val uriHandler = LocalUriHandler.current
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
                Spacer(Modifier.height(4.dp))
            }
        }
        item {
            Spacer(Modifier.height(16.dp))
            ExpressiveCard {
                ExpressiveCardHeader(
                    title = stringResource(R.string.help_translate_adns),
                    description = stringResource(R.string.help_translate_adns_description),
                )
                Spacer(Modifier.height(16.dp))
                Button(onClick = { uriHandler.openUri(WEBLATE_PROJECT_URL) }) {
                    Text(stringResource(R.string.open_weblate))
                }
            }
        }
    }
}
