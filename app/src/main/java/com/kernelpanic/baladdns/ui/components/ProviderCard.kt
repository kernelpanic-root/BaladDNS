package com.kernelpanic.baladdns.ui.components
import com.kernelpanic.baladdns.R
import androidx.compose.ui.res.stringResource


import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun ProviderCard(
    title: String,
    isEnhanced: Boolean,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {

    InfoCard(
        content = { contentColor ->
            Column() {
                Row() {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = contentColor
                    )
                    if (isEnhanced) {
                        Text(
                            text = stringResource(R.string.enhanced),
                            style = MaterialTheme.typography.titleMedium,
                            color = contentColor
                        )
                    }
                }

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor
                )
            }
        },
        selected = selected,
        onClick = onClick,
        modifier = Modifier
    )
}
