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
        holder.name.text = item.deviceName
        holder.location.text = item.location ?: "Локація не вказана"
        holder.id.text = "ID: ${item.deviceID}"

        // Очищаємо лісенер, щоб уникнути зациклення при оновленні UI
        holder.statusSwitch.setOnCheckedChangeListener(null)
        holder.statusSwitch.isChecked = item.isActive

        holder.statusSwitch.setOnCheckedChangeListener { _, isChecked ->
            onStatusChange(item, isChecked)
        }

        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, DeviceDetailsActivity::class.java).apply {
                putExtra("deviceID", item.deviceID)
                putExtra("deviceName", item.deviceName)
                putExtra("location", item.location)
                putExtra("isActive", item.isActive)
                putExtra("interval", item.checkIntervalSeconds)
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