package com.capstone.aquabell.ui.utils

object AlertGuidance {

    fun guidanceFor(sensor: String, status: SensorStatus, value: Double): String {
        val s = sensor.trim().lowercase()

        return when (s) {
            // === pH ===
            "ph", "pH".lowercase() -> when (status) {
                is SensorStatus.Caution -> if (value > 7.8) {
                    "pH slightly high — consider a partial water change or check source water."
                } else {
                    "pH slightly low — review buffering capacity or replace part of tank water."
                }
                is SensorStatus.Critical -> if (value > 8.5) {
                    "pH high — perform partial water change and inspect ammonia buildup."
                } else {
                    "pH low — add buffer or replace water immediately to prevent fish stress."
                }
                else -> "pH stable within optimal range."
            }

            // === Water Temperature ===
            "water_temp", "waterTemp", "water temperature" -> when (status) {
                is SensorStatus.Caution -> if (value > 28.0) {
                    "Water getting warm — evaporative fan may activate. Increase aeration if sustained."
                } else {
                    "Water cooling — heater may activate to stabilize fish environment."
                }
                is SensorStatus.Critical -> if (value > 32.0) {
                    "Water too hot — ensure fans are running and avoid feeding fish until cooled."
                } else {
                    "Water too cold — heater ON recommended to prevent stress."
                }
                else -> "Water temperature optimal."
            }

            // === Air Temperature ===
            "air_temp", "airTemp", "air temperature" -> when (status) {
                is SensorStatus.Caution -> if (value > 29.0) {
                    "Air warming — fans may activate to maintain environment stability."
                } else {
                    "Air cooling — monitor heater activity if grow area drops below comfort range."
                }
                is SensorStatus.Critical -> if (value > 35.0) {
                    "Air temperature very high — ensure ventilation fans are ON and cover tank to reduce heat."
                } else {
                    "Air temperature too low — check for cold drafts or add insulation."
                }
                else -> "Air temperature stable."
            }

            // === Humidity ===
            "humidity", "humid" -> when (status) {
                is SensorStatus.Caution -> if (value > 75.0) {
                    "Humidity rising — fans may activate to prevent algae and mold."
                } else {
                    "Humidity low — misting or covering may help plants avoid stress."
                }
                is SensorStatus.Critical -> if (value > 90.0) {
                    "Humidity very high — run fans longer or increase air exchange."
                } else {
                    "Humidity too low — plant transpiration may drop; cover or mist slightly."
                }
                else -> "Humidity stable and healthy."
            }

            // === Dissolved Oxygen ===
            "dissolved_oxygen", "do", "oxygen" -> when (status) {
                is SensorStatus.Caution -> "Dissolved oxygen slightly low — increase surface agitation."
                is SensorStatus.Critical -> "Dissolved oxygen critically low — boost aeration immediately."
                else -> "Oxygen level adequate."
            }

            // === Turbidity ===
            "turbidity" -> when (status) {
                is SensorStatus.Caution -> "Water slightly cloudy — check filter and feeding routine."
                is SensorStatus.Critical -> "High turbidity — perform partial water change and inspect filters."
                else -> "Water clarity normal."
            }

            "float_switch" -> when (status) {
                is SensorStatus.Critical -> "Water level low — solenoid valve opened to refill. Check for leaks or blocked intake."
                else -> "Water level normal."
            }

            // === Fallback ===
            else -> when (status) {
                is SensorStatus.Caution -> "Parameter in caution range — monitor and prepare adjustments."
                is SensorStatus.Critical -> "Parameter critical — take corrective action now."
                else -> "Parameter stable."
            }
        }
    }
}