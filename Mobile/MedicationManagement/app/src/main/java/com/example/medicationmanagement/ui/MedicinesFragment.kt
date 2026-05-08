package com.example.medicationmanagement.ui

import android.content.Intent
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
import com.example.medicationmanagement.AddMedicineActivity
import com.example.medicationmanagement.MedicineAdapter
import com.example.medicationmanagement.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch

/**
 * MedicinesFragment — Головний екран списку препаратів з StateFlow
 */
class MedicinesFragment : Fragment() {

    private lateinit var viewModel: MedicinesViewModel
    private lateinit var adapter: MedicineAdapter

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyStateContainer: View
    private lateinit var emptyStateText: TextView
    private lateinit var fabAddMedicine: FloatingActionButton

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_medicines, container, false)
        
        recyclerView = view.findViewById(R.id.medicinesRecyclerView)
        progressBar = view.findViewById(R.id.progressBar)
        emptyStateContainer = view.findViewById(R.id.emptyStateContainer)
        emptyStateText = view.findViewById(R.id.emptyStateText)
        fabAddMedicine = view.findViewById(R.id.fabAddMedicine)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val factory = MedicinesViewModelFactory(requireContext())
        viewModel = ViewModelProvider(this, factory)[MedicinesViewModel::class.java]

        setupRecyclerView()
        setupFlowObservers()

        fabAddMedicine.setOnClickListener {
            startActivity(Intent(requireContext(), AddMedicineActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.fetchMedicines()
    }

    private fun setupRecyclerView() {
        adapter = MedicineAdapter(emptyList()) { medicine ->
            // Quick Action: Issue (Вжити) — зменшити залишок на 1
            viewModel.issueMedicine(medicine.medicineID, 1)
            Toast.makeText(requireContext(), "Вжито 1 шт. ${medicine.name}", Toast.LENGTH_SHORT).show()
        }
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
    }

    private fun setupFlowObservers() {
        // Observe medicines list using StateFlow
        lifecycleScope.launch {
            viewModel.medicines.collect { medicines ->
                adapter.updateMedicines(medicines)
                if (medicines.isEmpty()) {
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

        // Observe loading state
        lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        }

        // Observe errors
        lifecycleScope.launch {
            viewModel.error.collect { errorMsg ->
                if (errorMsg != null) {
                    Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_LONG).show()
                    viewModel.clearError()
                }
            }
        }
    }
}
