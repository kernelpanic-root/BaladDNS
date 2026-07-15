package com.kernelpanic.baladdns.ui.screens.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class LanguageScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun languageScreenOffersTranslationContributions() {
        composeRule.setContent {
            MaterialTheme {
                LanguageScreen(onBack = {})
            }
        }

        composeRule
            .onNodeWithText("Help translate BaladDNS")
            .assertIsDisplayed()
    }
}
