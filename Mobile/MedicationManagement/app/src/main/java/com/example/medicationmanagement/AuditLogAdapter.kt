package com.example.medicationmanagement

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.medicationmanagement.model.AuditLog

class AuditLogAdapter(private var logs: List<AuditLog>) :
    RecyclerView.Adapter<AuditLogAdapter.LogViewHolder>() {

    class LogViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val action: TextView = itemView.findViewById(R.id.logAction)
        val user: TextView = itemView.findViewById(R.id.logUser)
        val details: TextView = itemView.findViewById(R.id.logDetails)
        val timestamp: TextView = itemView.findViewById(R.id.logTimestamp)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_log, parent, false)
        return LogViewHolder(view)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        val item = logs[position]
        holder.action.text = item.Action
        holder.user.text = "User: ${item.User}"
        holder.details.text = item.Details ?: "No details"
        holder.timestamp.text = item.Timestamp
    }

    override fun getItemCount(): Int = logs.size

    fun updateData(newLogs: List<AuditLog>) {
        logs = newLogs
        notifyDataSetChanged()
    }
}