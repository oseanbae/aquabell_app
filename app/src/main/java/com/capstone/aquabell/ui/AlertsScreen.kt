package com.capstone.aquabell.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color

data class AlertItem(
    val title: String,
    val description: String,
    val severity: Severity,
    val time: String
)

enum class Severity { INFO, WARNING, CRITICAL }

@Composable
fun AlertsScreen(modifier: Modifier = Modifier) {
    val outline = MaterialTheme.colorScheme.outline

    val alerts = listOf(
        AlertItem(
            title = "Low Dissolved Oxygen",
            description = "DO dropped below 6.0 mg/L. Auto mode increased aeration.",
            severity = Severity.CRITICAL,
            time = "10 min ago"
        ),
        AlertItem(
            title = "High Turbidity",
            description = "Turbidity reached 96 NTU. Check filtration and water clarity.",
            severity = Severity.WARNING,
            time = "27 min ago"
        ),
        AlertItem(
            title = "Stable pH",
            description = "pH holding at 7.1. No action required.",
            severity = Severity.INFO,
            time = "1 hr ago"
        ),
        AlertItem(
            title = "Water Temp Normalized",
            description = "Water temperature back to 28.1°C.",
            severity = Severity.INFO,
            time = "2 hr ago"
        )
    )

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
            items(alerts) { alert ->
                AlertCard(alert = alert, outline = outline)
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun AlertCard(alert: AlertItem, outline: Color) {
    val (chipColor, label) = when (alert.severity) {
        Severity.INFO -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) to "INFO"
        Severity.WARNING -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.20f) to "WARNING"
        Severity.CRITICAL -> MaterialTheme.colorScheme.error.copy(alpha = 0.18f) to "CRITICAL"
    }

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, outline)
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
                Text(alert.title, style = MaterialTheme.typography.titleSmall)
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
                alert.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
            Text(
                alert.time,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}


