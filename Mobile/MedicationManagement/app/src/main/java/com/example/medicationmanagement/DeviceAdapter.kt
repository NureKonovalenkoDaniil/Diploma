package com.example.medicationmanagement

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.medicationmanagement.DeviceDetailsActivity
import com.example.medicationmanagement.R
import com.example.medicationmanagement.model.IoTDevice

class DeviceAdapter(private var devices: List<IoTDevice>) :
    RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder>() {

    class DeviceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val type: TextView = itemView.findViewById(R.id.deviceType)
        val location: TextView = itemView.findViewById(R.id.deviceLocation)
        val status: TextView = itemView.findViewById(R.id.deviceStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_device, parent, false)
        return DeviceViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val device = devices[position]
        holder.type.text = "Type: ${device.type}"
        holder.location.text = "Location: ${device.location}"
        holder.status.text = if (device.isActive) "Active" else "Inactive"

        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, DeviceDetailsActivity::class.java).apply {
                putExtra("deviceID", device.deviceID)
            }
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = devices.size

    fun updateDevices(newList: List<IoTDevice>) {
        devices = newList
        notifyDataSetChanged()
    }
}