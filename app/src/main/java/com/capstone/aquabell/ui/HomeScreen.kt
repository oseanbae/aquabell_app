package com.capstone.aquabell.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.capstone.aquabell.R
import com.capstone.aquabell.data.FirebaseRepository
import com.capstone.aquabell.data.model.ControlMode
import com.capstone.aquabell.data.model.RelayStates
import com.capstone.aquabell.data.model.SensorRanges
import com.capstone.aquabell.ui.theme.AquabellTheme
import com.capstone.aquabell.ui.viewmodel.HomeViewModel

@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    val vm: HomeViewModel = viewModel()
    var selectedNavIndex by remember { mutableIntStateOf(1) } // center is Home
    val live by vm.live.collectAsState()
    val command by vm.command.collectAsState()
    val commandLoaded by vm.isCommandLoaded.collectAsState()
    val offline by vm.offlineCache.collectAsState()
    val connectionState by vm.connectionState.collectAsState()
    val isRefreshing by vm.isRefreshing.collectAsState()

    AquabellTheme {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            topBar = {
                TopBar()
            },
            bottomBar = {
                BottomNavBar(selectedIndex = selectedNavIndex, onSelected = { selectedNavIndex = it })
            }
        ) { inner ->
            when (selectedNavIndex) {
                0 -> AnalyticsScreen(modifier = Modifier.padding(inner))
                1 -> HomeContent(
                    modifier = Modifier.padding(inner),
                    live = live ?: offline,
                    command = command,
                    commandLoaded = commandLoaded,
                    connectionState = connectionState,
                    isRefreshing = isRefreshing,
                    onRefresh = { vm.refresh() },
                    onOverride = { overrides -> vm.setRelayOverrides(overrides) },
                    onSetActuatorMode = { actuator, mode -> vm.setActuatorMode(actuator, mode) },
                    onSetActuatorValue = { actuator, value -> vm.setActuatorValue(actuator, value) }
                )
                2 ->  {
                    val alertsVm = androidx.lifecycle.viewmodel.compose.viewModel<com.capstone.aquabell.ui.viewmodel.AlertsViewModel>()
                    AlertsScreen(modifier = Modifier.padding(inner), viewModel = alertsVm)
                }
                else -> {
                    DocsScreen(modifier = Modifier.padding(inner))
                }
            }
        }
    }
}

@Composable
private fun HomeContent(
    modifier: Modifier = Modifier,
    live: com.capstone.aquabell.data.model.LiveDataSnapshot?,
    command: com.capstone.aquabell.data.model.CommandControl,
    commandLoaded: Boolean,
    connectionState: FirebaseRepository.ConnectionState,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onOverride: (RelayStates) -> Unit,
    onSetActuatorMode: (actuator: String, mode: ControlMode) -> Unit,
    onSetActuatorValue: (actuator: String, value: Boolean) -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ConnectionStatusBanner(connectionState, isRefreshing, onRefresh)
        SectionHeader(title = "Dashboard")
        DashboardGrid(live)
        WaterLevelModule(live)
        ControlHeader()
        if (!commandLoaded) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            // Log the command state when rendering the control panel
            LaunchedEffect(command) {
                android.util.Log.d(
                    "HomeScreen",
                    "Rendering control panel with states: fan=${command.fan.mode}/${command.fan.value}, light=${command.light.mode}/${command.light.value}, pump=${command.pump.mode}/${command.pump.value}, valve=${command.valve.mode}/${command.valve.value}, cooler=${command.cooler.mode}/${command.cooler.value}, heater=${command.heater.mode}/${command.heater.value}"
                )
            }
            PerActuatorControlGrid(
                command = command,
                onSetActuatorMode = onSetActuatorMode,
                onSetActuatorValue = onSetActuatorValue,
                live = live
            )
        }
        Spacer(Modifier.height(16.dp))
    }
}



@Composable
fun ConnectionStatusBanner(
    connectionState: FirebaseRepository.ConnectionState,
    isRefreshing: Boolean = false,
    onRefresh: (() -> Unit)? = null
) {
    val bg: Color
    val fg: Color
    val text: String
    
    when (connectionState) {
        FirebaseRepository.ConnectionState.CONNECTED -> {
            bg = Color(0xFF1B5E20).copy(alpha = 0.12f)
            fg = Color(0xFF2E7D32)
            text = "Connected to Firebase"
        }
        FirebaseRepository.ConnectionState.CONNECTING -> {
            bg = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            fg = MaterialTheme.colorScheme.primary
            text = "Connecting to Firebase..."
        }
        FirebaseRepository.ConnectionState.NOT_CONNECTED -> {
            bg = MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
            fg = MaterialTheme.colorScheme.error
            text = "Not connected - showing cached data"
        }
    }
    
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, fg.copy(alpha = 0.5f)),
        colors = CardDefaults.cardColors(containerColor = bg),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (connectionState == FirebaseRepository.ConnectionState.CONNECTING || isRefreshing) {
                CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(16.dp), color = fg)
            }
            Text(text = text, color = fg, style = MaterialTheme.typography.bodySmall)
            if (onRefresh != null && connectionState == FirebaseRepository.ConnectionState.NOT_CONNECTED) {
                Spacer(Modifier.weight(1f))
                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh connection",
                        tint = fg,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar() {
    TopAppBar(
        title = {
            Column {
                Text("AquaBell", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                Text(
                    text = "Smart Aquaponic System",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        actions = {
            // Placeholder avatar
            Box(
                modifier = Modifier
                    .padding(end = 12.dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text("A", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        modifier = Modifier
            .padding(top = 8.dp, bottom = 12.dp)
    )
}

@Composable
fun DashboardGrid(live: com.capstone.aquabell.data.model.LiveDataSnapshot?) {
    val outline = MaterialTheme.colorScheme.outline
    data class MetricData(val title: String, val value: String, val iconRes: Int, val status: String, val statusColor: Color)
    
    val metrics: List<MetricData> = listOf(
        MetricData(
            "Air Temp.",
            if (live!=null) String.format("%.1f°C", live.airTemp) else "-",
            R.drawable.ic_thermometer,
            if (live!=null) getSensorStatus("Air Temp", live.airTemp).label else "Unknown",
            if (live!=null) getSensorStatus("Air Temp", live.airTemp).color else Color(0xFF9E9E9E)
        ),
        MetricData(
            "Air Humidity RH",
            if (live!=null) String.format("%.0f%%", live.airHumidity) else "-",
            R.drawable.ic_humidity,
            if (live!=null) getSensorStatus("Humidity", live.airHumidity).label else "Unknown",
            if (live!=null) getSensorStatus("Humidity", live.airHumidity).color else Color(0xFF9E9E9E)
        ),
        MetricData(
            "Water Temp.",
            if (live!=null) String.format("%.1f°C", live.waterTemp) else "-",
            R.drawable.ic_water_temp,
            if (live!=null) getSensorStatus("Water Temp", live.waterTemp).label else "Unknown",
            if (live!=null) getSensorStatus("Water Temp", live.waterTemp).color else Color(0xFF9E9E9E)
        ),
        MetricData(
            "pH Level",
            if (live!=null) String.format("%.1f pH", live.pH) else "-",
            R.drawable.ic_ph,
            if (live!=null) getSensorStatus("pH", live.pH).label else "Unknown",
            if (live!=null) getSensorStatus("pH", live.pH).color else Color(0xFF9E9E9E)
        ),
        MetricData(
            "Dissolved Oxygen",
            if (live!=null) String.format("%.1f mg/L", live.dissolvedOxygen) else "-",
            R.drawable.ic_oxygen,
            if (live!=null) getSensorStatus("Dissolved Oxygen", live.dissolvedOxygen).label else "Unknown",
            if (live!=null) getSensorStatus("Dissolved Oxygen", live.dissolvedOxygen).color else Color(0xFF9E9E9E)
        ),
        MetricData(
            "Turbidity Level",
            if (live!=null) String.format("%.0f NTU", live.turbidityNTU) else "-",
            R.drawable.ic_turbidity,
            if (live!=null) getSensorStatus("Turbidity", live.turbidityNTU).label else "Unknown",
            if (live!=null) getSensorStatus("Turbidity", live.turbidityNTU).color else Color(0xFF9E9E9E)
        ),
    )
    
    // Responsive grid layout (denser spacing for compactness)
    run {
        val configuration = LocalConfiguration.current
        val screenWidthDp = configuration.screenWidthDp.dp
        val isTablet = screenWidthDp > 600.dp

        if (isTablet) {
            // Tablet layout: 3 columns
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Row 1: First 3 metrics
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    metrics.take(3).forEach { metric ->
                        MetricCard(
                            title = metric.title,
                            value = metric.value,
                            status = metric.status,
                            iconRes = metric.iconRes,
                            borderColor = outline,
                            statusColor = metric.statusColor,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                // Row 2: Last 3 metrics
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    metrics.drop(3).forEach { metric ->
                        MetricCard(
                            title = metric.title,
                            value = metric.value,
                            status = metric.status,
                            iconRes = metric.iconRes,
                            borderColor = outline,
                            statusColor = metric.statusColor,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        } else {
            // Phone layout: 2 columns
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Row 1: First 2 metrics
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    metrics.take(2).forEach { metric ->
                        MetricCard(
                            title = metric.title,
                            value = metric.value,
                            status = metric.status,
                            iconRes = metric.iconRes,
                            borderColor = outline,
                            statusColor = metric.statusColor,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                // Row 2: Next 2 metrics
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    metrics.drop(2).take(2).forEach { metric ->
                        MetricCard(
                            title = metric.title,
                            value = metric.value,
                            status = metric.status,
                            iconRes = metric.iconRes,
                            borderColor = outline,
                            statusColor = metric.statusColor,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                // Row 3: Last 2 metrics
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    metrics.drop(4).forEach { metric ->
                        MetricCard(
                            title = metric.title,
                            value = metric.value,
                            status = metric.status,
                            iconRes = metric.iconRes,
                            borderColor = outline,
                            statusColor = metric.statusColor,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WaterLevelModule(live: com.capstone.aquabell.data.model.LiveDataSnapshot?) {
    val outline = MaterialTheme.colorScheme.outline
    val isLowWater = live?.floatTriggered == true
    
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(112.dp), // Slightly tighter to match compact sensor cards
        colors = CardDefaults.cardColors(
            containerColor = if (isLowWater) {
                Color(0xFFFF5722).copy(alpha = 0.05f) // Light red background for low water
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            width = 1.5.dp,
            color = if (isLowWater) Color(0xFFFF5722) else outline
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon with background
            Box(
                modifier = Modifier
                    .size(44.dp) // Slightly larger for visibility
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isLowWater) {
                            Color(0xFFFF5722).copy(alpha = 0.15f)
                        } else {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_float_switch),
                    contentDescription = "Water Level",
                    tint = if (isLowWater) Color(0xFFFF5722) else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp)
                )
            }
            
            // Content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Water Level",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (isLowWater) "LOW WATER LEVEL DETECTED" else "Water level is normal",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                    color = if (isLowWater) Color(0xFFFF5722) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = if (isLowWater) "⚠️ Check water supply immediately" else "✓ System operating normally",
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                    color = if (isLowWater) Color(0xFFFF5722) else MaterialTheme.colorScheme.tertiary
                )
            }
            
            // Status indicator
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(
                        if (isLowWater) Color(0xFFFF5722) else Color(0xFF4CAF50)
                    )
            )
        }
    }
}

@Composable
private fun MetricCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    status: String,
    iconRes: Int,
    borderColor: Color,
    statusColor: Color,
) {
    var showTooltip by remember { mutableStateOf(false) }

    // Auto-hide tooltip after 3 seconds
    LaunchedEffect(showTooltip) {
        if (showTooltip) {
            kotlinx.coroutines.delay(3000)
            showTooltip = false
        }
    }

    val adaptiveFontSize = when {
        value.length <= 8 -> 18.sp
        value.length <= 12 -> 16.sp
        value.length <= 16 -> 14.sp
        else -> 12.sp
    }
    
    Box(
        modifier = modifier
            .height(140.dp) // Extra room to always show status chip
    ) {
        OutlinedCard(
            modifier = Modifier.fillMaxSize(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, borderColor)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top row: Icon and Info button (increase icon prominence)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (title == "Water Level" && status == "Low") {
                                    Color(0xFFFF5722).copy(alpha = 0.15f)
                                } else {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = iconRes),
                            contentDescription = null,
                            // Theme-aware tint for contrast in dark/light
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    
                    // Info icon for tooltip
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clickable { showTooltip = !showTooltip },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Sensor information",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                
                // Center: Value with adaptive sizing (balanced prominence)
                AnimatedContent(
                    targetState = value,
                    transitionSpec = {
                        slideInVertically { it } + fadeIn() togetherWith
                        slideOutVertically { -it } + fadeOut()
                    },
                    label = "value_animation"
                ) { animatedValue ->
                    // Nudge elevation briefly on update
                    LaunchedEffect(animatedValue) {
                        // pulse elevation
                        kotlinx.coroutines.delay(250)
                    }
                    Text(
                        text = animatedValue,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = adaptiveFontSize
                        ),
                        color = if (title == "Water Level" && status == "Low") Color(0xFFFF5722) else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
                
                // Bottom: Title and Status
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(4.dp))
                    // Status chip for clear visibility
                    val bgColor = statusColor.copy(alpha = 0.15f)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(bgColor)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = status,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = statusColor,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
        
        // Tooltip
        if (showTooltip) {
            val tooltipText = when (title) {
                "Air Temp." -> "Air temperature for fish comfort and plant growth."
                "Air Humidity RH" -> "Air moisture levels to prevent stress or mold."
                "Water Temp." -> "Water temperature for fish health and nutrient uptake."
                "pH Level" -> "Water acidity/alkalinity for fish, plants, and bacteria."
                "Dissolved Oxygen" -> "Oxygen in water for fish survival and waste breakdown."
                "Turbidity Level" -> "Water clarity; high levels signal poor quality."
                "Water Level" -> "Water level status; LOW indicates water level is below threshold."
                else -> "Sensor information"
            }

            AnimatedContent(
                targetState = showTooltip,
                transitionSpec = {
                    (slideInVertically { fullHeight -> fullHeight / 3 } + fadeIn()) togetherWith
                    (slideOutVertically { fullHeight -> -fullHeight / 3 } + fadeOut())
                },
                label = "tooltip_animation"
            ) { visible ->
                if (visible) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 6.dp, end = 6.dp)
                            .width(240.dp)
                    ) {
                        OutlinedCard(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                        ) {
                            Text(
                                text = tooltipText,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(12.dp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

data class SensorStatus(
    val label: String,
    val color: Color
)

// Define the colors for clarity and reuse
// Assuming Color is a type from a library like androidx.compose.ui.graphics.Color or a custom type.
// I'll keep the original hex values as they are a reasonable representation.
val Green = Color(0xFF00C853) // Excellent
val Blue = Color(0xFF2962FF)  // Good / Acceptable
val Orange = Color(0xFFFFA000) // Caution
val Red = Color(0xFFD50000)   // Critical
val Gray = Color.Gray         // Unknown

fun getSensorStatus(parameter: String, value: Double): SensorStatus {
    return when (parameter) {
        "Turbidity" -> when {
            value <= SensorRanges.TURBIDITY_EXCELLENT_MAX -> SensorStatus("Excellent", Green)
            value <= SensorRanges.TURBIDITY_ACCEPTABLE_MAX -> SensorStatus("Normal", Blue)
            value <= SensorRanges.TURBIDITY_CAUTION_MAX -> SensorStatus("Caution", Orange)
            else -> SensorStatus("Critical", Red) // > TURBIDITY_CAUTION_MAX
        }

        "pH" -> when {
            value >= SensorRanges.PH_EXCELLENT_MIN && value <= SensorRanges.PH_EXCELLENT_MAX -> SensorStatus("Excellent", Green)
            (value >= SensorRanges.PH_ACCEPTABLE_MIN && value < SensorRanges.PH_EXCELLENT_MIN) || 
            (value > SensorRanges.PH_EXCELLENT_MAX && value <= SensorRanges.PH_ACCEPTABLE_MAX) -> SensorStatus("Normal", Blue)
            (value >= SensorRanges.PH_CAUTION_MIN && value < SensorRanges.PH_ACCEPTABLE_MIN) || 
            (value > SensorRanges.PH_ACCEPTABLE_MAX && value <= SensorRanges.PH_CAUTION_MAX) -> SensorStatus("Caution", Orange)
            else -> SensorStatus("Critical", Red) // < PH_CAUTION_MIN or > PH_CAUTION_MAX
        }

        "Dissolved Oxygen" -> when {
            value >= SensorRanges.DO_EXCELLENT_MIN -> SensorStatus("Excellent", Green)
            value >= SensorRanges.DO_ACCEPTABLE_MIN -> SensorStatus("Normal", Blue)
            value >= SensorRanges.DO_CAUTION_MIN -> SensorStatus("Caution", Orange)
            else -> SensorStatus("Critical", Red) // < DO_CAUTION_MIN
        }

        "Water Temp" -> when {
            value >= SensorRanges.WATER_TEMP_EXCELLENT_MIN && value <= SensorRanges.WATER_TEMP_EXCELLENT_MAX -> SensorStatus("Excellent", Green)
            (value >= SensorRanges.WATER_TEMP_ACCEPTABLE_MIN && value < SensorRanges.WATER_TEMP_EXCELLENT_MIN) || 
            (value > SensorRanges.WATER_TEMP_EXCELLENT_MAX && value <= SensorRanges.WATER_TEMP_ACCEPTABLE_MAX) -> SensorStatus("Normal", Blue)
            (value >= SensorRanges.WATER_TEMP_CAUTION_MIN && value < SensorRanges.WATER_TEMP_ACCEPTABLE_MIN) || 
            (value > SensorRanges.WATER_TEMP_ACCEPTABLE_MAX && value <= SensorRanges.WATER_TEMP_CAUTION_MAX) -> SensorStatus("Caution", Orange)
            else -> SensorStatus("Critical", Red) // < WATER_TEMP_CAUTION_MIN or > WATER_TEMP_CAUTION_MAX
        }

        "Air Temp" -> when {
            value >= SensorRanges.AIR_TEMP_EXCELLENT_MIN && value <= SensorRanges.AIR_TEMP_EXCELLENT_MAX -> SensorStatus("Excellent", Green)
            (value >= SensorRanges.AIR_TEMP_ACCEPTABLE_MIN && value < SensorRanges.AIR_TEMP_EXCELLENT_MIN) || 
            (value > SensorRanges.AIR_TEMP_EXCELLENT_MAX && value <= SensorRanges.AIR_TEMP_ACCEPTABLE_MAX) -> SensorStatus("Normal", Blue)
            (value >= SensorRanges.AIR_TEMP_CAUTION_MIN && value < SensorRanges.AIR_TEMP_ACCEPTABLE_MIN) || 
            (value > SensorRanges.AIR_TEMP_ACCEPTABLE_MAX && value <= SensorRanges.AIR_TEMP_CAUTION_MAX) -> SensorStatus("Caution", Orange)
            else -> SensorStatus("Critical", Red) // < AIR_TEMP_CAUTION_MIN or > AIR_TEMP_CAUTION_MAX
        }

        "Humidity" -> when {
            value >= SensorRanges.HUMIDITY_EXCELLENT_MIN && value <= SensorRanges.HUMIDITY_EXCELLENT_MAX -> SensorStatus("Excellent", Green)
            (value >= SensorRanges.HUMIDITY_ACCEPTABLE_MIN && value < SensorRanges.HUMIDITY_EXCELLENT_MIN) || 
            (value > SensorRanges.HUMIDITY_EXCELLENT_MAX && value <= SensorRanges.HUMIDITY_ACCEPTABLE_MAX) -> SensorStatus("Normal", Blue)
            (value >= SensorRanges.HUMIDITY_CAUTION_MIN && value < SensorRanges.HUMIDITY_ACCEPTABLE_MIN) || 
            (value > SensorRanges.HUMIDITY_ACCEPTABLE_MAX && value <= SensorRanges.HUMIDITY_CAUTION_MAX) -> SensorStatus("Caution", Orange)
            else -> SensorStatus("Critical", Red) // < HUMIDITY_CAUTION_MIN or > HUMIDITY_CAUTION_MAX
        }

        else -> SensorStatus("Unknown", Gray)
    }
}
@Composable
private fun ControlHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 12.dp)
    ) {
        Text(
            text = "Control Panel",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Manage each actuator below. Switch between Auto and Manual modes per device.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Changes update in real time.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}

@Composable
private fun ControlGrid(autoEnabled: Boolean, live: com.capstone.aquabell.data.model.LiveDataSnapshot?, onOverride: (RelayStates) -> Unit) {
    val outline = MaterialTheme.colorScheme.outline
    var lightOn by remember { mutableStateOf(false) }
    var fansOn by remember { mutableStateOf(false) }
    var pumpOn by remember { mutableStateOf(false) }
    var valveOpen by remember { mutableStateOf(false) }

    LaunchedEffect(live?.relayStates) {
        live?.relayStates?.let { rs ->
            lightOn = rs.light
            fansOn = rs.fan
            pumpOn = rs.waterPump
            valveOpen = rs.valve
        }
    }

    LaunchedEffect(autoEnabled) {
        if (autoEnabled) {
            lightOn = false
            fansOn = false
            pumpOn = false
            valveOpen = false
        }
    }

    // Pleasant, AquaBell-aligned accent colors per control
    val lightColor = Color(0xFFFFC107) // warm amber
    val fansColor = Color(0xFF42A5F5)  // sky blue
    val pumpColor = Color(0xFF26A69A)  // teal
    val valveColor = Color(0xFF66BB6A) // fresh green

    data class ControlSpec(
        val title: String,
        val iconRes: Int,
        val active: Boolean,
        val accent: Color,
        val toggle: () -> Unit
    )
    val tiles: List<ControlSpec> = listOf(
        ControlSpec("LIGHT", R.drawable.ic_light, lightOn, lightColor) { 
            lightOn = !lightOn; if (!autoEnabled) onOverride(RelayStates(fan = fansOn, light = lightOn, waterPump = pumpOn, valve = valveOpen))
        },
        ControlSpec("FANS", R.drawable.ic_fans, fansOn, fansColor) { 
            fansOn = !fansOn; if (!autoEnabled) onOverride(RelayStates(fan = fansOn, light = lightOn, waterPump = pumpOn, valve = valveOpen))
        },
        ControlSpec("PUMP", R.drawable.ic_pump, pumpOn, pumpColor) { 
            pumpOn = !pumpOn; if (!autoEnabled) onOverride(RelayStates(fan = fansOn, light = lightOn, waterPump = pumpOn, valve = valveOpen))
        },
        ControlSpec("VALVE", R.drawable.ic_valve, valveOpen, valveColor) { 
            valveOpen = !valveOpen; if (!autoEnabled) onOverride(RelayStates(fan = fansOn, light = lightOn, waterPump = pumpOn, valve = valveOpen))
        },
    )

    Column {
        // Auto mode notice
        if (autoEnabled) {
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Controls are disabled in Auto Mode",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        
        Column {
            // Row 1: First 2 controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                tiles.take(2).forEach { spec ->
                    val border = if (!autoEnabled && spec.active) spec.accent else outline
                    ControlTile(
                        title = spec.title,
                        iconRes = spec.iconRes,
                        active = spec.active,
                        enabled = !autoEnabled,
                        borderColor = border,
                        accent = spec.accent,
                        onClick = spec.toggle,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            Spacer(Modifier.height(12.dp))
            
            // Row 2: Last 2 controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                tiles.drop(2).forEach { spec ->
                    val border = if (!autoEnabled && spec.active) spec.accent else outline
                    ControlTile(
                        title = spec.title,
                        iconRes = spec.iconRes,
                        active = spec.active,
                        enabled = !autoEnabled,
                        borderColor = border,
                        accent = spec.accent,
                        onClick = spec.toggle,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ControlTile(
    modifier: Modifier = Modifier,
    title: String,
    iconRes: Int,
    active: Boolean,
    enabled: Boolean,
    borderColor: Color,
    accent: Color,
    onClick: () -> Unit
) {
    val isActive = active && enabled
    val enabledContainer = if (isActive) accent.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surface
    val container = if (enabled) enabledContainer else enabledContainer.copy(alpha = 0.6f)
    Box(
        modifier = modifier
            .height(120.dp)
    ) {
        OutlinedCard(
            modifier = Modifier
                .fillMaxSize()
                .clickable(enabled = enabled) { onClick() },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = container),
            border = BorderStroke(1.dp, borderColor)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    val tint = if (isActive) accent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(34.dp)
                    )
                }
                val labelColor = if (isActive) accent else MaterialTheme.colorScheme.onSurface
                Text(
                    title,
                    style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 1.5.sp),
                    color = labelColor
                )
            }
        }
    }
}

@Composable
private fun PerActuatorControlGrid(
    command: com.capstone.aquabell.data.model.CommandControl,
    onSetActuatorMode: (actuator: String, mode: ControlMode) -> Unit,
    onSetActuatorValue: (actuator: String, value: Boolean) -> Unit,
    live: com.capstone.aquabell.data.model.LiveDataSnapshot? = null,
) {
    val outline = MaterialTheme.colorScheme.outline


    // Accent colors
    val lightColor = Color(0xFFFFC107)
    val fansColor = Color(0xFF42A5F5)
    val pumpColor = Color(0xFF26A69A)
    val valveColor = Color(0xFF66BB6A)
    val coolerColor = Color(0xFF29B6F6)
    val heaterColor = Color(0xFFFF7043)

    data class Tile(
        val key: String,
        val title: String,
        val iconRes: Int,
        val active: Boolean,
        val mode: ControlMode,
        val accent: Color,
        val onToggleValue: () -> Unit,
        val onToggleMode: () -> Unit,
    )

    val tiles: List<Tile> = listOf(
        Tile(
            key = "light",
            title = "LIGHT",
            iconRes = R.drawable.ic_light,
            active = command.light.value,
            mode = command.light.mode,
            accent = lightColor,
            onToggleValue = { onSetActuatorValue("light", !command.light.value) },
            onToggleMode = {
                val next = if (command.light.mode == ControlMode.AUTO) ControlMode.MANUAL else ControlMode.AUTO
                onSetActuatorMode("light", next)
            }
        ),
        Tile(
            key = "fan",
            title = "FANS",
            iconRes = R.drawable.ic_fans,
            active = command.fan.value,
            mode = command.fan.mode,
            accent = fansColor,
            onToggleValue = { onSetActuatorValue("fan", !command.fan.value) },
            onToggleMode = {
                val next = if (command.fan.mode == ControlMode.AUTO) ControlMode.MANUAL else ControlMode.AUTO
                onSetActuatorMode("fan", next)
            }
        ),
        Tile(
            key = "pump",
            title = "PUMP",
            iconRes = R.drawable.ic_pump,
            active = command.pump.value,
            mode = command.pump.mode,
            accent = pumpColor,
            onToggleValue = { onSetActuatorValue("pump", !command.pump.value) },
            onToggleMode = {
                val next = if (command.pump.mode == ControlMode.AUTO) ControlMode.MANUAL else ControlMode.AUTO
                onSetActuatorMode("pump", next)
            }
        ),
        Tile(
            key = "valve",
            title = "VALVE",
            iconRes = R.drawable.ic_valve,
            active = command.valve.value,
            mode = command.valve.mode,
            accent = valveColor,
            onToggleValue = { onSetActuatorValue("valve", !command.valve.value) },
            onToggleMode = {
                val next = if (command.valve.mode == ControlMode.AUTO) ControlMode.MANUAL else ControlMode.AUTO
                onSetActuatorMode("valve", next)
            }
        ),
        Tile(
            key = "cooler",
            title = "COOLER",
            iconRes = R.drawable.ic_water_cooler,
            active = command.cooler.value,
            mode = command.cooler.mode,
            accent = coolerColor,
            onToggleValue = { onSetActuatorValue("cooler", !command.cooler.value) },
            onToggleMode = {
                val next = if (command.cooler.mode == ControlMode.AUTO) ControlMode.MANUAL else ControlMode.AUTO
                onSetActuatorMode("cooler", next)
            }
        ),
        Tile(
            key = "heater",
            title = "HEATER",
            iconRes = R.drawable.ic_water_heater,
            active = command.heater.value,
            mode = command.heater.mode,
            accent = heaterColor,
            onToggleValue = { onSetActuatorValue("heater", !command.heater.value) },
            onToggleMode = {
                val next = if (command.heater.mode == ControlMode.AUTO) ControlMode.MANUAL else ControlMode.AUTO
                onSetActuatorMode("heater", next)
            }
        ),
    )

    @Composable
    fun ActuatorTile(tile: Tile, modifier: Modifier = Modifier) {
        var isAuto by remember(tile.key) { mutableStateOf(tile.mode == ControlMode.AUTO) }
        LaunchedEffect(tile.mode) { isAuto = tile.mode == ControlMode.AUTO }

        val enabled = !isAuto
        val isActive = tile.active && enabled
        val border = if (isActive) tile.accent else MaterialTheme.colorScheme.outline
        val enabledContainer = if (isActive) tile.accent.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surface
        val container = if (enabled) enabledContainer else enabledContainer.copy(alpha = 0.6f)

        OutlinedCard(
            modifier = modifier.height(148.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = container),
            border = BorderStroke(1.dp, border)
        ) {
            // ✅ Center all elements vertically, no space-between
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Title row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        tile.title,
                        style = MaterialTheme.typography.labelLarge.copy(
                            letterSpacing = 1.5.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = if (isActive) tile.accent else MaterialTheme.colorScheme.onSurface
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "AUTO",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isAuto) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                        )
                        Spacer(Modifier.width(6.dp))
                        Switch(
                            checked = isAuto,
                            onCheckedChange = { checked ->
                                isAuto = checked
                                val next = if (checked) ControlMode.AUTO else ControlMode.MANUAL
                                tile.onToggleMode()
                            },
                            modifier = Modifier.scale(0.9f),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary,
                                uncheckedThumbColor = MaterialTheme.colorScheme.onSurface,
                                uncheckedTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            )
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                // ✅ Centered Icon + Status
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (isActive) tile.accent.copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            )
                            .clickable(enabled = enabled) { tile.onToggleValue() },
                        contentAlignment = Alignment.Center
                    ) {
                        val tint = if (isActive) tile.accent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        Icon(
                            painter = painterResource(id = tile.iconRes),
                            contentDescription = tile.title,
                            tint = tint,
                            modifier = Modifier.size(42.dp)
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    Text(
                        text = if (tile.active) "On" else "Off",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                        color = if (isActive) tile.accent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }

                Spacer(Modifier.height(8.dp))

                // Helper text
                val helper = if (isAuto)
                    "Automatic control is active."
                else
                    "Manual control is active."
                Text(
                    text = helper,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    Column {
        // Keep a responsive 2-column grid; we now have 6 tiles (3 rows)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            tiles.take(2).forEach { tile ->
                ActuatorTile(tile, modifier = Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            tiles.drop(2).take(2).forEach { tile ->
                ActuatorTile(tile, modifier = Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            tiles.drop(4).forEach { tile ->
                ActuatorTile(tile, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun BottomNavBar(selectedIndex: Int, onSelected: (Int) -> Unit) {
    val containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
    NavigationBar(
        containerColor = containerColor,
        tonalElevation = 8.dp
    ) {
        val items: List<Pair<Int, String>> = listOf(
            R.drawable.ic_analytics to "Analytics",
            R.drawable.ic_home to "Home",
            R.drawable.ic_bell to "Alerts",
            R.drawable.ic_documentation to "Guide"
        )
        items.forEachIndexed { index, pair ->
            val selected = selectedIndex == index
            val activeColor = MaterialTheme.colorScheme.primary
            val inactiveColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            val iconTint = if (selected) activeColor else inactiveColor
            NavigationBarItem(
                selected = selected,
                onClick = { onSelected(index) },
                icon = {
                    if (pair.second == "Docs") {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = pair.second,
                            modifier = Modifier
                                .size(if (selected) 26.dp else 24.dp)
                                .scale(if (selected) 1.06f else 1f),
                            tint = iconTint
                        )
                    } else if (pair.second == "Alerts") {
                        val vm = androidx.lifecycle.viewmodel.compose.viewModel<com.capstone.aquabell.ui.viewmodel.AlertsViewModel>()
                        val count = vm.unreadCount.collectAsState(initial = 0)
                        BadgedBox(badge = {
                            if (count.value > 0) {
                                Badge { Text(if (count.value > 99) "99+" else count.value.toString()) }
                            }
                        }) {
                            Icon(
                                painter = painterResource(id = pair.first),
                                contentDescription = pair.second,
                                modifier = Modifier.size(if (selected) 26.dp else 24.dp),
                                tint = iconTint
                            )
                        }
                    } else {
                        Icon(
                            painter = painterResource(id = pair.first),
                            contentDescription = pair.second,
                            modifier = Modifier
                                .size(if (selected) 26.dp else 24.dp)
                                .scale(if (selected) 1.06f else 1f),
                            tint = iconTint
                        )
                    }
                },
                label = {
                    Text(
                        pair.second,
                        color = if (selected) activeColor else inactiveColor
                    )
                }
            )
        }
    }
}