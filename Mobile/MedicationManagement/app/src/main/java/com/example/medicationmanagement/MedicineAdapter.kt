package com.example.medicationmanagement

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.medicationmanagement.model.Medicine
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

class MedicineAdapter(
    private var items: List<Medicine>
) : RecyclerView.Adapter<MedicineAdapter.MedViewHolder>() {

    class MedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.medName)
        val type: TextView = itemView.findViewById(R.id.medType)
        val expiry: TextView = itemView.findViewById(R.id.medExpiry)
        val quantity: TextView = itemView.findViewById(R.id.medQuantity)
        val status: TextView = itemView.findViewById(R.id.medStatus)
        val location: TextView = itemView.findViewById(R.id.medLocation)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MedViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_medicine, parent, false)
        return MedViewHolder(view)
    }

    override fun onBindViewHolder(holder: MedViewHolder, position: Int) {
        val item = items[position]
        holder.name.text = item.name
        holder.type.text = "${item.type} | ${item.category}"
        holder.quantity.text = item.quantity.toString()
        val context = holder.itemView.context
        val rawStatus = item.status.ifBlank { "Active" }
        holder.status.text = when (rawStatus.lowercase()) {
            "active" -> context.getString(R.string.status_active)
            "expired" -> context.getString(R.string.status_expired)
            "disposed" -> context.getString(R.string.status_disposed)
            "recalled" -> context.getString(R.string.status_recalled)
            else -> rawStatus
        }
        holder.location.text = item.storageLocationName ?: "—"

        // Форматування дати
        try {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val formatter = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            val date = parser.parse(item.expiryDate)
            if (date != null) {
                val formattedDate = formatter.format(date)
                holder.expiry.text = context.getString(R.string.medicine_valid_until, formattedDate)
                // Підсвітка протермінованих
                if (date.before(Date())) {
                    holder.expiry.setTextColor(context.getColor(android.R.color.holo_red_dark))
                    holder.expiry.text = context.getString(R.string.medicine_expired_uppercase, formattedDate)
                } else {
                    holder.expiry.setTextColor(context.getColor(android.R.color.darker_gray))
                }
            }
        } catch (e: Exception) {
            holder.expiry.text = item.expiryDate
        }

        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, MedicineDetailsActivity::class.java).apply {
                putExtra("medicineID", item.medicineID)
                putExtra("name", item.name)
                putExtra("type", item.type)
                putExtra("category", item.category)
                putExtra("quantity", item.quantity)
                putExtra("expiryDate", item.expiryDate)
            }
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateMedicines(newItems: List<Medicine>) {
        items = newItems
        notifyDataSetChanged()
    }

}