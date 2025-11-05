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

// Sensor range definitions for status calculation
object SensorRanges {
    //pH
    const val PH_EXCELLENT_MIN = 6.5
    const val PH_EXCELLENT_MAX = 7.5
    const val PH_ACCEPTABLE_MIN = 6.3
    const val PH_ACCEPTABLE_MAX = 7.8
    const val PH_CAUTION_MIN = 6.0
    const val PH_CAUTION_MAX = 8.5   // was 8.2, corrected to 8.5 per final table

    //Dissolved Oxygen (mg/L)
    const val DO_EXCELLENT_MIN = 6.0   // you put 6.5; adjust to real tilapia baseline
    const val DO_ACCEPTABLE_MIN = 5.0  // was 5.5, align to table
    const val DO_CAUTION_MIN = 4.0

    //Water Temperature (°C)
    const val WATER_TEMP_EXCELLENT_MIN = 26.0   // aligned to 26–28 best practice
    const val WATER_TEMP_EXCELLENT_MAX = 28.0
    const val WATER_TEMP_ACCEPTABLE_MIN = 24.0
    const val WATER_TEMP_ACCEPTABLE_MAX = 29.0
    const val WATER_TEMP_CAUTION_MIN = 22.0
    const val WATER_TEMP_CAUTION_MAX = 30.0
    // Critical handled in logic, not threshold constants here

    // Air Temperature (°C)
    const val AIR_TEMP_EXCELLENT_MIN = 22.0
    const val AIR_TEMP_EXCELLENT_MAX = 30.0
    const val AIR_TEMP_ACCEPTABLE_MIN = 20.0
    const val AIR_TEMP_ACCEPTABLE_MAX = 32.0
    const val AIR_TEMP_CAUTION_MIN = 18.0
    const val AIR_TEMP_CAUTION_MAX = 35.0

    //  Humidity (%)
    const val HUMIDITY_EXCELLENT_MIN = 60.0
    const val HUMIDITY_EXCELLENT_MAX = 75.0   // adjusted from 70% to match real grow-bed sweet spot
    const val HUMIDITY_ACCEPTABLE_MIN = 50.0
    const val HUMIDITY_ACCEPTABLE_MAX = 85.0
    const val HUMIDITY_CAUTION_MIN = 40.0
    const val HUMIDITY_CAUTION_MAX = 90.0

    // Turbidity (NTU)
    const val TURBIDITY_EXCELLENT_MAX = 50.0
    const val TURBIDITY_ACCEPTABLE_MAX = 120.0
    const val TURBIDITY_CAUTION_MAX = 250.0
}