package com.example.medicationmanagement.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.medicationmanagement.R
import com.example.medicationmanagement.api.UserDto

class UserAdapter(
    private val onDeleteClick: (UserDto) -> Unit,
    private val onRoleClick: (UserDto) -> Unit
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    private var users: List<UserDto> = emptyList()

    fun updateUsers(newUsers: List<UserDto>) {
        users = newUsers
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_user, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(users[position])
    }

    override fun getItemCount() = users.size

    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val userEmail: TextView = itemView.findViewById(R.id.user_email)
        private val userRole: TextView = itemView.findViewById(R.id.user_role)
        private val userStatus: TextView = itemView.findViewById(R.id.user_status)
        private val btnChangeRole: Button = itemView.findViewById(R.id.btn_change_role)
        private val btnDelete: Button = itemView.findViewById(R.id.btn_delete)

        fun bind(user: UserDto) {
            userEmail.text = user.email
            userRole.text = user.role
            userStatus.text = if (user.isEmailConfirmed) "✅ Confirmed" else "⏳ Pending"
            
            btnChangeRole.setOnClickListener {
                onRoleClick(user)
            }
            btnDelete.setOnClickListener {
                onDeleteClick(user)
            }
        }
    }
}
