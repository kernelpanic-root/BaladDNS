package com.kernelpanic.baladdns.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun ExpressiveIcon(
    icon: ImageVector,
    modifier: Modifier = Modifier.size(36.dp),
    selected: Boolean = false,
    bgcolor: Color? = null,
    color: Color? = null
) {
    val backgroundColor =  if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        bgcolor ?: MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    }
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        color ?: MaterialTheme.colorScheme.primary
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(20.dp)
        )
    }
}
