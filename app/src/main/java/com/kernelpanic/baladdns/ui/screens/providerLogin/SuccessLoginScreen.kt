package com.kernelpanic.baladdns.ui.screens.providerLogin
import com.kernelpanic.baladdns.R
import androidx.compose.ui.res.stringResource


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kernelpanic.baladdns.ui.components.IconVisual
import com.kernelpanic.baladdns.ui.components.OnboardingTemplate
import com.kernelpanic.baladdns.ui.components.StandardBottomBar
import com.kernelpanic.baladdns.ui.theme.pageTitle

@Preview
@Composable
fun SuccessLoginScreen(
    onFinishClicked: () -> Unit = {}
) {
    OnboardingTemplate(
        bottomBarContent = {
            StandardBottomBar(
                onNextClick = onFinishClicked,
                buttonText = stringResource(R.string.finish),
                message = stringResource(R.string.youre_almost_there)
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
                    text = stringResource(R.string.congratulations),
                    style = MaterialTheme.typography.pageTitle,
                    modifier = Modifier.padding(top = 16.dp)
                )
                Text(
                    text = stringResource(R.string.your_dns_settings_have_been_updated),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconVisual(Icons.Default.AccountCircle)
            }
        }
    )
}