package com.example.medicationmanagement.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.medicationmanagement.api.RetrofitClient
import com.example.medicationmanagement.model.IoTDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * SensorsViewModel — управління IoT датчиками та пристроями з StateFlow для UI
 */
class SensorsViewModel(private val context: Context) : ViewModel() {

    private val iotDeviceApi = RetrofitClient.getIoTDeviceApi(context)

    private val _devices = MutableStateFlow<List<IoTDevice>>(emptyList())
    val devices: StateFlow<List<IoTDevice>> = _devices.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun fetchDevices() {
        _isLoading.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                val response = iotDeviceApi.getDevices()
                if (response.isSuccessful) {
                    _devices.value = response.body() ?: emptyList()
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

    fun toggleDeviceStatus(deviceId: String, isActive: Boolean) {
        viewModelScope.launch {
            try {
                val response = iotDeviceApi.setDeviceStatus(deviceId, !isActive)
                if (response.isSuccessful) {
                    fetchDevices() // Reload list
                } else {
                    _error.value = context.getString(com.example.medicationmanagement.R.string.error_toggle_device_failed, response.code().toString())
                }
            } catch (e: Exception) {
                _error.value = context.getString(com.example.medicationmanagement.R.string.error_network, e.message ?: "")
            }
        }
    }
}

class SensorsViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SensorsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SensorsViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
