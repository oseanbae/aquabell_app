package com.capstone.aquabell.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.material3.Icon
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.capstone.aquabell.R
import com.capstone.aquabell.ui.theme.AquabellTheme
import com.capstone.aquabell.data.FirebaseRepository
import com.capstone.aquabell.data.model.ControlMode
import com.capstone.aquabell.data.model.RelayStates
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.compose.viewModel
import com.capstone.aquabell.ui.viewmodel.HomeViewModel

@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    val vm: HomeViewModel = viewModel()
    var selectedNavIndex by remember { mutableIntStateOf(1) } // center is Home
    val live by vm.live.collectAsState()
    val command by vm.command.collectAsState()
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
                    connectionState = connectionState,
                    isRefreshing = isRefreshing,
                    onRefresh = { vm.refresh() },
                    onOverride = { overrides -> vm.setRelayOverrides(overrides) },
                    onSetActuatorMode = { actuator, mode -> vm.setActuatorMode(actuator, mode) },
                    onSetActuatorValue = { actuator, value -> vm.setActuatorValue(actuator, value) }
                )
                else -> AlertsScreen(modifier = Modifier.padding(inner))
            }
        }
    }
}

@Composable
private fun HomeContent(
    modifier: Modifier = Modifier,
    live: com.capstone.aquabell.data.model.LiveDataSnapshot?,
    command: com.capstone.aquabell.data.model.CommandControl,
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
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ConnectionStatusBanner(connectionState, isRefreshing, onRefresh)
        SectionHeader(title = "Dashboard")
        DashboardGrid(live)
        WaterLevelModule(live)
        ControlHeader()
        PerActuatorControlGrid(
            command = command,
            onSetActuatorMode = onSetActuatorMode,
            onSetActuatorValue = onSetActuatorValue
        )
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

// AnalyticsScreen moved to its own file for clarity

// AlertsScreen moved to its own file

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
    data class MetricData(val title: String, val value: String, val iconRes: Int)
    
    val metrics: List<MetricData> = listOf(
        MetricData("Air Temp.", if (live!=null) String.format("%.1f°C", live.airTemp) else "-", R.drawable.ic_thermometer),
        MetricData("Air Humidity RH", if (live!=null) String.format("%.0f%%", live.airHumidity) else "-", R.drawable.ic_humidity),
        MetricData("Water Temp.", if (live!=null) String.format("%.1f°C", live.waterTemp) else "-", R.drawable.ic_water_temp),
        MetricData("pH Level", if (live!=null) String.format("%.1f pH", live.pH) else "-", R.drawable.ic_ph),
        MetricData("Dissolved Oxygen", if (live!=null) String.format("%.1f mg/L", live.dissolvedOxygen) else "-", R.drawable.ic_oxygen),
        MetricData("Turbidity Level", if (live!=null) String.format("%.0f NTU", live.turbidityNTU) else "-", R.drawable.ic_turbidity),
    )
    
    Column {
        // Row 1: First 2 metrics
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            metrics.take(2).forEach { metric ->
                MetricCard(
                    title = metric.title,
                    value = metric.value,
                    status = "Excellent",
                    iconRes = metric.iconRes,
                    borderColor = outline,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        Spacer(Modifier.height(12.dp))
        
        // Row 2: Next 2 metrics
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            metrics.drop(2).take(2).forEach { metric ->
                MetricCard(
                    title = metric.title,
                    value = metric.value,
                    status = "Excellent",
                    iconRes = metric.iconRes,
                    borderColor = outline,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        Spacer(Modifier.height(12.dp))
        
        // Row 3: Last 2 metrics
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            metrics.drop(4).forEach { metric ->
                MetricCard(
                    title = metric.title,
                    value = metric.value,
                    status = "Excellent",
                    iconRes = metric.iconRes,
                    borderColor = outline,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun WaterLevelModule(live: com.capstone.aquabell.data.model.LiveDataSnapshot?) {
    val outline = MaterialTheme.colorScheme.outline
    val isLowWater = live?.floatTriggered == true
    
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isLowWater) {
                Color(0xFFFF5722).copy(alpha = 0.05f) // Light red background for low water
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = 2.dp,
            color = if (isLowWater) Color(0xFFFF5722) else outline
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon with background
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
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
                    painter = painterResource(id = R.drawable.ic_valve),
                    contentDescription = "Water Level",
                    tint = if (isLowWater) Color(0xFFFF5722) else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
            
            // Content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Water Level",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (isLowWater) "LOW WATER LEVEL DETECTED" else "Water level is normal",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isLowWater) Color(0xFFFF5722) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = if (isLowWater) "⚠️ Check water supply immediately" else "✓ System operating normally",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isLowWater) Color(0xFFFF5722) else MaterialTheme.colorScheme.tertiary
                )
            }
            
            // Status indicator
            Box(
                modifier = Modifier
                    .size(24.dp)
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
) {
    var showTooltip by remember { mutableStateOf(false) }
    
    // Auto-hide tooltip after 3 seconds
    LaunchedEffect(showTooltip) {
        if (showTooltip) {
            kotlinx.coroutines.delay(3000)
            showTooltip = false
        }
    }
    
    Box(
        modifier = modifier
            .height(100.dp) // Increased height to accommodate longer text
    ) {
        OutlinedCard(
            modifier = Modifier.fillMaxSize(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, borderColor)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (title == "Water Level" && status == "Low") {
                                Color(0xFFFF5722).copy(alpha = 0.15f) // Red-orange background for low water level
                            } else {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = value, 
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = if (title == "Water Level" && status == "Low") Color(0xFFFF5722) else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = title, 
                        style = MaterialTheme.typography.bodySmall, 
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = status, 
                        style = MaterialTheme.typography.labelSmall, 
                        color = if (status == "Low") Color(0xFFFF5722) else MaterialTheme.colorScheme.tertiary
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


            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp, end = 8.dp)
                    .width(240.dp)
            ) {
                // Clean tooltip with elevation
                OutlinedCard(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Text(
                        text = tooltipText,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
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
                        modifier = Modifier.size(28.dp)
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
) {
    val outline = MaterialTheme.colorScheme.outline

    // Accent colors
    val lightColor = Color(0xFFFFC107)
    val fansColor = Color(0xFF42A5F5)
    val pumpColor = Color(0xFF26A69A)
    val valveColor = Color(0xFF66BB6A)

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
    )

    @Composable
    fun ActuatorTile(tile: Tile, modifier: Modifier = Modifier) {
        // Optimistic local UI state that mirrors Firestore but updates immediately on user action
        var localMode by remember(tile.key) { mutableStateOf(tile.mode) }
        var localActive by remember(tile.key) { mutableStateOf(tile.active) }

        // Sync local state whenever Firestore command changes for this tile
        LaunchedEffect(tile.mode, tile.active) {
            localMode = tile.mode
            localActive = tile.active
        }

        val enabled = localMode == ControlMode.MANUAL
        val isActive = localActive && enabled
        val border = if (isActive) tile.accent else outline
        val enabledContainer = if (isActive) tile.accent.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surface
        val container = if (enabled) enabledContainer else enabledContainer.copy(alpha = 0.6f)

        OutlinedCard(
            modifier = modifier
                .height(148.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = container),
            border = BorderStroke(1.dp, border)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        tile.title,
                        style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 1.5.sp),
                        color = if (isActive) tile.accent else MaterialTheme.colorScheme.onSurface
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (localMode == ControlMode.AUTO) "AUTO" else "MANUAL",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (localMode == ControlMode.AUTO) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                        )
                        Spacer(Modifier.width(6.dp))
                        Switch(
                            checked = localMode == ControlMode.MANUAL,
                            onCheckedChange = { _ ->
                                // Toggle locally for instant feedback
                                localMode = if (localMode == ControlMode.AUTO) ControlMode.MANUAL else ControlMode.AUTO
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

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (isActive) tile.accent.copy(alpha = 0.12f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                            .clickable(enabled = enabled) {
                                // Update UI instantly, then write to Firestore
                                localActive = !localActive
                                tile.onToggleValue()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        val tint = if (isActive) tile.accent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        Icon(
                            painter = painterResource(id = tile.iconRes),
                            contentDescription = null,
                            tint = tint,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    
                    Spacer(Modifier.height(8.dp))
                    
                    Text(
                        text = if (localActive) "On" else "Off",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                        color = if (isActive) tile.accent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }

                // Helper text for clarity
                val helper = if (localMode == ControlMode.AUTO) "Automatic control is active. Switch to Manual to override." else "Manual control is active."
                Text(
                    text = helper,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                )
            }
        }
    }

    Column {
        // Grid: 2 rows x 2 columns
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
            tiles.drop(2).forEach { tile ->
                ActuatorTile(tile, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun BottomNavBar(selectedIndex: Int, onSelected: (Int) -> Unit) {
    NavigationBar {
        val items: List<Pair<Int, String>> = listOf(
            R.drawable.ic_analytics to "Analytics",
            R.drawable.ic_home to "Home",
            R.drawable.ic_bell to "Alerts"
        )
        items.forEachIndexed { index, pair ->
            NavigationBarItem(
                selected = selectedIndex == index,
                onClick = { onSelected(index) },
                icon = { 
                    Icon(
                        painter = painterResource(id = pair.first),
                        contentDescription = pair.second,
                        modifier = Modifier.size(24.dp),
                        tint = Color.Unspecified
                    )
                },
                label = { Text(pair.second) }
            )
        }
    }
}


