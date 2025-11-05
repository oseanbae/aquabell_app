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

class AlertsViewModel(
    private val repository: FirebaseRepository = FirebaseRepository(),
) : ViewModel() {

    private val _alerts: MutableStateFlow<List<AlertEntry>> = MutableStateFlow(emptyList())
    val alerts: StateFlow<List<AlertEntry>> = _alerts

    private val _unreadCount: MutableStateFlow<Int> = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount

    private val existingIds: MutableSet<String> = mutableSetOf()
    private val deviceId: String = "aquabell_esp32"

    init {
        // Listen to stored alerts for device
        viewModelScope.launch {
            repository.listenToAlerts(deviceId).collectLatest { list ->
                _alerts.value = list
                _unreadCount.value = list.count { !it.acknowledged }
                existingIds.clear()
                existingIds.addAll(list.map { it.id })
            }
        }

        // Listen to live data and evaluate new alerts on transitions
        viewModelScope.launch {
            repository.liveData().collectLatest { live ->
                if (live == null) return@collectLatest
                val now = System.currentTimeMillis()
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
                )

                val context = FirebaseApp.getInstance().applicationContext
                val newAlerts = AlertEvaluator.evaluateAlerts(context, readings)
                    .filter { it.id !in existingIds }

                if (newAlerts.isNotEmpty()) {
                    // Persist each new alert
                    newAlerts.forEach { alert ->
                        viewModelScope.launch {
                            try { repository.pushAlert(alert) } catch (_: Throwable) {}
                        }
                    }
                }
            }
        }
    }

    fun acknowledgeAlert(id: String) {
        viewModelScope.launch {
            try {
                repository.acknowledgeAlert(deviceId, id)
            } catch (_: Throwable) { }
        }
    }

    private fun statusFor(sensor: String, value: Double): SensorStatus {
        return when (sensor.lowercase()) {
            "ph" -> when {
                value < 6.5 || value > 8.4 -> SensorStatus.Critical
                value < 6.8 || value > 8.0 -> SensorStatus.Caution
                value in 7.0..7.6 -> SensorStatus.Excellent
                else -> SensorStatus.Good
            }
            "temperature" -> when {
                value < 20.0 || value > 34.0 -> SensorStatus.Critical
                value < 22.0 || value > 32.0 -> SensorStatus.Caution
                value in 26.0..30.0 -> SensorStatus.Excellent
                else -> SensorStatus.Good
            }
            "dissolved_oxygen", "do", "oxygen" -> when {
                value < 3.0 -> SensorStatus.Critical
                value < 5.0 -> SensorStatus.Caution
                value >= 7.0 -> SensorStatus.Excellent
                else -> SensorStatus.Good
            }
            "turbidity" -> when {
                value >= 150.0 -> SensorStatus.Critical
                value >= 80.0 -> SensorStatus.Caution
                value <= 20.0 -> SensorStatus.Excellent
                else -> SensorStatus.Good
            }
            else -> SensorStatus.Good
        }
    }
}



