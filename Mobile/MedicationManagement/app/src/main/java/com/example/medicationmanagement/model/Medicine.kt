package com.example.medicationmanagement.model

data class Medicine(
    val medicineID: Int = 0,
    val name: String = "",
    val type: String = "",
    val expiryDate: String = "",
    val quantity: Int = 0,
    val category: String = "",
    val status: String = "Active",
    val manufacturer: String? = null,
    val batchNumber: String? = null,
    val description: String? = null,
    val minStorageTemp: Double? = null,
    val maxStorageTemp: Double? = null,
    val minStorageHumidity: Double? = null,
    val maxStorageHumidity: Double? = null,
    val storageLocationId: Int? = null,
    val storageLocationName: String? = null
)
