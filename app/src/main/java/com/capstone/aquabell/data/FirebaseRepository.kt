package com.capstone.aquabell.data

import android.util.Log
import com.capstone.aquabell.data.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

class FirebaseRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val rtdb: FirebaseDatabase = FirebaseDatabase.getInstance("https://aquabell-cap2025-default-rtdb.asia-southeast1.firebasedatabase.app/"),
) {
    companion object { private const val TAG = "FirebaseRepo" }

    private val deviceId: String get() = "aquabell_esp32"

    // Connection state: not connected (no internet/firebase), connecting, connected
    enum class ConnectionState { NOT_CONNECTED, CONNECTING, CONNECTED }
    val connectionState: MutableStateFlow<ConnectionState> = MutableStateFlow(ConnectionState.CONNECTING)

    private val appContext: Context = FirebaseApp.getInstance().applicationContext
    private val connectivityManager: ConnectivityManager = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val repoScope: CoroutineScope = CoroutineScope(Dispatchers.IO)

    init {
        try {
            // Use default settings - Firestore persistence is enabled by default
            // No need to explicitly configure cache settings for basic functionality
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to configure Firestore: ${t.message}")
        }
        // Firestore doesn't provide a direct connection callback; we infer from listeners/errors.
        
        // Set a timeout to move from CONNECTING to CONNECTED if we get any response
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            delay(5000) // 5 second timeout
            if (connectionState.value == ConnectionState.CONNECTING) {
                // If still connecting after 5 seconds, try to force a connection test
                try {
                    val snap = liveDataDoc().get(com.google.firebase.firestore.Source.SERVER).await()
                    connectionState.tryEmit(ConnectionState.CONNECTED)
                } catch (t: Throwable) {
                    connectionState.tryEmit(ConnectionState.NOT_CONNECTED)
                }
            }
        }

        // Do not mirror to actuator_states; ESP32 owns actuator_states

        // Monitor network and update state proactively
        registerNetworkCallback()
    }

    private fun hasInternet(): Boolean {
        val network: Network = connectivityManager.activeNetwork ?: return false
        val caps: NetworkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun registerNetworkCallback() {
        try {
            connectivityManager.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    // Network back: enable Firestore and mark connecting, then force refresh
                    db.enableNetwork()
                    connectionState.tryEmit(ConnectionState.CONNECTING)
                    repoScope.launch {
                        try { forceRefresh() } catch (_: Throwable) {}
                    }
                }
                override fun onLost(network: Network) {
                    // Network lost: disable Firestore and mark not connected
                    db.disableNetwork()
                    connectionState.tryEmit(ConnectionState.NOT_CONNECTED)
                }
            })
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to register network callback: ${t.message}")
            // Fallback: set initial state based on current connectivity
            if (!hasInternet()) connectionState.tryEmit(ConnectionState.NOT_CONNECTED)
        }
    }

    // Removed actuator_states initialization. ESP32 is the single source of truth for actuator_states.

    private fun liveDataDoc(): DocumentReference = db.collection("live_data").document(deviceId)
    // Removed controlDoc() and commandControlDoc() - now using RTDB for actuator control
    private fun logsCollection() = db.collection("sensor_logs").document(deviceId).collection("logs")
    
    // RTDB references
    private fun commandsRef(): DatabaseReference = rtdb.getReference("commands").child(deviceId)
    private fun actuatorStatesRef(): DatabaseReference = rtdb.getReference("actuator_states").child(deviceId)

    fun liveData(): Flow<LiveDataSnapshot> = callbackFlow {
        val docRef = db.collection("live_data").document(deviceId)
        val listener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                snapshot.toObject(LiveDataSnapshot::class.java)?.let { 
                    trySend(it).isSuccess
                    connectionState.tryEmit(ConnectionState.CONNECTED)
                }
            } else {
                trySend(LiveDataSnapshot())
                connectionState.tryEmit(ConnectionState.NOT_CONNECTED)
            }
        }
        awaitClose { listener.remove() }
    }

    fun sensorLogs(): Flow<List<SensorLog>> = callbackFlow {
        val collectionRef = db.collection("sensor_logs").document(deviceId).collection("logs")
        val listener = collectionRef.orderBy("timestamp").limitToLast(500).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val items = snapshot.documents.mapNotNull { it.toObject(SensorLog::class.java) }
                trySend(items).isSuccess
                connectionState.tryEmit(ConnectionState.CONNECTED)
            } else {
                trySend(emptyList())
                connectionState.tryEmit(ConnectionState.NOT_CONNECTED)
            }
        }
        awaitClose { listener.remove() }
    }

    // RTDB Methods for actuator control - replaces Firestore control methods
    suspend fun sendCommand(deviceId: String, actuator: String, isAuto: Boolean, value: Boolean) {
        try {
            val updates = mapOf(
                "isAuto" to isAuto,
                "value" to value
            )
            rtdb.getReference("commands").child(deviceId).child(actuator).updateChildren(updates)
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to send command for $actuator: ${e.message}")
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending command for $actuator", e)
        }
    }

    // Convenience overload using repository deviceId
    suspend fun sendCommand(actuator: String, isAuto: Boolean, value: Boolean) {
        sendCommand(deviceId, actuator, isAuto, value)
    }

    fun observeActuatorState(deviceId: String, actuator: String, callback: (Boolean) -> Unit) {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val state = snapshot.getValue(ActuatorState::class.java)
                    val isOn = state?.value ?: false
                    callback(isOn)
                    connectionState.tryEmit(ConnectionState.CONNECTED)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing actuator state for $actuator", e)
                    callback(false)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "RTDB actuator state listener cancelled for $actuator: ${error.message}")
                callback(false)
                connectionState.tryEmit(ConnectionState.NOT_CONNECTED)
            }
        }
        
        rtdb.getReference("actuator_states").child(deviceId).child(actuator).addValueEventListener(listener)
    }

    suspend fun getCachedLiveData(): LiveDataSnapshot? {
        return try {
            val snap = liveDataDoc().get(com.google.firebase.firestore.Source.CACHE).await()
            snap.toObject(LiveDataSnapshot::class.java)
        } catch (t: Throwable) {
            null
        }
    }

    suspend fun forceRefresh() {
        try {
            // Force a server fetch to update connection state
            val snap = liveDataDoc().get(com.google.firebase.firestore.Source.SERVER).await()
            val obj = snap.toObject(LiveDataSnapshot::class.java)
            if (obj != null) {
                connectionState.tryEmit(ConnectionState.CONNECTED)
            } else {
                connectionState.tryEmit(ConnectionState.NOT_CONNECTED)
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Force refresh failed: ${t.message}")
            connectionState.tryEmit(ConnectionState.NOT_CONNECTED)
        }
    }

    // Legacy RTDB methods - kept for backward compatibility but deprecated
    // Use sendCommand() and observeActuatorState() instead
    
    @Deprecated("Use sendCommand() instead")
    suspend fun setActuatorState(actuator: String, isAuto: Boolean, value: Boolean) {
        sendCommand(deviceId, actuator, isAuto, value)
    }

    @Deprecated("Use sendCommand() instead") 
    suspend fun setActuatorAutoMode(actuator: String, isAuto: Boolean) {
        // Get current value first, then update
        try {
            commandsRef().child(actuator).child("value").get().addOnSuccessListener { snapshot ->
                val currentValue = snapshot.getValue(Boolean::class.java) ?: false
                repoScope.launch {
                    sendCommand(deviceId, actuator, isAuto, currentValue)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting $actuator auto mode", e)
        }
    }

    @Deprecated("Use sendCommand() instead")
    suspend fun setActuatorValueRTDB(actuator: String, value: Boolean) {
        // Get current isAuto first, then update
        try {
            commandsRef().child(actuator).child("isAuto").get().addOnSuccessListener { snapshot ->
                val currentIsAuto = snapshot.getValue(Boolean::class.java) ?: true
                repoScope.launch {
                    sendCommand(deviceId, actuator, currentIsAuto, value)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting $actuator value", e)
        }
    }

    // ---------------------------------------------------------------------
    // RTDB-backed flows kept for UI/ViewModel compatibility
    // ---------------------------------------------------------------------

    fun actuatorCommands(): Flow<ActuatorCommands> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val commands = snapshot.getValue(ActuatorCommands::class.java) ?: ActuatorCommands()
                    trySend(commands)
                    connectionState.tryEmit(ConnectionState.CONNECTED)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing actuator commands", e)
                    trySend(ActuatorCommands())
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "RTDB listener cancelled: ${error.message}")
                trySend(ActuatorCommands())
                connectionState.tryEmit(ConnectionState.NOT_CONNECTED)
            }
        }

        commandsRef().addValueEventListener(listener)
        awaitClose { commandsRef().removeEventListener(listener) }
    }

    fun actuatorStates(): Flow<ActuatorStates> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val states = snapshot.getValue(ActuatorStates::class.java) ?: ActuatorStates()
                    trySend(states)
                    connectionState.tryEmit(ConnectionState.CONNECTED)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing actuator states", e)
                    trySend(ActuatorStates())
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "RTDB actuator states listener cancelled: ${error.message}")
                trySend(ActuatorStates())
                connectionState.tryEmit(ConnectionState.NOT_CONNECTED)
            }
        }

        actuatorStatesRef().addValueEventListener(listener)
        awaitClose { actuatorStatesRef().removeEventListener(listener) }
    }

    // ---------------------------------------------------------------------
    // Compatibility shims for Firestore-era APIs used in UI
    // These now write/read from RTDB under /commands and /actuator_states
    // ---------------------------------------------------------------------

    fun commandControl(): Flow<com.capstone.aquabell.data.model.CommandControl> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val cmds = snapshot.getValue(ActuatorCommands::class.java) ?: ActuatorCommands()
                    val mapped = com.capstone.aquabell.data.model.CommandControl(
                        fan = ActuatorCommand(mode = if (cmds.fan.isAuto) ControlMode.AUTO else ControlMode.MANUAL, value = cmds.fan.value),
                        light = ActuatorCommand(mode = if (cmds.light.isAuto) ControlMode.AUTO else ControlMode.MANUAL, value = cmds.light.value),
                        pump = ActuatorCommand(mode = if (cmds.pump.isAuto) ControlMode.AUTO else ControlMode.MANUAL, value = cmds.pump.value),
                        valve = ActuatorCommand(mode = if (cmds.valve.isAuto) ControlMode.AUTO else ControlMode.MANUAL, value = cmds.valve.value),
                    )
                    trySend(mapped)
                    connectionState.tryEmit(ConnectionState.CONNECTED)
                } catch (e: Exception) {
                    Log.e(TAG, "Error mapping commandControl()", e)
                    trySend(com.capstone.aquabell.data.model.CommandControl())
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "RTDB commandControl listener cancelled: ${error.message}")
                trySend(com.capstone.aquabell.data.model.CommandControl())
                connectionState.tryEmit(ConnectionState.NOT_CONNECTED)
            }
        }

        commandsRef().addValueEventListener(listener)
        awaitClose { commandsRef().removeEventListener(listener) }
    }

    suspend fun setControlMode(mode: ControlMode) {
        try {
            val isAuto = mode == ControlMode.AUTO
            val updates = mapOf(
                "fan/isAuto" to isAuto,
                "light/isAuto" to isAuto,
                "pump/isAuto" to isAuto,
                "valve/isAuto" to isAuto,
            )
            commandsRef().updateChildren(updates)
                .addOnFailureListener { e -> Log.e(TAG, "setControlMode failed: ${e.message}") }
        } catch (e: Exception) {
            Log.e(TAG, "setControlMode error", e)
        }
    }

    suspend fun setRelayOverrides(overrides: RelayStates) {
        try {
            val updates = mapOf(
                "fan/isAuto" to false, "fan/value" to overrides.fan,
                "light/isAuto" to false, "light/value" to overrides.light,
                "pump/isAuto" to false, "pump/value" to overrides.waterPump,
                "valve/isAuto" to false, "valve/value" to overrides.valve,
            )
            commandsRef().updateChildren(updates)
                .addOnFailureListener { e -> Log.e(TAG, "setRelayOverrides failed: ${e.message}") }
        } catch (e: Exception) {
            Log.e(TAG, "setRelayOverrides error", e)
        }
    }

    suspend fun setActuatorMode(actuator: String, mode: ControlMode) {
        try {
            commandsRef().child(actuator).child("value").get().addOnSuccessListener { snap ->
                val currentValue = snap.getValue(Boolean::class.java) ?: false
                repoScope.launch {
                    sendCommand(deviceId, actuator, mode == ControlMode.AUTO, currentValue)
                }
            }.addOnFailureListener { e ->
                Log.e(TAG, "setActuatorMode failed for $actuator: ${e.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "setActuatorMode error for $actuator", e)
        }
    }

    // ViewModel compatibility: toggle helpers
    fun toggleActuatorAutoMode(actuator: String) {
        try {
            // Read current command state, flip isAuto, preserve current value, and send
            commandsRef().child(actuator).get().addOnSuccessListener { snapshot ->
                val cmd = snapshot.getValue(ActuatorState::class.java) ?: ActuatorState()
                val newIsAuto = !cmd.isAuto
                repoScope.launch {
                    sendCommand(deviceId, actuator, newIsAuto, cmd.value)
                }
            }.addOnFailureListener { e ->
                Log.e(TAG, "Failed to toggle $actuator auto mode: ${e.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling $actuator auto mode", e)
        }
    }

    fun toggleActuatorValue(actuator: String) {
        try {
            // Read current command state, flip value, keep current isAuto, and send
            commandsRef().child(actuator).get().addOnSuccessListener { snapshot ->
                val cmd = snapshot.getValue(ActuatorState::class.java) ?: ActuatorState()
                val newValue = !cmd.value
                repoScope.launch {
                    sendCommand(deviceId, actuator, cmd.isAuto, newValue)
                }
            }.addOnFailureListener { e ->
                Log.e(TAG, "Failed to toggle $actuator value: ${e.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling $actuator value", e)
        }
    }
}
