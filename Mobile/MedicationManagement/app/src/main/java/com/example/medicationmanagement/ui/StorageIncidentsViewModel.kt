package com.example.medicationmanagement.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.medicationmanagement.api.RetrofitClient
import com.example.medicationmanagement.api.StorageIncidentDto
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * StorageIncidentsViewModel — управління інцидентами зберігання з StateFlow для UI
 */
class StorageIncidentsViewModel(private val context: Context) : ViewModel() {

    private val storageIncidentApi = RetrofitClient.getStorageIncidentApi(context)

    private val _incidents = MutableStateFlow<List<StorageIncidentDto>>(emptyList())
    val incidents: StateFlow<List<StorageIncidentDto>> = _incidents.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun fetchIncidents() {
        _isLoading.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                val response = storageIncidentApi.getAll()
                if (response.isSuccessful) {
                    _incidents.value = response.body() ?: emptyList()
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

    fun resolveIncident(incidentId: Int, resolution: String) {
        viewModelScope.launch {
            try {
                val response = storageIncidentApi.resolve(incidentId, mapOf("comment" to resolution))
                if (response.isSuccessful) {
                    fetchIncidents()
                } else {
                    _error.value = context.getString(com.example.medicationmanagement.R.string.error_resolve_incident_failed, response.code().toString())
                }
            } catch (e: Exception) {
                _error.value = context.getString(com.example.medicationmanagement.R.string.error_generic, e.message ?: "")
            }
        }
    }

    fun deleteIncident(incidentId: Int) {
        viewModelScope.launch {
            try {
                val response = storageIncidentApi.delete(incidentId)
                if (response.isSuccessful) {
                    fetchIncidents()
                } else {
                    _error.value = context.getString(com.example.medicationmanagement.R.string.error_delete_incident_failed, response.code().toString())
                }
            } catch (e: Exception) {
                _error.value = context.getString(com.example.medicationmanagement.R.string.error_generic, e.message ?: "")
            }
        }
    }

    private var pollingJob: Job? = null

    fun startPolling() {
        if (pollingJob != null && pollingJob!!.isActive) {
            return
        }
        pollingJob = viewModelScope.launch {
            while (isActive) {
                try {
                    val response = storageIncidentApi.getAll()
                    if (response.isSuccessful) {
                        _incidents.value = response.body() ?: emptyList()
                        _error.value = null
                    }
                } catch (e: Exception) {
                    // Ignore
                }
                delay(5000) // Опитування кожні 5 секунд
            }
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    override fun onCleared() {
        stopPolling()
        super.onCleared()
    }
}

class StorageIncidentsViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StorageIncidentsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StorageIncidentsViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
