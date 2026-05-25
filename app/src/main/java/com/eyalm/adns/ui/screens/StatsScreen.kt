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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eyalm.adns.data.ListIcon
import com.eyalm.adns.data.network.NextDnsDomainData
import com.eyalm.adns.data.network.toHexId
import com.eyalm.adns.ui.components.ListIconView
import com.eyalm.adns.viewmodel.MainViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun StatsScreen(
    innerPadding: PaddingValues,
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = viewModel()
) {
    var selectedFilterText by remember { mutableStateOf("30 days") }
    val filterOptions = listOf("24 hours", "7 days", "30 days")

    val filterMap = mapOf(
        "24 hours" to "-24h",
        "7 days" to "-7d",
        "30 days" to "-30d"
    )


    val stats by viewModel.stats.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    LaunchedEffect(Unit) {
        if (stats == null) {
            viewModel
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(innerPadding)
            .statusBarsPadding()
    ) {
        if (stats == null && errorMessage.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularWavyProgressIndicator(modifier = Modifier.size(64.dp))
            }
        } else if (errorMessage.isNotEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(errorMessage)
            }
        } else {
            val allowedSeries = stats!!.first.data.firstOrNull { it.status == "default" || it.status == "allowed" }?.queries ?: emptyList()
            val blockedSeries = stats!!.first.data.firstOrNull { it.status == "blocked" }?.queries ?: emptyList()

            val size = minOf(allowedSeries.size, blockedSeries.size)
            val totalPoints = (0 until size).map { i -> (allowedSeries[i] + blockedSeries[i]).toFloat() }
            val blockedPoints = blockedSeries.map { it.toFloat() }

            val totalQueriesSum = allowedSeries.sum() + blockedSeries.sum()
            val blockedQueriesSum = blockedSeries.sum()
            val blockedPercent = if (totalQueriesSum > 0) (blockedQueriesSum.toFloat() / totalQueriesSum * 100).toInt() else 0

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                item { Spacer(modifier = Modifier.height(8.dp)) }

                item {
                    TotalQueriesCard(
                        totalCount = formatInteger(totalQueriesSum),
                        blockedCount = "${formatInteger(blockedQueriesSum)} ($blockedPercent%) blocked",
                        totalQueriesPoints = totalPoints,
                        blockedQueriesPoints = blockedPoints,
                        filterOptions = filterOptions,
                        selectedFilter = selectedFilterText,
                        onFilterSelected = { filter ->
                            selectedFilterText = filter
                            viewModel.getPeriod(filterMap[filter] ?: "-30d")
                        },
                    )
                }

                item {
                    BlockedQueriesCard(
                        domains = stats!!.second.data
                    )
                }
                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TotalQueriesCard(
    totalCount: String,
    blockedCount: String,
    totalQueriesPoints: List<Float>,
    blockedQueriesPoints: List<Float>,
    filterOptions: List<String>,
    selectedFilter: String,
    onFilterSelected: (String) -> Unit,
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
                        text = "TOTAL QUERIES",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = totalCount,
                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 42.sp, fontWeight = FontWeight.Black),
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                shape = CircleShape
            ) {
                Text(
                    text = blockedCount,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
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
                        lineColor = MaterialTheme.colorScheme.primary,
                        strokeWidth = 5.dp,
                        modifier = Modifier.fillMaxSize()
                    )
                    WavyLineChart(
                        points = blockedQueriesPoints,
                        lineColor = MaterialTheme.colorScheme.error,
                        strokeWidth = 5.dp,
                        modifier = Modifier.fillMaxSize().padding(top = 40.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                filterOptions.forEachIndexed { index, option ->
                    SegmentedButton(
                        selected = selectedFilter == option,
                        onClick = { onFilterSelected(option) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = filterOptions.size),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f),
                            activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            inactiveContainerColor = Color.Transparent,
                            inactiveContentColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                            activeBorderColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f),
                            inactiveBorderColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                        )
                    ) {
                        Text(option, fontWeight = if (selectedFilter == option) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
        }
    }
}

@Composable
fun BlockedQueriesCard(
    domains: List<NextDnsDomainData>,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.7f))
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) {

            Text(
                text = "Blocked queries",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 20.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (domains.isEmpty()) {
                Text(
                    text = "No blocked queries in this period",
                    modifier = Modifier.padding(32.dp).align(Alignment.CenterHorizontally),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                domains.forEachIndexed { index, item ->
                    BlockedQueryRow(item = item)
                    if (index < domains.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 76.dp, end = 24.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BlockedQueryRow(item: NextDnsDomainData) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {


        ListIconView(
            icon = ListIcon.Url("https://favicons.nextdns.io/${item.domain.toHexId()}@3x.png"),
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
        )
        

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            HighlightedDomainText(domain = item.domain)
            Text(
                text = "${formatInteger(item.queries)} queries",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun HighlightedDomainText(domain: String) {
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
    strokeWidth: Dp = 3.dp
) {
    Canvas(modifier = modifier) {
        if (points.isEmpty()) return@Canvas
        val width = size.width
        val height = size.height
        val maxVal = points.maxOrNull() ?: 1f
        val minVal = points.minOrNull() ?: 0f
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