package com.example.medicationmanagement

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.medicationmanagement.api.StorageIncidentDto
import com.example.medicationmanagement.utils.RoleHelper
import com.google.android.material.chip.Chip
import java.text.SimpleDateFormat
import java.util.Locale

class StorageIncidentAdapter(
    private var items: List<StorageIncidentDto>,
    private val onResolveClick: (StorageIncidentDto) -> Unit
) : RecyclerView.Adapter<StorageIncidentAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val type: TextView = itemView.findViewById(R.id.incidentType)
        val description: TextView = itemView.findViewById(R.id.incidentDescription)
        val date: TextView = itemView.findViewById(R.id.incidentDate)
        val severityChip: Chip = itemView.findViewById(R.id.severityChip)
        val btnResolve: Button = itemView.findViewById(R.id.btnResolve)
        val resolvedLabel: TextView = itemView.findViewById(R.id.resolvedLabel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_incident, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val context = holder.itemView.context
        val item = items[position]

        // Translate / display incident type
        holder.type.text = when (item.incidentType.lowercase()) {
            "temperaturedeviation", "temperature" -> context.getString(R.string.incident_type_temp)
            "humiditydeviation", "humidity" -> context.getString(R.string.incident_type_humidity)
            else -> item.incidentType
        }

        holder.description.text = item.description

        try {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val formatter = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
            val dateObj = parser.parse(item.detectedAt)
            holder.date.text = if (dateObj != null) formatter.format(dateObj) else item.detectedAt
        } catch (e: Exception) {
            holder.date.text = item.detectedAt
        }

        // Severity styling
        holder.severityChip.text = when (item.severity.lowercase()) {
            "critical" -> context.getString(R.string.incident_severity_critical)
            "warning" -> context.getString(R.string.incident_severity_warning)
            else -> item.severity
        }

        val chipColorRes = when (item.severity.lowercase()) {
            "critical" -> android.R.color.holo_red_dark
            "warning" -> android.R.color.holo_orange_dark
            else -> android.R.color.darker_gray
        }
        holder.severityChip.setTextColor(context.getColor(chipColorRes))

        // Resolution status and button visibility
        val role = RoleHelper.getCurrentRole(context)
        val isManager = RoleHelper.isManager(role)

        if (item.isResolved) {
            holder.btnResolve.visibility = View.GONE
            holder.resolvedLabel.visibility = View.VISIBLE
            val resolvedTime = try {
                val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                val formatter = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                val resolvedDate = item.resolvedAt?.let { parser.parse(it) }
                if (resolvedDate != null) formatter.format(resolvedDate) else item.resolvedAt.orEmpty()
            } catch (e: Exception) {
                item.resolvedAt.orEmpty()
            }
            holder.resolvedLabel.text = context.getString(R.string.resolved_incident_at, resolvedTime)
        } else {
            holder.resolvedLabel.visibility = View.GONE
            if (isManager) {
                holder.btnResolve.visibility = View.VISIBLE
                holder.btnResolve.setOnClickListener {
                    onResolveClick(item)
                }
            } else {
                holder.btnResolve.visibility = View.GONE
            }
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateIncidents(newItems: List<StorageIncidentDto>) {
        items = newItems
        notifyDataSetChanged()
    }
}
