package com.eyalm.adns.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

enum class SegmentPosition {
    Single,
    First,
    Middle,
    Last,
}

fun segmentPosition(index: Int, size: Int): SegmentPosition {
    require(size > 0) { "A segmented group must contain at least one item." }
    if (index !in 0 until size) throw IndexOutOfBoundsException("index=$index, size=$size")
    return when {
        size == 1 -> SegmentPosition.Single
        index == 0 -> SegmentPosition.First
        index == size - 1 -> SegmentPosition.Last
        else -> SegmentPosition.Middle
    }
}

private fun SegmentPosition.shape() = RoundedCornerShape(
    topStart = if (this == SegmentPosition.Single || this == SegmentPosition.First) 12.dp else 2.dp,
    topEnd = if (this == SegmentPosition.Single || this == SegmentPosition.First) 12.dp else 2.dp,
    bottomStart = if (this == SegmentPosition.Single || this == SegmentPosition.Last) 12.dp else 2.dp,
    bottomEnd = if (this == SegmentPosition.Single || this == SegmentPosition.Last) 12.dp else 2.dp,
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SegmentedSettingRow(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    position: SegmentPosition = SegmentPosition.Single,
    enabled: Boolean = true,
    selected: Boolean = false,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    supporting: (@Composable () -> Unit)? = null,
    alignment: Alignment.Vertical = ListItemDefaults.verticalAlignment(),
    indicatorColor: Color? = null,
    selectedColor: Color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
    onClick: () -> Unit = {},
) {
    val shape = position.shape()
    val containerColor = MaterialTheme.colorScheme.surfaceContainer
    val itemColors = ListItemDefaults.colors(containerColor = containerColor, selectedContainerColor = selectedColor)

    SegmentedListItem(
        selected = selected,
        onClick = { if (enabled) onClick() },
        shapes = ListItemDefaults.shapes(shape = shape, selectedShape = shape),
        colors = itemColors,
        leadingContent = leading,
        trailingContent = trailing,
        verticalAlignment = alignment,
        supportingContent = if (description != null || supporting != null) {
            {
                Column {
                    description?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    supporting?.invoke()
                }
            }
        } else {
            null
        },
        modifier = modifier.then(
            if (indicatorColor != null) {
                Modifier.drawWithContent {
                    drawContent()
                    val widthPx = 4.dp.toPx()
                    drawRect(
                        color = indicatorColor,
                        topLeft = Offset.Zero,
                        size = Size(widthPx, size.height)
                    )
                }
            } else Modifier
        ),
        content = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            )
        },
    )
}

// TODO APPROVED but replace RecreationItemGroup
@Composable
fun NavigationSettingRow(
    title: String,
    description: String? = null,
    position: SegmentPosition = SegmentPosition.Single,
    enabled: Boolean = true,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
) = SegmentedSettingRow(
    title = title,
    description = description,
    position = position,
    enabled = enabled,
    leading = leading?.let {
        {
            Row(verticalAlignment = Alignment.CenterVertically) {
                it()
                Spacer(modifier = Modifier.width(4.dp)) // TODO decide
            }
        }
    },
    trailing = trailing,
    alignment = Alignment.CenterVertically,
    onClick = onClick,
)

// TODO APPROVED apart from recreation see todo comment in that file.
@Composable
fun ToggleSettingRow(
    title: String,
    description: String? = null,
    checked: Boolean,
    position: SegmentPosition = SegmentPosition.Single,
    enabled: Boolean = true,
    saving: Boolean = false,
    leading: (@Composable () -> Unit)? = null,
    toggle: @Composable (Boolean, (Boolean) -> Unit) -> Unit,
    onCheckedChange: (Boolean) -> Unit,
) {
    val canChange = enabled && !saving
    SegmentedSettingRow(
        title = title,
        description = description,
        position = position,
        enabled = enabled,
        selected = checked,
        leading = leading,
        alignment = Alignment.CenterVertically,
        trailing = { toggle(checked) { if (canChange) onCheckedChange(it) } },
        onClick = { if (canChange) onCheckedChange(!checked) },
    )
}

// TODO Approved!
@Composable
fun RadioSettingRow(
    title: String,
    description: String? = null,
    selected: Boolean, // or to enable second color
    position: SegmentPosition = SegmentPosition.Single,
    enabled: Boolean = true,
    radio: @Composable (Boolean, () -> Unit) -> Unit,
    onClick: () -> Unit,
) = SegmentedSettingRow(
    title = title,
    description = description,
    position = position,
    enabled = enabled,
    selected = selected,
    trailing = { // prev leading
        radio(selected) { if (enabled) onClick() }
    },
    onClick = onClick,
    alignment = Alignment.CenterVertically
)

@Composable
fun ActionSettingRow(
    title: String,
    description: String? = null,
    position: SegmentPosition = SegmentPosition.Single,
    enabled: Boolean = true,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
) = SegmentedSettingRow(
    title = title,
    description = description,
    position = position,
    enabled = enabled,
    leading = leading,
    trailing = trailing,
    onClick = onClick,
)

// TODO Not approved - see todos
// Same for here:  +4.dp for usages with icons?
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ResourceSettingRow(
    title: String,
    description: String? = null,
    position: SegmentPosition = SegmentPosition.Single,
    enabled: Boolean = true,
    selected: Boolean = false,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    supporting: (@Composable () -> Unit)? = null,
    alignment: Alignment.Vertical = ListItemDefaults.verticalAlignment(),
    indicatorColor: Color? = null,
    onClick: () -> Unit = {},
) = SegmentedSettingRow(
    title = title,
    description = description,
    position = position,
    enabled = enabled,
    selected = selected,
    leading = leading,
    trailing = trailing,
    alignment = alignment,
    supporting = supporting,
    indicatorColor = indicatorColor,
    onClick = onClick,
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpandableResourceSettingRow(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    expanded: Boolean = false,
    selected: Boolean = expanded,
    position: SegmentPosition = SegmentPosition.Single,
    enabled: Boolean = true,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    indicatorColor: Color? = null,
    onClick: () -> Unit = {},
    content: @Composable () -> Unit = {},
) {
    val shape = position.shape()
    val containerColor = MaterialTheme.colorScheme.surfaceContainer
    val selectedColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)

    SegmentedListItem(
        selected = selected,
        onClick = { if (enabled) onClick() },
        shapes = ListItemDefaults.shapes(shape = shape, selectedShape = shape),
        colors = ListItemDefaults.colors(
            containerColor = containerColor,
            selectedContainerColor = selectedColor
        ),
        modifier = modifier.then(
            if (indicatorColor != null) {
                Modifier.drawWithContent {
                    drawContent()
                    val widthPx = 4.dp.toPx()
                    drawRect(
                        color = indicatorColor,
                        topLeft = Offset.Zero,
                        size = Size(widthPx, size.height)
                    )
                }
            } else Modifier
        ),
        content = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 36.dp)
                ) {
                    if (leading != null) {
                        leading()
                        Spacer(Modifier.width(16.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                        )
                        if (description != null && !expanded) {
                            Text(
                                text = description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    if (trailing != null) {
                        Spacer(Modifier.width(16.dp))
                        trailing()
                    }
                }
                if (expanded) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        content()
                    }
                }
            }
        }
    )
}
