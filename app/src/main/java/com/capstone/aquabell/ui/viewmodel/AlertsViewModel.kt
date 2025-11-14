package com.capstone.aquabell.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.capstone.aquabell.data.FirebaseRepository
import com.capstone.aquabell.data.model.AlertEntry
import com.capstone.aquabell.ui.utils.AlertEvaluator
import com.capstone.aquabell.ui.utils.SensorStatus
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.capstone.aquabell.data.model.SensorRanges

class AlertsViewModel(
    private val repository: FirebaseRepository = FirebaseRepository(),
) : ViewModel() {

    private val _alerts: MutableStateFlow<List<AlertEntry>> = MutableStateFlow(emptyList())
    val alerts: StateFlow<List<AlertEntry>> = _alerts

    private val _unreadCount: MutableStateFlow<Int> = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount

    private val existingIds: MutableSet<String> = mutableSetOf()
    init {
        // === Listen to stored alerts (global) ===
        viewModelScope.launch {
            repository.listenToAlerts().collectLatest { list ->
                _alerts.value = list
                _unreadCount.value = list.count { !it.acknowledged }
                existingIds.clear()
                existingIds.addAll(list.map { it.alertId })
            }
        }

        // === Listen to live data and evaluate new alerts ===
        viewModelScope.launch {
            repository.liveData().collectLatest { live ->
                val now = System.currentTimeMillis()

                // Build all sensor readings
                val readings = listOf(
                    AlertEvaluator.SensorReading(
                        sensor = "pH",
                        value = live.pH,
                        status = statusFor("pH", live.pH),
                        timestamp = now
                    ),
                    AlertEvaluator.SensorReading(
                        sensor = "temperature",
                        value = live.waterTemp,
                        status = statusFor("temperature", live.waterTemp),
                        timestamp = now
                    ),
                    AlertEvaluator.SensorReading(
                        sensor = "dissolved_oxygen",
                        value = live.dissolvedOxygen,
                        status = statusFor("dissolved_oxygen", live.dissolvedOxygen),
                        timestamp = now
                    ),
                    AlertEvaluator.SensorReading(
                        sensor = "turbidity",
                        value = live.turbidityNTU,
                        status = statusFor("turbidity", live.turbidityNTU),
                        timestamp = now
                    ),
                    AlertEvaluator.SensorReading(
                        sensor = "air_temp",
                        value = live.airTemp,
                        status = statusFor("air_temp", live.airTemp),
                        timestamp = now
                    ),
                    AlertEvaluator.SensorReading(
                        sensor = "humidity",
                        value = live.airHumidity,
                        status = statusFor("humidity", live.airHumidity),
                        timestamp = now
                    ),
                    AlertEvaluator.SensorReading(
                        sensor = "float_switch",
                        value = if (live.floatTriggered) 1.0 else 0.0,
                        status = if (live.floatTriggered) SensorStatus.Critical else SensorStatus.Good,
                        timestamp = now
                    )

                )

                // Evaluate and push only new alerts
                val context = FirebaseApp.getInstance().applicationContext
                val newAlerts = AlertEvaluator.evaluateAlerts(context, readings)
                    .filter { it.alertId !in existingIds }

                if (newAlerts.isNotEmpty()) {
                    newAlerts.forEach { alert ->
                        viewModelScope.launch {
                            try {
                                repository.pushAlert(alert)
                                // Show local notification for the new alert
                                try {
                                    com.capstone.aquabell.ui.utils.NotificationUtils.showAlertNotification(context, alert)
                                } catch (_: Throwable) { }
                            } catch (_: Throwable) { }
                        }
                    }
                }
            }
        }
    }


    fun acknowledgeAlert(id: String) {
        viewModelScope.launch {
            try {
                repository.acknowledgeAlert(id)

                // Reset cooldown for this sensor (e.g. "ph_critical_173..."
                val sensor = id.substringBefore("_")
                val context = FirebaseApp.getInstance().applicationContext
                AlertEvaluator.resetCooldown(context, sensor)

            } catch (_: Throwable) { }
        }
    }

    fun clearResolvedAlerts() {
        viewModelScope.launch {
            try {
                // Get all acknowledged alerts before deletion
                val acknowledgedAlerts = _alerts.value.filter { it.acknowledged }

                repository.deleteResolvedAlerts()

                // Reset cooldowns for those sensors
                val context = FirebaseApp.getInstance().applicationContext
                acknowledgedAlerts.forEach { alert ->
                    val sensor = alert.alertId.substringBefore("_")
                    AlertEvaluator.resetCooldown(context, sensor)
                }

            } catch (_: Throwable) { }
        }
    }

    private fun statusFor(sensor: String, value: Double): SensorStatus {
        return when (sensor.lowercase()) {

            // === pH ===
            "ph" -> when {
                value < SensorRanges.PH_CAUTION_MIN || value > SensorRanges.PH_CAUTION_MAX -> SensorStatus.Critical
                value < SensorRanges.PH_ACCEPTABLE_MIN || value > SensorRanges.PH_ACCEPTABLE_MAX -> SensorStatus.Caution
                value in SensorRanges.PH_EXCELLENT_MIN..SensorRanges.PH_EXCELLENT_MAX -> SensorStatus.Excellent
                else -> SensorStatus.Good
            }

            // === Water Temperature ===
            "temperature", "water_temp", "water temperature" -> when {
                value < SensorRanges.WATER_TEMP_CAUTION_MIN || value > SensorRanges.WATER_TEMP_CAUTION_MAX -> SensorStatus.Critical
                value < SensorRanges.WATER_TEMP_ACCEPTABLE_MIN || value > SensorRanges.WATER_TEMP_ACCEPTABLE_MAX -> SensorStatus.Caution
                value in SensorRanges.WATER_TEMP_EXCELLENT_MIN..SensorRanges.WATER_TEMP_EXCELLENT_MAX -> SensorStatus.Excellent
                else -> SensorStatus.Good
            }

            // === Dissolved Oxygen ===
            "dissolved_oxygen", "do", "oxygen" -> when {
                value < SensorRanges.DO_CAUTION_MIN -> SensorStatus.Critical
                value < SensorRanges.DO_ACCEPTABLE_MIN -> SensorStatus.Caution
                value >= SensorRanges.DO_EXCELLENT_MIN -> SensorStatus.Excellent
                else -> SensorStatus.Good
            }

            // === Turbidity ===
            "turbidity" -> when {
                value >= SensorRanges.TURBIDITY_CAUTION_MAX -> SensorStatus.Critical
                value >= SensorRanges.TURBIDITY_ACCEPTABLE_MAX -> SensorStatus.Caution
                value <= SensorRanges.TURBIDITY_EXCELLENT_MAX -> SensorStatus.Excellent
                else -> SensorStatus.Good
            }

            // === Air Temperature ===
            "air_temp", "air temperature" -> when {
                value < SensorRanges.AIR_TEMP_CAUTION_MIN || value > SensorRanges.AIR_TEMP_CAUTION_MAX -> SensorStatus.Critical
                value < SensorRanges.AIR_TEMP_ACCEPTABLE_MIN || value > SensorRanges.AIR_TEMP_ACCEPTABLE_MAX -> SensorStatus.Caution
                value in SensorRanges.AIR_TEMP_EXCELLENT_MIN..SensorRanges.AIR_TEMP_EXCELLENT_MAX -> SensorStatus.Excellent
                else -> SensorStatus.Good
            }

            // === Humidity ===
            "humidity" -> when {
                value < SensorRanges.HUMIDITY_CAUTION_MIN || value > SensorRanges.HUMIDITY_CAUTION_MAX -> SensorStatus.Critical
                value < SensorRanges.HUMIDITY_ACCEPTABLE_MIN || value > SensorRanges.HUMIDITY_ACCEPTABLE_MAX -> SensorStatus.Caution
                value in SensorRanges.HUMIDITY_EXCELLENT_MIN..SensorRanges.HUMIDITY_EXCELLENT_MAX -> SensorStatus.Excellent
                else -> SensorStatus.Good
            }

            // === Float Switch (water level) ===
            "float_switch" -> if (value >= 0.5) SensorStatus.Critical else SensorStatus.Good

            // === Default Fallback ===
            else -> SensorStatus.Good
        }
    }

}



