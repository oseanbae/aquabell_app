package com.capstone.aquabell.ui.utils

sealed class SensorStatus(val nameValue: String) {
    object Excellent : SensorStatus("excellent")
    object Good : SensorStatus("good")
    object Caution : SensorStatus("caution")
    object Critical : SensorStatus("critical")

    fun isAlerting(): Boolean = this is Caution || this is Critical

    override fun toString(): String = nameValue

    companion object {
        fun fromString(value: String?): SensorStatus = when (value?.lowercase()) {
            "excellent" -> Excellent
            "good" -> Good
            "caution" -> Caution
            "critical" -> Critical
            else -> Good // default to non-alerting
        }
    }
}


