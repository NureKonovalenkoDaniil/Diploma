package com.example.medicationmanagement.api

import android.content.Context
import android.content.Intent
import com.example.medicationmanagement.LoginActivity
import com.example.medicationmanagement.utils.TokenManager
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val context: Context) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val requestBuilder = chain.request().newBuilder()
        val tokenManager = TokenManager.getInstance(context)
        
        tokenManager.getToken()?.let { token ->
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }
        
        val response = chain.proceed(requestBuilder.build())
        
        // Глобальна обробка 401 Unauthorized
        if (response.code == 401) {
            tokenManager.clearToken()
            val intent = Intent(context, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            context.startActivity(intent)
        }
        
        return response
    }
}
