package com.example.medicationmanagement.api

import com.example.medicationmanagement.utils.TokenManager
import okhttp3.Interceptor
import okhttp3.Response

/**
 * TokenInterceptor автоматично додає Bearer токен до всіх HTTP запитів
 */
class TokenInterceptor(private val tokenManager: TokenManager) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val token = tokenManager.getToken()

        val newRequest = if (token != null) {
            originalRequest.newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            originalRequest
        }

        return chain.proceed(newRequest)
    }
}
