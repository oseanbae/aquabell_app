package com.capstone.aquabell.ui.utils

import android.annotation.SuppressLint
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
        readings: List<SensorReading>,
        autoEnabled: Map<String, Boolean> = emptyMap() // sensor -> auto/manual
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

            // Update stored status regardless
            setLastStatus(context, reading.sensor, nowStatus)

            if (!transitionedToAlert) continue

            // cooldown gate
            val lastAlertTs = getLastAlertTs(context, reading.sensor)
            val withinCooldown = reading.timestamp - lastAlertTs < COOLDOWN_MILLIS
            if (withinCooldown) continue

            val type = if (isCritical) "critical" else "caution"
            val title = buildTitle(reading.sensor, reading.value)

            // pass autoEnabled info to guidance
            val isAuto = autoEnabled[reading.sensor.lowercase()] ?: true
            val message = AlertGuidance.guidanceFor(reading.sensor, nowStatus, reading.value, isAuto)

            val alert = AlertEntry(
                alertId = buildAlertId(reading.sensor, reading.timestamp, type),
                type = type,
                title = title,
                message = message,
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
        return "${sensor.lowercase()}_${status}_${timestamp}"
    }

    @SuppressLint("DefaultLocale")
    private fun buildTitle(sensor: String, value: Double): String {
        val s = sensor.lowercase()
        val formatted = when (s) {
            "ph" -> String.format("%.2f", value)
            "temperature", "water_temp", "water temperature" -> String.format("%.1f°C", value)
            "air_temp", "air temperature", "airTemp" -> String.format("%.1f°C", value)
            "humidity" -> String.format("%.0f%%", value)
            "dissolved_oxygen", "do", "oxygen" -> String.format("%.1f mg/L", value)
            "turbidity" -> String.format("%.0f NTU", value)
            "float_switch" -> if (value >= 0.5) "LOW" else "Normal"

            else -> String.format("%.2f", value)
        }

        val label = when (s) {
            "ph" -> "pH"
            "temperature", "water_temp", "water temperature" -> "Water Temp"
            "air_temp", "air temperature", "airTemp" -> "Air Temp"
            "humidity" -> "Humidity"
            "dissolved_oxygen", "do", "oxygen" -> "Dissolved O₂"
            "turbidity" -> "Turbidity"
            "float_switch" -> "Water Level"
            else -> sensor.capitalize()
        }

        return "$label: $formatted"
    }

    suspend fun resetCooldown(context: Context, sensor: String) {
        val key = longPreferencesKey("last_alert_ts_${sensor.lowercase()}")
        context.alertDataStore.edit { prefs ->
            prefs.remove(key)
        }
    }
}


