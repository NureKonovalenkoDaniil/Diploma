package com.example.medicationmanagement.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.medicationmanagement.api.RetrofitClient
import com.example.medicationmanagement.model.Notification
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * NotificationsViewModel — управління сповіщеннями з StateFlow для UI
 * Включає автоматичне опитування кожні 30 секунд
 */
class NotificationsViewModel(private val context: Context) : ViewModel() {

    private val notificationApi = RetrofitClient.getNotificationApi(context)

    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications: StateFlow<List<Notification>> = _notifications.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var pollingJob: Job? = null

    fun fetchNotifications() {
        _isLoading.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                val response = notificationApi.getNotifications()
                if (response.isSuccessful) {
                    val list = response.body() ?: emptyList()
                    // Сортуємо: спершу непрочитані, потім за датою спадання
                    _notifications.value = list.sortedWith(
                        compareBy<Notification>({ it.isRead }, { -parseDate(it.createdAt) })
                    )
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

    fun startPolling() {
        if (pollingJob != null && pollingJob!!.isActive) {
            return // Polling already running
        }

        pollingJob = viewModelScope.launch {
            while (isActive) {
                try {
                    val response = notificationApi.getNotifications()
                    if (response.isSuccessful) {
                        val list = response.body() ?: emptyList()
                        _notifications.value = list.sortedWith(
                            compareBy<Notification>({ it.isRead }, { -parseDate(it.createdAt) })
                        )
                        _error.value = null
                    }
                } catch (e: Exception) {
                    // Ignore errors in polling to avoid spamming errors
                }
                delay(5000) // Poll every 5 seconds
            }
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    fun markAsRead(notificationId: Int) {
        viewModelScope.launch {
            try {
                val response = notificationApi.markAsRead(notificationId)
                if (response.isSuccessful) {
                    fetchNotifications() // Перезавантажити список
                } else {
                    _error.value = context.getString(com.example.medicationmanagement.R.string.error_mark_notification_failed, response.code().toString())
                }
            } catch (e: Exception) {
                _error.value = context.getString(com.example.medicationmanagement.R.string.error_generic, e.message ?: "")
            }
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            try {
                val response = notificationApi.markAllAsRead()
                if (response.isSuccessful) {
                    fetchNotifications()
                } else {
                    _error.value = context.getString(com.example.medicationmanagement.R.string.error_mark_notification_failed, response.code().toString())
                }
            } catch (e: Exception) {
                _error.value = context.getString(com.example.medicationmanagement.R.string.error_generic, e.message ?: "")
            }
        }
    }

    private fun parseDate(dateStr: String): Long {
        try {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            return parser.parse(dateStr)?.time ?: 0L
        } catch (e: Exception) {
            return 0L
        }
    }

    override fun onCleared() {
        stopPolling()
        super.onCleared()
    }
}

class NotificationsViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NotificationsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NotificationsViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
