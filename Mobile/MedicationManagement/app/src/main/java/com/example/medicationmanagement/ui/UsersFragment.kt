package com.example.medicationmanagement.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.medicationmanagement.R
import com.example.medicationmanagement.api.UserDto
import com.example.medicationmanagement.ui.adapter.UserAdapter
import com.example.medicationmanagement.utils.RoleHelper
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class UsersFragment : Fragment() {
    private val viewModel: UsersViewModel by viewModels {
        UsersViewModelFactory(requireContext())
    }

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyState: TextView
    private lateinit var fabCreateManager: FloatingActionButton
    private lateinit var adapter: UserAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_users, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.users_list)
        progressBar = view.findViewById(R.id.users_loading)
        emptyState = view.findViewById(R.id.users_empty)
        fabCreateManager = view.findViewById(R.id.fab_create_manager)

        adapter = UserAdapter(onDeleteClick = { user -> confirmDelete(user) })
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        val role = RoleHelper.getCurrentRole(requireContext())
        if (RoleHelper.isAdmin(role)) {
            fabCreateManager.visibility = View.VISIBLE
            fabCreateManager.setOnClickListener { showCreateManagerDialog() }
        } else {
            fabCreateManager.visibility = View.GONE
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.users.collect { users ->
                adapter.updateUsers(users)
                emptyState.visibility = if (users.isEmpty()) View.VISIBLE else View.GONE
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.error.collect { error ->
                if (error != null) {
                    Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
                }
            }
        }

        viewModel.fetchUsers()
    }

    private fun confirmDelete(user: UserDto) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.confirm_delete_user))
            .setMessage(getString(R.string.delete_user_message, user.email))
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                viewModel.deactivateUser(user.id)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun showCreateManagerDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_create_manager, null)
        val emailInput = dialogView.findViewById<EditText>(R.id.inputManagerEmail)
        val passwordInput = dialogView.findViewById<EditText>(R.id.inputManagerPassword)
        val orgIdInput = dialogView.findViewById<EditText>(R.id.inputManagerOrgId)

        // Заповнюємо OrganizationId за замовчуванням
        val defaultOrgId = RoleHelper.getOrganizationId(requireContext())
        orgIdInput.setText(defaultOrgId)

        val role = RoleHelper.getCurrentRole(requireContext())
        if (role == "OrganizationAdmin") {
            orgIdInput.isEnabled = false // Блокуємо зміну організації для адміна організації
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.create_manager))
            .setView(dialogView)
            .setNegativeButton(getString(R.string.cancel), null)
            .setPositiveButton(getString(R.string.create)) { _, _ ->
                val email = emailInput.text.toString().trim()
                val password = passwordInput.text.toString().trim()
                val orgId = orgIdInput.text.toString().trim()

                if (email.isBlank() || password.length < 4 || orgId.isBlank()) {
                    Toast.makeText(requireContext(), R.string.create_manager_validation_error, Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                viewModel.createManager(email, password, orgId)
            }
            .show()
    }
}
