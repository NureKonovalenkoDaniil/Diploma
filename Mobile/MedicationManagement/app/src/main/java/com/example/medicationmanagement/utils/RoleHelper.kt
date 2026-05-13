package com.example.medicationmanagement.utils

import android.content.Context
import com.auth0.jwt.JWT
import com.auth0.jwt.exceptions.JWTDecodeException

object RoleHelper {

    fun getCurrentRole(context: Context): String? {
        val token = TokenManager.getInstance(context).getToken() ?: return null
        return try {
            val decodedToken = JWT.decode(token)
            decodedToken.getClaim("role").asString()
                ?: decodedToken.getClaim("http://schemas.microsoft.com/ws/2008/06/identity/claims/role").asString()
        } catch (_: JWTDecodeException) {
            null
        } catch (_: Exception) {
            null
        }
    }

    fun getOrganizationId(context: Context): String? {
        val token = TokenManager.getInstance(context).getToken() ?: return null
        return try {
            val decodedToken = JWT.decode(token)
            decodedToken.getClaim("OrganizationId").asString()
        } catch (_: Exception) {
            null
        }
    }

    fun isAdmin(role: String?): Boolean = role == "Administrator"

    fun isManager(role: String?): Boolean = role == "Administrator" || role == "Manager"
    
    fun isUser(role: String?): Boolean = role == "Administrator" || role == "Manager" || role == "User"

    fun hasFullAccess(role: String?): Boolean = role == "Administrator" || role == "Manager" || role == "User"

    fun canManageMedicines(role: String?): Boolean = hasFullAccess(role)
}
