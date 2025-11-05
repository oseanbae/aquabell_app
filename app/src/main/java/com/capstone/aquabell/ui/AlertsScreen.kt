package com.capstone.aquabell.ui

import android.text.format.DateUtils
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Badge
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.capstone.aquabell.data.model.AlertEntry
import com.capstone.aquabell.ui.viewmodel.AlertsViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.StateFlow
import androidx.compose.foundation.ExperimentalFoundationApi
import kotlin.math.roundToInt
//import androidx.compose.material3.DismissDirection
//import androidx.compose.material3.DismissValue
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AlertsScreen(
    modifier: Modifier = Modifier,
    viewModel: AlertsViewModel,
) {
    val outline = MaterialTheme.colorScheme.outline
    val alerts = viewModel.alerts.collectAsStateWithLifecycleCompat(emptyList())

    val grouped = remember(alerts.value) { groupByDay(alerts.value) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Alerts",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            grouped.forEach { (header, itemsForDay) ->
                item(key = "header_$header") {
                    Text(
                        text = header,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }
                items(itemsForDay, key = { it.id }) { alert ->
                    Box(modifier = Modifier.animateItemPlacement()) {
                        SwipeableAlertRow(
                            alert = alert,
                            outline = outline,
                            onAcknowledge = { viewModel.acknowledgeAlert(alert.id) }
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableAlertRow(
    alert: AlertEntry,
    outline: Color,
    onAcknowledge: () -> Unit,
) {
    val scope = rememberCoroutineScope()

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                scope.launch { onAcknowledge() }
            }
            false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromEndToStart = true,
        backgroundContent = { AcknowledgeBackground() },
        content = {
            AlertCard(alert = alert, outline = outline)
        }
    )
}


@Composable
private fun AcknowledgeBackground() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.CenterEnd
    ) {
        Row(modifier = Modifier.padding(end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.material3.Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Acknowledge",
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(8.dp))
            Text("Acknowledge", color = MaterialTheme.colorScheme.primary)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AlertCard(alert: AlertEntry, outline: Color) {
    val (chipColor, label) = when (alert.status.lowercase()) {
        "critical" -> MaterialTheme.colorScheme.error.copy(alpha = 0.18f) to "CRITICAL"
        "caution" -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.20f) to "CAUTION"
        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) to alert.status.uppercase()
    }

    // Subtle shake on first appearance for critical
    val firstDraw = remember { mutableStateOf(true) }
    val target = if (firstDraw.value && alert.status.equals("critical", true)) 6f else 0f
    val offsetX by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        finishedListener = { firstDraw.value = false }
    )

    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .offset { IntOffset((offsetX * kotlin.math.sin(System.currentTimeMillis().toFloat())).roundToInt(), 0) },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${alert.sensor}: ${formatValue(alert.sensor, alert.value)}", style = MaterialTheme.typography.titleSmall)
                Text(
                    label,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier
                        .background(chipColor, RoundedCornerShape(999.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            Text(
                alert.guidance,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
            val rel = remember(alert.timestamp) {
                DateUtils.getRelativeTimeSpanString(alert.timestamp, System.currentTimeMillis(), 60_000L)
            }
            Text(
                rel.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

private fun formatValue(sensor: String, value: Double): String {
    return when (sensor.lowercase()) {
        "ph" -> String.format("%.2f", value)
        "temperature" -> String.format("%.1f°C", value)
        "dissolved_oxygen", "do" -> String.format("%.1f mg/L", value)
        "turbidity" -> String.format("%.0f NTU", value)
        else -> String.format("%.2f", value)
    }
}

@Composable
private fun <T> StateFlow<T>.collectAsStateWithLifecycleCompat(initial: T): androidx.compose.runtime.State<T> {
    return this.collectAsState(initial)
}

private fun groupByDay(alerts: List<AlertEntry>): Map<String, List<AlertEntry>> {
    if (alerts.isEmpty()) return emptyMap()
    val now = java.util.Calendar.getInstance()
    val today = now.clone() as java.util.Calendar
    val yesterday = (now.clone() as java.util.Calendar).apply { add(java.util.Calendar.DAY_OF_YEAR, -1) }

    fun isSameDay(ts: Long, cal: java.util.Calendar): Boolean {
        val c = java.util.Calendar.getInstance().apply { timeInMillis = ts }
        return c.get(java.util.Calendar.YEAR) == cal.get(java.util.Calendar.YEAR) &&
            c.get(java.util.Calendar.DAY_OF_YEAR) == cal.get(java.util.Calendar.DAY_OF_YEAR)
    }

    val (todayList, other1) = alerts.partition { isSameDay(it.timestamp, today) }
    val (yesterdayList, olderList) = other1.partition { isSameDay(it.timestamp, yesterday) }

    val map = linkedMapOf<String, List<AlertEntry>>()
    if (todayList.isNotEmpty()) map["Today"] = todayList
    if (yesterdayList.isNotEmpty()) map["Yesterday"] = yesterdayList
    if (olderList.isNotEmpty()) map["Older"] = olderList
    return map
}

