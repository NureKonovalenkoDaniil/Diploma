package com.example.medicationmanagement.api

import android.content.Context
import com.example.medicationmanagement.utils.TokenManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    private const val BASE_URL = "http://10.0.2.2:5000/" // Емулятор Android

    private var retrofit: Retrofit? = null

    fun getClient(context: Context): Retrofit {
        if (retrofit == null) {
            val tokenManager = TokenManager(context)

            val authInterceptor = okhttp3.Interceptor { chain ->
                val requestBuilder = chain.request().newBuilder()
                tokenManager.getToken()?.let {
                    requestBuilder.addHeader("Authorization", "Bearer $it")
                }
                chain.proceed(requestBuilder.build())
            }

            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(authInterceptor)
                .addInterceptor(loggingInterceptor)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()

            retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
        return retrofit!!
    }

    inline fun <reified T> createService(context: Context): T {
        return getClient(context).create(T::class.java)
    }
}
