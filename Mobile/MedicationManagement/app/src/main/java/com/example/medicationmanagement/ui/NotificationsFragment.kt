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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.medicationmanagement.NotificationAdapter
import com.example.medicationmanagement.R

class NotificationsFragment : Fragment() {

    private lateinit var viewModel: NotificationsViewModel
    private lateinit var adapter: NotificationAdapter

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyStateText: TextView
    private lateinit var btnMarkAllRead: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_notifications, container, false)
        
        recyclerView = view.findViewById(R.id.notificationsRecyclerView)
        progressBar = view.findViewById(R.id.progressBar)
        emptyStateText = view.findViewById(R.id.emptyStateText)
        btnMarkAllRead = view.findViewById(R.id.btnMarkAllRead)

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
            Toast.makeText(requireContext(), "Всі сповіщення позначено як прочитані", Toast.LENGTH_SHORT).show()
            (activity as? MainActivity)?.updateNotificationBadge()
        }
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
        viewModel.notifications.observe(viewLifecycleOwner) { notifications ->
            adapter.updateNotifications(notifications)
            
            if (notifications.isEmpty()) {
                emptyStateText.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
                btnMarkAllRead.visibility = View.GONE
            } else {
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

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(viewLifecycleOwner) { errorMsg ->
            if (errorMsg != null) {
                Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_LONG).show()
            }
        }
    }
}
