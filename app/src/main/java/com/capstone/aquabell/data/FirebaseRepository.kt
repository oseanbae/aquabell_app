package com.capstone.aquabell.data

import android.util.Log
import com.capstone.aquabell.data.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.Flow
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
import kotlinx.coroutines.suspendCancellableCoroutine
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

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
    private fun alertsCollection(uid: String, deviceId: String) =
        db.collection("users").document(uid).collection("devices").document(deviceId).collection("alerts")
    
    // RTDB references
    private fun commandsRef(): DatabaseReference = rtdb.getReference("commands").child(deviceId)
    private fun actuatorStatesRef(): DatabaseReference = rtdb.getReference("actuator_states").child(deviceId)

    fun liveData(): Flow<LiveDataSnapshot> = callbackFlow {
		val ref = FirebaseDatabase.getInstance().getReference("live_data/$deviceId")
		val listener = object : ValueEventListener {
			override fun onDataChange(snapshot: DataSnapshot) {
				if (snapshot.exists()) {
					val data = snapshot.getValue(LiveDataSnapshot::class.java)
					if (data != null) {
						trySend(data).isSuccess
						connectionState.tryEmit(ConnectionState.CONNECTED)
					} else {
						trySend(LiveDataSnapshot())
						connectionState.tryEmit(ConnectionState.NOT_CONNECTED)
					}
				} else {
					trySend(LiveDataSnapshot())
					connectionState.tryEmit(ConnectionState.NOT_CONNECTED)
				}
			}

			override fun onCancelled(error: DatabaseError) {
				connectionState.tryEmit(ConnectionState.NOT_CONNECTED)
				close(error.toException())
			}
		}

		ref.addValueEventListener(listener)
		awaitClose { ref.removeEventListener(listener) }
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

    // ---------------------------------------------------------------------
    // Alerts (Firestore)
    // ---------------------------------------------------------------------

    suspend fun pushAlert(alert: com.capstone.aquabell.data.model.AlertEntry) {
        try {
            ensureAuth()
            val uid = auth.currentUser?.uid ?: throw IllegalStateException("No auth user")
            alertsCollection(uid, deviceId).document(alert.id).set(
                mapOf(
                    "id" to alert.id,
                    "sensor" to alert.sensor,
                    "value" to alert.value,
                    "status" to alert.status,
                    "guidance" to alert.guidance,
                    "timestamp" to com.google.firebase.Timestamp(alert.timestamp / 1000, ((alert.timestamp % 1000) * 1_000_000).toInt()),
                    "acknowledged" to alert.acknowledged
                )
            ).await()
        } catch (e: Exception) {
            Log.e(TAG, "pushAlert failed: ${e.message}", e)
            throw e
        }
    }

    fun listenToAlerts(deviceId: String): kotlinx.coroutines.flow.Flow<List<com.capstone.aquabell.data.model.AlertEntry>> = callbackFlow {
        try {
            repoScope.launch { ensureAuth() }

            // The listener registration needs to be created inside the flow
            val uid = auth.currentUser?.uid

            val listenerRegistration = if (uid != null) {
                alertsCollection(uid, deviceId)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            Log.e(TAG, "listenToAlerts error: ${error.message}", error)
                            trySend(emptyList()).isSuccess
                            return@addSnapshotListener
                        }
                        if (snapshot == null) {
                            trySend(emptyList()).isSuccess
                            return@addSnapshotListener
                        }
                        val items = snapshot.documents.mapNotNull { doc ->
                            try {
                                val data = doc.data ?: return@mapNotNull null
                                val ts = (data["timestamp"] as? com.google.firebase.Timestamp) ?: com.google.firebase.Timestamp.now()
                                com.capstone.aquabell.data.model.AlertEntry(
                                    id = data["id"] as? String ?: doc.id,
                                    sensor = data["sensor"] as? String ?: "",
                                    value = (data["value"] as? Number)?.toDouble() ?: 0.0,
                                    status = data["status"] as? String ?: "caution",
                                    guidance = data["guidance"] as? String ?: "",
                                    timestamp = ts.toDate().time,
                                    acknowledged = data["acknowledged"] as? Boolean ?: false
                                )
                            } catch (t: Throwable) {
                                Log.w(TAG, "Failed to parse alert: ${t.message}")
                                null
                            }
                        }
                        trySend(items).isSuccess
                        connectionState.tryEmit(ConnectionState.CONNECTED)
                    }
            } else {
                // If there's no user ID, we can't listen to alerts.
                // Send an empty list and log the issue.
                Log.w(TAG, "Cannot listen to alerts: user is not authenticated.")
                trySend(emptyList()).isSuccess
                null // The listener registration is null
            }

            // When the flow is closed, remove the listener if it was created
            awaitClose { listenerRegistration?.remove() }

        } catch (t: Throwable) {
            Log.e(TAG, "listenToAlerts setup failed: ${t.message}", t)
            trySend(emptyList()).isSuccess
            awaitClose { /* no-op */ }
        }
    }


    suspend fun acknowledgeAlert(deviceId: String, alertId: String) {
        try {
            ensureAuth()
            val uid = auth.currentUser?.uid ?: throw IllegalStateException("No auth user")
            alertsCollection(uid, deviceId).document(alertId).update("acknowledged", true).await()
        } catch (e: Exception) {
            Log.e(TAG, "acknowledgeAlert failed: ${e.message}", e)
            throw e
        }
    }

    // RTDB Methods for actuator control - replaces Firestore control methods
    suspend fun sendCommand(deviceId: String, actuator: String, isAuto: Boolean, value: Boolean) {
        val updates = mapOf(
            "isAuto" to isAuto,
            "value" to value
        )
        val ref = rtdb.getReference("commands").child(deviceId).child(actuator)
        try {
            ensureAuth()
            ref.updateChildrenAwait(updates)
            Log.i(TAG, "sendCommand success → /commands/$deviceId/$actuator {isAuto=$isAuto, value=$value}")
        } catch (t: Throwable) {
            Log.e(TAG, "sendCommand failed for $actuator: ${t.message}", t)
            throw t
        }
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
        try {
            val currentValueSnap = commandsRef().child(actuator).child("value").get().awaitTask()
            val currentValue = currentValueSnap.getValue(Boolean::class.java) ?: false
            sendCommand(deviceId, actuator, isAuto, currentValue)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting $actuator auto mode", e)
            throw e
        }
    }

    @Deprecated("Use sendCommand() instead")
    suspend fun setActuatorValueRTDB(actuator: String, value: Boolean) {
        // Get current isAuto first, then update
        try {
            val isAutoSnap = commandsRef().child(actuator).child("isAuto").get().awaitTask()
            val currentIsAuto = isAutoSnap.getValue(Boolean::class.java) ?: true
            sendCommand(deviceId, actuator, currentIsAuto, value)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting $actuator value", e)
            throw e
        }
    }

    // ---------------------------------------------------------------------
    // RTDB-backed flows kept for UI/ViewModel compatibility
    // ---------------------------------------------------------------------



    // ---------------------------------------------------------------------
    // Compatibility shims for Firestore-era APIs used in UI
    // These now write/read from RTDB under /commands
    // ---------------------------------------------------------------------

    suspend fun getCurrentActuatorState(): CommandControl? {
        return try {
            val dbRef = FirebaseDatabase.getInstance()
                .getReference("commands")
                .child(deviceId)

            Log.d(TAG, "Fetching actuator state from RTDB path: /commands/$deviceId")

            val snapshot = suspendCoroutine<DataSnapshot?> { cont ->
                dbRef.get()
                    .addOnSuccessListener { 
                        Log.d(TAG, "✅ RTDB fetch successful, snapshot exists: ${it.exists()}")
                        if (it.exists()) {
                            Log.d(TAG, "Raw snapshot data: ${it.value}")
                        }
                        cont.resume(it) 
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "❌ RTDB fetch failed: ${e.message}", e)
                        cont.resume(null)
                    }
                    .addOnCanceledListener {
                        Log.w(TAG, "⚠️ RTDB fetch canceled")
                        cont.resume(null)
                    }
            }

            snapshot?.let { dataSnapshot ->
                if (dataSnapshot.exists()) {
                    val commands = dataSnapshot.getValue(ActuatorCommands::class.java) ?: ActuatorCommands()
                    Log.d(TAG, "Parsed ActuatorCommands: fan=${commands.fan.isAuto}/${commands.fan.value}, light=${commands.light.isAuto}/${commands.light.value}, pump=${commands.pump.isAuto}/${commands.pump.value}, valve=${commands.valve.isAuto}/${commands.valve.value}, cooler=${commands.cooler.isAuto}/${commands.cooler.value}, heater=${commands.heater.isAuto}/${commands.heater.value}")
                    
                    val result = CommandControl(
                        fan = ActuatorCommand(
                            mode = if (commands.fan.isAuto) ControlMode.AUTO else ControlMode.MANUAL,
                            value = commands.fan.value
                        ),
                        light = ActuatorCommand(
                            mode = if (commands.light.isAuto) ControlMode.AUTO else ControlMode.MANUAL,
                            value = commands.light.value
                        ),
                        pump = ActuatorCommand(
                            mode = if (commands.pump.isAuto) ControlMode.AUTO else ControlMode.MANUAL,
                            value = commands.pump.value
                        ),
                        valve = ActuatorCommand(
                            mode = if (commands.valve.isAuto) ControlMode.AUTO else ControlMode.MANUAL,
                            value = commands.valve.value
                        ),
                        cooler = ActuatorCommand(
                            mode = if (commands.cooler.isAuto) ControlMode.AUTO else ControlMode.MANUAL,
                            value = commands.cooler.value
                        ),
                        heater = ActuatorCommand(
                            mode = if (commands.heater.isAuto) ControlMode.AUTO else ControlMode.MANUAL,
                            value = commands.heater.value
                        )
                    )
                    Log.d(TAG, "✅ Returning CommandControl: fan=${result.fan.mode}/${result.fan.value}, light=${result.light.mode}/${result.light.value}, pump=${result.pump.mode}/${result.pump.value}, valve=${result.valve.mode}/${result.valve.value}, cooler=${result.cooler.mode}/${result.cooler.value}, heater=${result.heater.mode}/${result.heater.value}")
                    result
                } else {
                    Log.w(TAG, "❌ No data found at /commands/$deviceId")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception fetching current actuator state: ${e.message}", e)
            null
        }
    }

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
                        cooler = ActuatorCommand(mode = if (cmds.cooler.isAuto) ControlMode.AUTO else ControlMode.MANUAL, value = cmds.cooler.value),
                        heater = ActuatorCommand(mode = if (cmds.heater.isAuto) ControlMode.AUTO else ControlMode.MANUAL, value = cmds.heater.value),
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
        val isAuto = mode == ControlMode.AUTO
        val updates = mapOf(
            "fan/isAuto" to isAuto,
            "light/isAuto" to isAuto,
            "pump/isAuto" to isAuto,
            "valve/isAuto" to isAuto,
            "cooler/isAuto" to isAuto,
            "heater/isAuto" to isAuto,
        )
        try {
            commandsRef().updateChildrenAwait(updates)
            Log.i(TAG, "setControlMode success → isAuto=$isAuto for all")
        } catch (e: Exception) {
            Log.e(TAG, "setControlMode failed: ${e.message}", e)
            throw e
        }
    }

    suspend fun setRelayOverrides(overrides: RelayStates) {
        val updates = mapOf(
            "fan/isAuto" to false, "fan/value" to overrides.fan,
            "light/isAuto" to false, "light/value" to overrides.light,
            "pump/isAuto" to false, "pump/value" to overrides.waterPump,
            "valve/isAuto" to false, "valve/value" to overrides.valve,
            "cooler/isAuto" to false, "cooler/value" to overrides.cooler,
            "heater/isAuto" to false, "heater/value" to overrides.heater,
        )
        try {
            commandsRef().updateChildrenAwait(updates)
            Log.i(TAG, "setRelayOverrides success → $updates")
        } catch (e: Exception) {
            Log.e(TAG, "setRelayOverrides failed: ${e.message}", e)
            throw e
        }
    }

    suspend fun setActuatorMode(actuator: String, mode: ControlMode) {
        try {
            val snap = commandsRef().child(actuator).child("value").get().awaitTask()
            val currentValue = snap.getValue(Boolean::class.java) ?: false
            sendCommand(deviceId, actuator, mode == ControlMode.AUTO, currentValue)
        } catch (e: Exception) {
            Log.e(TAG, "setActuatorMode error for $actuator", e)
            throw e
        }
    }

    // ---------------------------------------------------------------------
    // Tasks bridging helpers
    // ---------------------------------------------------------------------
    private suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { cont ->
        this.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                @Suppress("UNCHECKED_CAST")
                val result: T? = task.result as T?
                if (result != null || task.result == null) {
                    // For Void tasks, result is null; resume with Unit cast if caller expects Unit
                    if (result == null) {
                        try {
                            @Suppress("UNCHECKED_CAST")
                            cont.resume(Unit as T)
                        } catch (_: Throwable) {
                            cont.resumeWithException(IllegalStateException("Task result was null"))
                        }
                    } else {
                        cont.resume(result)
                    }
                }
            } else {
                val ex = task.exception ?: RuntimeException("Task failed without exception")
                cont.resumeWithException(ex)
            }
        }
        // No cancellation bridge for Firebase Task
    }

    private suspend fun DatabaseReference.updateChildrenAwait(updates: Map<String, Any?>) = suspendCancellableCoroutine<Unit> { cont ->
        this.updateChildren(updates).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                cont.resume(Unit)
            } else {
                val ex = task.exception ?: RuntimeException("updateChildren failed without exception")
                cont.resumeWithException(ex)
            }
        }
    }

    private suspend fun ensureAuth() {
        if (auth.currentUser != null) return
        try {
            signInAnonymouslyAwait()
            Log.i(TAG, "FirebaseAuth anonymous sign-in success")
        } catch (t: Throwable) {
            Log.e(TAG, "FirebaseAuth anonymous sign-in failed: ${t.message}", t)
            throw t
        }
    }

    private suspend fun signInAnonymouslyAwait(): AuthResult {
        val task = auth.signInAnonymously()
        return task.awaitTask()
    }

    // Analytics methods
    suspend fun getDailyLogs(): List<DailyAnalytics> {
        return try {
            ensureAuth()
            val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
            
            // Fetch from daily_logs collection
            val dailyLogsCollection = db.collection("daily_logs")
            val querySnapshot = dailyLogsCollection
                .whereLessThan("date", today) // Only historical data, not today
                .orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(30) // Last 30 days
                .get(com.google.firebase.firestore.Source.SERVER)
                .await()

            querySnapshot.documents.mapNotNull { doc ->
                try {
                    val data = doc.data ?: return@mapNotNull null
                    val dateStr = data["date"] as? String ?: doc.id
                    val waterTemp = (data["avgWaterTemp"] as? Number)?.toDouble()
                        ?: (data["waterTemp"] as? Number)?.toDouble() ?: 0.0
                    val airTemp = (data["avgAirTemp"] as? Number)?.toDouble()
                        ?: (data["airTemp"] as? Number)?.toDouble() ?: 0.0
                    val airHumidity = (data["avgAirHumidity"] as? Number)?.toDouble()
                        ?: (data["airHumidity"] as? Number)?.toDouble() ?: 0.0
                    val pH = (data["avgPH"] as? Number)?.toDouble()
                        ?: (data["pH"] as? Number)?.toDouble() ?: 0.0
                    val dissolvedOxygen = (data["avgDO"] as? Number)?.toDouble()
                        ?: (data["dissolvedOxygen"] as? Number)?.toDouble() ?: 0.0
                    val turbidityNTU = (data["avgTurbidityNTU"] as? Number)?.toDouble()
                        ?: (data["avgTurbidity"] as? Number)?.toDouble()
                        ?: (data["turbidityNTU"] as? Number)?.toDouble() ?: 0.0

                    DailyAnalytics(
                        date = dateStr,
                        airTemp = airTemp,
                        airHumidity = airHumidity,
                        waterTemp = waterTemp,
                        pH = pH,
                        dissolvedOxygen = dissolvedOxygen,
                        turbidityNTU = turbidityNTU,
                        timestamp = data["timestamp"] as? com.google.firebase.Timestamp ?: com.google.firebase.Timestamp.now(),
                        isLive = false
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing daily log document: ${e.message}", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching daily logs: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun getTodaySensorLogs(): List<SensorLog> {
        return try {
            ensureAuth()
            val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
            
            // Fetch from sensor_logs/{today}/entries
            val sensorLogsCollection = db.collection("sensor_logs").document(today).collection("entries")
            val querySnapshot = sensorLogsCollection
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(144) // 24 hours * 6 entries per hour (10-minute intervals)
                .get(com.google.firebase.firestore.Source.SERVER)
                .await()

            querySnapshot.documents.mapNotNull { doc ->
                try {
                    val data = doc.data ?: return@mapNotNull null
                    SensorLog(
                        waterTemp = (data["waterTemp"] as? Number)?.toDouble() ?: 0.0,
                        pH = (data["pH"] as? Number)?.toDouble() ?: 0.0,
                        dissolvedOxygen = (data["dissolvedOxygen"] as? Number)?.toDouble() ?: 0.0,
                        turbidityNTU = (data["turbidityNTU"] as? Number)?.toDouble() ?: 0.0,
                        airTemp = (data["airTemp"] as? Number)?.toDouble() ?: 0.0,
                        airHumidity = (data["airHumidity"] as? Number)?.toDouble() ?: 0.0,
                        floatTriggered = (data["floatTriggered"] as? Boolean) ?: false,
                        timestamp = data["timestamp"] as? com.google.firebase.Timestamp ?: com.google.firebase.Timestamp.now()
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing sensor log document: ${e.message}", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching today's sensor logs: ${e.message}", e)
            emptyList()
        }
    }

    fun observeTodaySensorLogs(): Flow<List<SensorLog>> = callbackFlow {
        try {
            repoScope.launch { ensureAuth() }
            val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
            val ref = db.collection("sensor_logs").document(today).collection("entries")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)

            val registration = ref.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Snapshot listener error for today logs: ${error.message}", error)
                    trySend(emptyList()).isSuccess
                    return@addSnapshotListener
                }
                if (snapshot == null) {
                    trySend(emptyList()).isSuccess
                    return@addSnapshotListener
                }
                val logs: List<SensorLog> = snapshot.documents.mapNotNull { doc ->
                    try {
                        val data = doc.data ?: return@mapNotNull null
                        SensorLog(
                            waterTemp = (data["waterTemp"] as? Number)?.toDouble() ?: 0.0,
                            pH = (data["pH"] as? Number)?.toDouble() ?: 0.0,
                            dissolvedOxygen = (data["dissolvedOxygen"] as? Number)?.toDouble() ?: 0.0,
                            turbidityNTU = (data["turbidityNTU"] as? Number)?.toDouble() ?: 0.0,
                            airTemp = (data["airTemp"] as? Number)?.toDouble() ?: 0.0,
                            airHumidity = (data["airHumidity"] as? Number)?.toDouble() ?: 0.0,
                            floatTriggered = (data["floatTriggered"] as? Boolean) ?: false,
                            timestamp = data["timestamp"] as? com.google.firebase.Timestamp ?: com.google.firebase.Timestamp.now()
                        )
                    } catch (t: Throwable) {
                        Log.w(TAG, "Failed to parse sensor log: ${t.message}")
                        null
                    }
                }
                trySend(logs).isSuccess
                connectionState.tryEmit(ConnectionState.CONNECTED)
            }

            awaitClose { registration.remove() }
        } catch (t: Throwable) {
            Log.e(TAG, "observeTodaySensorLogs setup failed: ${t.message}", t)
            trySend(emptyList()).isSuccess
            awaitClose { /* no-op */ }
        }
    }

    suspend fun saveDailyAnalytics(analytics: DailyAnalytics) {
        try {
            ensureAuth()
            val dailyLogsCollection = db.collection("daily_logs")
            val documentId = analytics.date
            
            val data = mapOf(
                "date" to analytics.date,
                "airTemp" to analytics.airTemp,
                "airHumidity" to analytics.airHumidity,
                "waterTemp" to analytics.waterTemp,
                "pH" to analytics.pH,
                "dissolvedOxygen" to analytics.dissolvedOxygen,
                "turbidityNTU" to analytics.turbidityNTU,
                "timestamp" to analytics.timestamp,
                "isLive" to analytics.isLive
            )
            
            dailyLogsCollection.document(documentId).set(data).await()
            Log.d(TAG, "Successfully saved daily analytics for ${analytics.date}")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving daily analytics: ${e.message}", e)
            throw e
        }
    }
}
// Convenience overload using repository deviceId
//    suspend fun sendCommand(actuator: String, isAuto: Boolean, value: Boolean) {
//        sendCommand(deviceId, actuator, isAuto, value)
//    }

//    fun observeActuatorState(deviceId: String, actuator: String, callback: (Boolean) -> Unit) {
//        val listener = object : ValueEventListener {
//            override fun onDataChange(snapshot: DataSnapshot) {
//                try {
//                    val state = snapshot.getValue(ActuatorState::class.java)
//                    val isOn = state?.value ?: false
//                    callback(isOn)
//                    connectionState.tryEmit(ConnectionState.CONNECTED)
//                } catch (e: Exception) {
//                    Log.e(TAG, "Error parsing actuator state for $actuator", e)
//                    callback(false)
//                }
//            }
//
//            override fun onCancelled(error: DatabaseError) {
//                Log.e(TAG, "RTDB actuator state listener cancelled for $actuator: ${error.message}")
//                callback(false)
//                connectionState.tryEmit(ConnectionState.NOT_CONNECTED)
//            }
//        }
//
//        rtdb.getReference("actuator_states").child(deviceId).child(actuator).addValueEventListener(listener)
//    }

//    private val currentStates: MutableMap<String, ActuatorState> = mutableMapOf(
//        "fan" to ActuatorState(),
//        "light" to ActuatorState(),
//        "pump" to ActuatorState(),
//        "valve" to ActuatorState()
//    )

// ViewModel compatibility: toggle helpers
//    fun toggleActuatorAutoMode(actuator: String) {
//        repoScope.launch {
//            try {
//                val snapshot = commandsRef().child(actuator).get().awaitTask()
//                val cmd = snapshot.getValue(ActuatorState::class.java) ?: ActuatorState()
//                val newIsAuto = !cmd.isAuto
//                sendCommand(deviceId, actuator, newIsAuto, cmd.value)
//            } catch (e: Exception) {
//                Log.e(TAG, "Failed to toggle $actuator auto mode: ${e.message}", e)
//            }
//        }
//    }
//
//    fun toggleActuatorValue(actuator: String) {
//        repoScope.launch {
//            try {
//                val snapshot = commandsRef().child(actuator).get().awaitTask()
//                val cmd = snapshot.getValue(ActuatorState::class.java) ?: ActuatorState()
//                val newValue = !cmd.value
//                sendCommand(deviceId, actuator, cmd.isAuto, newValue)
//            } catch (e: Exception) {
//                Log.e(TAG, "Failed to toggle $actuator value: ${e.message}", e)
//            }
//        }
//    }

//fun actuatorCommands(): Flow<ActuatorCommands> = callbackFlow {
//        val listener = object : ValueEventListener {
//            override fun onDataChange(snapshot: DataSnapshot) {
//                try {
//                    val commands = snapshot.getValue(ActuatorCommands::class.java) ?: ActuatorCommands()
//                    // Keep local currentStates in sync with /commands
//                    currentStates["fan"] = commands.fan
//                    currentStates["light"] = commands.light
//                    currentStates["pump"] = commands.pump
//                    currentStates["valve"] = commands.valve
//                    trySend(commands)
//                    connectionState.tryEmit(ConnectionState.CONNECTED)
//                } catch (e: Exception) {
//                    Log.e(TAG, "Error parsing actuator commands", e)
//                    trySend(ActuatorCommands())
//                }
//            }
//
//            override fun onCancelled(error: DatabaseError) {
//                Log.e(TAG, "RTDB listener cancelled: ${error.message}")
//                trySend(ActuatorCommands())
//                connectionState.tryEmit(ConnectionState.NOT_CONNECTED)
//            }
//        }
//
//        commandsRef().addValueEventListener(listener)
//        awaitClose { commandsRef().removeEventListener(listener) }
//    }
//
//    // Public write APIs that accept explicit values from UI, no stale reads
//    fun updateActuatorValue(actuator: String, value: Boolean) {
//        val current = currentStates[actuator] ?: ActuatorState()
//        repoScope.launch {
//            try {
//                sendCommand(deviceId, actuator, current.isAuto, value)
//            } catch (_: Throwable) { }
//        }
//    }
//
//    fun updateActuatorAuto(actuator: String, isAuto: Boolean) {
//        val current = currentStates[actuator] ?: ActuatorState()
//        repoScope.launch {
//            try {
//                sendCommand(deviceId, actuator, isAuto, current.value)
//            } catch (_: Throwable) { }
//        }
//    }
//
//    fun actuatorStates(): Flow<ActuatorStates> = callbackFlow {
//        val listener = object : ValueEventListener {
//            override fun onDataChange(snapshot: DataSnapshot) {
//                try {
//                    val states = snapshot.getValue(ActuatorStates::class.java) ?: ActuatorStates()
//                    trySend(states)
//                    connectionState.tryEmit(ConnectionState.CONNECTED)
//                } catch (e: Exception) {
//                    Log.e(TAG, "Error parsing actuator states", e)
//                    trySend(ActuatorStates())
//                }
//            }
//
//            override fun onCancelled(error: DatabaseError) {
//                Log.e(TAG, "RTDB actuator states listener cancelled: ${error.message}")
//                trySend(ActuatorStates())
//                connectionState.tryEmit(ConnectionState.NOT_CONNECTED)
//            }
//        }
//
//        actuatorStatesRef().addValueEventListener(listener)
//        awaitClose { actuatorStatesRef().removeEventListener(listener) }
//    }