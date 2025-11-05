package com.capstone.aquabell.ui.utils

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.capstone.aquabell.data.model.AlertEntry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private const val ALERT_DATASTORE_NAME = "alert_prefs"
private const val COOLDOWN_MILLIS: Long = 5 * 60 * 1000L // 5 minutes

private val Context.alertDataStore by preferencesDataStore(name = ALERT_DATASTORE_NAME)

object AlertEvaluator {

    /**
     * Input model for a single sensor's latest reading.
     */
    data class SensorReading(
        val sensor: String,
        val value: Double,
        val status: SensorStatus,
        val timestamp: Long
    )

    /**
     * Evaluate alert events for latest readings.
     * Emits alerts only on transitions:
     *  - normal (Excellent|Good) -> Caution|Critical
     *  - Caution -> Critical (escalation)
     * Applies per-sensor cooldown of 5 minutes.
     */
    suspend fun evaluateAlerts(
        context: Context,
        readings: List<SensorReading>
    ): List<AlertEntry> {
        if (readings.isEmpty()) return emptyList()
        val results = mutableListOf<AlertEntry>()
        for (reading in readings) {
            val previousStatus = getLastStatus(context, reading.sensor)
            val nowStatus = reading.status

            val wasNormal = previousStatus is SensorStatus.Excellent || previousStatus is SensorStatus.Good
            val isCaution = nowStatus is SensorStatus.Caution
            val isCritical = nowStatus is SensorStatus.Critical
            val wasCaution = previousStatus is SensorStatus.Caution

            val transitionedToAlert = (wasNormal && (isCaution || isCritical)) || (wasCaution && isCritical)

            // Update stored status regardless, to track transitions precisely next time
            setLastStatus(context, reading.sensor, nowStatus)

            if (!transitionedToAlert) continue

            // cooldown gate
            val lastAlertTs = getLastAlertTs(context, reading.sensor)
            val withinCooldown = reading.timestamp - lastAlertTs < COOLDOWN_MILLIS
            if (withinCooldown) continue

            val statusString = if (isCritical) "critical" else "caution"
            val guidance = AlertGuidance.guidanceFor(reading.sensor, nowStatus, reading.value)
            val alert = AlertEntry(
                id = buildAlertId(reading.sensor, reading.timestamp, statusString),
                sensor = reading.sensor,
                value = reading.value,
                status = statusString,
                guidance = guidance,
                timestamp = reading.timestamp,
                acknowledged = false
            )
            results.add(alert)

            // record cooldown timestamp
            setLastAlertTs(context, reading.sensor, reading.timestamp)
        }
        return results
    }

    private fun statusKey(sensor: String): Preferences.Key<String> =
        stringPreferencesKey("last_status_${'$'}{sensor.lowercase()}")

    private fun alertTsKey(sensor: String): Preferences.Key<Long> =
        longPreferencesKey("last_alert_ts_${'$'}{sensor.lowercase()}")

    private suspend fun getLastStatus(context: Context, sensor: String): SensorStatus {
        val key = statusKey(sensor)
        val value = context.alertDataStore.data.map { it[key] }.first()
        return SensorStatus.fromString(value)
    }

    private suspend fun setLastStatus(context: Context, sensor: String, status: SensorStatus) {
        val key = statusKey(sensor)
        context.alertDataStore.edit { prefs ->
            prefs[key] = status.toString()
        }
    }

    private suspend fun getLastAlertTs(context: Context, sensor: String): Long {
        val key = alertTsKey(sensor)
        return context.alertDataStore.data.map { it[key] ?: 0L }.first()
    }

    private suspend fun setLastAlertTs(context: Context, sensor: String, ts: Long) {
        val key = alertTsKey(sensor)
        context.alertDataStore.edit { prefs ->
            prefs[key] = ts
        }
    }

    private fun buildAlertId(sensor: String, timestamp: Long, status: String): String {
        return "${'$'}{sensor.lowercase()}_${'$'}status_${'$'}timestamp"
    }
}


