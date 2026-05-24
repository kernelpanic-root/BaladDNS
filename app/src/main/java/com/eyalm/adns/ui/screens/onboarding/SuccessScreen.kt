package com.eyalm.adns.ui.screens.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.eyalm.adns.ui.components.IconVisual
import com.eyalm.adns.ui.components.OnboardingTemplate
import com.eyalm.adns.ui.components.StandardBottomBar
import com.eyalm.adns.ui.theme.pageTitle

@Preview
@Composable
fun SuccessScreen(
    onFinishClicked: () -> Unit = {}
) {
    OnboardingTemplate(
        bottomBarContent = {
            StandardBottomBar(
                onNextClick = onFinishClicked,
                buttonText = "Finish",
                message = "You're almost there!"
            )
        },
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Congratulations!",
                    style = MaterialTheme.typography.pageTitle,
                    modifier = Modifier.padding(top = 16.dp)
                )

                Text(
                    text = "Application activated successfully.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconVisual(Icons.Filled.CheckCircle)
            }
        }
    )
}