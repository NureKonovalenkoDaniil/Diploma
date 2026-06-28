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

    private var onLongClickListener: ((StorageIncidentDto) -> Unit)? = null

    fun setOnLongClickListener(listener: (StorageIncidentDto) -> Unit) {
        onLongClickListener = listener
    }

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

        // Довгий клік — видалення (тільки для менеджерів/адмінів)
        val role = RoleHelper.getCurrentRole(context)
        val isManager = RoleHelper.isManager(role)
        if (isManager) {
            holder.itemView.setOnLongClickListener {
                onLongClickListener?.invoke(item)
                true
            }
        }

        // Safe values mapping from both old DTO and new server DTO
        val incidentType = item.incidentType
        val detectedAt = item.detectedAt
        val resolvedAt = item.resolvedAt
        val isResolved = item.isResolvedCalculated

        // Calculate severity safely if not provided
        val severity = item.severity ?: item.status?.let {
            val valDet = item.detectedValue ?: 0f
            val min = item.expectedMin ?: 0f
            val max = item.expectedMax ?: 0f
            val diff = if (valDet < min) min - valDet else if (valDet > max) valDet - max else 0f
            if (diff > 5f) "critical" else "warning"
        } ?: "warning"

        // Build description safely if null or blank
        val description = if (item.description.isNullOrBlank() && item.deviceId != null) {
            "Пристрій ${item.deviceId} (${item.deviceLocation ?: "Без локації"}) зафіксував ${item.detectedValue} (норма: ${item.expectedMin}..${item.expectedMax})"
        } else {
            item.description.orEmpty()
        }

        // Translate / display incident type
        holder.type.text = when (incidentType.lowercase()) {
            "temperaturedeviation", "temperature", "temperatureviolation" -> context.getString(R.string.incident_type_temp)
            "humiditydeviation", "humidity", "humidityviolation" -> context.getString(R.string.incident_type_humidity)
            else -> incidentType
        }

        holder.description.text = description

        try {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val formatter = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
            val dateObj = parser.parse(detectedAt)
            holder.date.text = if (dateObj != null) formatter.format(dateObj) else detectedAt
        } catch (e: Exception) {
            holder.date.text = detectedAt
        }

        // Severity styling
        holder.severityChip.text = when (severity.lowercase()) {
            "critical" -> context.getString(R.string.incident_severity_critical)
            "warning" -> context.getString(R.string.incident_severity_warning)
            else -> severity
        }

        val chipColorRes = when (severity.lowercase()) {
            "critical" -> android.R.color.holo_red_dark
            "warning" -> android.R.color.holo_orange_dark
            else -> android.R.color.darker_gray
        }
        holder.severityChip.setTextColor(context.getColor(chipColorRes))

        // Resolution status and button visibility
        val roleForBtn = RoleHelper.getCurrentRole(context)
        val isManagerForBtn = RoleHelper.isManager(roleForBtn)

        if (isResolved) {
            holder.btnResolve.visibility = View.GONE
            holder.resolvedLabel.visibility = View.VISIBLE
            val resolvedTime = try {
                val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                val formatter = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                val resolvedDate = resolvedAt?.let { parser.parse(it) }
                if (resolvedDate != null) formatter.format(resolvedDate) else resolvedAt.orEmpty()
            } catch (e: Exception) {
                resolvedAt.orEmpty()
            }
            holder.resolvedLabel.text = context.getString(R.string.resolved_incident_at, resolvedTime)
        } else {
            holder.resolvedLabel.visibility = View.GONE
            if (isManagerForBtn) {
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
