package com.example.medicationmanagement.ui

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.medicationmanagement.api.ApiClient
import com.example.medicationmanagement.api.IoTDeviceApi
import com.example.medicationmanagement.model.IoTDevice
import kotlinx.coroutines.launch

class SensorsViewModel(private val context: Context) : ViewModel() {

    private val api = ApiClient.createService<IoTDeviceApi>(context)

    private val _devices = MutableLiveData<List<IoTDevice>>()
    val devices: LiveData<List<IoTDevice>> get() = _devices

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> get() = _error

    fun fetchDevices() {
        _isLoading.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                val response = api.getDevices()
                if (response.isSuccessful) {
                    _devices.value = response.body() ?: emptyList()
                } else {
                    _error.value = "Помилка завантаження: ${response.code()}"
                }
            } catch (e: Exception) {
                _error.value = "Помилка мережі"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleDeviceStatus(deviceId: String, isActive: Boolean) {
        viewModelScope.launch {
            try {
                val response = api.setDeviceStatus(deviceId, !isActive)
                if (response.isSuccessful) {
                    fetchDevices() // Reload list
                } else {
                    _error.value = "Не вдалося змінити статус: ${response.code()}"
                }
            } catch (e: Exception) {
                _error.value = "Помилка мережі"
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
