package com.example.medicationmanagement.api

import android.content.Context
import android.content.Intent
import com.example.medicationmanagement.LoginActivity
import com.example.medicationmanagement.utils.TokenManager
import okhttp3.Interceptor
import okhttp3.Response

/**
 * ErrorInterceptor для глобальної обробки помилок HTTP
 * Особливо обробляє 401 Unauthorized - автоматично очищує токен та редиректить на LoginActivity
 */
class ErrorInterceptor(private val context: Context, private val tokenManager: TokenManager) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())

        // Обробка 401 Unauthorized
        if (response.code == 401) {
            // Очистити токен
            tokenManager.clearToken()

            // Редиректити на Login
            val intent = Intent(context, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            context.startActivity(intent)
        }

        return response
    }
}
