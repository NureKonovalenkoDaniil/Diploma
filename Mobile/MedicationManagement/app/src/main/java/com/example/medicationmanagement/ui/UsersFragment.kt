package com.example.medicationmanagement.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.medicationmanagement.R
import com.example.medicationmanagement.api.UserDto
import com.example.medicationmanagement.ui.adapter.UserAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class UsersFragment : Fragment() {
    private val viewModel: UsersViewModel by viewModels {
        UsersViewModelFactory(requireContext())
    }

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyState: TextView
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

        adapter = UserAdapter(
            onDeleteClick = { user -> confirmDelete(user) },
            onRoleClick = { user -> showRoleDialog(user) }
        )
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

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

    private fun showRoleDialog(user: UserDto) {
        val roles = arrayOf("User", "Manager", "Administrator")
        var selectedRole = user.role

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.change_user_role))
            .setSingleChoiceItems(roles, roles.indexOf(user.role)) { _, which ->
                selectedRole = roles[which]
            }
            .setPositiveButton(getString(R.string.save_changes)) { _, _ ->
                viewModel.changeUserRole(user.id, selectedRole)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
}
