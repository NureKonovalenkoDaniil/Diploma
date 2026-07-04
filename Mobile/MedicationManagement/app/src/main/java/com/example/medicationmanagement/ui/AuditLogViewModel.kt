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

    fun fetchLogs(
        from: String? = null,
        to: String? = null,
        user: String? = null,
        action: String? = null
    ) {
        _isLoading.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                val response = auditLogApi.getAll(
                    from = if (from.isNullOrBlank()) null else from,
                    to = if (to.isNullOrBlank()) null else to,
                    user = if (user.isNullOrBlank()) null else user,
                    action = if (action.isNullOrBlank()) null else action
                )
                if (response.isSuccessful) {
                    _logs.value = response.body()?.sortedByDescending { parseDate(it.timestamp) } ?: emptyList()
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
