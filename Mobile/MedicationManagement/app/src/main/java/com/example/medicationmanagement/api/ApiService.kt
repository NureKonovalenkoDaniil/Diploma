package com.example.medicationmanagement.api

import com.example.medicationmanagement.model.IoTDevice
import com.example.medicationmanagement.model.LifecycleEvent
import com.example.medicationmanagement.model.Medicine
import com.example.medicationmanagement.model.Notification
import retrofit2.Response
import retrofit2.http.*

// ──────────────────────────────────────────
// Auth Models
// ──────────────────────────────────────────
data class LoginRequest(val email: String, val password: String)
data class LoginResponse(val token: String)
data class RegisterRequest(val email: String, val password: String)
data class ConfirmEmailRequest(val email: String, val code: String)
data class ResendConfirmationRequest(val email: String)

// ──────────────────────────────────────────
// Auth API
// ──────────────────────────────────────────
interface AuthApi {
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<Any>

    @POST("api/auth/confirm-email")
    suspend fun confirmEmail(@Body request: ConfirmEmailRequest): Response<Any>

    @POST("api/auth/resend-confirmation")
    suspend fun resendConfirmation(@Body request: ResendConfirmationRequest): Response<Any>
}

// ──────────────────────────────────────────
// Medicine API
// ──────────────────────────────────────────
interface MedicineApi {
    @GET("api/medicine")
    suspend fun getMedicines(): Response<List<Medicine>>

    @GET("api/medicine/{id}")
    suspend fun getMedicine(@Path("id") id: Int): Response<Medicine>

    @POST("api/medicine")
    suspend fun createMedicine(@Body medicine: Medicine): Response<Medicine>

    @PATCH("api/medicine/{id}")
    suspend fun updateMedicine(@Path("id") id: Int, @Body patchOperations: List<Map<String, Any>>): Response<Medicine>

    @DELETE("api/medicine/{id}")
    suspend fun deleteMedicine(@Path("id") id: Int): Response<Unit>
}

// ──────────────────────────────────────────
// Lifecycle API (Medicine Diary)
// ──────────────────────────────────────────
data class LifecycleEventRequest(
    val medicineId: Int,
    val eventType: String,
    val quantity: Int,
    val description: String? = null
)

interface LifecycleApi {
    @POST("api/medicinelifecycle")
    suspend fun addEvent(@Body event: LifecycleEventRequest): Response<Any>

    @GET("api/medicinelifecycle/medicine/{id}")
    suspend fun getEventsByMedicineId(@Path("id") medicineId: Int): Response<List<LifecycleEvent>>
}

// ──────────────────────────────────────────
// IoT Device API
// ──────────────────────────────────────────
interface IoTDeviceApi {
    @GET("api/iotdevice")
    suspend fun getDevices(): Response<List<IoTDevice>>

    @POST("api/iotdevice")
    suspend fun createDevice(@Body device: Map<String, Any>): Response<IoTDevice>

    @PATCH("api/iotdevice/setstatus/{id}")
    suspend fun setDeviceStatus(@Path("id") deviceId: String, @Query("isActive") isActive: Boolean): Response<Any>
}

// ──────────────────────────────────────────
// Notification API
// ──────────────────────────────────────────
interface NotificationApi {
    @GET("api/notification")
    suspend fun getNotifications(): Response<List<Notification>>

    @PATCH("api/notification/{id}/read")
    suspend fun markAsRead(@Path("id") notificationId: Int): Response<Any>

    @PATCH("api/notification/read-all")
    suspend fun markAllAsRead(): Response<Any>
}
