package com.example.medicationmanagement.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.medicationmanagement.api.RetrofitClient
import com.example.medicationmanagement.api.StorageLocationDto
import com.example.medicationmanagement.model.IoTDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EditDeviceViewModel(private val context: Context) : ViewModel() {
    private val iotDeviceApi = RetrofitClient.getIoTDeviceApi(context)
    private val storageLocationApi = RetrofitClient.getStorageLocationApi(context)

    private val _device = MutableStateFlow<IoTDevice?>(null)
    val device = _device.asStateFlow()

    private val _locations = MutableStateFlow<List<StorageLocationDto>>(emptyList())
    val locations = _locations.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving = _isSaving.asStateFlow()

    private val _success = MutableStateFlow(false)
    val success = _success.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    init {
        fetchLocations()
    }

    fun fetchLocations() {
        viewModelScope.launch {
            try {
                val response = storageLocationApi.getAll()
                if (response.isSuccessful) {
                    _locations.value = response.body() ?: emptyList()
                }
            } catch (e: Exception) {
                // Ignore or log error
            }
        }
    }

    fun loadDevice(deviceId: String) {
        if (_device.value != null) return // Already loaded

        _isLoading.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                val response = iotDeviceApi.getDevice(deviceId)
                if (response.isSuccessful) {
                    _device.value = response.body()
                } else {
                    _error.value = "Помилка завантаження деталей: ${response.code()}"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Помилка мережі"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateDevice(deviceId: String, patchOps: List<Map<String, Any?>>) {
        _isSaving.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                val response = iotDeviceApi.patchDevice(deviceId, patchOps)
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

class EditDeviceViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EditDeviceViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return EditDeviceViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
