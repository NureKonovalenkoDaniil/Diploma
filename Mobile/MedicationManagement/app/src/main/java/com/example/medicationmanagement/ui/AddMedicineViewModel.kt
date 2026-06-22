package com.example.medicationmanagement.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.medicationmanagement.api.RetrofitClient
import com.example.medicationmanagement.model.Medicine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AddMedicineViewModel(private val context: Context) : ViewModel() {
    private val medicineApi = RetrofitClient.getMedicineApi(context)

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _success = MutableStateFlow(false)
    val success = _success.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    fun createMedicine(medicine: Medicine) {
        _isLoading.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                val response = medicineApi.createMedicine(medicine)
                if (response.isSuccessful) {
                    _success.value = true
                } else {
                    _error.value = "Помилка створення препарату: ${response.code()}"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Помилка мережі"
            } finally {
                _isLoading.value = false
            }
        }
    }
}

class AddMedicineViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AddMedicineViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AddMedicineViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
