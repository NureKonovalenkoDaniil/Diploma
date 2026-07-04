package com.example.medicationmanagement.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.medicationmanagement.MainActivity
import com.example.medicationmanagement.model.Notification
import com.example.medicationmanagement.NotificationAdapter
import com.example.medicationmanagement.R
import kotlinx.coroutines.launch

class NotificationsFragment : Fragment() {

    private lateinit var viewModel: NotificationsViewModel
    private lateinit var adapter: NotificationAdapter

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyStateContainer: View
    private lateinit var emptyStateText: TextView
    private lateinit var btnMarkAllRead: TextView
    private lateinit var chipFilterGroup: com.google.android.material.chip.ChipGroup
    private lateinit var chipAll: com.google.android.material.chip.Chip
    private lateinit var chipViolation: com.google.android.material.chip.Chip
    private lateinit var chipRestored: com.google.android.material.chip.Chip
    private lateinit var chipExpiry: com.google.android.material.chip.Chip
    private lateinit var chipLowStock: com.google.android.material.chip.Chip

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_notifications, container, false)
        
        recyclerView = view.findViewById(R.id.notificationsRecyclerView)
        progressBar = view.findViewById(R.id.progressBar)
        emptyStateContainer = view.findViewById(R.id.emptyStateContainer)
        emptyStateText = view.findViewById(R.id.emptyStateText)
        btnMarkAllRead = view.findViewById(R.id.btnMarkAllRead)
        chipFilterGroup = view.findViewById(R.id.chipFilterGroup)
        chipAll = view.findViewById(R.id.chipAll)
        chipViolation = view.findViewById(R.id.chipViolation)
        chipRestored = view.findViewById(R.id.chipRestored)
        chipExpiry = view.findViewById(R.id.chipExpiry)
        chipLowStock = view.findViewById(R.id.chipLowStock)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val factory = NotificationsViewModelFactory(requireContext())
        viewModel = ViewModelProvider(this, factory)[NotificationsViewModel::class.java]

        setupRecyclerView()
        setupObservers()

        btnMarkAllRead.setOnClickListener {
            viewModel.markAllAsRead()
            Toast.makeText(requireContext(), R.string.notifications_all_read, Toast.LENGTH_SHORT).show()
            (activity as? MainActivity)?.updateNotificationBadge()
        }

        chipAll.setOnClickListener { applyFilter("all") }
        chipViolation.setOnClickListener { applyFilter("StorageViolation") }
        chipRestored.setOnClickListener { applyFilter("StorageRestored") }
        chipExpiry.setOnClickListener { applyFilter("Expiry") }
        chipLowStock.setOnClickListener { applyFilter("LowStock") }

        // Start polling when fragment view is created
        viewModel.startPolling()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Stop polling when fragment view is destroyed
        viewModel.stopPolling()
    }

    override fun onResume() {
        super.onResume()
        viewModel.fetchNotifications()
    }

    private fun setupRecyclerView() {
        adapter = NotificationAdapter(emptyList()) { notification ->
            viewModel.markAsRead(notification.notificationId)
            (activity as? MainActivity)?.updateNotificationBadge()
        }
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.notifications.collect { notifications ->
                // store current notifications for filtering
                currentNotifications = notifications

                // Apply current chip filter
                val selectedFilter = when {
                    chipViolation.isChecked -> "StorageViolation"
                    chipRestored.isChecked -> "StorageRestored"
                    chipExpiry.isChecked -> "Expiry"
                    chipLowStock.isChecked -> "LowStock"
                    else -> "all"
                }

                val filtered = when (selectedFilter) {
                    "all" -> notifications
                    else -> notifications.filter { it.type.equals(selectedFilter, ignoreCase = true) }
                }

                adapter.updateNotifications(filtered)
                
                if (notifications.isEmpty()) {
                    emptyStateContainer.visibility = View.VISIBLE
                    emptyStateText.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                    btnMarkAllRead.visibility = View.GONE
                } else {
                    emptyStateContainer.visibility = View.GONE
                    emptyStateText.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                    
                    // Якщо є хоча б одне непрочитане, показуємо кнопку "Прочитати всі"
                    if (notifications.any { !it.isRead }) {
                        btnMarkAllRead.visibility = View.VISIBLE
                    } else {
                        btnMarkAllRead.visibility = View.GONE
                    }
                }
            }
        }

        // keep badge updated when list changes (optional)
        lifecycleScope.launch {
            viewModel.notifications.collect { list ->
                (activity as? MainActivity)?.updateNotificationBadge()
            }
        }

        lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        }

        lifecycleScope.launch {
            viewModel.error.collect { errorMsg ->
                if (errorMsg != null) {
                    Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private var currentNotifications: List<Notification> = emptyList()

    private fun applyFilter(filter: String) {
        // update chip selection
        chipAll.isChecked = filter == "all"
        chipViolation.isChecked = filter.equals("StorageViolation", ignoreCase = true)
        chipRestored.isChecked = filter.equals("StorageRestored", ignoreCase = true)
        chipExpiry.isChecked = filter.equals("Expiry", ignoreCase = true)
        chipLowStock.isChecked = filter.equals("LowStock", ignoreCase = true)

        val filtered = when (filter) {
            "all" -> currentNotifications
            else -> currentNotifications.filter { it.type.equals(filter, ignoreCase = true) }
        }

        adapter.updateNotifications(filtered)
    }
}
