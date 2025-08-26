package com.capstone.aquabell.data.model

import com.google.firebase.Timestamp

data class RelayStates(
    val fan: Boolean = false,
    val light: Boolean = false,
    val waterPump: Boolean = false,
    val valve: Boolean = false,
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


