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
import com.example.medicationmanagement.AddDeviceActivity
import com.example.medicationmanagement.DeviceAdapter
import com.example.medicationmanagement.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch

class SensorsFragment : Fragment() {

    private lateinit var viewModel: SensorsViewModel
    private lateinit var adapter: DeviceAdapter

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyStateContainer: View
    private lateinit var emptyStateText: TextView
    private lateinit var fabAddSensor: FloatingActionButton

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_sensors, container, false)
        
        recyclerView = view.findViewById(R.id.sensorsRecyclerView)
        progressBar = view.findViewById(R.id.progressBar)
        emptyStateContainer = view.findViewById(R.id.emptyStateContainer)
        emptyStateText = view.findViewById(R.id.emptyStateText)
        fabAddSensor = view.findViewById(R.id.fabAddSensor)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val factory = SensorsViewModelFactory(requireContext())
        viewModel = ViewModelProvider(this, factory)[SensorsViewModel::class.java]

        setupRecyclerView()
        setupObservers()

        fabAddSensor.setOnClickListener {
            startActivity(Intent(requireContext(), AddDeviceActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.fetchDevices()
    }

    private fun setupRecyclerView() {
        adapter = DeviceAdapter(emptyList()) { device, isActive ->
            viewModel.toggleDeviceStatus(device.deviceID, !isActive)
        }
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.devices.collect { devices ->
                adapter.updateDevices(devices)
                if (devices.isEmpty()) {
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
}
