package com.example.medicationmanagement

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.medicationmanagement.model.StorageCondition

class StorageConditionAdapter(private var items: List<StorageCondition>) :
    RecyclerView.Adapter<StorageConditionAdapter.ConditionViewHolder>() {

    class ConditionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val device: TextView = view.findViewById(R.id.conditionDevice)
        val temp: TextView = view.findViewById(R.id.conditionTemp)
        val humidity: TextView = view.findViewById(R.id.conditionHumidity)
        val time: TextView = view.findViewById(R.id.conditionTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConditionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_condition, parent, false)
        return ConditionViewHolder(view)
    }

    override fun onBindViewHolder(holder: ConditionViewHolder, position: Int) {
        val item = items[position]
        holder.device.text = "Device: ${item.deviceID}"
        holder.temp.text = "Temperature: ${item.temperature}°C"
        holder.humidity.text = "Humidity: ${item.humidity}%"
        holder.time.text = "Timestamp: ${item.timestamp}"
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<StorageCondition>) {
        items = newItems
        notifyDataSetChanged()
    }
}