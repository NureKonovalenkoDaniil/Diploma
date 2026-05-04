package com.example.medicationmanagement.ui

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.medicationmanagement.api.ApiClient
import com.example.medicationmanagement.api.NotificationApi
import com.example.medicationmanagement.model.Notification
import kotlinx.coroutines.launch

class NotificationsViewModel(private val context: Context) : ViewModel() {

    private val api = ApiClient.createService<NotificationApi>(context)

    private val _notifications = MutableLiveData<List<Notification>>()
    val notifications: LiveData<List<Notification>> get() = _notifications

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> get() = _error

    fun fetchNotifications() {
        _isLoading.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                val response = api.getNotifications()
                if (response.isSuccessful) {
                    val list = response.body() ?: emptyList()
                    // Сортуємо: спершу непрочитані, потім за датою спадання
                    _notifications.value = list.sortedWith(compareBy({ it.isRead }, { -parseDate(it.createdAt) }))
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

    fun markAsRead(notificationId: Int) {
        viewModelScope.launch {
            try {
                val response = api.markAsRead(notificationId)
                if (response.isSuccessful) {
                    fetchNotifications() // Перезавантажити список
                }
            } catch (e: Exception) {
                // Ignore silent errors for marking as read
            }
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            try {
                val response = api.markAllAsRead()
                if (response.isSuccessful) {
                    fetchNotifications()
                }
            } catch (e: Exception) {
                // Ignore silent errors
            }
        }
    }

    private fun parseDate(dateStr: String): Long {
        try {
            val parser = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
            return parser.parse(dateStr)?.time ?: 0L
        } catch (e: Exception) {
            return 0L
        }
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
