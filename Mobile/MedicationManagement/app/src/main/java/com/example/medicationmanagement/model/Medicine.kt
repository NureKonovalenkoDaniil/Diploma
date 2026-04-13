package com.example.medicationmanagement.model

data class Medicine(
    val medicineID: Int,
    val name: String,
    val type: String,
    val expiryDate: String,
    val quantity: Int,
    val category: String
)
