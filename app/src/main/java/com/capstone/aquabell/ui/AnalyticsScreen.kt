package com.capstone.aquabell.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.capstone.aquabell.R
import com.capstone.aquabell.data.model.*
import com.capstone.aquabell.ui.theme.AquabellTheme
import com.capstone.aquabell.ui.viewmodel.AnalyticsViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AnalyticsScreen(modifier: Modifier = Modifier) {
    val viewModel: AnalyticsViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()

    AquabellTheme {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with title and controls
            AnalyticsHeader(
                isLoading = uiState.isLoading,
                sortOrder = sortOrder,
                onToggleSort = { viewModel.toggleSortOrder() },
                onRefresh = { viewModel.refresh() }
            )

            // Error state
            uiState.error?.let { error ->
                ErrorCard(error = error)
            }

            // Loading state
            if (uiState.isLoading && uiState.dailyAnalytics.isEmpty()) {
                LoadingCard()
            } else {
                // Analytics content
                AnalyticsContent(
                    dailyAnalytics = uiState.dailyAnalytics,
                    sortOrder = sortOrder
                )
            }
        }
    }
}

@Composable
private fun AnalyticsHeader(
    isLoading: Boolean,
    sortOrder: SortOrder,
    onToggleSort: () -> Unit,
    onRefresh: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Analytics",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
        )
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Sort toggle button
            OutlinedButton(
                onClick = onToggleSort,
                modifier = Modifier.height(36.dp)
            ) {
                Text(
                    text = if (sortOrder == SortOrder.NEWEST_FIRST) "Newest ↓" else "Oldest ↑",
                    style = MaterialTheme.typography.labelMedium
                )
            }
            
            // Refresh button
            IconButton(
                onClick = onRefresh,
                enabled = !isLoading
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    modifier = Modifier.rotate(if (isLoading) 180f else 0f)
                )
            }
        }
    }
}

@Composable
private fun AnalyticsContent(
    dailyAnalytics: List<DailyAnalytics>,
    sortOrder: SortOrder
) {
    if (dailyAnalytics.isEmpty()) {
        EmptyStateCard()
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(dailyAnalytics) { dailyData ->
                AnalyticsAccordionCard(dailyData = dailyData)
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun AnalyticsAccordionCard(dailyData: DailyAnalytics) {
    var isExpanded by remember { mutableStateOf(false) }
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(300),
        label = "arrow_rotation"
    )

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (dailyData.isLive) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.outline
            }
        )
    ) {
        Column {
            // Header - clickable to expand/collapse
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Date icon
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                if (dailyData.isLive) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "📅",
                            fontSize = 20.sp
                        )
                    }
                    
                    Column {
                        Text(
                            text = formatDate(dailyData.date),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        if (dailyData.isLive) {
                            LiveIndicator()
                        }
                    }
                }
                
                // Expand arrow
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    modifier = Modifier.rotate(rotationAngle),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            
            // Expanded content
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(animationSpec = tween(300)),
                exit = shrinkVertically(animationSpec = tween(300))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Sensor metrics
                    SensorMetricsGrid(dailyData = dailyData)
                }
            }
        }
    }
}

@Composable
private fun LiveIndicator() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
        )
        Text(
            text = "Live (Updating)",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun SensorMetricsGrid(dailyData: DailyAnalytics) {
    val metrics = listOf(
        SensorMetric(
            label = "Air Temperature",
            value = dailyData.airTemp,
            unit = "°C",
            status = calculateSensorStatus("Air Temp", dailyData.airTemp),
            iconRes = R.drawable.ic_thermometer
        ),
        SensorMetric(
            label = "Air Humidity",
            value = dailyData.airHumidity,
            unit = "%",
            status = calculateSensorStatus("Humidity", dailyData.airHumidity),
            iconRes = R.drawable.ic_humidity
        ),
        SensorMetric(
            label = "Water Temperature",
            value = dailyData.waterTemp,
            unit = "°C",
            status = calculateSensorStatus("Water Temp", dailyData.waterTemp),
            iconRes = R.drawable.ic_water_temp
        ),
        SensorMetric(
            label = "pH Level",
            value = dailyData.pH,
            unit = "pH",
            status = calculateSensorStatus("pH", dailyData.pH),
            iconRes = R.drawable.ic_ph
        ),
        SensorMetric(
            label = "Dissolved Oxygen",
            value = dailyData.dissolvedOxygen,
            unit = "mg/L",
            status = calculateSensorStatus("Dissolved Oxygen", dailyData.dissolvedOxygen),
            iconRes = R.drawable.ic_oxygen
        ),
        SensorMetric(
            label = "Turbidity",
            value = dailyData.turbidityNTU,
            unit = "NTU",
            status = calculateSensorStatus("Turbidity", dailyData.turbidityNTU),
            iconRes = R.drawable.ic_turbidity
        )
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        metrics.forEach { metric ->
            SensorMetricRow(metric = metric)
        }
    }
}

@Composable
private fun SensorMetricRow(metric: SensorMetric) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon and label
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                painter = painterResource(id = metric.iconRes),
                contentDescription = metric.label,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            
            Text(
                text = metric.label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
        }
        
        // Value and status
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "${String.format("%.1f", metric.value)} ${metric.unit}",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface
            )
            
            // Status chip
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(metric.status.color.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = metric.status.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = metric.status.color
                )
            }
        }
    }
    
    // Animated progress bar
    AnimatedSensorBar(
        progress = metric.status.progress,
        color = metric.status.color
    )
}

@Composable
private fun AnimatedSensorBar(
    progress: Float,
    color: Color
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(800),
        label = "progress_animation"
    )
    
    val animatedColor by animateColorAsState(
        targetValue = color,
        animationSpec = tween(800),
        label = "color_animation"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animatedProgress)
                .fillMaxHeight()
                .background(animatedColor)
        )
    }
}

@Composable
private fun LoadingCard() {
    OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator()
                    Text(
                text = "Loading analytics data...",
                style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
}

@Composable
private fun ErrorCard(error: String) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Error",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error
            )
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun EmptyStateCard() {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Subtle animated alpha could be added later if desired
            Text(text = "📭", fontSize = 48.sp)
            Text(
                text = "No records found",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Data will appear here once daily logs are available.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

// Helper functions
private fun formatDate(dateString: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outputFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val date = inputFormat.parse(dateString)
        outputFormat.format(date ?: Date())
    } catch (e: Exception) {
        dateString
    }
}

private fun calculateSensorStatus(parameter: String, value: Double): com.capstone.aquabell.data.model.SensorStatus {
    return when (parameter) {
        "Air Temp" -> calculateAirTempStatus(value)
        "Humidity" -> calculateHumidityStatus(value)
        "Water Temp" -> calculateWaterTempStatus(value)
        "pH" -> calculatePHStatus(value)
        "Dissolved Oxygen" -> calculateDOStatus(value)
        "Turbidity" -> calculateTurbidityStatus(value)
        else -> com.capstone.aquabell.data.model.SensorStatus("Unknown", Color.Gray, 0.5f)
    }
}

private fun calculateAirTempStatus(value: Double): com.capstone.aquabell.data.model.SensorStatus {
    return when {
        value in SensorRanges.AIR_TEMP_EXCELLENT_MIN..SensorRanges.AIR_TEMP_EXCELLENT_MAX -> 
            com.capstone.aquabell.data.model.SensorStatus("Excellent", Color(0xFF4CAF50), 0.8f)
        value in SensorRanges.AIR_TEMP_ACCEPTABLE_MIN..SensorRanges.AIR_TEMP_ACCEPTABLE_MAX -> 
            com.capstone.aquabell.data.model.SensorStatus("Good", Color(0xFF2196F3), 0.6f)
        value in SensorRanges.AIR_TEMP_CAUTION_MIN..SensorRanges.AIR_TEMP_CAUTION_MAX -> 
            com.capstone.aquabell.data.model.SensorStatus("Caution", Color(0xFFFF9800), 0.4f)
        else -> com.capstone.aquabell.data.model.SensorStatus("Critical", Color(0xFFF44336), 0.2f)
    }
}

private fun calculateHumidityStatus(value: Double): com.capstone.aquabell.data.model.SensorStatus {
    return when {
        value in SensorRanges.HUMIDITY_EXCELLENT_MIN..SensorRanges.HUMIDITY_EXCELLENT_MAX -> 
            com.capstone.aquabell.data.model.SensorStatus("Excellent", Color(0xFF4CAF50), 0.8f)
        value in SensorRanges.HUMIDITY_ACCEPTABLE_MIN..SensorRanges.HUMIDITY_ACCEPTABLE_MAX -> 
            com.capstone.aquabell.data.model.SensorStatus("Good", Color(0xFF2196F3), 0.6f)
        value in SensorRanges.HUMIDITY_CAUTION_MIN..SensorRanges.HUMIDITY_CAUTION_MAX -> 
            com.capstone.aquabell.data.model.SensorStatus("Caution", Color(0xFFFF9800), 0.4f)
        else -> com.capstone.aquabell.data.model.SensorStatus("Critical", Color(0xFFF44336), 0.2f)
    }
}

private fun calculateWaterTempStatus(value: Double): com.capstone.aquabell.data.model.SensorStatus {
    return when {
        value in SensorRanges.WATER_TEMP_EXCELLENT_MIN..SensorRanges.WATER_TEMP_EXCELLENT_MAX -> 
            com.capstone.aquabell.data.model.SensorStatus("Excellent", Color(0xFF4CAF50), 0.8f)
        value in SensorRanges.WATER_TEMP_ACCEPTABLE_MIN..SensorRanges.WATER_TEMP_ACCEPTABLE_MAX -> 
            com.capstone.aquabell.data.model.SensorStatus("Good", Color(0xFF2196F3), 0.6f)
        value in SensorRanges.WATER_TEMP_CAUTION_MIN..SensorRanges.WATER_TEMP_CAUTION_MAX -> 
            com.capstone.aquabell.data.model.SensorStatus("Caution", Color(0xFFFF9800), 0.4f)
        else -> com.capstone.aquabell.data.model.SensorStatus("Critical", Color(0xFFF44336), 0.2f)
    }
}

private fun calculatePHStatus(value: Double): com.capstone.aquabell.data.model.SensorStatus {
    return when {
        value in SensorRanges.PH_EXCELLENT_MIN..SensorRanges.PH_EXCELLENT_MAX -> 
            com.capstone.aquabell.data.model.SensorStatus("Excellent", Color(0xFF4CAF50), 0.8f)
        value in SensorRanges.PH_ACCEPTABLE_MIN..SensorRanges.PH_ACCEPTABLE_MAX -> 
            com.capstone.aquabell.data.model.SensorStatus("Good", Color(0xFF2196F3), 0.6f)
        value in SensorRanges.PH_CAUTION_MIN..SensorRanges.PH_CAUTION_MAX -> 
            com.capstone.aquabell.data.model.SensorStatus("Caution", Color(0xFFFF9800), 0.4f)
        else -> com.capstone.aquabell.data.model.SensorStatus("Critical", Color(0xFFF44336), 0.2f)
    }
}

private fun calculateDOStatus(value: Double): com.capstone.aquabell.data.model.SensorStatus {
    return when {
        value >= SensorRanges.DO_EXCELLENT_MIN -> 
            com.capstone.aquabell.data.model.SensorStatus("Excellent", Color(0xFF4CAF50), 0.8f)
        value >= SensorRanges.DO_ACCEPTABLE_MIN -> 
            com.capstone.aquabell.data.model.SensorStatus("Good", Color(0xFF2196F3), 0.6f)
        value >= SensorRanges.DO_CAUTION_MIN -> 
            com.capstone.aquabell.data.model.SensorStatus("Caution", Color(0xFFFF9800), 0.4f)
        else -> com.capstone.aquabell.data.model.SensorStatus("Critical", Color(0xFFF44336), 0.2f)
    }
}

private fun calculateTurbidityStatus(value: Double): com.capstone.aquabell.data.model.SensorStatus {
    return when {
        value <= SensorRanges.TURBIDITY_EXCELLENT_MAX -> 
            com.capstone.aquabell.data.model.SensorStatus("Excellent", Color(0xFF4CAF50), 0.8f)
        value <= SensorRanges.TURBIDITY_ACCEPTABLE_MAX -> 
            com.capstone.aquabell.data.model.SensorStatus("Good", Color(0xFF2196F3), 0.6f)
        value <= SensorRanges.TURBIDITY_CAUTION_MAX -> 
            com.capstone.aquabell.data.model.SensorStatus("Caution", Color(0xFFFF9800), 0.4f)
        else -> com.capstone.aquabell.data.model.SensorStatus("Critical", Color(0xFFF44336), 0.2f)
    }
}