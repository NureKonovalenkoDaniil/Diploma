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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_incidents, container, false)
        
        recyclerView = view.findViewById(R.id.incidentsRecyclerView)
        progressBar = view.findViewById(R.id.progressBar)
        emptyStateContainer = view.findViewById(R.id.emptyStateContainer)
        emptyStateText = view.findViewById(R.id.emptyStateText)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val factory = StorageIncidentsViewModelFactory(requireContext())
        viewModel = ViewModelProvider(this, factory)[StorageIncidentsViewModel::class.java]

        setupRecyclerView()
        setupObservers()
    }

    override fun onResume() {
        super.onResume()
        viewModel.fetchIncidents()
    }

    private fun setupRecyclerView() {
        adapter = StorageIncidentAdapter(emptyList()) { incident ->
            showResolveDialog(incident)
        }
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.incidents.collect { incidents ->
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
}
