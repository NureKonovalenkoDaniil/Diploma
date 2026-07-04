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
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.medicationmanagement.R
import com.example.medicationmanagement.api.StorageLocationDto
import com.example.medicationmanagement.ui.adapter.StorageLocationAdapter
import com.example.medicationmanagement.utils.RoleHelper
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

/**
 * StorageLocationsFragment — окрема вкладка для локацій зберігання, побудована на існуючих IoT-даних.
 */
class StorageLocationsFragment : Fragment() {

    private lateinit var viewModel: StorageLocationsViewModel
    private lateinit var adapter: StorageLocationAdapter

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyStateContainer: View
    private lateinit var emptyStateText: TextView
    private lateinit var fabAddLocation: FloatingActionButton

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_storage_locations, container, false)

        recyclerView = view.findViewById(R.id.storageLocationsRecyclerView)
        progressBar = view.findViewById(R.id.progressBar)
        emptyStateContainer = view.findViewById(R.id.emptyStateContainer)
        emptyStateText = view.findViewById(R.id.emptyStateText)
        fabAddLocation = view.findViewById(R.id.fabAddLocation)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val factory = StorageLocationsViewModelFactory(requireContext())
        viewModel = androidx.lifecycle.ViewModelProvider(this, factory)[StorageLocationsViewModel::class.java]

        setupRecyclerView()
        setupObservers()

        val role = RoleHelper.getCurrentRole(requireContext())
        if (RoleHelper.hasFullAccess(role)) {
            fabAddLocation.show()
            fabAddLocation.setOnClickListener { showLocationFormDialog() }
        } else {
            fabAddLocation.hide()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.fetchLocations()
        viewModel.fetchDevices()
    }

    private fun setupRecyclerView() {
        adapter = StorageLocationAdapter(emptyList()) { location ->
            showLocationActionsDialog(location)
        }
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.locations.collect { locations ->
                adapter.updateItems(locations)
                if (locations.isEmpty()) {
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

    private fun showLocationDialog(location: StorageLocationDto) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(location.name)
            .setMessage(
                buildString {
                    appendLine(location.address ?: getString(R.string.storage_location_no_address))
                    appendLine(location.locationType)
                    appendLine(location.iotDeviceId?.let { getString(R.string.storage_location_linked_device, it) }
                        ?: getString(R.string.storage_location_no_device))
                }
            )
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun showLocationActionsDialog(location: StorageLocationDto) {
        val role = RoleHelper.getCurrentRole(requireContext())
        val canEdit = RoleHelper.hasFullAccess(role)

        if (!canEdit) {
            showLocationDialog(location)
            return
        }

        val actions = arrayOf(
            getString(R.string.edit),
            getString(R.string.delete),
            getString(android.R.string.cancel)
        )

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(location.name)
            .setItems(actions) { dialog, which ->
                when (which) {
                    0 -> showLocationFormDialog(location)
                    1 -> confirmDeleteLocation(location)
                    else -> dialog.dismiss()
                }
            }
            .show()
    }

    private fun showLocationFormDialog(location: StorageLocationDto? = null) {
        val intent = android.content.Intent(requireContext(), com.example.medicationmanagement.EditStorageLocationActivity::class.java)
        if (location != null) {
            intent.putExtra("locationId", location.locationId)
            intent.putExtra("name", location.name)
            intent.putExtra("address", location.address)
            intent.putExtra("locationType", location.locationType)
            intent.putExtra("iotDeviceId", location.iotDeviceId)
        }
        startActivity(intent)
    }

    private fun confirmDeleteLocation(location: StorageLocationDto) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.confirm_delete)
            .setMessage(getString(R.string.storage_location_delete_message, location.name))
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.delete) { _, _ ->
                lifecycleScope.launch {
                    val ok = viewModel.deleteLocation(location.locationId)
                    val msgRes = if (ok) R.string.storage_location_deleted else R.string.storage_location_delete_failed
                    Toast.makeText(requireContext(), msgRes, Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }
}