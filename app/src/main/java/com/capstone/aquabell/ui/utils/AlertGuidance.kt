package com.capstone.aquabell.ui.utils
import com.capstone.aquabell.data.model.SensorRanges
object AlertGuidance {

    // Public API
    fun guidanceFor(
        sensor: String,
        status: SensorStatus,
        value: Double,
        autoEnabled: Boolean
    ): String {
        val baseMessage = guidanceInternal(sensor.trim().lowercase(), status, value)

        return if (!autoEnabled && (status is SensorStatus.Caution || status is SensorStatus.Critical)) {
            "Auto control disabled — system will NOT correct this condition. $baseMessage"
        } else {
            baseMessage
        }
    }

    // Internal logic — unchanged messages, only reorganized
    private fun guidanceInternal(sensor: String, status: SensorStatus, value: Double): String {
        return when (sensor) {

            // === pH (dosing auto-managed) ===
            "ph" -> when (status) {
                is SensorStatus.Caution ->
                    if (value > SensorRanges.PH_ACCEPTABLE_MAX)
                        "pH slightly high — system may dose DOWN if it rises further. Monitor stability."
                    else
                        "pH slightly low — system may dose UP if it drops further. Keep an eye on trends."

                is SensorStatus.Critical ->
                    if (value > SensorRanges.PH_CAUTION_MAX)
                        "pH high — auto dosing is active. Avoid feeding and monitor tank for stress."
                    else
                        "pH low — auto dosing is active. Check for dead spots or excessive waste."

                else -> "pH stable."
            }

            // === Water Temp (cooler/heater logic) ===
            "water_temp", "watertemp", "water temperature" -> when (status) {
                is SensorStatus.Caution ->
                    if (value > SensorRanges.WATER_TEMP_EXCELLENT_MAX)
                        "Water warming — cooler may activate. Watch for continued rise."
                    else
                        "Water cooling — heater may activate to stabilize temperature."

                is SensorStatus.Critical ->
                    if (value > SensorRanges.WATER_TEMP_CAUTION_MAX)
                        "Water too hot — cooler working at max. Reduce lighting and avoid feeding."
                    else
                        "Water too cold — heater active. Ensure insulation and check for drafts."

                else -> "Water temperature normal."
            }

            // === Air Temp (fan logic) ===
            "air_temp", "airtemp", "air temperature" -> when (status) {
                is SensorStatus.Caution ->
                    if (value > SensorRanges.AIR_TEMP_EXCELLENT_MAX)
                        "Air warming — fans may activate."
                    else
                        "Air cooling — monitor if it continues dropping."

                is SensorStatus.Critical ->
                    if (value > SensorRanges.AIR_TEMP_CAUTION_MAX)
                        "Air very hot — ventilation needed. Fans should already be ON."
                    else
                        "Air very cold — environment may affect plant growth."

                else -> "Air temperature normal."
            }

            // === Humidity ===
            "humidity", "humid" -> when (status) {
                is SensorStatus.Caution ->
                    if (value > SensorRanges.HUMIDITY_ACCEPTABLE_MAX)
                        "Humidity high — fans may activate to manage moisture."
                    else
                        "Humidity low — plants may transpire less. Monitor leaf health."

                is SensorStatus.Critical ->
                    if (value > SensorRanges.HUMIDITY_CAUTION_MAX)
                        "Humidity extremely high — ventilation required. Fans should be ON."
                    else
                        "Humidity extremely low — may affect plant health. Check airflow."

                else -> "Humidity normal."
            }

            // === Dissolved Oxygen (no actuator) ===
            "dissolved_oxygen", "do", "oxygen" -> when (status) {
                is SensorStatus.Caution ->
                    "Dissolved oxygen slightly low — water agitation may be needed if it drops further."

                is SensorStatus.Critical ->
                    "Dissolved oxygen very low — increase aeration immediately."

                else -> "Oxygen level normal."
            }

            // === Turbidity (no actuator) ===
            "turbidity" -> when (status) {
                is SensorStatus.Caution ->
                    "Water becoming cloudy — check feeding amount and filter flow."

                is SensorStatus.Critical ->
                    "Water very cloudy — inspect filters and consider partial water change."

                else -> "Water clarity normal."
            }

            // === Float Switch (direct valve control) ===
            "float_switch" -> when (status) {
                is SensorStatus.Critical ->
                    "Water level low — refill valve opened automatically. Check for leaks or blockage."

                else -> "Water level normal."
            }

            // === Fallback ===
            else -> when (status) {
                is SensorStatus.Caution -> "Parameter in caution range — monitor for changes."
                is SensorStatus.Critical -> "Parameter critical — system may be correcting automatically."
                else -> "Parameter normal."
            }
        }
    }
}