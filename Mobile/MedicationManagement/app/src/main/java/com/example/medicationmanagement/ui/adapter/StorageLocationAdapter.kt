package com.example.medicationmanagement.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.medicationmanagement.R
import com.example.medicationmanagement.api.StorageLocationDto

class StorageLocationAdapter(
    private var items: List<StorageLocationDto>,
    private val onClick: (StorageLocationDto) -> Unit
) : RecyclerView.Adapter<StorageLocationAdapter.StorageLocationViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StorageLocationViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_storage_location, parent, false)
        return StorageLocationViewHolder(view)
    }

    override fun onBindViewHolder(holder: StorageLocationViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<StorageLocationDto>) {
        items = newItems
        notifyDataSetChanged()
    }

    inner class StorageLocationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.findViewById(R.id.location_name)
        private val address: TextView = itemView.findViewById(R.id.location_address)
        private val meta: TextView = itemView.findViewById(R.id.location_meta)
        private val linkedDevice: TextView = itemView.findViewById(R.id.location_device)

        fun bind(location: StorageLocationDto) {
            title.text = location.name
            address.text = location.address ?: itemView.context.getString(R.string.storage_location_no_address)
            val context = itemView.context
            meta.text = when (location.locationType?.lowercase() ?: "") {
                "refrigerator" -> context.getString(R.string.location_type_refrigerator)
                "shelf" -> context.getString(R.string.location_type_shelf)
                "warehouse" -> context.getString(R.string.location_type_warehouse)
                "cabinet" -> context.getString(R.string.location_type_cabinet)
                else -> context.getString(R.string.location_type_other)
            }
            linkedDevice.text = location.iotDeviceId?.let {
                itemView.context.getString(R.string.storage_location_linked_device, it)
            } ?: itemView.context.getString(R.string.storage_location_no_device)

            itemView.setOnClickListener { onClick(location) }
        }
    }
}