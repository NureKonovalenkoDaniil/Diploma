package com.example.medicationmanagement.api

import com.example.medicationmanagement.model.IoTDevice
import com.example.medicationmanagement.model.LifecycleEvent
import com.example.medicationmanagement.model.Medicine
import com.example.medicationmanagement.model.Notification
import com.google.gson.annotations.SerializedName
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
data class CreateManagerRequest(val email: String, val password: String, val organizationId: String)

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

    @GET("api/auth/users")
    suspend fun getUsers(): Response<List<UserDto>>

    @DELETE("api/auth/users/{id}")
    suspend fun deleteUser(@Path("id") id: String): Response<Unit>

    @POST("api/auth/assign-role")
    suspend fun assignRole(@Body request: AssignRoleRequest): Response<Any>

    @POST("api/auth/create-manager")
    suspend fun createManager(@Body request: CreateManagerRequest): Response<Any>
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
    suspend fun updateMedicine(@Path("id") id: Int, @Body patchOperations: List<Map<String, Any?>>): Response<Medicine>

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

    @GET("api/iotdevice/{id}")
    suspend fun getDevice(@Path("id") id: String): Response<IoTDevice>

    @POST("api/iotdevice")
    suspend fun createDevice(@Body device: Map<String, Any>): Response<IoTDevice>

    @PATCH("api/iotdevice/setstatus/{id}")
    suspend fun setDeviceStatus(@Path("id") deviceId: String, @Query("isActive") isActive: Boolean): Response<Any>

    @DELETE("api/iotdevice/{id}")
    suspend fun deleteDevice(@Path("id") id: String): Response<Unit>
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

interface StorageConditionApi {
    @GET("api/iotdevice/conditions/{deviceId}")
    suspend fun getByDeviceId(@Path("deviceId") deviceId: String): Response<List<StorageConditionDto>>
}

// ──────────────────────────────────────────
// Storage Location API
// ──────────────────────────────────────────
data class StorageLocationDto(
    @SerializedName(value = "locationId", alternate = ["LocationId"])
    val locationId: Int,
    @SerializedName(value = "name", alternate = ["Name"])
    val name: String,
    @SerializedName(value = "address", alternate = ["Address"])
    val address: String?,
    @SerializedName(value = "locationType", alternate = ["LocationType"])
    val locationType: String,
    @SerializedName(value = "iotDeviceId", alternate = ["ioTDeviceId", "IoTDeviceId"])
    val iotDeviceId: String?,
    @SerializedName(value = "iotDeviceLocation", alternate = ["ioTDeviceLocation", "IoTDeviceLocation"])
    val iotDeviceLocation: String?
)

data class StorageConditionDto(
    val conditionID: Int,
    val temperature: Double,
    val humidity: Double,
    val timestamp: String,
    val deviceID: String,
    val deviceLocation: String?
)

interface StorageLocationApi {
    @GET("api/storagelocation")
    suspend fun getAll(): Response<List<StorageLocationDto>>

    @GET("api/storagelocation/{id}")
    suspend fun getById(@Path("id") id: Int): Response<StorageLocationDto>

    @POST("api/storagelocation")
    suspend fun create(@Body location: Map<String, Any?>): Response<StorageLocationDto>

    @PUT("api/storagelocation/{id}")
    suspend fun update(@Path("id") id: Int, @Body location: Map<String, Any?>): Response<StorageLocationDto>

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
    val id: String,
    val email: String,
    val userName: String?,
    val roles: List<String>,
    val organizationId: String
)

data class UpdateUserRoleRequest(
    val role: String
)

data class AssignRoleRequest(
    val email: String,
    val roleName: String
)

interface UserApi {
    @GET("api/auth/users")
    suspend fun getAll(): Response<List<UserDto>>

    @DELETE("api/auth/users/{id}")
    suspend fun delete(@Path("id") id: String): Response<Unit>

    @POST("api/auth/assign-role")
    suspend fun updateRole(@Body request: AssignRoleRequest): Response<Any>
}

// ──────────────────────────────────────────
// Audit Log API
// ──────────────────────────────────────────
data class AuditLogDto(
    val id: Int,
    val user: String?,
    val action: String?,
    val entityType: String?,
    val entityId: Int?,
    val timestamp: String?,
    val details: String?,
    val severity: String?
)

interface AuditLogApi {
    @GET("api/auditlog")
    suspend fun getAll(
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
        @Query("user") user: String? = null,
        @Query("action") action: String? = null
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
