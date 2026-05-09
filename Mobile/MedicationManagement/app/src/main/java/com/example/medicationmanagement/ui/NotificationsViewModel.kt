package com.example.medicationmanagement.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.medicationmanagement.api.RetrofitClient
import com.example.medicationmanagement.model.Notification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NotificationsViewModel(private val context: Context) : ViewModel() {

    private val api = RetrofitClient.getNotificationApi(context)

    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications: StateFlow<List<Notification>> = _notifications.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun fetchNotifications() {
        _isLoading.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                val resp = api.getNotifications()
                if (resp.isSuccessful) {
                    _notifications.value = resp.body() ?: emptyList()
                } else {
                    _error.value = "Помилка: ${resp.code()}"
                }
            } catch (e: Exception) {
                _error.value = "Помилка мережі: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun markAsRead(notificationId: Int) {
        viewModelScope.launch {
            try {
                val resp = api.markAsRead(notificationId)
                if (resp.isSuccessful) {
                    fetchNotifications()
                } else {
                    _error.value = "Не вдалося позначити як прочитане"
                }
            } catch (e: Exception) {
                _error.value = "Помилка мережі: ${e.message}"
            }
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            try {
                val resp = api.markAllAsRead()
                if (resp.isSuccessful) {
                    fetchNotifications()
                } else {
                    _error.value = "Не вдалося позначити всі як прочитані"
                }
            } catch (e: Exception) {
                _error.value = "Помилка мережі: ${e.message}"
            }
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
package com.example.medicationmanagement.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.medicationmanagement.api.RetrofitClient
import com.example.medicationmanagement.model.Notification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * NotificationsViewModel — управління сповіщеннями з StateFlow для UI
 */
class NotificationsViewModel(private val context: Context) : ViewModel() {

    private val notificationApi = RetrofitClient.getNotificationApi(context)

    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications: StateFlow<List<Notification>> = _notifications.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

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
                    _error.value = "Помилка завантаження: ${response.code()}"
                }
            } catch (e: Exception) {
                _error.value = "Помилка мережі: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun markAsRead(notificationId: Int) {
        viewModelScope.launch {
            try {
                val response = notificationApi.markAsRead(notificationId)
                if (response.isSuccessful) {
                    fetchNotifications() // Перезавантажити список
                } else {
                    _error.value = "Помилка позначення: ${response.code()}"
                }
            } catch (e: Exception) {
                _error.value = "Помилка: ${e.message}"
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
                    _error.value = "Помилка позначення: ${response.code()}"
                }
            } catch (e: Exception) {
                _error.value = "Помилка: ${e.message}"
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
