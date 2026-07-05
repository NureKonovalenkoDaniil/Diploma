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
    private val onDeleteClick: (UserDto) -> Unit
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
        private val userName: TextView = itemView.findViewById(R.id.user_name)
        private val userRole: TextView = itemView.findViewById(R.id.user_role)
        private val userStatus: TextView = itemView.findViewById(R.id.user_status)
        private val userOrganization: TextView = itemView.findViewById(R.id.user_organization)
        private val btnDelete: Button = itemView.findViewById(R.id.btn_delete)

        fun bind(user: UserDto) {
            userEmail.text = user.email
            userName.text = user.userName ?: user.email.substringBefore('@')
            val context = itemView.context
            val displayRoles = user.roles.map { role ->
                when (role.lowercase()) {
                    "administrator" -> context.getString(R.string.role_administrator)
                    "organizationadmin" -> context.getString(R.string.role_organization_admin)
                    "manager" -> context.getString(R.string.role_manager)
                    "user" -> context.getString(R.string.role_user)
                    else -> role
                }
            }.joinToString(", ").ifBlank { context.getString(R.string.role_user) }
            userRole.text = displayRoles
            userStatus.text = user.id
            userOrganization.text = user.organizationName ?: user.organizationId.ifBlank { "—" }
            
            btnDelete.setOnClickListener {
                onDeleteClick(user)
            }
        }
    }
}
