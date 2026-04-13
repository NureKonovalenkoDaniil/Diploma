package com.example.medicationmanagement.model

data class IoTDevice(
    val deviceID: Int,
    val location: String,
    val type: String,
    val parameters: String,
    val isActive: Boolean,
    val minTemperature: Double,
    val maxTemperature: Double,
    val minHumidity: Double,
    val maxHumidity: Double
)
