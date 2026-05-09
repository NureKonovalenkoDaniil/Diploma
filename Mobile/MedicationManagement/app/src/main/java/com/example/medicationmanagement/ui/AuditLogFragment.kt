package com.example.medicationmanagement.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.medicationmanagement.R
import com.example.medicationmanagement.ui.adapter.AuditLogAdapter
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.launch

class AuditLogFragment : Fragment() {
    private val viewModel: AuditLogViewModel by viewModels {
        AuditLogViewModelFactory(requireContext())
    }

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyState: TextView
    private lateinit var filterChips: ChipGroup
    private lateinit var adapter: AuditLogAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_audit_log, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.audit_log_list)
        progressBar = view.findViewById(R.id.audit_log_loading)
        emptyState = view.findViewById(R.id.audit_log_empty)
        filterChips = view.findViewById(R.id.audit_log_filters)

        adapter = AuditLogAdapter()
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        // Setup filter chips
        filterChips.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isEmpty()) {
                viewModel.clearFilter()
            } else {
                val chipId = checkedIds.first()
                val filterType = when (chipId) {
                    R.id.chip_medicine -> "Medicine"
                    R.id.chip_storage -> "StorageLocation"
                    R.id.chip_incident -> "StorageIncident"
                    R.id.chip_device -> "IoTDevice"
                    R.id.chip_user -> "User"
                    else -> null
                }
                if (filterType != null) {
                    viewModel.filterByAction(filterType)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.logs.collect { logs ->
                adapter.updateLogs(logs)
                emptyState.visibility = if (logs.isEmpty()) View.VISIBLE else View.GONE
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.error.collect { error ->
                if (error != null) {
                    Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
                }
            }
        }

        viewModel.fetchLogs()
    }
}
