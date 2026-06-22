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

class EditMedicineViewModel(private val context: Context) : ViewModel() {
    private val medicineApi = RetrofitClient.getMedicineApi(context)

    private val _medicine = MutableStateFlow<Medicine?>(null)
    val medicine = _medicine.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving = _isSaving.asStateFlow()

    private val _success = MutableStateFlow(false)
    val success = _success.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    fun loadMedicine(medicineId: Int) {
        if (_medicine.value != null) return // Already loaded

        _isLoading.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                val response = medicineApi.getMedicine(medicineId)
                if (response.isSuccessful) {
                    _medicine.value = response.body()
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

    fun updateMedicine(medicineId: Int, patchOps: List<Map<String, Any?>>) {
        _isSaving.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                val response = medicineApi.updateMedicine(medicineId, patchOps)
                if (response.isSuccessful) {
                    _success.value = true
                } else {
                    _error.value = "Помилка збереження змін"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Помилка мережі"
            } finally {
                _isSaving.value = false
            }
        }
    }
}

class EditMedicineViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EditMedicineViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return EditMedicineViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
