package com.example.medicationmanagement.model

data class AuditLog(
    val id: Int = 0,
    val user: String = "",
    val action: String = "",
    val entityType: String? = null,
    val entityId: Int? = null,
    val timestamp: String = "",
    val details: String = "",
    val severity: String = "Info"
)
