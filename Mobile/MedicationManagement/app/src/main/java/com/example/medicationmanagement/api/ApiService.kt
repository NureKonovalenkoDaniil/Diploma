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

// ──────────────────────────────────────────
// Storage Location API
// ──────────────────────────────────────────
data class StorageLocationDto(
    val id: Int,
    val name: String,
    val deviceId: String?,
    val currentCondition: StorageConditionDto?
)

data class StorageConditionDto(
    val temperature: Double?,
    val humidity: Double?,
    val timestamp: String
)

interface StorageLocationApi {
    @GET("api/storagelocation")
    suspend fun getAll(): Response<List<StorageLocationDto>>

    @GET("api/storagelocation/{id}")
    suspend fun getById(@Path("id") id: Int): Response<StorageLocationDto>

    @POST("api/storagelocation")
    suspend fun create(@Body location: Map<String, Any>): Response<StorageLocationDto>

    @PUT("api/storagelocation/{id}")
    suspend fun update(@Path("id") id: Int, @Body location: Map<String, Any>): Response<StorageLocationDto>

    @DELETE("api/storagelocation/{id}")
    suspend fun delete(@Path("id") id: Int): Response<Unit>
}

// ──────────────────────────────────────────
// Storage Incident API
// ──────────────────────────────────────────
data class StorageIncidentDto(
    val id: Int,
    val storageLocationId: Int,
    val incidentType: String,
    val severity: String,
    val description: String,
    val detectedAt: String,
    val resolvedAt: String?,
    val isResolved: Boolean
)

interface StorageIncidentApi {
    @GET("api/storageincident")
    suspend fun getAll(): Response<List<StorageIncidentDto>>

    @GET("api/storageincident/{id}")
    suspend fun getById(@Path("id") id: Int): Response<StorageIncidentDto>

    @DELETE("api/storageincident/{id}")
    suspend fun delete(@Path("id") id: Int): Response<Unit>

    @POST("api/storageincident/{id}/resolve")
    suspend fun resolve(@Path("id") id: Int, @Body comment: Map<String, String>): Response<StorageIncidentDto>
}

// ──────────────────────────────────────────
// User API
// ──────────────────────────────────────────
data class UserDto(
    val id: Int,
    val email: String,
    val role: String,
    val isEmailConfirmed: Boolean
)

data class UpdateUserRoleRequest(
    val role: String
)

interface UserApi {
    @GET("api/user")
    suspend fun getAll(): Response<List<UserDto>>

    @GET("api/user/{id}")
    suspend fun getById(@Path("id") id: Int): Response<UserDto>

    @DELETE("api/user/{id}")
    suspend fun delete(@Path("id") id: Int): Response<Unit>

    @PUT("api/user/{id}/role")
    suspend fun updateRole(@Path("id") id: Int, @Body request: UpdateUserRoleRequest): Response<UserDto>
}

// ──────────────────────────────────────────
// Audit Log API
// ──────────────────────────────────────────
data class AuditLogDto(
    val id: Int,
    val userId: Int?,
    val userEmail: String?,
    val action: String,
    val entityType: String,
    val entityId: Int?,
    val timestamp: String,
    val details: String?
)

interface AuditLogApi {
    @GET("api/auditlog")
    suspend fun getAll(
        @Query("entityType") entityType: String? = null,
        @Query("userId") userId: Int? = null
    ): Response<List<AuditLogDto>>

    @GET("api/auditlog/{id}")
    suspend fun getById(@Path("id") id: Int): Response<AuditLogDto>
}

// ──────────────────────────────────────────
// Medicine Actions (Quick Actions)
// ──────────────────────────────────────────
data class QuantityRequest(
    val quantity: Int,
    val description: String? = null
)

data class MoveRequest(
    val targetLocationId: Int,
    val description: String? = null
)

interface MedicineActionsApi {
    @POST("api/medicine/{id}/receive")
    suspend fun receive(@Path("id") id: Int, @Body request: QuantityRequest): Response<Medicine>

    @POST("api/medicine/{id}/issue")
    suspend fun issue(@Path("id") id: Int, @Body request: QuantityRequest): Response<Medicine>

    @POST("api/medicine/{id}/dispose")
    suspend fun dispose(@Path("id") id: Int, @Body request: QuantityRequest): Response<Medicine>

    @POST("api/medicine/{id}/move")
    suspend fun move(@Path("id") id: Int, @Body request: MoveRequest): Response<Medicine>
}
