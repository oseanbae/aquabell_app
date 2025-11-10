package com.capstone.aquabell.data.model

import androidx.compose.ui.graphics.Color
import com.google.firebase.Timestamp

// Daily analytics data model
data class DailyAnalytics(
    val date: String, // Format: "2025-10-18"
    val airTemp: Double = 0.0,
    val airHumidity: Double = 0.0,
    val waterTemp: Double = 0.0,
    val pH: Double = 0.0,
    val dissolvedOxygen: Double = 0.0,
    val turbidityNTU: Double = 0.0,
    val timestamp: Timestamp = Timestamp.now(),
    val isLive: Boolean = false // true for current day, false for historical
)

// UI state for analytics screen
data class AnalyticsUiState(
    val dailyAnalytics: List<DailyAnalytics> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val sortOrder: SortOrder = SortOrder.NEWEST_FIRST
)

enum class SortOrder {
    NEWEST_FIRST,
    OLDEST_FIRST
}

// Sensor metric data for display
data class SensorMetric(
    val label: String,
    val value: Double,
    val unit: String,
    val status: SensorStatus,
    val iconRes: Int
)

data class SensorStatus(
    val label: String,
    val color: Color,
    val progress: Float // 0.0 to 1.0 for progress bar
)