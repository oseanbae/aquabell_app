package com.capstone.aquabell.data.model

import com.google.firebase.Timestamp
import com.google.firebase.database.PropertyName

data class RelayStates(
    val fan: Boolean = false,
    val light: Boolean = false,
    val waterPump: Boolean = false,
    val valve: Boolean = false,
    val cooler: Boolean = false,
    val heater: Boolean = false,
)

data class LiveDataSnapshot(
    val airHumidity: Double = 0.0,
    val airTemp: Double = 0.0,
    val dissolvedOxygen: Double = 0.0,
    val floatTriggered: Boolean = false,
    val pH: Double = 0.0,
    val turbidityNTU: Double = 0.0,
    val waterTemp: Double = 0.0,
    val relayStates: RelayStates = RelayStates(),
)

enum class ControlMode { AUTO, MANUAL }

data class ControlCommands(
    val mode: ControlMode = ControlMode.AUTO,
    val override: RelayStates = RelayStates(),
)

// New schema for per-actuator commands in command_control collection
data class ActuatorCommand(
    val mode: ControlMode = ControlMode.AUTO,
    val value: Boolean = false,
)

data class CommandControl(
    val fan: ActuatorCommand = ActuatorCommand(),
    val light: ActuatorCommand = ActuatorCommand(),
    val pump: ActuatorCommand = ActuatorCommand(),
    val valve: ActuatorCommand = ActuatorCommand(),
    val cooler: ActuatorCommand = ActuatorCommand(),
    val heater: ActuatorCommand = ActuatorCommand(),
)

data class SensorLog(
    val waterTemp: Double = 0.0,
    val pH: Double = 0.0,
    val dissolvedOxygen: Double = 0.0,
    val turbidityNTU: Double = 0.0,
    val airTemp: Double = 0.0,
    val airHumidity: Double = 0.0,
    val floatTriggered: Boolean = false,
    val timestamp: Timestamp = Timestamp.now(),
)

// RTDB Models for actuator commands
data class ActuatorState(
    @get:com.google.firebase.database.PropertyName("isAuto")
    val isAuto: Boolean = true,
    @get:com.google.firebase.database.PropertyName("value")
    val value: Boolean = false
)

data class ActuatorCommands(
    val fan: ActuatorState = ActuatorState(),
    val light: ActuatorState = ActuatorState(),
    val pump: ActuatorState = ActuatorState(),
    val valve: ActuatorState = ActuatorState(),
    val cooler: ActuatorState = ActuatorState(),
    val heater: ActuatorState = ActuatorState()
)

data class CommandsRoot(
    val aquabell_esp32: ActuatorCommands = ActuatorCommands()
)

// RTDB Models for actuator states (actual confirmed states from ESP32)
data class ActuatorStates(
    val fan: ActuatorState = ActuatorState(),
    val light: ActuatorState = ActuatorState(),
    val pump: ActuatorState = ActuatorState(),
    val valve: ActuatorState = ActuatorState(),
    val cooler: ActuatorState = ActuatorState(),
    val heater: ActuatorState = ActuatorState()
)


