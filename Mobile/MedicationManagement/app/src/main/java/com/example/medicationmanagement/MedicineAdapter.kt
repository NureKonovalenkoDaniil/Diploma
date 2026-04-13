package com.example.medicationmanagement

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.medicationmanagement.model.Medicine

class MedicineAdapter(private var items: List<Medicine>) :
    RecyclerView.Adapter<MedicineAdapter.MedViewHolder>() {

    class MedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.medName)
        val type: TextView = itemView.findViewById(R.id.medType)
        val category: TextView = itemView.findViewById(R.id.medCategory)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MedViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_medicine, parent, false)
        return MedViewHolder(view)
    }

    override fun onBindViewHolder(holder: MedViewHolder, position: Int) {
        val item = items[position]
        holder.name.text = item.name
        holder.type.text = item.type
        holder.category.text = item.category

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