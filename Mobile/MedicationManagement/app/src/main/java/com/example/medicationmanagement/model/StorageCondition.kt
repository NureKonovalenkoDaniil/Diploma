package com.example.medicationmanagement.model

data class StorageCondition(
    val deviceID: String,
    val temperature: Double,
    val humidity: Double,
    val timestamp: String
)
