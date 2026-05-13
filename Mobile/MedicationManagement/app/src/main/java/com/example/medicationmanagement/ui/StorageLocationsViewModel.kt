package com.example.medicationmanagement.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.medicationmanagement.api.RetrofitClient
import com.example.medicationmanagement.api.StorageLocationDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StorageLocationsViewModel(private val context: Context) : ViewModel() {

    private val storageLocationApi = RetrofitClient.getStorageLocationApi(context)

    private val _locations = MutableStateFlow<List<StorageLocationDto>>(emptyList())
    val locations: StateFlow<List<StorageLocationDto>> = _locations.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun fetchLocations() {
        _isLoading.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                val response = storageLocationApi.getAll()
                if (response.isSuccessful) {
                    _locations.value = response.body() ?: emptyList()
                } else {
                    _error.value = "Помилка завантаження: ${response.code()}"
                }
            } catch (e: Exception) {
                _error.value = "Помилка мережі: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    suspend fun createLocation(payload: Map<String, Any?>): Boolean {
        return try {
            val response = storageLocationApi.create(payload)
            if (response.isSuccessful) {
                fetchLocations()
                true
            } else {
                _error.value = "Помилка створення: ${response.code()}"
                false
            }
        } catch (e: Exception) {
            _error.value = "Помилка мережі: ${e.message}"
            false
        }
    }

    suspend fun updateLocation(locationId: Int, payload: Map<String, Any?>): Boolean {
        return try {
            val response = storageLocationApi.update(locationId, payload)
            if (response.isSuccessful) {
                fetchLocations()
                true
            } else {
                _error.value = "Помилка оновлення: ${response.code()}"
                false
            }
        } catch (e: Exception) {
            _error.value = "Помилка мережі: ${e.message}"
            false
        }
    }

    suspend fun deleteLocation(locationId: Int): Boolean {
        return try {
            val response = storageLocationApi.delete(locationId)
            if (response.isSuccessful) {
                fetchLocations()
                true
            } else {
                _error.value = "Помилка видалення: ${response.code()}"
                false
            }
        } catch (e: Exception) {
            _error.value = "Помилка мережі: ${e.message}"
            false
        }
    }
}

class StorageLocationsViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StorageLocationsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StorageLocationsViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}