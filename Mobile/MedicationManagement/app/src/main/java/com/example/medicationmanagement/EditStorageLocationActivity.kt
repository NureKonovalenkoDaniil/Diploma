package com.example.medicationmanagement

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.medicationmanagement.ui.StorageLocationsViewModel
import com.example.medicationmanagement.ui.StorageLocationsViewModelFactory
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class EditStorageLocationActivity : AppCompatActivity() {

    private lateinit var viewModel: StorageLocationsViewModel

    private lateinit var toolbar: MaterialToolbar
    private lateinit var inputName: TextInputEditText
    private lateinit var inputAddress: TextInputEditText
    private lateinit var typeSpinner: AutoCompleteTextView
    private lateinit var deviceSpinner: AutoCompleteTextView
    private lateinit var btnSave: MaterialButton

    private var locationId: Int = -1
    private var initialType: String? = null
    private var initialDeviceId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_storage_location)

        val factory = StorageLocationsViewModelFactory(this)
        viewModel = ViewModelProvider(this, factory)[StorageLocationsViewModel::class.java]

        toolbar = findViewById(R.id.toolbar)
        inputName = findViewById(R.id.inputLocationName)
        inputAddress = findViewById(R.id.inputLocationAddress)
        typeSpinner = findViewById(R.id.inputLocationTypeSpinner)
        deviceSpinner = findViewById(R.id.inputLocationDeviceSpinner)
        btnSave = findViewById(R.id.btnSave)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        // Parse intent extras
        locationId = intent.getIntExtra("locationId", -1)
        val name = intent.getStringExtra("name")
        val address = intent.getStringExtra("address")
        initialType = intent.getStringExtra("locationType")
        initialDeviceId = intent.getStringExtra("iotDeviceId")

        val isEdit = locationId != -1

        if (isEdit) {
            toolbar.title = getString(R.string.storage_location_edit_title)
            btnSave.text = getString(R.string.save_changes)
            inputName.setText(name)
            inputAddress.setText(address.orEmpty())
        } else {
            toolbar.title = getString(R.string.storage_location_add_title)
            btnSave.text = getString(R.string.create)
        }

        setupSpinners(isEdit)

        btnSave.setOnClickListener {
            saveLocation(isEdit)
        }

        viewModel.fetchDevices()
    }

    private fun setupSpinners(isEdit: Boolean) {
        // 1. Setup Location Type Spinner
        val originalTypes = listOf("Refrigerator", "Shelf", "Warehouse", "Cabinet", "Other")
        val displayTypes = originalTypes.map {
            when (it) {
                "Refrigerator" -> getString(R.string.location_type_refrigerator)
                "Shelf" -> getString(R.string.location_type_shelf)
                "Warehouse" -> getString(R.string.location_type_warehouse)
                "Cabinet" -> getString(R.string.location_type_cabinet)
                else -> getString(R.string.location_type_other)
            }
        }

        val typeAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, displayTypes)
        typeSpinner.setAdapter(typeAdapter)

        // Set initial type if editing
        if (isEdit && initialType != null) {
            val idx = originalTypes.indexOf(initialType)
            if (idx >= 0) {
                typeSpinner.setText(displayTypes[idx], false)
            }
        } else {
            // Default to Refrigerator or first type
            typeSpinner.setText(displayTypes[0], false)
        }

        // 2. Setup IoT Device Spinner observing
        lifecycleScope.launch {
            viewModel.devices.collect { devices ->
                val displayDevices = mutableListOf<String>()
                displayDevices.add(getString(R.string.no_device_option))

                devices.forEach { dev ->
                    val locStr = if (dev.location == "Unassigned" || dev.location.isNullOrEmpty()) {
                        getString(R.string.unassigned)
                    } else {
                        dev.location
                    }
                    displayDevices.add("${dev.deviceID} ($locStr)")
                }

                val deviceAdapter = ArrayAdapter(this@EditStorageLocationActivity, android.R.layout.simple_dropdown_item_1line, displayDevices)
                deviceSpinner.setAdapter(deviceAdapter)

                // Set initial device selection
                if (isEdit && !initialDeviceId.isNullOrEmpty()) {
                    val devIdx = devices.indexOfFirst { it.deviceID == initialDeviceId }
                    if (devIdx >= 0) {
                        deviceSpinner.setText(displayDevices[devIdx + 1], false)
                    } else {
                        deviceSpinner.setText(displayDevices[0], false)
                    }
                } else {
                    deviceSpinner.setText(displayDevices[0], false)
                }
            }
        }
    }

    private fun saveLocation(isEdit: Boolean) {
        val name = inputName.text.toString().trim()
        val address = inputAddress.text.toString().trim().ifBlank { null }

        // Find selected location type
        val originalTypes = listOf("Refrigerator", "Shelf", "Warehouse", "Cabinet", "Other")
        val displayTypes = originalTypes.map {
            when (it) {
                "Refrigerator" -> getString(R.string.location_type_refrigerator)
                "Shelf" -> getString(R.string.location_type_shelf)
                "Warehouse" -> getString(R.string.location_type_warehouse)
                "Cabinet" -> getString(R.string.location_type_cabinet)
                else -> getString(R.string.location_type_other)
            }
        }

        val typeText = typeSpinner.text.toString()
        val typeIdx = displayTypes.indexOf(typeText)
        val type = if (typeIdx >= 0) originalTypes[typeIdx] else "Other"

        // Find selected device
        val devices = viewModel.devices.value
        val deviceText = deviceSpinner.text.toString()
        var deviceId: String? = null

        if (deviceText != getString(R.string.no_device_option)) {
            // Find device by prefix matching deviceID
            val match = devices.firstOrNull { dev ->
                deviceText.startsWith(dev.deviceID)
            }
            deviceId = match?.deviceID
        }

        if (name.isBlank()) {
            Toast.makeText(this, R.string.storage_location_validation_error, Toast.LENGTH_SHORT).show()
            return
        }

        val payload = mapOf(
            "locationId" to (if (isEdit) locationId else 0),
            "name" to name,
            "address" to address,
            "locationType" to type,
            "ioTDeviceId" to deviceId,
            "ioTDeviceLocation" to null
        )

        lifecycleScope.launch {
            btnSave.isEnabled = false
            val ok = if (isEdit) {
                viewModel.updateLocation(locationId, payload)
            } else {
                viewModel.createLocation(payload)
            }

            btnSave.isEnabled = true
            if (ok) {
                val msgRes = if (isEdit) R.string.storage_location_updated else R.string.storage_location_created
                Toast.makeText(this@EditStorageLocationActivity, msgRes, Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this@EditStorageLocationActivity, R.string.storage_location_save_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
