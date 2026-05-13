package com.example.medicationmanagement.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.medicationmanagement.api.CreateManagerRequest
import com.example.medicationmanagement.api.RetrofitClient
import com.example.medicationmanagement.api.UserDto
import com.example.medicationmanagement.utils.RoleHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UsersViewModel — управління користувачами системи з RBAC функціональністю (для адміністраторів)
 */
class UsersViewModel(private val context: Context) : ViewModel() {

    private val userApi = RetrofitClient.getUserApi(context)
    private val authApi = RetrofitClient.getAuthApi(context)

    private val _users = MutableStateFlow<List<UserDto>>(emptyList())
    val users: StateFlow<List<UserDto>> = _users.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun fetchUsers() {
        _isLoading.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                val response = userApi.getAll()
                if (response.isSuccessful) {
                    _users.value = response.body() ?: emptyList()
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

    fun createManager(email: String, password: String) {
        val organizationId = RoleHelper.getOrganizationId(context)
        if (organizationId.isNullOrBlank()) {
            _error.value = "Не вдалося визначити OrganizationId з токена"
            return
        }

        viewModelScope.launch {
            try {
                val response = authApi.createManager(CreateManagerRequest(email, password, organizationId))
                if (response.isSuccessful) {
                    fetchUsers()
                } else {
                    _error.value = "Не вдалося створити менеджера: ${response.code()}"
                }
            } catch (e: Exception) {
                _error.value = "Помилка: ${e.message}"
            }
        }
    }

    fun deactivateUser(userId: String) {
        viewModelScope.launch {
            try {
                val response = userApi.delete(userId)
                if (response.isSuccessful) {
                    fetchUsers()
                } else {
                    _error.value = "Не вдалося деактивувати користувача: ${response.code()}"
                }
            } catch (e: Exception) {
                _error.value = "Помилка: ${e.message}"
            }
        }
    }
}

class UsersViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UsersViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return UsersViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
