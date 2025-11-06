package com.capstone.aquabell.ui

import android.text.format.DateUtils
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SwapVert
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
import androidx.compose.runtime.setValue
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
import com.capstone.aquabell.ui.theme.AccentDanger
import com.capstone.aquabell.ui.theme.AccentSuccess
import com.capstone.aquabell.ui.theme.AccentWarning
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

    var sortDescending by remember { mutableStateOf(true) }
    val dismissedIds = remember { mutableStateOf(setOf<String>()) }

    val displayed = remember(alerts.value, sortDescending, dismissedIds.value) {
        val sorted = if (sortDescending) alerts.value.sortedByDescending { it.timestamp } else alerts.value.sortedBy { it.timestamp }
        sorted.filter { it.alertId !in dismissedIds.value }
    }

    val grouped = remember(displayed) { groupByDay(displayed) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Alerts",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.material3.OutlinedButton(
                    onClick = { sortDescending = !sortDescending },
                    modifier = Modifier.height(36.dp)
                ) {
                    Text(
                        text = if (sortDescending) "Newest ↓" else "Oldest ↑",
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                val hasResolved = alerts.value.any { it.acknowledged }
                androidx.compose.material3.OutlinedButton(
                    onClick = { viewModel.clearResolvedAlerts() },
                    enabled = hasResolved,
                    modifier = Modifier.height(36.dp)
                ) {
                    Text(
                        text = "Clear Resolved",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (hasResolved) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
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
                items(itemsForDay, key = { it.alertId }) { alert ->
                    Box(modifier = Modifier.animateItem()) {
                        SwipeableAlertRow(
                            alert = alert,
                            outline = outline,
                            onAcknowledge = { viewModel.acknowledgeAlert(alert.alertId) },
                            onRemove = { dismissedIds.value = dismissedIds.value + alert.alertId }
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
    onRemove: () -> Unit,
) {
    // Call rememberSwipeToDismissBoxState directly.
    // The keys inside will ensure it recomposes only when needed.
    val swipeState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value -> true
        },
    )

    // When fully swiped, acknowledge/remove then settle back
    LaunchedEffect(swipeState.currentValue, alert.alertId) {
        when (swipeState.currentValue) {
            SwipeToDismissBoxValue.EndToStart -> { onAcknowledge(); swipeState.snapTo(SwipeToDismissBoxValue.Settled) }
            SwipeToDismissBoxValue.StartToEnd -> { onRemove(); swipeState.snapTo(SwipeToDismissBoxValue.Settled) }
            else -> Unit
        }
    }

    SwipeToDismissBox(
        state = swipeState,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
        backgroundContent = { SwipeBackground(swipeState) },
        content = { AlertCard(alert = alert, outline = outline) }
    )
}

@Composable
private fun SwipeBackground(state: androidx.compose.material3.SwipeToDismissBoxState) {
    val target = state.targetValue
    val bgTarget = when (target) {
        SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.primaryContainer
        SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.errorContainer
        else -> Color.Transparent
    }
    val bgColor by animateColorAsState(targetValue = bgTarget, animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing))
    val align = if (target == SwipeToDismissBoxValue.EndToStart) Alignment.CenterEnd else Alignment.CenterStart
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(bgColor, RoundedCornerShape(16.dp)),
        contentAlignment = align
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (target == SwipeToDismissBoxValue.EndToStart) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            } else if (target == SwipeToDismissBoxValue.StartToEnd) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AlertCard(alert: AlertEntry, outline: Color) {
    val isAck = alert.acknowledged
    // Fix: Use Triple instead of nested Pairs
    val (chipColor, chipText, label) = if (isAck) {
        Triple(AccentSuccess.copy(alpha = 0.18f), MaterialTheme.colorScheme.onSurface, "RESOLVED")
    } else {
        when (alert.type.lowercase()) {
            "critical" -> Triple(AccentDanger.copy(alpha = 0.18f), MaterialTheme.colorScheme.onSurface, "CRITICAL")
            "caution" -> Triple(AccentWarning.copy(alpha = 0.20f), MaterialTheme.colorScheme.onSurface, "CAUTION")
            else -> Triple(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), MaterialTheme.colorScheme.onSurface, alert.type.uppercase())
        }
    }

    // Subtle shake on first appearance for critical
    val firstDraw = remember { mutableStateOf(true) }
    val target = if (firstDraw.value && alert.type.equals("critical", true)) 6f else 0f
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
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isAck) AccentSuccess else when (alert.type.lowercase()) {
                "critical" -> AccentDanger
                "caution" -> AccentWarning
                else -> outline
            }
        )
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
                Text(
                    alert.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isAck) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    label,
                    color = chipText,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier
                        .background(chipColor, RoundedCornerShape(999.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            Text(
                alert.message,
                style = MaterialTheme.typography.bodySmall,
                color = if (isAck) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
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

// value formatting now handled when creating alert.title in AlertEvaluator

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

