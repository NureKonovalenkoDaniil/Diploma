package com.example.medicationmanagement.api

import android.content.Context
import com.example.medicationmanagement.utils.TokenManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * RetrofitClient - Singleton для налаштування Retrofit з усіма необхідними interceptors
 */
object RetrofitClient {
    private const val BASE_URL = "http://10.0.2.2:5001/" // Для Android Emulator

    @Volatile
    private var retrofit: Retrofit? = null

    fun getRetrofit(context: Context): Retrofit {
        return retrofit ?: synchronized(this) {
            retrofit ?: buildRetrofit(context).also { retrofit = it }
        }
    }

    private fun buildRetrofit(context: Context): Retrofit {
        val tokenManager = TokenManager.getInstance(context)

        // Logging Interceptor для дебагу
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        // OkHttp Client з interceptors
        val client = OkHttpClient.Builder()
            .addInterceptor(TokenInterceptor(tokenManager))
            .addInterceptor(ErrorInterceptor(context.applicationContext, tokenManager))
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // ──────────────────────────────────────────
    // API Service Getters
    // ──────────────────────────────────────────

    fun getAuthApi(context: Context): AuthApi {
        return getRetrofit(context).create(AuthApi::class.java)
    }

    fun getMedicineApi(context: Context): MedicineApi {
        return getRetrofit(context).create(MedicineApi::class.java)
    }

    fun getMedicineActionsApi(context: Context): MedicineActionsApi {
        return getRetrofit(context).create(MedicineActionsApi::class.java)
    }

    fun getLifecycleApi(context: Context): LifecycleApi {
        return getRetrofit(context).create(LifecycleApi::class.java)
    }

    fun getIoTDeviceApi(context: Context): IoTDeviceApi {
        return getRetrofit(context).create(IoTDeviceApi::class.java)
    }

    fun getNotificationApi(context: Context): NotificationApi {
        return getRetrofit(context).create(NotificationApi::class.java)
    }

    fun getStorageLocationApi(context: Context): StorageLocationApi {
        return getRetrofit(context).create(StorageLocationApi::class.java)
    }

    fun getStorageConditionApi(context: Context): StorageConditionApi {
        return getRetrofit(context).create(StorageConditionApi::class.java)
    }

    fun getStorageIncidentApi(context: Context): StorageIncidentApi {
        return getRetrofit(context).create(StorageIncidentApi::class.java)
    }

    fun getUserApi(context: Context): UserApi {
        return getRetrofit(context).create(UserApi::class.java)
    }

    fun getAuditLogApi(context: Context): AuditLogApi {
        return getRetrofit(context).create(AuditLogApi::class.java)
    }
}
