package com.capstone.aquabell.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.capstone.aquabell.data.FirebaseRepository
import com.capstone.aquabell.data.model.CommandControl
import com.capstone.aquabell.data.model.ControlMode
import com.capstone.aquabell.data.model.LiveDataSnapshot
import com.capstone.aquabell.data.model.RelayStates
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withTimeoutOrNull

class HomeViewModel(
    private val repository: FirebaseRepository = FirebaseRepository()
) : ViewModel() {

    private val _live: MutableStateFlow<LiveDataSnapshot?> = MutableStateFlow(null)
    val live: StateFlow<LiveDataSnapshot?> = _live

    private val _command: MutableStateFlow<CommandControl> = MutableStateFlow(CommandControl())
    val command: StateFlow<CommandControl> = _command

    val connectionState: StateFlow<FirebaseRepository.ConnectionState> = repository.connectionState

    private val _isRefreshing: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    private val _offlineCache: MutableStateFlow<LiveDataSnapshot?> = MutableStateFlow(null)
    val offlineCache: StateFlow<LiveDataSnapshot?> = _offlineCache

    private val _isCommandLoaded = MutableStateFlow(false)
    val isCommandLoaded: StateFlow<Boolean> = _isCommandLoaded

    init {
        // Load cached snapshot first for offline state
        viewModelScope.launch {
            _offlineCache.value = repository.getCachedLiveData()
        }

        // Listen for live sensor data updates
        viewModelScope.launch {
            repository.liveData().collectLatest { snapshot ->
                _live.value = snapshot
            }
        }

        // 🔹 Initial sync from Firebase RTDB when app starts - this ensures we get the current state immediately
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    Log.d("HomeViewModel", "Starting initial actuator state fetch from RTDB...")
                    
                    // Use timeout to prevent hanging if Firebase is slow
                    val currentState = withTimeoutOrNull(10000) { // 10 second timeout
                        repository.getCurrentActuatorState()
                    }
                    
                    if (currentState != null) {
                        _command.value = currentState
                        Log.d("HomeViewModel", "✅ SUCCESS: Loaded initial actuator states from RTDB:")
                        Log.d("HomeViewModel", "  - Fan: ${currentState.fan.mode}/${currentState.fan.value}")
                        Log.d("HomeViewModel", "  - Light: ${currentState.light.mode}/${currentState.light.value}")
                        Log.d("HomeViewModel", "  - Pump: ${currentState.pump.mode}/${currentState.pump.value}")
                        Log.d("HomeViewModel", "  - Valve: ${currentState.valve.mode}/${currentState.valve.value}")
                    } else {
                        Log.w("HomeViewModel", "❌ FAILED: No data returned from RTDB - will use default states")
                        Log.w("HomeViewModel", "Current default states: fan=${_command.value.fan.mode}/${_command.value.fan.value}, light=${_command.value.light.mode}/${_command.value.light.value}")
                    }
                    
                    // Mark as loaded ONLY after we've attempted to load the initial state
                    _isCommandLoaded.value = true
                    Log.d("HomeViewModel", "Marked commandLoaded = true")
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "❌ ERROR loading initial actuator states: ${e.message}", e)
                _isCommandLoaded.value = true // still mark loaded so UI can render
            }
        }

        // Listen for command control (actuator modes + values) - this provides real-time updates AFTER initial load
        viewModelScope.launch {
            repository.commandControl().collectLatest { control ->
                _command.value = control
                Log.d("HomeViewModel", "Updated actuator states from stream: fan=${control.fan.mode}/${control.fan.value}, light=${control.light.mode}/${control.light.value}, pump=${control.pump.mode}/${control.pump.value}, valve=${control.valve.mode}/${control.valve.value}")
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                withContext(Dispatchers.IO) {
                    repository.forceRefresh()
                    val updated = withTimeoutOrNull(5000) { // 5 second timeout for refresh
                        repository.getCurrentActuatorState()
                    }
                    updated?.let { 
                        _command.value = it
                        Log.d("HomeViewModel", "Refreshed actuator states: fan=${it.fan.mode}/${it.fan.value}, light=${it.light.mode}/${it.light.value}, pump=${it.pump.mode}/${it.pump.value}, valve=${it.valve.mode}/${it.valve.value}")
                    }
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error during refresh: ${e.message}", e)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun setRelayOverrides(overrides: RelayStates) {
        viewModelScope.launch {
            repository.setRelayOverrides(overrides)
        }
    }

    fun setActuatorMode(actuator: String, mode: ControlMode) {
        viewModelScope.launch {
            repository.setActuatorMode(actuator, mode)
        }
    }

    fun setActuatorValue(actuator: String, value: Boolean) {
        viewModelScope.launch {
            repository.setActuatorValueRTDB(actuator, value)
        }
    }

    // Debug method to test RTDB connection
    fun testRTDBConnection() {
        viewModelScope.launch {
            try {
                Log.d("HomeViewModel", "Testing RTDB connection...")
                val testResult = repository.getCurrentActuatorState()
                if (testResult != null) {
                    Log.d("HomeViewModel", "✅ RTDB test successful: ${testResult}")
                } else {
                    Log.w("HomeViewModel", "❌ RTDB test failed: no data returned")
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "❌ RTDB test error: ${e.message}", e)
            }
        }
    }
}
