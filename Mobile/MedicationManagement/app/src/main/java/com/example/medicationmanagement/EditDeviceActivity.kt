package com.example.medicationmanagement

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.medicationmanagement.ui.EditDeviceViewModel
import com.example.medicationmanagement.ui.EditDeviceViewModelFactory
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.launch

class EditDeviceActivity : AppCompatActivity() {

    private lateinit var viewModel: EditDeviceViewModel

    private lateinit var toolbar: MaterialToolbar
    private lateinit var typeInput: EditText
    private lateinit var locationSpinner: AutoCompleteTextView
    private lateinit var minTempInput: EditText
    private lateinit var maxTempInput: EditText
    private lateinit var minHumidityInput: EditText
    private lateinit var maxHumidityInput: EditText
    private lateinit var saveBtn: Button

    private var deviceId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_device)

        val factory = EditDeviceViewModelFactory(this)
        viewModel = ViewModelProvider(this, factory)[EditDeviceViewModel::class.java]

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        typeInput = findViewById(R.id.editDeviceType)
        locationSpinner = findViewById(R.id.inputEditDeviceLocationSpinner)
        minTempInput = findViewById(R.id.editMinTemp)
        maxTempInput = findViewById(R.id.editMaxTemp)
        minHumidityInput = findViewById(R.id.editMinHumidity)
        maxHumidityInput = findViewById(R.id.editMaxHumidity)
        saveBtn = findViewById(R.id.saveDeviceBtn)

        deviceId = intent.getStringExtra("deviceID")
        if (deviceId == null) {
            Toast.makeText(this, "Device ID not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        viewModel.loadDevice(deviceId!!)

        saveBtn.setOnClickListener {
            sendPatchUpdate()
        }

        setupObservers()
    }

    private fun setupObservers() {
        // Observe locations and populate Spinner
        lifecycleScope.launch {
            viewModel.locations.collect { locations ->
                val displayLocations = mutableListOf<String>()
                displayLocations.add(getString(R.string.unassigned)) // first option is Unassigned

                locations.forEach {
                    displayLocations.add(it.name)
                }

                val adapter = ArrayAdapter(this@EditDeviceActivity, android.R.layout.simple_dropdown_item_1line, displayLocations)
                locationSpinner.setAdapter(adapter)

                // Set selection if device is already loaded
                viewModel.device.value?.let { device ->
                    selectSpinnerLocation(device.location)
                }
            }
        }

        lifecycleScope.launch {
            viewModel.device.collect { device ->
                if (device != null) {
                    typeInput.setText(device.type)
                    minTempInput.setText(device.minTemperature.toString())
                    maxTempInput.setText(device.maxTemperature.toString())
                    minHumidityInput.setText(device.minHumidity.toString())
                    maxHumidityInput.setText(device.maxHumidity.toString())

                    // Set selection in spinner
                    selectSpinnerLocation(device.location)
                }
            }
        }

        lifecycleScope.launch {
            viewModel.isSaving.collect { isSaving ->
                saveBtn.isEnabled = !isSaving
                saveBtn.text = if (isSaving) "Збереження..." else getString(R.string.save_changes)
            }
        }

        lifecycleScope.launch {
            viewModel.success.collect { isSuccess ->
                if (isSuccess) {
                    Toast.makeText(this@EditDeviceActivity, "Пристрій оновлено", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }

        lifecycleScope.launch {
            viewModel.error.collect { errorMsg ->
                if (errorMsg != null) {
                    Toast.makeText(this@EditDeviceActivity, errorMsg, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun selectSpinnerLocation(locationName: String) {
        val adapter = locationSpinner.adapter ?: return
        val textToSet = if (locationName == "Unassigned" || locationName.isEmpty()) {
            getString(R.string.unassigned)
        } else {
            locationName
        }

        // Set the text of AutoCompleteTextView without filtering
        locationSpinner.setText(textToSet, false)
    }

    private fun sendPatchUpdate() {
        val selectedLocText = locationSpinner.text.toString()
        val location = if (selectedLocText != getString(R.string.unassigned)) {
            selectedLocText
        } else {
            "Unassigned"
        }

        val patch = listOf(
            mapOf("op" to "replace", "path" to "/type", "value" to typeInput.text.toString()),
            mapOf("op" to "replace", "path" to "/location", "value" to location),
            mapOf("op" to "replace", "path" to "/minTemperature", "value" to (minTempInput.text.toString().toDoubleOrNull() ?: 2.0)),
            mapOf("op" to "replace", "path" to "/maxTemperature", "value" to (maxTempInput.text.toString().toDoubleOrNull() ?: 8.0)),
            mapOf("op" to "replace", "path" to "/minHumidity", "value" to (minHumidityInput.text.toString().toDoubleOrNull() ?: 30.0)),
            mapOf("op" to "replace", "path" to "/maxHumidity", "value" to (maxHumidityInput.text.toString().toDoubleOrNull() ?: 60.0))
        )
        viewModel.updateDevice(deviceId!!, patch)
    }
}