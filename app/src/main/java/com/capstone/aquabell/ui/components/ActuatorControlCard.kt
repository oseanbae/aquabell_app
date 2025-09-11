package com.capstone.aquabell.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.capstone.aquabell.data.model.ActuatorState

@Composable
fun ActuatorControlCard(
    title: String,
    state: ActuatorState,
    onToggleAutoMode: () -> Unit,
    onToggleValue: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Auto/Manual Toggle
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (state.isAuto) "AUTO" else "MANUAL",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (state.isAuto) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                    )
                    Switch(
                        checked = state.isAuto,
                        onCheckedChange = { onToggleAutoMode() }
                    )
                }
                
                // On/Off Toggle (only enabled in manual mode)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (state.value) "ON" else "OFF",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (state.value) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                    Switch(
                        checked = state.value,
                        onCheckedChange = { onToggleValue() },
                        enabled = !state.isAuto // Only enabled in manual mode
                    )
                }
            }
            
            // Status indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Mode: ${if (state.isAuto) "Automatic" else "Manual"}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Status: ${if (state.value) "Active" else "Inactive"}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}


