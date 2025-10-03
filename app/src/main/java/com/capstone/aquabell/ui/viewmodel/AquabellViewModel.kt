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
    fun setActuatorAutoMode(actuator: String, isAuto: Boolean) {
        viewModelScope.launch {
            val currentValue = when (actuator) {
                "fan" -> actuatorStates.value.fan.value
                "light" -> actuatorStates.value.light.value
                "pump" -> actuatorStates.value.pump.value
                "valve" -> actuatorStates.value.valve.value
                else -> false
            }
            repository.sendCommand(actuator, isAuto, currentValue)
        }
    }

    fun setActuatorValue(actuator: String, value: Boolean) {
        viewModelScope.launch {
            val currentIsAuto = when (actuator) {
                "fan" -> actuatorStates.value.fan.isAuto
                "light" -> actuatorStates.value.light.isAuto
                "pump" -> actuatorStates.value.pump.isAuto
                "valve" -> actuatorStates.value.valve.isAuto
                else -> true
            }
            if (!currentIsAuto) {
                repository.sendCommand(actuator, false, value)
            }
        }
    }

    // Force refresh data
    fun refreshData() {
        viewModelScope.launch {
            repository.forceRefresh()
        }
    }
}

