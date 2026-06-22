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

    private val mockApis = java.util.concurrent.ConcurrentHashMap<Class<*>, Any>()

    fun <T : Any> registerMockApi(apiClass: Class<T>, mockInstance: T) {
        mockApis[apiClass] = mockInstance
    }

    fun clearMockApis() {
        mockApis.clear()
    }

    // ──────────────────────────────────────────
    // API Service Getters
    // ──────────────────────────────────────────

    fun getAuthApi(context: Context): AuthApi {
        return (mockApis[AuthApi::class.java] as? AuthApi) ?: getRetrofit(context).create(AuthApi::class.java)
    }

    fun getMedicineApi(context: Context): MedicineApi {
        return (mockApis[MedicineApi::class.java] as? MedicineApi) ?: getRetrofit(context).create(MedicineApi::class.java)
    }

    fun getMedicineActionsApi(context: Context): MedicineActionsApi {
        return (mockApis[MedicineActionsApi::class.java] as? MedicineActionsApi) ?: getRetrofit(context).create(MedicineActionsApi::class.java)
    }

    fun getLifecycleApi(context: Context): LifecycleApi {
        return (mockApis[LifecycleApi::class.java] as? LifecycleApi) ?: getRetrofit(context).create(LifecycleApi::class.java)
    }

    fun getIoTDeviceApi(context: Context): IoTDeviceApi {
        return (mockApis[IoTDeviceApi::class.java] as? IoTDeviceApi) ?: getRetrofit(context).create(IoTDeviceApi::class.java)
    }

    fun getNotificationApi(context: Context): NotificationApi {
        return (mockApis[NotificationApi::class.java] as? NotificationApi) ?: getRetrofit(context).create(NotificationApi::class.java)
    }

    fun getStorageLocationApi(context: Context): StorageLocationApi {
        return (mockApis[StorageLocationApi::class.java] as? StorageLocationApi) ?: getRetrofit(context).create(StorageLocationApi::class.java)
    }

    fun getStorageConditionApi(context: Context): StorageConditionApi {
        return (mockApis[StorageConditionApi::class.java] as? StorageConditionApi) ?: getRetrofit(context).create(StorageConditionApi::class.java)
    }

    fun getStorageIncidentApi(context: Context): StorageIncidentApi {
        return (mockApis[StorageIncidentApi::class.java] as? StorageIncidentApi) ?: getRetrofit(context).create(StorageIncidentApi::class.java)
    }

    fun getUserApi(context: Context): UserApi {
        return (mockApis[UserApi::class.java] as? UserApi) ?: getRetrofit(context).create(UserApi::class.java)
    }

    fun getAuditLogApi(context: Context): AuditLogApi {
        return (mockApis[AuditLogApi::class.java] as? AuditLogApi) ?: getRetrofit(context).create(AuditLogApi::class.java)
    }

    fun setRetrofitForTesting(retrofitInstance: Retrofit?) {
        retrofit = retrofitInstance
    }
}
