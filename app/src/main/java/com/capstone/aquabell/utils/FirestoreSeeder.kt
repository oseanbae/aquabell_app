package com.capstone.aquabell.utils

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

// TODO: Remove FirestoreSeeder before production
object FirestoreSeeder {
    private const val TAG = "FirestoreSeeder"

    /**
     * Seeds the `/daily_logs` collection with mock documents for recent dates.
     * Safe, realistic ranges are used for aquaponics systems.
     *
     * This function is idempotent per date key; it will overwrite existing mock documents
     * that share the same document id (the ISO date string).
     */
    fun seedDailyLogs(db: FirebaseFirestore = FirebaseFirestore.getInstance(), days: Int = 6) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val now = java.util.Calendar.getInstance()
                val fmt = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())

                repeat(days) { i ->
                    val cal = now.clone() as java.util.Calendar
                    cal.add(java.util.Calendar.DAY_OF_YEAR, -(days - 1 - i))
                    val dateStr = fmt.format(cal.time)

                    val payload = mapOf(
                        "date" to dateStr,
                        // Water Temp: 25–28°C
                        "avgWaterTemp" to randomInRange(25.0, 28.0),
                        // DO: 6–8 mg/L
                        "avgDO" to randomInRange(6.0, 8.0),
                        // pH: 7–9
                        "avgPH" to randomInRange(7.0, 9.0),
                        // Turbidity: 150–250 NTU (match field name avgTurbidityNTU)
                        "avgTurbidityNTU" to randomInRange(150.0, 250.0),
                        // Air Temp: 24–30°C
                        "avgAirTemp" to randomInRange(24.0, 30.0),
                        // Air Humidity: 50–70%
                        "avgAirHumidity" to randomInRange(50.0, 80.0),
                        "timestamp" to Timestamp.now()
                    )

                    try {
                        db.collection("daily_logs").document(dateStr).set(payload).await()
                        Log.i(TAG, "Seeded daily_logs/$dateStr successfully: $payload")
                    } catch (t: Throwable) {
                        Log.e(TAG, "Failed to seed daily_logs/$dateStr: ${t.message}", t)
                    }
                }
            } catch (t: Throwable) {
                Log.e(TAG, "Seeder runtime error: ${t.message}", t)
            }
        }
    }

    private fun randomInRange(min: Double, max: Double): Double {
        return Random.nextDouble(min, max)
    }
}


