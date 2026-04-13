package com.example.medicationmanagement.model

data class StorageCondition(
    val deviceID: Int,
    val temperature: Double,
    val humidity: Double,
    val timestamp: String
)
