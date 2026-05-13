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
            logAction.text = log.action ?: "—"
            val entityType = if (log.entityType.isNullOrBlank()) "—" else log.entityType
            logEntity.text = "$entityType (ID: ${log.entityId?.toString() ?: "—"})"
            logUser.text = if (log.user.isNullOrBlank()) "System" else log.user
            logTimestamp.text = formatDate(log.timestamp)
            logDetails.text = if (log.details.isNullOrBlank()) "—" else log.details
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
