# Firebase Integration Guide for Aquabell

This guide explains how the Aquabell app integrates with both Firebase Firestore and Firebase Realtime Database (RTDB) for comprehensive IoT device management.

## Architecture Overview

### Firebase Firestore
- **Purpose**: Sensor data storage and retrieval
- **Collections**:
  - `live_data/{deviceId}`: Current sensor readings (overwritten every 10s)
  - `sensor_logs/{deviceId}/logs`: Historical sensor data (5-min or hourly averages)

### Firebase Realtime Database
- **Purpose**: Real-time actuator command control
- **Path**: `/commands/aquabell_esp32/`
- **Schema**: 
  ```json
  {
    "commands": {
      "aquabell_esp32": {
        "fan": { "isAuto": true, "value": false },
        "light": { "isAuto": true, "value": false },
        "pump": { "isAuto": false, "value": true },
        "valve": { "isAuto": false, "value": false }
      }
    }
  }
  ```

## Key Components

### 1. Data Models (`Models.kt`)

#### Firestore Models
- `LiveDataSnapshot`: Current sensor readings
- `SensorLog`: Historical sensor data with timestamps
- `RelayStates`: Legacy relay state structure

#### RTDB Models
- `ActuatorState`: Individual actuator state (`isAuto`, `value`)
- `ActuatorCommands`: Collection of all actuator states
- `CommandsRoot`: Root structure for RTDB commands

### 2. FirebaseRepository (`FirebaseRepository.kt`)

#### Firestore Methods
- `liveData()`: Flow of current sensor data
- `sensorLogs()`: Flow of historical sensor logs
- `getCachedLiveData()`: Get cached data when offline

#### RTDB Methods
- `actuatorCommands()`: Flow of real-time actuator states
- `setActuatorAutoMode(actuator, isAuto)`: Set auto/manual mode
- `setActuatorValue(actuator, value)`: Set actuator on/off state
- `setActuatorState(actuator, isAuto, value)`: Set both mode and value
- `toggleActuatorAutoMode(actuator)`: Toggle between auto/manual
- `toggleActuatorValue(actuator)`: Toggle on/off state

### 3. ViewModel (`AquabellViewModel.kt`)

Provides reactive state management with:
- Individual actuator state flows (`fanState`, `lightState`, etc.)
- Convenience methods for each actuator
- Connection state monitoring
- Data refresh capabilities

### 4. UI Components

#### ActuatorControlCard
- Visual control for individual actuators
- Auto/Manual toggle switch
- On/Off toggle (enabled only in manual mode)
- Real-time status display

#### DashboardScreen
- Complete dashboard showing sensor data and actuator controls
- Connection status indicator
- Historical data display

## Usage Examples

### Basic Setup

```kotlin
// In your Activity or Fragment
val viewModel: AquabellViewModel by viewModels()

// Collect data
val fanState by viewModel.fanState.collectAsState()
val liveData by viewModel.liveData.collectAsState()
```

### Controlling Actuators

```kotlin
// Toggle fan between auto and manual mode
viewModel.toggleFanAutoMode()

// Toggle fan on/off (only works in manual mode)
viewModel.toggleFanValue()

// Set specific state
viewModel.setFanState(isAuto = false, value = true)

// Generic actuator control
viewModel.toggleActuatorAutoMode("pump")
viewModel.setActuatorValue("valve", true)
```

### Monitoring Data

```kotlin
// Monitor connection state
val connectionState by viewModel.connectionState.collectAsState()

// Monitor live sensor data
val liveData by viewModel.liveData.collectAsState()

// Monitor specific actuator
val pumpState by viewModel.pumpState.collectAsState()
```

## Firebase Rules

### Firestore Rules
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /live_data/{deviceId} {
      allow read, write: if request.auth != null;
    }
    match /sensor_logs/{deviceId}/logs/{logId} {
      allow read, write: if request.auth != null;
    }
  }
}
```

### RTDB Rules
```json
{
  "rules": {
    "commands": {
      "$deviceId": {
        ".read": "auth != null",
        ".write": "auth != null",
        "fan": {
          "isAuto": { ".validate": "newData.isBoolean()" },
          "value": { ".validate": "newData.isBoolean()" }
        },
        "light": {
          "isAuto": { ".validate": "newData.isBoolean()" },
          "value": { ".validate": "newData.isBoolean()" }
        },
        "pump": {
          "isAuto": { ".validate": "newData.isBoolean()" },
          "value": { ".validate": "newData.isBoolean()" }
        },
        "valve": {
          "isAuto": { ".validate": "newData.isBoolean()" },
          "value": { ".validate": "newData.isBoolean()" }
        }
      }
    }
  }
}
```

## Dependencies

Add to your `build.gradle.kts`:

```kotlin
implementation(platform("com.google.firebase:firebase-bom:34.1.0"))
implementation("com.google.firebase:firebase-auth-ktx:22.3.1")
implementation("com.google.firebase:firebase-firestore-ktx:25.1.1")
implementation("com.google.firebase:firebase-database-ktx:21.0.0")
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")
```

## Error Handling

The repository includes comprehensive error handling:
- Connection state monitoring
- Automatic retry mechanisms
- Graceful degradation when offline
- Detailed logging for debugging

## Best Practices

1. **Always check connection state** before performing critical operations
2. **Use the ViewModel** for state management rather than direct repository access
3. **Handle offline scenarios** gracefully with cached data
4. **Monitor logs** for RTDB connection issues
5. **Test thoroughly** with both online and offline scenarios

## Troubleshooting

### Common Issues

1. **RTDB not updating**: Check Firebase rules and authentication
2. **Firestore data not loading**: Verify collection names and document IDs
3. **Connection state stuck on CONNECTING**: Check network connectivity and Firebase configuration
4. **Actuator controls not working**: Ensure RTDB rules allow write access

### Debug Logs

Enable debug logging by checking the `FirebaseRepo` tag in Logcat for detailed information about:
- Connection state changes
- Data flow updates
- Error conditions
- RTDB operations


