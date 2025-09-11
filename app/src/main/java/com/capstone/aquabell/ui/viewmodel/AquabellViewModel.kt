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

    // Individual actuator states for easy access
    val fanState: StateFlow<ActuatorState> = actuatorCommands
        .map { it.fan }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ActuatorState()
        )

    val lightState: StateFlow<ActuatorState> = actuatorCommands
        .map { it.light }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ActuatorState()
        )

    val pumpState: StateFlow<ActuatorState> = actuatorCommands
        .map { it.pump }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ActuatorState()
        )

    val valveState: StateFlow<ActuatorState> = actuatorCommands
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

    fun setActuatorAutoMode(actuator: String, isAuto: Boolean) {
        viewModelScope.launch {
            repository.setActuatorAutoMode(actuator, isAuto)
        }
    }

    fun setActuatorValue(actuator: String, value: Boolean) {
        viewModelScope.launch {
            repository.setActuatorValueRTDB(actuator, value)
        }
    }

    fun setActuatorState(actuator: String, isAuto: Boolean, value: Boolean) {
        viewModelScope.launch {
            repository.setActuatorState(actuator, isAuto, value)
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

