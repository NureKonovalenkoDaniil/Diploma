package com.example.medicationmanagement.model

data class LifecycleEvent(
    val eventId: Int,
    val medicineId: Int,
    val eventType: String,
    val quantity: Int,
    val eventDate: String,
    val performedBy: String?,
    val description: String?
)
