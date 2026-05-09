package com.example.medicationmanagement

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.medicationmanagement.model.Notification
import java.text.SimpleDateFormat
import java.util.Locale

class NotificationAdapter(
    private var items: List<Notification>,
    private val onNotificationClick: (Notification) -> Unit
) : RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val card: CardView = itemView.findViewById(R.id.notificationCard)
        val title: TextView = itemView.findViewById(R.id.notificationTitle)
        val message: TextView = itemView.findViewById(R.id.notificationMessage)
        val date: TextView = itemView.findViewById(R.id.notificationDate)
        val icon: ImageView = itemView.findViewById(R.id.notificationIcon)
        val unreadDot: View = itemView.findViewById(R.id.unreadDot)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val item = items[position]
        holder.title.text = item.title
        holder.message.text = item.message

        try {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val formatter = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
            val dateObj = parser.parse(item.createdAt)
            holder.date.text = if (dateObj != null) formatter.format(dateObj) else item.createdAt
        } catch (e: Exception) {
            holder.date.text = item.createdAt
        }

        // Візуальне розрізнення (Колір/Іконки)
        when (item.type.lowercase()) {
            "alert", "incident" -> {
                holder.icon.setImageResource(android.R.drawable.ic_dialog_alert)
                holder.icon.setColorFilter(holder.itemView.context.getColor(android.R.color.holo_red_dark))
            }
            "warning" -> {
                holder.icon.setImageResource(android.R.drawable.ic_dialog_alert)
                holder.icon.setColorFilter(holder.itemView.context.getColor(android.R.color.holo_orange_dark))
            }
            else -> {
                holder.icon.setImageResource(android.R.drawable.ic_dialog_info)
                // Use default tint (primary) set in XML
                holder.icon.clearColorFilter()
            }
        }

        // Стиль для непрочитаних
        if (!item.isRead) {
            holder.title.setTypeface(null, Typeface.BOLD)
            holder.message.setTypeface(null, Typeface.BOLD)
            holder.unreadDot.visibility = View.VISIBLE
            holder.card.setCardBackgroundColor(holder.itemView.context.getColor(R.color.unread_bg))
        } else {
            holder.title.setTypeface(null, Typeface.NORMAL)
            holder.message.setTypeface(null, Typeface.NORMAL)
            holder.unreadDot.visibility = View.GONE
            holder.card.setCardBackgroundColor(holder.itemView.context.getColor(R.color.white))
        }

        holder.itemView.setOnClickListener {
            if (!item.isRead) {
                onNotificationClick(item)
            }
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateNotifications(newItems: List<Notification>) {
        items = newItems
        notifyDataSetChanged()
    }
}
