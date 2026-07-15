package com.kernelpanic.baladdns.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SelectableCard(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null
) {
    InfoCard(
        content = { contentColor ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (leadingIcon != null) {
                    leadingIcon()
                    Spacer(modifier = Modifier.width(16.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = contentColor
                    )
                    if (description.isNotEmpty()) {
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = contentColor.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        },
        selected = selected,
        onClick = onClick,
        modifier = modifier,
    )
}