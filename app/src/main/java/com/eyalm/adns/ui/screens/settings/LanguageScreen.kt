package com.eyalm.adns.ui.screens.settings

import android.app.Activity
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.eyalm.adns.R
import com.eyalm.adns.data.LocaleHelper
import com.eyalm.adns.ui.components.ExpressiveListItem

// TODO: Move to a dynamic approach

@Composable
fun LanguageScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val currentLang = remember { LocaleHelper.getLanguage(context) }

    SettingsCategoryScreenTemplate(
        onBack = onBack,
        title = stringResource(R.string.language),
        description = stringResource(R.string.language_description)
    ) {
        item {
            ExpressiveListItem(
                title = "English",
                onClick = {
                    LocaleHelper.setLocale(context, "en")
                    (context as? Activity)?.recreate()
                },
                isFirst = true,
                isSelected = currentLang == "en",
                altLeadingContent = {
                    RadioButton(
                        currentLang == "en",
                        onClick = { },
                    )
                },
            )
        }

        item {
            ExpressiveListItem(
                title = "עברית",
                onClick = {
                    LocaleHelper.setLocale(context, "iw")
                    (context as? Activity)?.recreate()
                },
                isLast = true,
                isSelected = currentLang == "iw",
                altLeadingContent = {
                    RadioButton(
                        currentLang == "iw",
                        onClick = { },
                    )
                },
            )
        }
    }
}
