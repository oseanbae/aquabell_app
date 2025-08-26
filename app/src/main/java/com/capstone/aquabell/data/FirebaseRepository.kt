package com.capstone.aquabell.data

import android.util.Log
import com.capstone.aquabell.data.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
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
            val settings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build()
            db.firestoreSettings = settings
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to enable persistence: ${t.message}")
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

    private fun liveDataDoc(): DocumentReference = db.collection("live_data").document(deviceId)
    private fun controlDoc(): DocumentReference = db.collection("control_commands").document(deviceId)
    private fun logsCollection() = db.collection("sensor_logs").document(deviceId).collection("logs")

    fun liveData(): Flow<LiveDataSnapshot> = callbackFlow {
        val reg = liveDataDoc().addSnapshotListener { snap, err ->
            if (err != null) {
                trySend(LiveDataSnapshot())
                connectionState.tryEmit(ConnectionState.NOT_CONNECTED)
                return@addSnapshotListener
            }
            val obj = snap?.toObject(LiveDataSnapshot::class.java) ?: LiveDataSnapshot()
            trySend(obj)
            val fromCache = snap?.metadata?.isFromCache == true
            if (fromCache) {
                connectionState.tryEmit(ConnectionState.NOT_CONNECTED)
            } else {
                // If we get any data from server (not cache), we're connected
                connectionState.tryEmit(ConnectionState.CONNECTED)
            }
        }
        awaitClose { reg.remove() }
    }

    fun sensorLogs(): Flow<List<SensorLog>> = callbackFlow {
        val reg = logsCollection().orderBy("timestamp").limitToLast(500).addSnapshotListener { qs, err ->
            if (err != null) {
                trySend(emptyList())
                connectionState.tryEmit(ConnectionState.NOT_CONNECTED)
                return@addSnapshotListener
            }
            val items = qs?.documents?.mapNotNull { it.toObject(SensorLog::class.java) } ?: emptyList()
            trySend(items)
            val fromCache = qs?.metadata?.isFromCache == true
            if (fromCache) {
                connectionState.tryEmit(ConnectionState.NOT_CONNECTED)
            } else {
                connectionState.tryEmit(ConnectionState.CONNECTED)
            }
        }
        awaitClose { reg.remove() }
    }

    suspend fun setControlMode(mode: ControlMode) {
        controlDoc().set(ControlCommands(mode = mode)).addOnFailureListener {
            Log.e(TAG, "setControlMode failed", it)
        }
    }

    suspend fun setRelayOverrides(overrides: RelayStates) {
        controlDoc().set(ControlCommands(mode = ControlMode.MANUAL, override = overrides)).addOnFailureListener {
            Log.e(TAG, "setRelayOverrides failed", it)
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
}


