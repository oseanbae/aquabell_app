package com.capstone.aquabell.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.capstone.aquabell.data.FirebaseRepository
import com.capstone.aquabell.data.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AquabellViewModel(
    private val repository: FirebaseRepository = FirebaseRepository()
) : ViewModel() {

    // Connection state
    val connectionState = repository.connectionState

    // Firestore data flows
    val liveData: StateFlow<LiveDataSnapshot> = repository.liveData()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = LiveDataSnapshot()
        )

    val sensorLogs: StateFlow<List<SensorLog>> = repository.sensorLogs()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // RTDB data flows
    val actuatorCommands: StateFlow<ActuatorCommands> = repository.actuatorCommands()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ActuatorCommands()
        )

    val actuatorStates: StateFlow<ActuatorStates> = repository.actuatorStates()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ActuatorStates()
        )

    // Individual actuator states for UI display - use confirmed states from ESP32
    val fanState: StateFlow<ActuatorState> = actuatorStates
        .map { it.fan }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ActuatorState()
        )

    val lightState: StateFlow<ActuatorState> = actuatorStates
        .map { it.light }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ActuatorState()
        )

    val pumpState: StateFlow<ActuatorState> = actuatorStates
        .map { it.pump }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ActuatorState()
        )

    val valveState: StateFlow<ActuatorState> = actuatorStates
        .map { it.valve }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ActuatorState()
        )

    // Individual confirmed actuator states (actual states from ESP32)
    val fanConfirmedState: StateFlow<ActuatorState> = actuatorStates
        .map { it.fan }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ActuatorState()
        )

    val lightConfirmedState: StateFlow<ActuatorState> = actuatorStates
        .map { it.light }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ActuatorState()
        )

    val pumpConfirmedState: StateFlow<ActuatorState> = actuatorStates
        .map { it.pump }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ActuatorState()
        )

    val valveConfirmedState: StateFlow<ActuatorState> = actuatorStates
        .map { it.valve }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ActuatorState()
        )

    // Actuator control functions
    fun toggleActuatorAutoMode(actuator: String) {
        viewModelScope.launch {
            repository.toggleActuatorAutoMode(actuator)
        }
    }

    fun toggleActuatorValue(actuator: String) {
        repository.toggleActuatorValue(actuator)
    }

    // New helpers for direct RTDB updates via sendCommand()
    fun updateActuatorAuto(actuator: String, isAuto: Boolean) {
        viewModelScope.launch {
            // get current value from confirmed states when available; fallback to commands
            val current = when (actuator) {
                "fan" -> actuatorStates.value.fan.value
                "light" -> actuatorStates.value.light.value
                "pump" -> actuatorStates.value.pump.value
                "valve" -> actuatorStates.value.valve.value
                else -> false
            }
            repository.sendCommand(actuator, isAuto, current)
        }
    }

    fun updateActuatorValue(actuator: String, value: Boolean) {
        viewModelScope.launch {
            val currentIsAuto = when (actuator) {
                "fan" -> actuatorStates.value.fan.isAuto
                "light" -> actuatorStates.value.light.isAuto
                "pump" -> actuatorStates.value.pump.isAuto
                "valve" -> actuatorStates.value.valve.isAuto
                else -> true
            }
            repository.sendCommand(actuator, currentIsAuto, value)
        }
    }

    // Back-compat convenience used by HomeScreen helpers
    fun setActuatorState(actuator: String, isAuto: Boolean, value: Boolean) {
        viewModelScope.launch {
            repository.sendCommand(actuator, isAuto, value)
        }
    }

    // New methods for direct actuator control
    fun setActuatorAutoMode(actuator: String, isAuto: Boolean) {
        android.util.Log.d("AquabellViewModel", "setActuatorAutoMode called: $actuator, isAuto: $isAuto")
        viewModelScope.launch {
            // Get current value from confirmed states
            val currentValue = when (actuator) {
                "fan" -> actuatorStates.value.fan.value
                "light" -> actuatorStates.value.light.value
                "pump" -> actuatorStates.value.pump.value
                "valve" -> actuatorStates.value.valve.value
                else -> false
            }
            android.util.Log.d("AquabellViewModel", "Current value for $actuator: $currentValue")
            repository.sendCommand(actuator, isAuto, currentValue)
        }
    }

    fun sendCommand(actuator: String, value: Boolean) {
        android.util.Log.d("AquabellViewModel", "sendCommand called: $actuator, value: $value")
        viewModelScope.launch {
            // Get current isAuto from confirmed states
            val currentIsAuto = when (actuator) {
                "fan" -> actuatorStates.value.fan.isAuto
                "light" -> actuatorStates.value.light.isAuto
                "pump" -> actuatorStates.value.pump.isAuto
                "valve" -> actuatorStates.value.valve.isAuto
                else -> true
            }
            android.util.Log.d("AquabellViewModel", "Current isAuto for $actuator: $currentIsAuto")
            repository.sendCommand(actuator, currentIsAuto, value)
        }
    }

    // Convenience functions for specific actuators
    fun toggleFanAutoMode() = toggleActuatorAutoMode("fan")
    fun toggleFanValue() = toggleActuatorValue("fan")
    fun setFanState(isAuto: Boolean, value: Boolean) = setActuatorState("fan", isAuto, value)

    fun toggleLightAutoMode() = toggleActuatorAutoMode("light")
    fun toggleLightValue() = toggleActuatorValue("light")
    fun setLightState(isAuto: Boolean, value: Boolean) = setActuatorState("light", isAuto, value)

    fun togglePumpAutoMode() = toggleActuatorAutoMode("pump")
    fun togglePumpValue() = toggleActuatorValue("pump")
    fun setPumpState(isAuto: Boolean, value: Boolean) = setActuatorState("pump", isAuto, value)

    fun toggleValveAutoMode() = toggleActuatorAutoMode("valve")
    fun toggleValveValue() = toggleActuatorValue("valve")
    fun setValveState(isAuto: Boolean, value: Boolean) = setActuatorState("valve", isAuto, value)

    // Force refresh data
    fun refreshData() {
        viewModelScope.launch {
            repository.forceRefresh()
        }
    }
}

