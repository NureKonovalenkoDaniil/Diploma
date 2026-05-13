package com.example.medicationmanagement

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.medicationmanagement.model.LifecycleEvent
import java.text.SimpleDateFormat
import java.util.Locale

class LifecycleEventAdapter(private var items: List<LifecycleEvent>) :
    RecyclerView.Adapter<LifecycleEventAdapter.EventViewHolder>() {

    class EventViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val action: TextView = itemView.findViewById(R.id.eventAction)
        val date: TextView = itemView.findViewById(R.id.eventDate)
        val description: TextView = itemView.findViewById(R.id.eventDescription)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_lifecycle_event, parent, false)
        return EventViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val item = items[position]
        val context = holder.itemView.context
        
        holder.action.text = when(item.eventType.uppercase()) {
            "CREATED" -> context.getString(R.string.lifecycle_event_created)
            "RECEIVED" -> context.getString(R.string.lifecycle_event_received)
            "ISSUED" -> context.getString(R.string.lifecycle_event_issued)
            "MOVED" -> context.getString(R.string.lifecycle_event_moved)
            "DISPOSED" -> context.getString(R.string.lifecycle_event_disposed)
            "EDITED" -> context.getString(R.string.lifecycle_event_edited)
            else -> item.eventType
        }
        
        holder.description.text = item.description ?: context.getString(R.string.lifecycle_no_description)
        
        try {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val formatter = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
            val parsedDate = parser.parse(item.eventDate)
            holder.date.text = if (parsedDate != null) formatter.format(parsedDate) else item.eventDate
        } catch (e: Exception) {
            holder.date.text = item.eventDate
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateEvents(newItems: List<LifecycleEvent>) {
        items = newItems
        notifyDataSetChanged()
    }
}
