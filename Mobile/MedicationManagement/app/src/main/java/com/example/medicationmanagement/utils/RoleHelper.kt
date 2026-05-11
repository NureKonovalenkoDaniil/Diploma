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

    // ────────────────────────────────────────────────────────
    // RBAC: Administrator | Manager | User | Device
    // ────────────────────────────────────────────────────────

    /**
     * Administrator має весь доступ
     */
    fun isAdmin(role: String?): Boolean = role == "Administrator"

    /**
     * Manager та Administrator можуть управляти ресурсами (окрім користувачів та логів)
     */
    fun isManager(role: String?): Boolean = role == "Administrator" || role == "Manager"

    /**
     * User та Manager можуть переглядати та редагувати препарати, датчики, інциденти
     */
    fun canManageMedicines(role: String?): Boolean = role in listOf("Administrator", "Manager", "User")

    /**
     * Тільки Administrator може створювати/видаляти/редагувати користувачів
     */
    fun canManageUsers(role: String?): Boolean = isAdmin(role)

    /**
     * Тільки Administrator може переглядати журнал аудиту
     */
    fun canViewAuditLog(role: String?): Boolean = isAdmin(role)

    /**
     * User та Manager можуть виконувати Quick Actions
     */
    fun canPerformQuickActions(role: String?): Boolean = canManageMedicines(role)

    /**
     * Доступ до списку користувачів мають усі людські ролі; створення менеджерів лишається тільки для Administrator
     */
    fun canViewUsers(role: String?): Boolean = role in listOf("Administrator", "Manager", "User")

    /**
     * Manager та Administrator можуть вирішувати інциденти
     */
    fun canResolveIncidents(role: String?): Boolean = isManager(role)

    /**
     * Тільки Administrator може видаляти інциденти
     */
    fun canDeleteIncidents(role: String?): Boolean = isAdmin(role)
}
