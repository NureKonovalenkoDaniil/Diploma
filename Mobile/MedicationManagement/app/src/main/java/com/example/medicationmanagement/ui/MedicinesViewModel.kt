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
                    _error.value = context.getString(com.example.medicationmanagement.R.string.error_loading, response.code().toString())
                }
            } catch (e: Exception) {
                _error.value = context.getString(com.example.medicationmanagement.R.string.error_network, e.message ?: "")
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
                    _error.value = context.getString(com.example.medicationmanagement.R.string.error_issue_medicine_failed, response.code().toString())
                }
            } catch (e: Exception) {
                _error.value = context.getString(com.example.medicationmanagement.R.string.error_generic, e.message ?: "")
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
                    _error.value = context.getString(com.example.medicationmanagement.R.string.error_receive_medicine_failed, response.code().toString())
                }
            } catch (e: Exception) {
                _error.value = context.getString(com.example.medicationmanagement.R.string.error_generic, e.message ?: "")
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
                    _error.value = context.getString(com.example.medicationmanagement.R.string.error_dispose_medicine_failed, response.code().toString())
                }
            } catch (e: Exception) {
                _error.value = context.getString(com.example.medicationmanagement.R.string.error_generic, e.message ?: "")
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
                    _error.value = context.getString(com.example.medicationmanagement.R.string.error_delete_medicine_failed, response.code().toString())
                }
            } catch (e: Exception) {
                _error.value = context.getString(com.example.medicationmanagement.R.string.error_generic, e.message ?: "")
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun fetchLowStock() {
        _isLoading.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                val response = medicineApi.getLowStock()
                if (response.isSuccessful) {
                    _medicines.value = response.body() ?: emptyList()
                } else {
                    _error.value = context.getString(com.example.medicationmanagement.R.string.error_loading, response.code().toString())
                }
            } catch (e: Exception) {
                _error.value = context.getString(com.example.medicationmanagement.R.string.error_network, e.message ?: "")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun fetchExpiring() {
        _isLoading.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                val response = medicineApi.getExpiring()
                if (response.isSuccessful) {
                    _medicines.value = response.body() ?: emptyList()
                } else {
                    _error.value = context.getString(com.example.medicationmanagement.R.string.error_loading, response.code().toString())
                }
            } catch (e: Exception) {
                _error.value = context.getString(com.example.medicationmanagement.R.string.error_network, e.message ?: "")
            } finally {
                _isLoading.value = false
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
