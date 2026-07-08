package com.eyalm.adns.ui.screens


import android.icu.text.NumberFormat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eyalm.adns.R
import com.eyalm.adns.data.Locales
import com.eyalm.adns.data.nextdns.api.NextDnsDeviceItem
import com.eyalm.adns.data.nextdns.analytics.AnalyticsPeriod
import com.eyalm.adns.data.nextdns.analytics.ListCard
import com.eyalm.adns.data.nextdns.analytics.PercentCard
import com.eyalm.adns.data.nextdns.analytics.StatsRegistry
import com.eyalm.adns.data.nextdns.model.ListIcon
import com.eyalm.adns.data.nextdns.model.nextDnsFaviconUrl
import com.eyalm.adns.ui.components.refresh.AdnsPullToRefresh
import com.eyalm.adns.ui.components.GenericStatsListCard
import com.eyalm.adns.ui.components.GenericStatsPercentCard
import com.eyalm.adns.ui.components.ListIconView
import com.eyalm.adns.viewmodel.nextdns.CardState
import com.eyalm.adns.viewmodel.nextdns.StatsViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun StatsScreen(
    innerPadding: PaddingValues,
    modifier: Modifier = Modifier,
    statsViewModel: StatsViewModel = viewModel(),
) {
    val uiState by statsViewModel.state.collectAsState()
    val filterOptions = remember {
        AnalyticsPeriod.entries.map { period ->
            Locales.getString("timeRangeSelector", "ranges", period.localeKey) to period
        }
    }
    val stats = uiState.graph
    val isRefreshing = uiState.refreshing
    val cardStates = uiState.cards
    AdnsPullToRefresh(
        refreshing = isRefreshing,
        onRefresh = statsViewModel::refresh,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .statusBarsPadding()
        ) {
            if (uiState.initialLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularWavyProgressIndicator(modifier = Modifier.size(64.dp))
                }
            } else if (stats == null && uiState.graphError != null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(R.string.cannot_load_stats))
                }
            } else {
                val allowedSeries =
                    stats?.data?.firstOrNull { it.status == "default" || it.status == "allowed" }?.queries
                        ?: emptyList()
                val blockedSeries =
                    stats?.data?.firstOrNull { it.status == "blocked" }?.queries
                        ?: emptyList()

                val size = minOf(allowedSeries.size, blockedSeries.size)
                val totalPoints =
                    (0 until size).map { i -> (allowedSeries[i] + blockedSeries[i]).toFloat() }
                val blockedPoints = (0 until size).map { i -> blockedSeries[i].toFloat() }
                val maxQueries = (totalPoints.maxOrNull() ?: 1f).coerceAtLeast(1f)

                val totalQueriesSum = allowedSeries.sum() + blockedSeries.sum()
                val blockedQueriesSum = blockedSeries.sum()
                val blockedPercent =
                    if (totalQueriesSum > 0) (blockedQueriesSum.toFloat() / totalQueriesSum * 100).toInt() else 0

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {

                    item { Spacer(modifier = Modifier.height(8.dp)) }

                    item {
                        TotalQueriesCard(
                            totalCount = if (uiState.graphLoading && stats == null) null else formatInteger(totalQueriesSum),
                            blockedCount = if (uiState.graphLoading && stats == null) null else stringResource(R.string.blocked, formatInteger(blockedQueriesSum), blockedPercent),
                            totalQueriesPoints = totalPoints,
                            blockedQueriesPoints = blockedPoints,
                            maxQueries = maxQueries,
                            filterOptions = filterOptions.map { it.first },
                            selectedFilter = filterOptions
                                .first { it.second == uiState.scope.period }
                                .first,
                            onFilterSelected = { label ->
                                filterOptions.firstOrNull { it.first == label }
                                    ?.second
                                    ?.let(statsViewModel::selectPeriod)
                            },
                            devices = uiState.devices,
                            selectedDeviceId = uiState.scope.deviceId,
                            onDeviceSelected = statsViewModel::selectDevice,
                            isLoading = uiState.graphLoading
                        )
                    }
                    items(StatsRegistry.cards, key = { it.key }) { card ->
                        val state = cardStates[card.key] ?: CardState.Loading
                        when (card) {
                            is ListCard -> GenericStatsListCard(card, state)
                            is PercentCard -> GenericStatsPercentCard(card, state)
                        }
                    }

                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }
        }
    }

}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TotalQueriesCard(
    totalCount: String?,
    blockedCount: String?,
    totalQueriesPoints: List<Float>,
    blockedQueriesPoints: List<Float>,
    maxQueries: Float,
    filterOptions: List<String>,
    selectedFilter: String,
    onFilterSelected: (String) -> Unit,
    devices: List<NextDnsDeviceItem>,
    selectedDeviceId: String?,
    onDeviceSelected: (String?) -> Unit,
    isLoading: Boolean = false,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f))
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.total_queries),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = totalCount ?: "—",
                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 42.sp, fontWeight = FontWeight.Black),
                color = MaterialTheme.colorScheme.primary.copy(alpha = if (totalCount == null) 0.5f else 1f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = if (blockedCount == null) 0.3f else 0.7f),
                shape = CircleShape
            ) {
                Text(
                    text = blockedCount ?: "—",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = if (blockedCount == null) 0.5f else 1f),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Box(modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
            ) {
                if (totalQueriesPoints.size >= 2) {
                    WavyLineChart(
                        points = totalQueriesPoints,
                        lineColor = MaterialTheme.colorScheme.primary.copy(alpha = if (isLoading) 0.5f else 1f),
                        strokeWidth = 5.dp,
                        maxY = maxQueries,
                        modifier = Modifier.fillMaxSize()
                    )
                    WavyLineChart(
                        points = blockedQueriesPoints,
                        lineColor = MaterialTheme.colorScheme.error.copy(alpha = if (isLoading) 0.5f else 1f),
                        strokeWidth = 5.dp,
                        maxY = maxQueries,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (isLoading) {
                    CircularWavyProgressIndicator(
                        modifier = Modifier.align(Alignment.Center).size(40.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            var expanded by remember { mutableStateOf(false) }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box {
                    Button(onClick = { expanded = true }) {
                        Text(selectedFilter, fontWeight = FontWeight.Bold)
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                    ) {
                        filterOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    expanded = false
                                    onFilterSelected(option)
                                },
                            )
                        }
                    }
                }
                AnalyticsDeviceSelector(
                    devices = devices,
                    selectedDeviceId = selectedDeviceId,
                    onSelected = onDeviceSelected,
                )
            }
        }
    }
}

@Composable
private fun AnalyticsDeviceSelector(
    devices: List<NextDnsDeviceItem>,
    selectedDeviceId: String?,
    onSelected: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = when (selectedDeviceId) {
        null -> Locales.getString("deviceSelector", "all")
        "__UNIDENTIFIED__" -> Locales.getString(
            "analytics",
            "devices",
            "unidentified",
            "name",
        )
        else -> devices.firstOrNull { it.id == selectedDeviceId }?.name ?: selectedDeviceId
    }
    Box(modifier = modifier) {
        Button(
            onClick = { expanded = true },
        ) {
            Text(selectedName, fontWeight = FontWeight.Bold)
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                text = { Text(Locales.getString("deviceSelector", "all")) },
                onClick = {
                    expanded = false
                    onSelected(null)
                },
            )
            devices.filterNot { it.id == "__UNIDENTIFIED__" }.forEach { device ->
                DropdownMenuItem(
                    text = { Text(device.name ?: device.id) },
                    onClick = {
                        expanded = false
                        onSelected(device.id)
                    },
                )
            }
            DropdownMenuItem(
                text = {
                    Text(
                        Locales.getString(
                            "analytics",
                            "devices",
                            "unidentified",
                            "name",
                        )
                    )
                },
                onClick = {
                    expanded = false
                    onSelected("__UNIDENTIFIED__")
                },
            )

        }
    }
}

@Composable
internal fun HighlightedDomainText(domain: String) {
    val parts = domain.split(".")
    val annotatedString = if (parts.size >= 2) {
        val baseDomain = parts.takeLast(2).joinToString(".")
        val subdomain = domain.removeSuffix(baseDomain)

        buildAnnotatedString {
            withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)) {
                append(subdomain)
            }
            withStyle(style = SpanStyle(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            ) {
                append(baseDomain)
            }
        }
    } else {
        buildAnnotatedString {
            withStyle(style = SpanStyle(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            ) {
                append(domain)
            }
        }
    }

    Text(
        text = annotatedString,
        style = MaterialTheme.typography.bodyLarge,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
fun WavyLineChart(
    points: List<Float>,
    lineColor: Color,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 3.dp,
    maxY: Float? = null
) {
    Canvas(modifier = modifier) {
        if (points.size < 2) return@Canvas
        val width = size.width
        val height = size.height
        val maxVal = maxY ?: points.maxOrNull() ?: 1f
        val minVal = if (maxY != null) 0f else (points.minOrNull() ?: 0f)
        val range = (maxVal - minVal).coerceAtLeast(1f)

        val path = Path()
        val stepX = width / (points.size - 1)

        val coords = points.mapIndexed { index, value ->
            val x = index * stepX
            val y = height - ((value - minVal) / range) * height
            Offset(x, y)
        }

        path.moveTo(coords[0].x, coords[0].y)
        for (i in 0 until coords.size - 1) {
            val p0 = coords[i]
            val p1 = coords[i + 1]
            val controlPointX = (p0.x + p1.x) / 2
            path.cubicTo(controlPointX, p0.y, controlPointX, p1.y, p1.x, p1.y)
        }

        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(
                width = strokeWidth.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}

private fun formatInteger(number: Int): String {
    return NumberFormat.getNumberInstance(Locale.US).format(number)
}
