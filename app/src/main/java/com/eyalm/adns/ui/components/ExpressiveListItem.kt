package com.eyalm.adns.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.eyalm.adns.data.ListIcon

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpressiveListItem(
    onClick: () -> Unit,
    isSelected: Boolean = false,
    altLeadingContent: (@Composable (isEnabled: Boolean) -> Unit)? = null,
    altContent: (@Composable () -> Unit)? = null,
    icon: ImageVector? = null,
    altIconUrl: String? = null,
    secondIcon: ImageVector? = null,
    interactiveItem: (@Composable (isSelected: Boolean, onClick: () -> Unit) -> Unit)? = null,
    title: String,
    description: String?,
    isFirst: Boolean = false,
    isLast: Boolean = false,
) {
    val containerColor = MaterialTheme.colorScheme.surfaceContainer
    val itemColors = ListItemDefaults.colors(containerColor = containerColor)

    val itemShape = remember(isFirst, isLast) {
        RoundedCornerShape(
            topStart = if (isFirst) 12.dp else 0.dp,
            topEnd = if (isFirst) 12.dp else 0.dp,
            bottomStart = if (isLast) 12.dp else 0.dp,
            bottomEnd = if (isLast) 12.dp else 0.dp
        )
    }

    val itemShapes = ListItemDefaults.shapes(shape = itemShape)

    val leading = remember(icon, altLeadingContent, isSelected) {
        @Composable {
            if (icon != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ExpressiveIcon(icon)
                    Spacer(Modifier.width(4.dp))
                }
            }
            if (altLeadingContent != null) {
                altLeadingContent(isSelected)
            }
            if (altIconUrl != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ListIconView(
                        icon = ListIcon.Url(altIconUrl),
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(Modifier.width(4.dp))
                }
            }
        }
    }

    val supportingTextStyle = MaterialTheme.typography.bodyMedium
    val supportingTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    val supporting = remember(description, supportingTextStyle, supportingTextColor, altContent) {
        @Composable {
            Column {
                description?.let {
                    Text(
                        text = it,
                        style = supportingTextStyle,
                        color = supportingTextColor
                    )
                }
                if (altContent != null) {
                    altContent()
                }
            }

        }
    }

    val titleTextStyle = MaterialTheme.typography.titleMedium
    val titleTextColor = MaterialTheme.colorScheme.onSurface
    val mainContent = remember(title, titleTextStyle, titleTextColor) {
        @Composable {
            Text(
                text = title,
                style = titleTextStyle.copy(fontWeight = FontWeight.Bold),
                color = titleTextColor
            )
        }
    }

    val trailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
    val trailing = remember(secondIcon, interactiveItem, isSelected, onClick, trailingIconColor) {
        @Composable {
            if (secondIcon != null) {
                Icon(
                    imageVector = secondIcon,
                    contentDescription = null,
                    tint = trailingIconColor.copy(alpha = 0.7f)
                )
            }
            if (interactiveItem != null) {
                interactiveItem(isSelected, onClick)
            }

        }
    }


    SegmentedListItem(
        selected = isSelected,
        colors = itemColors,
        onClick = onClick,
        verticalAlignment = Alignment.CenterVertically,
        shapes = itemShapes,
        leadingContent = leading,
        trailingContent = trailing,
        supportingContent = supporting,
        content = mainContent
    )
}
