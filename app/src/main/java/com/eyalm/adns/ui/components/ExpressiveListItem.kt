package com.eyalm.adns.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpressiveListItem(
    onClick: () -> Unit,
    isSelected: Boolean = false,
    icon: ImageVector?,
    secondIcon: ImageVector? = null,
    title: String,
    description: String?,
    isFirst: Boolean = false,
    isLast: Boolean = false,

    ) {
    SegmentedListItem(
        selected = isSelected,
        onClick = onClick,
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shapes = ListItemDefaults.shapes(
            shape = RoundedCornerShape(
                topStart = if (isFirst) 12.dp else 0.dp,
                topEnd = if (isFirst) 12.dp else 0.dp,
                bottomStart = if (isLast) 12.dp else 0.dp,
                bottomEnd = if (isLast) 12.dp else 0.dp
            )
        ),
        leadingContent = {
            Row {
                if (icon != null) {
                    ExpressiveIcon(icon)
                }
                Spacer(modifier = Modifier.width(4.dp))
            }

        },
        trailingContent = {
            if (secondIcon != null) {
                Icon(
                    imageVector = secondIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

        },
        supportingContent = {
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

        },
        content = {

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

        }
    )
}