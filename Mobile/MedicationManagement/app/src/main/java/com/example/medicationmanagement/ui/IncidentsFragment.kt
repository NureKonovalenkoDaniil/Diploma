package com.example.medicationmanagement.ui

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.medicationmanagement.R
import com.example.medicationmanagement.StorageIncidentAdapter
import com.example.medicationmanagement.api.StorageIncidentDto
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

class IncidentsFragment : Fragment() {

    private lateinit var viewModel: StorageIncidentsViewModel
    private lateinit var adapter: StorageIncidentAdapter

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyStateContainer: View
    private lateinit var emptyStateText: TextView
    private lateinit var chipFilterGroup: ChipGroup
    private lateinit var chipAll: Chip
    private lateinit var chipActive: Chip
    private lateinit var chipResolved: Chip

    private var allIncidents: List<StorageIncidentDto> = emptyList()
    private var currentFilter: String = "all"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_incidents, container, false)

        recyclerView = view.findViewById(R.id.incidentsRecyclerView)
        progressBar = view.findViewById(R.id.progressBar)
        emptyStateContainer = view.findViewById(R.id.emptyStateContainer)
        emptyStateText = view.findViewById(R.id.emptyStateText)
        chipFilterGroup = view.findViewById(R.id.incidentFilterChips)
        chipAll = view.findViewById(R.id.chipIncidentAll)
        chipActive = view.findViewById(R.id.chipIncidentActive)
        chipResolved = view.findViewById(R.id.chipIncidentResolved)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val factory = StorageIncidentsViewModelFactory(requireContext())
        viewModel = ViewModelProvider(this, factory)[StorageIncidentsViewModel::class.java]

        setupRecyclerView()
        setupObservers()
        setupChipFilters()
    }

    override fun onResume() {
        super.onResume()
        viewModel.fetchIncidents()
    }

    private fun setupChipFilters() {
        chipAll.setOnClickListener {
            currentFilter = "all"
            applyFilter()
        }
        chipActive.setOnClickListener {
            currentFilter = "active"
            applyFilter()
        }
        chipResolved.setOnClickListener {
            currentFilter = "resolved"
            applyFilter()
        }
    }

    private fun applyFilter() {
        val filtered = when (currentFilter) {
            "active" -> allIncidents.filter { !it.isResolvedCalculated }
            "resolved" -> allIncidents.filter { it.isResolvedCalculated }
            else -> allIncidents
        }
        updateList(filtered)
    }

    private fun setupRecyclerView() {
        adapter = StorageIncidentAdapter(emptyList()) { incident ->
            showResolveDialog(incident)
        }
        // Довгий клік — видалення
        adapter.setOnLongClickListener { incident ->
            showDeleteDialog(incident)
        }
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.incidents.collect { incidents ->
                allIncidents = incidents
                applyFilter()
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

    private fun updateList(incidents: List<StorageIncidentDto>) {
        adapter.updateIncidents(incidents)
        if (incidents.isEmpty()) {
            emptyStateContainer.visibility = View.VISIBLE
            emptyStateText.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            emptyStateContainer.visibility = View.GONE
            emptyStateText.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }

    private fun showResolveDialog(incident: StorageIncidentDto) {
        val context = requireContext()
        val inputLayout = TextInputLayout(context).apply {
            hint = context.getString(R.string.resolve_dialog_hint)
        }
        val input = TextInputEditText(context).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
        }
        inputLayout.addView(input)

        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle(R.string.resolve_dialog_title)
            .setMessage(R.string.resolve_dialog_message)
            .setView(inputLayout)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.resolve_dialog_submit, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val comment = input.text?.toString().orEmpty().trim()
                if (comment.isEmpty()) {
                    Toast.makeText(context, "Введіть опис вжитих заходів", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                viewModel.resolveIncident(incident.id, comment)
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun showDeleteDialog(incident: StorageIncidentDto) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.incident_delete_confirm_title)
            .setMessage(R.string.incident_delete_confirm_msg)
            .setPositiveButton(R.string.delete) { _, _ ->
                viewModel.deleteIncident(incident.id)
                Toast.makeText(requireContext(), R.string.incident_deleted, Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}
