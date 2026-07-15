package com.kernelpanic.baladdns.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun InfoCard(
    content: @Composable (contentColor: androidx.compose.ui.graphics.Color) -> Unit,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (!selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primary,
        label = "selectable_card_bg"
    )

    val contentColor = if (!selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onPrimary

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        onClick = onClick,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                content(contentColor)
            }
        }
    }
}
