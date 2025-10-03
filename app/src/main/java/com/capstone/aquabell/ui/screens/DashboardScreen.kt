package com.capstone.aquabell.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.capstone.aquabell.ui.components.ActuatorControlCard
import com.capstone.aquabell.ui.viewmodel.AquabellViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: AquabellViewModel = AquabellViewModel()
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val liveData by viewModel.liveData.collectAsState()
    val sensorLogs by viewModel.sensorLogs.collectAsState()
    
    // Actuator states
    val fanState by viewModel.fanState.collectAsState()
    val lightState by viewModel.lightState.collectAsState()
    val pumpState by viewModel.pumpState.collectAsState()
    val valveState by viewModel.valveState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Aquabell Dashboard") },
                actions = {
                    // Connection status indicator
                    Text(
                        text = when (connectionState) {
                            com.capstone.aquabell.data.FirebaseRepository.ConnectionState.CONNECTED -> "🟢 Connected"
                            com.capstone.aquabell.data.FirebaseRepository.ConnectionState.CONNECTING -> "🟡 Connecting"
                            com.capstone.aquabell.data.FirebaseRepository.ConnectionState.NOT_CONNECTED -> "🔴 Disconnected"
                        },
                        modifier = Modifier.padding(16.dp)
                    )
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Live Sensor Data Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Live Sensor Data",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Water Temp: ${String.format("%.1f", liveData.waterTemp)}°C")
                                Text("pH: ${String.format("%.2f", liveData.pH)}")
                                Text("DO: ${String.format("%.1f", liveData.dissolvedOxygen)} mg/L")
                            }
                            Column {
                                Text("Air Temp: ${String.format("%.1f", liveData.airTemp)}°C")
                                Text("Humidity: ${String.format("%.1f", liveData.airHumidity)}%")
                                Text("Turbidity: ${String.format("%.1f", liveData.turbidityNTU)} NTU")
                            }
                        }
                    }
                }
            }

            // Actuator Controls Section
            item {
                Text(
                    text = "Actuator Controls",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                ActuatorControlCard(
                    title = "Fan",
                    state = fanState,
                    onToggleAutoMode = { isChecked -> viewModel.updateActuatorAuto("fan", isChecked) },
                    onToggleValue = { isChecked -> viewModel.updateActuatorValue("fan", isChecked) }
                )
            }

            item {
                ActuatorControlCard(
                    title = "Light",
                    state = lightState,
                    onToggleAutoMode = { isChecked -> viewModel.updateActuatorAuto("light", isChecked) },
                    onToggleValue = { isChecked -> viewModel.updateActuatorValue("light", isChecked) }
                )
            }

            item {
                ActuatorControlCard(
                    title = "Pump",
                    state = pumpState,
                    onToggleAutoMode = { isChecked -> viewModel.updateActuatorAuto("pump", isChecked) },
                    onToggleValue = { isChecked -> viewModel.updateActuatorValue("pump", isChecked) }
                )
            }

            item {
                ActuatorControlCard(
                    title = "Valve",
                    state = valveState,
                    onToggleAutoMode = { isChecked -> viewModel.updateActuatorAuto("valve", isChecked) },
                    onToggleValue = { isChecked -> viewModel.updateActuatorValue("valve", isChecked) }
                )
            }

            // Sensor Logs Section
            item {
                Text(
                    text = "Recent Sensor Logs (${sensorLogs.size} entries)",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            items(sensorLogs.takeLast(10)) { log ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "Time: ${log.timestamp.toDate()}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("WT: ${String.format("%.1f", log.waterTemp)}°C")
                            Text("pH: ${String.format("%.2f", log.pH)}")
                            Text("DO: ${String.format("%.1f", log.dissolvedOxygen)}")
                        }
                    }
                }
            }
        }
    }
}


