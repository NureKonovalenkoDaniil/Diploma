package com.example.medicationmanagement.ui

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.medicationmanagement.api.ApiClient
import com.example.medicationmanagement.api.LifecycleApi
import com.example.medicationmanagement.api.LifecycleEventRequest
import com.example.medicationmanagement.api.MedicineApi
import com.example.medicationmanagement.model.Medicine
import kotlinx.coroutines.launch

class MedicinesViewModel(private val context: Context) : ViewModel() {

    private val medicineApi = ApiClient.createService<MedicineApi>(context)
    private val lifecycleApi = ApiClient.createService<LifecycleApi>(context)

    private val _medicines = MutableLiveData<List<Medicine>>()
    val medicines: LiveData<List<Medicine>> get() = _medicines

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> get() = _error

    fun fetchMedicines() {
        _isLoading.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                val response = medicineApi.getMedicines()
                if (response.isSuccessful) {
                    _medicines.value = response.body() ?: emptyList()
                } else {
                    _error.value = "Failed to load: ${response.code()}"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Network error"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun consumeMedicine(medicine: Medicine) {
        if (medicine.quantity <= 0) return

        viewModelScope.launch {
            try {
                // 1. PATCH quantity (-1)
                // JSON Patch protocol
                val patchBody = listOf(
                    mapOf(
                        "op" to "replace",
                        "path" to "/quantity",
                        "value" to (medicine.quantity - 1)
                    )
                )
                val patchResponse = medicineApi.updateMedicine(medicine.medicineID, patchBody)

                if (patchResponse.isSuccessful) {
                    // 2. POST Lifecycle Event
                    val event = LifecycleEventRequest(
                        medicineId = medicine.medicineID,
                        eventType = "Dispensed",
                        quantity = 1,
                        description = "Вжито 1 шт. через мобільний додаток"
                    )
                    lifecycleApi.addEvent(event)
                    
                    // Reload list
                    fetchMedicines()
                } else {
                    _error.value = "Failed to update quantity: ${patchResponse.code()}"
                }
            } catch (e: Exception) {
                _error.value = "Error: ${e.message}"
            }
        }
    }
}

class MedicinesViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MedicinesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MedicinesViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
