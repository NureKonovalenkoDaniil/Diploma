package com.example.medicationmanagement.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.medicationmanagement.api.QuantityRequest
import com.example.medicationmanagement.api.RetrofitClient
import com.example.medicationmanagement.model.Medicine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * MedicinesViewModel — управління препаратами з StateFlow для UI
 */
class MedicinesViewModel(private val context: Context) : ViewModel() {

    private val medicineApi = RetrofitClient.getMedicineApi(context)
    private val medicineActionsApi = RetrofitClient.getMedicineActionsApi(context)

    private val _medicines = MutableStateFlow<List<Medicine>>(emptyList())
    val medicines: StateFlow<List<Medicine>> = _medicines.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun fetchMedicines() {
        _isLoading.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                val response = medicineApi.getMedicines()
                if (response.isSuccessful) {
                    _medicines.value = response.body() ?: emptyList()
                } else {
                    _error.value = "Помилка завантаження: ${response.code()}"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Помилка мережі"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Quick Action: Issue (Вжити) — зменшити залишок препарату
     */
    fun issueMedicine(medicineId: Int, quantity: Int) {
        viewModelScope.launch {
            try {
                val response = medicineActionsApi.issue(medicineId, QuantityRequest(quantity))
                if (response.isSuccessful) {
                    fetchMedicines() // Перезавантажити список
                } else {
                    _error.value = "Помилка вжиття: ${response.code()}"
                }
            } catch (e: Exception) {
                _error.value = "Помилка: ${e.message}"
            }
        }
    }

    /**
     * Quick Action: Receive (Видати) — збільшити залишок препарату
     */
    fun receiveMedicine(medicineId: Int, quantity: Int) {
        viewModelScope.launch {
            try {
                val response = medicineActionsApi.receive(medicineId, QuantityRequest(quantity))
                if (response.isSuccessful) {
                    fetchMedicines()
                } else {
                    _error.value = "Помилка видачі: ${response.code()}"
                }
            } catch (e: Exception) {
                _error.value = "Помилка: ${e.message}"
            }
        }
    }

    /**
     * Quick Action: Dispose (Утилізувати) — видалити препарат
     */
    fun disposeMedicine(medicineId: Int, quantity: Int) {
        viewModelScope.launch {
            try {
                val response = medicineActionsApi.dispose(medicineId, QuantityRequest(quantity))
                if (response.isSuccessful) {
                    fetchMedicines()
                } else {
                    _error.value = "Помилка утилізації: ${response.code()}"
                }
            } catch (e: Exception) {
                _error.value = "Помилка: ${e.message}"
            }
        }
    }

    fun deleteMedicine(medicineId: Int) {
        viewModelScope.launch {
            try {
                val response = medicineApi.deleteMedicine(medicineId)
                if (response.isSuccessful) {
                    fetchMedicines()
                } else {
                    _error.value = "Помилка видалення: ${response.code()}"
                }
            } catch (e: Exception) {
                _error.value = "Помилка: ${e.message}"
            }
        }
    }

    fun clearError() {
        _error.value = null
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
