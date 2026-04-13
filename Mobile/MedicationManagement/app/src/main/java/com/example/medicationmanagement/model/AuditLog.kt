package com.example.medicationmanagement.model

data class AuditLog(
    val Id: Int,
    val Action: String,
    val User: String,
    val Timestamp: String,
    val Details: String? = null
)
