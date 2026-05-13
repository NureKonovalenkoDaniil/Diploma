package com.example.medicationmanagement.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.medicationmanagement.api.RetrofitClient
import com.example.medicationmanagement.api.AuditLogDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * AuditLogViewModel — управління журналом аудиту системи з StateFlow для UI
 */
class AuditLogViewModel(private val context: Context) : ViewModel() {

    private val auditLogApi = RetrofitClient.getAuditLogApi(context)

    private val _logs = MutableStateFlow<List<AuditLogDto>>(emptyList())
    val logs: StateFlow<List<AuditLogDto>> = _logs.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _selectedFilter = MutableStateFlow<String?>(null)
    val selectedFilter: StateFlow<String?> = _selectedFilter.asStateFlow()

    private var allLogs: List<AuditLogDto> = emptyList()

    fun fetchLogs() {
        _isLoading.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                val response = auditLogApi.getAll()
                if (response.isSuccessful) {
                    allLogs = response.body()?.sortedByDescending { parseDate(it.timestamp) } ?: emptyList()
                    applyFilter()
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

    fun filterByAction(entityType: String) {
        _selectedFilter.value = entityType
        applyFilter()
    }

    fun clearFilter() {
        _selectedFilter.value = null
        applyFilter()
    }

    private fun applyFilter() {
        val filter = _selectedFilter.value
        _logs.value = if (filter.isNullOrBlank()) {
            allLogs
        } else {
            allLogs.filter { it.entityType?.equals(filter, ignoreCase = true) == true }
        }
    }

    private fun parseDate(dateStr: String?): Long {
        if (dateStr.isNullOrBlank()) return 0L
        try {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            parser.timeZone = java.util.TimeZone.getTimeZone("UTC")
            return parser.parse(dateStr)?.time ?: 0L
        } catch (e: Exception) {
            return 0L
        }
    }
}

class AuditLogViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuditLogViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuditLogViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
