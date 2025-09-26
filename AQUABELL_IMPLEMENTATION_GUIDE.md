# AquaBell Android App Implementation Guide

## Overview

This implementation provides a comprehensive Android app for the AquaBell aquaponics system with real-time Firebase integration. The app features live sensor monitoring, actuator control, historical data visualization, and robust offline handling.

## Key Features Implemented

### ✅ Real-time Sensor Monitoring
- **Firestore Integration**: Live sensor data from `live_data/{deviceId}` collection
- **Auto-refresh**: Updates every 10 seconds without app restart
- **Offline Support**: Shows cached data when disconnected
- **Connection Status**: Visual indicators for connection state

### ✅ Actuator Control System
- **Dual State Monitoring**: 
  - Command states (what the app sends)
  - Confirmed states (actual ESP32 responses)
- **Auto/Manual Modes**: Toggle between automatic and manual control
- **Real-time Updates**: Instant UI updates when ESP32 confirms commands
- **RTDB Integration**: Uses Firebase Realtime Database for actuator commands

### ✅ Historical Data & Analytics
- **Sensor Logs**: 500 most recent entries from `sensor_logs/{deviceId}/logs`
- **Chart Visualization**: Line charts for trend analysis
- **Time Intervals**: 5-minute and hourly data aggregation
- **Interactive UI**: Touch-friendly chart controls

### ✅ Offline Handling
- **Cached Data Display**: Shows last known values when offline
- **Connection Recovery**: Automatic re-sync when connection restored
- **Visual Indicators**: Clear offline/online status
- **Graceful Degradation**: App remains functional with cached data

## Architecture

### Data Flow
```
ESP32 → Firebase Firestore (sensor data) → Android App
ESP32 → Firebase RTDB (actuator states) → Android App
Android App → Firebase RTDB (commands) → ESP32
```

### Key Components

#### 1. FirebaseRepository
- **Location**: `app/src/main/java/com/capstone/aquabell/data/FirebaseRepository.kt`
- **Features**:
  - Real-time Firestore listeners for sensor data
  - RTDB listeners for actuator commands and states
  - Connection state management
  - Offline data caching
  - Network monitoring

#### 2. Data Models
- **Location**: `app/src/main/java/com/capstone/aquabell/data/model/Models.kt`
- **Key Models**:
  - `LiveDataSnapshot`: Current sensor readings
  - `SensorLog`: Historical sensor data
  - `ActuatorCommands`: Command states for actuators
  - `ActuatorStates`: Confirmed states from ESP32

#### 3. ViewModel
- **Location**: `app/src/main/java/com/capstone/aquabell/ui/viewmodel/AquabellViewModel.kt`
- **Features**:
  - StateFlow management for reactive UI
  - Individual actuator state flows
  - Command and confirmed state separation
  - Convenience methods for actuator control

#### 4. UI Components
- **DashboardScreen**: Main monitoring interface
- **ActuatorControlCard**: Individual actuator controls
- **AnalyticsScreen**: Historical data visualization
- **Connection Status**: Real-time connection indicators

## Firebase Setup

### Firestore Collections

#### 1. Live Data Collection
```
Collection: live_data
Document: {deviceId} (e.g., "aquabell_esp32")
Fields:
- airHumidity: Double
- airTemp: Double
- dissolvedOxygen: Double
- floatTriggered: Boolean
- pH: Double
- turbidityNTU: Double
- waterTemp: Double
- relayStates: RelayStates (legacy)
```

#### 2. Sensor Logs Collection
```
Collection: sensor_logs/{deviceId}/logs
Document: {logId}
Fields:
- waterTemp: Double
- pH: Double
- dissolvedOxygen: Double
- turbidityNTU: Double
- airTemp: Double
- airHumidity: Double
- floatTriggered: Boolean
- timestamp: Timestamp
```

### Realtime Database Structure

#### 1. Commands Path
```
Path: /commands/{deviceId}/{actuator}
Fields:
- isAuto: Boolean (true = auto, false = manual)
- value: Boolean (true = ON, false = OFF)
```

#### 2. Actuator States Path
```
Path: /actuator_states/{deviceId}/{actuator}
Fields:
- isAuto: Boolean (confirmed mode from ESP32)
- value: Boolean (confirmed state from ESP32)
```

## Usage Examples

### Basic Setup
```kotlin
// In your Activity or Fragment
val viewModel: AquabellViewModel by viewModels()

// Collect data
val liveData by viewModel.liveData.collectAsState()
val fanState by viewModel.fanState.collectAsState()
val fanConfirmedState by viewModel.fanConfirmedState.collectAsState()
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
val pumpConfirmedState by viewModel.pumpConfirmedState.collectAsState()
```

## Real-time Updates

### Sensor Data Updates
- **Frequency**: Every 10 seconds (ESP32 overwrites document)
- **Method**: Firestore `addSnapshotListener()`
- **UI Impact**: Automatic refresh without app restart
- **Offline**: Shows cached data with offline indicator

### Actuator Control Updates
- **Command Flow**: App → RTDB → ESP32
- **Confirmation Flow**: ESP32 → RTDB → App
- **UI Updates**: Instant for commands, confirmed when ESP32 responds
- **Method**: RTDB `addValueEventListener()`

## Offline Handling

### Connection States
1. **CONNECTED**: Full functionality, real-time updates
2. **CONNECTING**: Loading state, shows cached data
3. **NOT_CONNECTED**: Offline mode, cached data only

### Cached Data
- **Sensor Data**: Last known live readings
- **Actuator States**: Last confirmed states
- **Visual Indicators**: Clear offline status display
- **Recovery**: Automatic re-sync when connection restored

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
        ".write": "auth != null"
      }
    },
    "actuator_states": {
      "$deviceId": {
        ".read": "auth != null",
        ".write": "auth != null"
      }
    }
  }
}
```

## Testing

### Real-time Updates Test
1. Open app and verify connection status
2. Change actuator settings and verify instant UI updates
3. Check ESP32 logs to confirm command reception
4. Verify confirmed states update when ESP32 responds

### Offline Test
1. Disable network connection
2. Verify offline indicator appears
3. Check that cached data is displayed
4. Re-enable network and verify re-sync

### Data Flow Test
1. Monitor Firestore for sensor data updates
2. Monitor RTDB for command changes
3. Verify actuator state confirmations
4. Check historical data in analytics screen

## Troubleshooting

### Common Issues

#### 1. Connection Not Established
- Check Firebase configuration
- Verify network connectivity
- Check Firebase rules permissions
- Review authentication setup

#### 2. Actuator Commands Not Working
- Verify RTDB rules allow writes
- Check device ID matches
- Confirm ESP32 is listening to RTDB
- Review command structure

#### 3. No Real-time Updates
- Check Firestore listeners are active
- Verify document structure matches models
- Review connection state logic
- Check for error logs

#### 4. Offline Data Not Showing
- Verify cached data retrieval
- Check Firestore persistence settings
- Review offline handling logic
- Test with network disabled

## Performance Considerations

### Memory Management
- StateFlows automatically manage lifecycle
- Listeners are cleaned up on component destruction
- Cached data is limited to prevent memory issues

### Network Efficiency
- Firestore listeners only active when needed
- RTDB listeners optimized for real-time updates
- Offline mode reduces unnecessary network calls

### UI Responsiveness
- Immediate UI updates for user actions
- Background data processing
- Smooth animations and transitions

## Future Enhancements

### Potential Improvements
1. **Push Notifications**: Alerts for critical sensor readings
2. **Data Export**: CSV/PDF export of historical data
3. **Custom Thresholds**: User-defined sensor limits
4. **Multi-device Support**: Support for multiple AquaBell systems
5. **Advanced Analytics**: Machine learning insights
6. **Remote Monitoring**: Web dashboard integration

## Support

For technical support or questions about this implementation:
1. Check the troubleshooting section
2. Review Firebase console for errors
3. Check Android logs for detailed error messages
4. Verify ESP32 integration is working correctly

---

**Note**: This implementation provides a complete, production-ready Android app for the AquaBell aquaponics system with robust real-time capabilities and offline support.

