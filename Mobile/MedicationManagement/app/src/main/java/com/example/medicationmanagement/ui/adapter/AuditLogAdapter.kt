package com.example.medicationmanagement.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.medicationmanagement.R
import com.example.medicationmanagement.api.AuditLogDto
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class AuditLogAdapter : RecyclerView.Adapter<AuditLogAdapter.AuditLogViewHolder>() {

    private var logs: List<AuditLogDto> = emptyList()

    fun updateLogs(newLogs: List<AuditLogDto>) {
        logs = newLogs
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AuditLogViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_audit_log, parent, false)
        return AuditLogViewHolder(view)
    }

    override fun onBindViewHolder(holder: AuditLogViewHolder, position: Int) {
        holder.bind(logs[position])
    }

    override fun getItemCount() = logs.size

    inner class AuditLogViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val logAction: TextView = itemView.findViewById(R.id.log_action)
        private val logEntity: TextView = itemView.findViewById(R.id.log_entity)
        private val logUser: TextView = itemView.findViewById(R.id.log_user)
        private val logTimestamp: TextView = itemView.findViewById(R.id.log_timestamp)
        private val logDetails: TextView = itemView.findViewById(R.id.log_details)

        fun bind(log: AuditLogDto) {
            logAction.text = log.action?.let { translateAction(it) } ?: "—"
            val entityType = if (log.entityType.isNullOrBlank()) "—" else log.entityType
            logEntity.text = "$entityType (ID: ${log.entityId?.toString() ?: "—"})"
            logUser.text = if (log.user.isNullOrBlank()) "System" else log.user
            logTimestamp.text = formatDate(log.timestamp)
            logDetails.text = if (log.details.isNullOrBlank()) "—" else log.details
        }

        private fun translateAction(action: String): String {
            val isUk = Locale.getDefault().language == "uk"
            return when (action) {
                "Login" -> if (isUk) "Вхід" else "Login"
                "Register" -> if (isUk) "Реєстрація" else "Registration"
                "Confirm Email" -> if (isUk) "Підтвердження Email" else "Confirm Email"
                "Reset Password" -> if (isUk) "Скидання пароля" else "Reset Password"
                "Create Manager" -> if (isUk) "Створення менеджера" else "Create Manager"
                "Delete User" -> if (isUk) "Видалення користувача" else "Delete User"
                "Create Medicine" -> if (isUk) "Створення препарату" else "Create Medicine"
                "Update Medicine" -> if (isUk) "Оновлення препарату" else "Update Medicine"
                "Delete Medicine" -> if (isUk) "Видалення препарату" else "Delete Medicine"
                "Add Lifecycle Event" -> if (isUk) "Додавання події" else "Add Lifecycle Event"
                "Create Storage Location" -> if (isUk) "Створення локації" else "Create Storage Location"
                "Update Storage Location" -> if (isUk) "Оновлення локації" else "Update Storage Location"
                "Delete Storage Location" -> if (isUk) "Видалення локації" else "Delete Storage Location"
                "Create Storage Incident" -> if (isUk) "Реєстрація інциденту" else "Create Storage Incident"
                "Resolve Storage Incident" -> if (isUk) "Вирішення інциденту" else "Resolve Storage Incident"
                "Device Claim" -> if (isUk) "Запит токена пристрою" else "Device Claim"
                "Device Login" -> if (isUk) "Вхід пристрою" else "Device Login"
                "Create Sensor" -> if (isUk) "Створення сенсора" else "Create Sensor"
                "Delete Sensor" -> if (isUk) "Видалення сенсора" else "Delete Sensor"
                "Update Sensor" -> if (isUk) "Оновлення сенсора" else "Update Sensor"
                "Create Condition" -> if (isUk) "Створення показника" else "Create Condition"
                "Delete Condition" -> if (isUk) "Видалення показника" else "Delete Condition"
                "Create Role" -> if (isUk) "Створення ролі" else "Create Role"
                "Assign Role" -> if (isUk) "Призначення ролі" else "Assign Role"
                "Activate Sensor" -> if (isUk) "Активація сенсора" else "Activate Sensor"
                "Deactivate Sensor" -> if (isUk) "Деактивація сенсора" else "Deactivate Sensor"
                "Receive Medicine" -> if (isUk) "Надходження препарату" else "Receive Medicine"
                "Issue Medicine" -> if (isUk) "Видача препарату" else "Issue Medicine"
                "Auto Expired" -> if (isUk) "Автоматичне прострочення" else "Auto Expired"
                "Move Medicine" -> if (isUk) "Переміщення препарату" else "Move Medicine"
                "Dispose Medicine" -> if (isUk) "Утилізація препарату" else "Dispose Medicine"
                "Incident Created" -> if (isUk) "Створення інциденту" else "Incident Created"
                "Incident Auto-Resolved" -> if (isUk) "Автовирішення інциденту" else "Incident Auto-Resolved"
                "Expiry Notification Sent" -> if (isUk) "Надіслано сповіщення про термін" else "Expiry Notification Sent"
                else -> action
            }
        }

        private fun formatDate(dateStr: String?): String {
            if (dateStr.isNullOrBlank()) return "—"
            return try {
                val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                parser.timeZone = TimeZone.getTimeZone("UTC")
                val formatter = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                parser.parse(dateStr)?.let { formatter.format(it) } ?: dateStr
            } catch (e: Exception) {
                dateStr
            }
        }
    }
}
