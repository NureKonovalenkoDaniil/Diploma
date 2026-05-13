package com.example.medicationmanagement

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.medicationmanagement.model.IoTDevice
import com.google.android.material.switchmaterial.SwitchMaterial

class DeviceAdapter(
    private var items: List<IoTDevice>,
    private val onStatusChange: (IoTDevice, Boolean) -> Unit
) : RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder>() {

    class DeviceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val card: com.google.android.material.card.MaterialCardView = itemView as com.google.android.material.card.MaterialCardView
        val name: TextView = itemView.findViewById(R.id.deviceName)
        val location: TextView = itemView.findViewById(R.id.deviceLocation)
        val id: TextView = itemView.findViewById(R.id.deviceId)
        val statusSwitch: SwitchMaterial = itemView.findViewById(R.id.deviceStatusSwitch)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_device, parent, false)
        return DeviceViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val item = items[position]
        holder.name.text = item.type.ifBlank { "Датчик ${item.deviceID}" }
        holder.location.text = item.location.ifBlank { "Локація не вказана" }
        holder.id.text = "ID: ${item.deviceID}"

        // Очищаємо лісенер, щоб уникнути зациклення при оновленні UI
        holder.statusSwitch.setOnCheckedChangeListener(null)
        holder.statusSwitch.isChecked = item.isActive

        holder.statusSwitch.setOnCheckedChangeListener { _, isChecked ->
            onStatusChange(item, isChecked)
        }

        // Visual indicator: active -> accent stroke, inactive -> outline muted
        try {
            if (item.isActive) {
                holder.card.strokeColor = holder.itemView.context.getColor(android.R.color.holo_green_dark)
                holder.card.strokeWidth = 3
            } else {
                holder.card.strokeColor = holder.itemView.context.getColor(android.R.color.darker_gray)
                holder.card.strokeWidth = 1
            }
        } catch (_: Exception) {
            // ignore color setting on older devices
        }

        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, DeviceDetailsActivity::class.java).apply {
                putExtra("deviceID", item.deviceID)
                putExtra("deviceName", item.type.ifBlank { "Датчик ${item.deviceID}" })
                putExtra("location", item.location)
                putExtra("isActive", item.isActive)
            }
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateDevices(newItems: List<IoTDevice>) {
        items = newItems
        notifyDataSetChanged()
    }
}