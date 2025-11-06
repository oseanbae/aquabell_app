package com.capstone.aquabell.data.model

data class AlertEntry(
    val alertId: String = "",
    val type: String, // caution | critical
    val title: String,
    val message: String,
    val timestamp: Long,
    val acknowledged: Boolean = false
)


