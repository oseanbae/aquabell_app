package com.capstone.aquabell.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import com.capstone.aquabell.data.FirebaseRepository
import com.capstone.aquabell.data.model.SensorLog
import kotlinx.coroutines.flow.collectLatest
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun AnalyticsScreen(modifier: Modifier = Modifier) {
    val outline = MaterialTheme.colorScheme.outline
    val primary = MaterialTheme.colorScheme.primary
    val repo = remember { FirebaseRepository() }
    var logs by remember { mutableStateOf<List<SensorLog>>(emptyList()) }
    var interval by remember { mutableStateOf(AnalyticsInterval.FIVE_MIN) }

    LaunchedEffect(Unit) {
        repo.sensorLogs().collectLatest { logs = it }
    }

    val bucketMillis = when (interval) {
        AnalyticsInterval.FIVE_MIN -> 5L * 60L * 1000L
        AnalyticsInterval.HOURLY -> 60L * 60L * 1000L
    }

    val zone = remember { ZoneId.systemDefault() }
    val timeFormatter = remember(interval) {
        DateTimeFormatter.ofPattern(if (interval == AnalyticsInterval.FIVE_MIN) "HH:mm" else "HH:00")
    }

    val buckets = logs
        .groupBy { log ->
            val millis = log.timestamp.seconds * 1000L + log.timestamp.nanoseconds / 1_000_000L
            (millis / bucketMillis) * bucketMillis
        }
        .toSortedMap()

    val labels = buckets.keys.map { keyMillis ->
        val dt = Instant.ofEpochMilli(keyMillis).atZone(zone).toLocalDateTime()
        timeFormatter.format(dt)
    }

    fun List<SensorLog>.avg(selector: (SensorLog) -> Double): Float {
        if (isEmpty()) return 0f
        var sum = 0.0
        for (item in this) sum += selector(item)
        return (sum / size).toFloat()
    }

    val airTempC = buckets.values.map { it.avg { s -> s.airTemp } }
    val airHumidity = buckets.values.map { it.avg { s -> s.airHumidity } }
    val waterTempC = buckets.values.map { it.avg { s -> s.waterTemp } }
    val phLevel = buckets.values.map { it.avg { s -> s.pH } }
    val dissolvedOxygen = buckets.values.map { it.avg { s -> s.dissolvedOxygen } }
    val turbidity = buckets.values.map { it.avg { s -> s.turbidityNTU } }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Analytics",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IntervalChip(
                label = "5 min",
                selected = interval == AnalyticsInterval.FIVE_MIN,
                onClick = { interval = AnalyticsInterval.FIVE_MIN },
                outline = outline
            )
            IntervalChip(
                label = "Hourly",
                selected = interval == AnalyticsInterval.HOURLY,
                onClick = { interval = AnalyticsInterval.HOURLY },
                outline = outline
            )
        }

        LineChartCard(
            title = "Air Temperature (°C)",
            labels = labels,
            values = airTempC,
            lineColor = primary,
            outline = outline
        )

        LineChartCard(
            title = "Air Humidity (%)",
            labels = labels,
            values = airHumidity,
            lineColor = MaterialTheme.colorScheme.tertiary,
            outline = outline
        )

        LineChartCard(
            title = "Water Temperature (°C)",
            labels = labels,
            values = waterTempC,
            lineColor = MaterialTheme.colorScheme.primary,
            outline = outline
        )

        LineChartCard(
            title = "pH Level",
            labels = labels,
            values = phLevel,
            lineColor = MaterialTheme.colorScheme.secondary,
            outline = outline
        )

        LineChartCard(
            title = "Dissolved Oxygen (mg/L)",
            labels = labels,
            values = dissolvedOxygen,
            lineColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
            outline = outline
        )

        LineChartCard(
            title = "Turbidity (NTU)",
            labels = labels,
            values = turbidity,
            lineColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.9f),
            outline = outline
        )
    }
}

@Composable
private fun LineChartCard(
    title: String,
    labels: List<String>,
    values: List<Float>,
    lineColor: androidx.compose.ui.graphics.Color,
    outline: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, outline),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                if (values.isEmpty() || values.all { it == values.first() }) {
                    // Avoid divide-by-zero when all points equal
                    val y = size.height / 2
                    drawLine(
                        color = lineColor,
                        start = androidx.compose.ui.geometry.Offset(16f, y),
                        end = androidx.compose.ui.geometry.Offset(size.width - 16f, y),
                        strokeWidth = 6f
                    )
                    return@Canvas
                }

                val chartPadding = 24f
                val xStep = (size.width - chartPadding * 2) / (values.size - 1)
                val minY = (values.minOrNull() ?: 0f) - 0.5f
                val maxY = (values.maxOrNull() ?: 1f) + 0.5f
                val yRange = (maxY - minY).coerceAtLeast(1f)

                // Axes
                drawLine(
                    color = outline,
                    start = androidx.compose.ui.geometry.Offset(chartPadding, size.height - chartPadding),
                    end = androidx.compose.ui.geometry.Offset(size.width - chartPadding, size.height - chartPadding),
                    strokeWidth = 2f
                )
                drawLine(
                    color = outline,
                    start = androidx.compose.ui.geometry.Offset(chartPadding, chartPadding),
                    end = androidx.compose.ui.geometry.Offset(chartPadding, size.height - chartPadding),
                    strokeWidth = 2f
                )

                // Line path
                val path = Path()
                values.forEachIndexed { index, value ->
                    val x = chartPadding + xStep * index
                    val normalized = (value - minY) / yRange
                    val y = size.height - chartPadding - normalized * (size.height - chartPadding * 2)
                    if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(
                    path = path,
                    color = lineColor,
                    style = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                labels.forEach { label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

private enum class AnalyticsInterval { FIVE_MIN, HOURLY }

@Composable
private fun IntervalChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    outline: androidx.compose.ui.graphics.Color,
) {
    val bg = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
    val fg = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    OutlinedCard(
        border = BorderStroke(1.dp, outline),
        shape = RoundedCornerShape(50),
        colors = CardDefaults.cardColors(containerColor = bg)
    ) {
        Text(
            text = label,
            color = fg,
            modifier = Modifier
                .clickable { onClick() }
                .padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium
        )
    }
}


