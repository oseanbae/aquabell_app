package com.capstone.aquabell.ui.viewmodel

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
import kotlinx.coroutines.launch

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

    init {
        viewModelScope.launch {
            // Warm cached snapshot for offline state
            _offlineCache.value = repository.getCachedLiveData()
        }
        viewModelScope.launch {
            repository.liveData().collectLatest { _live.value = it }
        }
        viewModelScope.launch {
            repository.commandControl().collectLatest { _command.value = it }
        }
        // connectionState is exposed directly from repository
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                repository.forceRefresh()
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
}


