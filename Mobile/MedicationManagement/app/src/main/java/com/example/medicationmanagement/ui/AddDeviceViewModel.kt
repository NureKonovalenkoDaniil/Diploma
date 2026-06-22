package com.example.medicationmanagement.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.medicationmanagement.api.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AddDeviceViewModel(private val context: Context) : ViewModel() {
    private val iotDeviceApi = RetrofitClient.getIoTDeviceApi(context)

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _success = MutableStateFlow(false)
    val success = _success.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    var deviceId: String = ""
    var location: String = ""

    fun addDevice(id: String, loc: String, defaultType: String) {
        deviceId = id
        location = loc
        _isLoading.value = true
        _error.value = null

        val deviceData = mapOf(
            "deviceID" to deviceId,
            "location" to location,
            "type" to defaultType,
            "parameters" to "{}",
            "isActive" to true,
            "minTemperature" to 2.0f,
            "maxTemperature" to 8.0f,
            "minHumidity" to 30.0f,
            "maxHumidity" to 60.0f
        )

        viewModelScope.launch {
            try {
                val response = iotDeviceApi.createDevice(deviceData)
                if (response.isSuccessful) {
                    _success.value = true
                } else {
                    _error.value = "Пристрій вже прив'язаний або некоректні дані"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Помилка мережі"
            } finally {
                _isLoading.value = false
            }
        }
    }
}

class AddDeviceViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AddDeviceViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AddDeviceViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
