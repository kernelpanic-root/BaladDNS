package com.kernelpanic.baladdns.ui.screens.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.v2.createComposeRule
import org.junit.Rule
import org.junit.Test

class SettingsScreenLayoutRefreshTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun layoutOwnsPullToRefreshAroundTheWholeScaffold() {
        composeRule.setContent {
            MaterialTheme {
                SettingsScreenLayout(
                    title = "Settings",
                    onBack = null,
                    showAppBarTitle = true,
                    refreshing = true,
                    onRefresh = {},
                ) {
                    Text("Content")
                }
            }
        }
    }
}
