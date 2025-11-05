package com.capstone.aquabell.ui.utils

object AlertGuidance {
    fun guidanceFor(sensor: String, status: SensorStatus, value: Double): String {
        val s = sensor.trim().lowercase()
        return when (s) {
            "ph", "pH".lowercase() -> when (status) {
                is SensorStatus.Caution -> if (value > 7.8) {
                    "pH slightly high — consider partial water change or buffer adjustment."
                } else {
                    "pH slightly low — consider adding buffer and review water source."
                }
                is SensorStatus.Critical -> if (value > 8.2) {
                    "pH high — add buffer solution and check water source immediately."
                } else {
                    "pH low — raise alkalinity; check CO₂ and perform water change."
                }
                else -> "pH within acceptable range."
            }
            "temperature", "temp" -> when (status) {
                is SensorStatus.Caution -> "Temperature drifting — verify heater/chiller and ambient conditions."
                is SensorStatus.Critical -> "Temperature out of safe range — intervene (heater/chiller, aeration)."
                else -> "Temperature stable."
            }
            "turbidity" -> when (status) {
                is SensorStatus.Caution -> "Water clarity reduced — check filtration and feeding routine."
                is SensorStatus.Critical -> "High turbidity — perform water change and inspect filter immediately."
                else -> "Clarity normal."
            }
            "tds" -> when (status) {
                is SensorStatus.Caution -> "TDS elevated — review mineral load and source water."
                is SensorStatus.Critical -> "TDS high — perform partial water change and check mixing."
                else -> "TDS acceptable."
            }
            "dissolved_oxygen", "do", "oxygen" -> when (status) {
                is SensorStatus.Caution -> "Oxygen slightly low — increase surface agitation."
                is SensorStatus.Critical -> "Oxygen critically low — start aeration immediately."
                else -> "Oxygen adequate."
            }
            else -> when (status) {
                is SensorStatus.Caution -> "Parameter in caution range — monitor closely and prepare adjustments."
                is SensorStatus.Critical -> "Parameter critical — take corrective action now."
                else -> "Parameter within range."
            }
        }
    }
}


