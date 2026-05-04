package com.example.medicationmanagement.model

data class Notification(
    val notificationId: Int,
    val type: String,
    val title: String,
    val message: String,
    val targetRole: String?,
    val isRead: Boolean,
    val createdAt: String,
    val relatedEntityType: String?,
    val relatedEntityId: Int?
)
