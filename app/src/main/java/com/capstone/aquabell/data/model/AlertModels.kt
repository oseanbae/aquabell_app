package com.capstone.aquabell.data.model

data class AlertEntry(
    val id: String = "",
    val sensor: String,
    val value: Double,
    val status: String, // caution | critical
    val guidance: String,
    val timestamp: Long,
    val acknowledged: Boolean = false
)


