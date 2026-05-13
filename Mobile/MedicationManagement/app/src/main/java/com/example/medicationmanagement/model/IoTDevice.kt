package com.example.medicationmanagement.model

data class IoTDevice(
    val deviceID: String = "",
    val location: String = "",
    val type: String = "",
    val parameters: String = "",
    val isActive: Boolean = false,
    val minTemperature: Double = 0.0,
    val maxTemperature: Double = 0.0,
    val minHumidity: Double = 0.0,
    val maxHumidity: Double = 0.0
)
