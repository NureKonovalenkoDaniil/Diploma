package com.example.medicationmanagement.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.medicationmanagement.AddMedicineActivity
import com.example.medicationmanagement.MedicineAdapter
import com.example.medicationmanagement.R
import com.example.medicationmanagement.utils.RoleHelper
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

/**
 * MedicinesFragment — Головний екран списку препаратів з StateFlow і фільтрами
 */
class MedicinesFragment : Fragment() {

    private lateinit var viewModel: MedicinesViewModel
    private lateinit var adapter: MedicineAdapter

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyStateContainer: View
    private lateinit var emptyStateText: TextView
    private lateinit var searchInput: TextInputEditText
    private lateinit var fabAddMedicine: FloatingActionButton
    private lateinit var chipFilterGroup: ChipGroup
    private lateinit var chipAll: Chip
    private lateinit var chipLowStock: Chip
    private lateinit var chipExpiring: Chip

    private var allMedicines: List<com.example.medicationmanagement.model.Medicine> = emptyList()
    private var searchQuery: String = ""
    private var currentFilter: String = "all"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_medicines, container, false)

        recyclerView = view.findViewById(R.id.medicinesRecyclerView)
        progressBar = view.findViewById(R.id.progressBar)
        emptyStateContainer = view.findViewById(R.id.emptyStateContainer)
        emptyStateText = view.findViewById(R.id.emptyStateText)
        searchInput = view.findViewById(R.id.medicineSearchInput)
        fabAddMedicine = view.findViewById(R.id.fabAddMedicine)
        chipFilterGroup = view.findViewById(R.id.medicineFilterChips)
        chipAll = view.findViewById(R.id.chipMedAll)
        chipLowStock = view.findViewById(R.id.chipMedLowStock)
        chipExpiring = view.findViewById(R.id.chipMedExpiring)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val factory = MedicinesViewModelFactory(requireContext())
        viewModel = ViewModelProvider(this, factory)[MedicinesViewModel::class.java]

        setupRecyclerView()
        setupFlowObservers()

        val currentRole = RoleHelper.getCurrentRole(requireContext())
        fabAddMedicine.visibility = if (RoleHelper.canManageMedicines(currentRole)) View.VISIBLE else View.GONE

        searchInput.addTextChangedListener { text ->
            searchQuery = text?.toString().orEmpty()
            applyLocalSearch()
        }

        fabAddMedicine.setOnClickListener {
            startActivity(Intent(requireContext(), AddMedicineActivity::class.java))
        }

        // Chip фільтри
        chipAll.setOnClickListener { applyChipFilter("all") }
        chipLowStock.setOnClickListener { applyChipFilter("low_stock") }
        chipExpiring.setOnClickListener { applyChipFilter("expiring") }

        // Початкове завантаження
        viewModel.fetchMedicines()
    }

    override fun onResume() {
        super.onResume()
        reloadForCurrentFilter()
    }

    private fun applyChipFilter(filter: String) {
        if (currentFilter == filter) return
        currentFilter = filter
        searchInput.setText("")
        searchQuery = ""
        reloadForCurrentFilter()
    }

    private fun reloadForCurrentFilter() {
        when (currentFilter) {
            "low_stock" -> viewModel.fetchLowStock()
            "expiring" -> viewModel.fetchExpiring()
            else -> viewModel.fetchMedicines()
        }
    }

    private fun setupRecyclerView() {
        adapter = MedicineAdapter(emptyList())
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
    }

    private fun setupFlowObservers() {
        lifecycleScope.launch {
            viewModel.medicines.collect { medicines ->
                allMedicines = medicines
                applyLocalSearch()
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
                    viewModel.clearError()
                }
            }
        }
    }

    private fun applyLocalSearch() {
        val filteredMedicines = if (searchQuery.isBlank()) {
            allMedicines
        } else {
            val query = searchQuery.trim().lowercase()
            allMedicines.filter { medicine ->
                medicine.name.lowercase().contains(query) ||
                    medicine.type.lowercase().contains(query) ||
                    medicine.category.lowercase().contains(query)
            }
        }

        adapter.updateMedicines(filteredMedicines)
        if (filteredMedicines.isEmpty()) {
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
