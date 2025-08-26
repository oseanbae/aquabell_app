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
import androidx.compose.foundation.Canvas
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
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

@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    val repo = remember { FirebaseRepository() }
    var autoModeEnabled by remember { mutableStateOf(true) }
    var selectedNavIndex by remember { mutableIntStateOf(1) } // center is Home
    var live by remember { mutableStateOf<com.capstone.aquabell.data.model.LiveDataSnapshot?>(null) }
    var offline by remember { mutableStateOf<com.capstone.aquabell.data.model.LiveDataSnapshot?>(null) }
    var connectionState by remember { mutableStateOf(FirebaseRepository.ConnectionState.CONNECTING) }
    var isRefreshing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // Warm cached snapshot for offline state
        offline = repo.getCachedLiveData()
        repo.connectionState.collectLatest { connectionState = it }
        repo.liveData().collectLatest { live = it }
    }

    fun onRefresh() {
        isRefreshing = true
        CoroutineScope(Dispatchers.IO).launch {
            try {
                repo.forceRefresh()
            } finally {
                isRefreshing = false
            }
        }
    }

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
                    autoModeEnabled = autoModeEnabled,
                    onToggleAuto = { enabled ->
                        autoModeEnabled = enabled
                        val mode = if (enabled) ControlMode.AUTO else ControlMode.MANUAL
                        // fire-and-forget; repository handles failure logging
                        CoroutineScope(Dispatchers.IO).launch { repo.setControlMode(mode) }
                    },
                    live = live ?: offline,
                    connectionState = connectionState,
                    isRefreshing = isRefreshing,
                    onRefresh = { onRefresh() },
                    onOverride = { overrides ->
                        CoroutineScope(Dispatchers.IO).launch { repo.setRelayOverrides(overrides) }
                    }
                )
                else -> AlertsScreen(modifier = Modifier.padding(inner))
            }
        }
    }
}

@Composable
private fun HomeContent(
    modifier: Modifier = Modifier,
    autoModeEnabled: Boolean,
    onToggleAuto: (Boolean) -> Unit,
    live: com.capstone.aquabell.data.model.LiveDataSnapshot?,
    connectionState: FirebaseRepository.ConnectionState,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onOverride: (RelayStates) -> Unit,
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
        ControlHeader(autoModeEnabled = autoModeEnabled, onToggleAuto = onToggleAuto)
        ControlGrid(autoEnabled = autoModeEnabled, live = live, onOverride = onOverride)
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
private fun MetricCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    status: String,
    iconRes: Int,
    borderColor: Color,
) {
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
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
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
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
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
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }
    }
}

@Composable
private fun ControlHeader(autoModeEnabled: Boolean, onToggleAuto: (Boolean) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Control",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (autoModeEnabled) "Auto Mode" else "Manual Mode",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (autoModeEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                )
                Spacer(Modifier.width(8.dp))
                Switch(
                    checked = autoModeEnabled,
                    onCheckedChange = onToggleAuto,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                    )
                )
            }
        }
        
        // Mode description
        Text(
            text = if (autoModeEnabled) {
                "System automatically controls all devices based on sensor readings"
            } else {
                "You can manually control each device using the buttons below"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.padding(top = 4.dp)
        )
        
        // Mode status indicator
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(
                        if (autoModeEnabled) MaterialTheme.colorScheme.primary 
                        else MaterialTheme.colorScheme.secondary
                    )
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (autoModeEnabled) "Auto Mode Active" else "Manual Mode Active",
                style = MaterialTheme.typography.labelMedium,
                color = if (autoModeEnabled) MaterialTheme.colorScheme.primary 
                        else MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Medium
            )
        }
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


