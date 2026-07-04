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
import com.example.medicationmanagement.ui.AddDeviceViewModel
import com.example.medicationmanagement.ui.AddDeviceViewModelFactory
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.launch

class AddDeviceActivity : AppCompatActivity() {

    private lateinit var viewModel: AddDeviceViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_device)

        val factory = AddDeviceViewModelFactory(this)
        viewModel = ViewModelProvider(this, factory)[AddDeviceViewModel::class.java]

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        val inputId = findViewById<EditText>(R.id.inputId)
        val locationSpinner = findViewById<AutoCompleteTextView>(R.id.inputDeviceLocationSpinner)
        val inputType = findViewById<EditText>(R.id.inputType)
        val inputMinTemp = findViewById<EditText>(R.id.inputMinTemp)
        val inputMaxTemp = findViewById<EditText>(R.id.inputMaxTemp)
        val inputMinHumidity = findViewById<EditText>(R.id.inputMinHumidity)
        val inputMaxHumidity = findViewById<EditText>(R.id.inputMaxHumidity)
        val btnAdd = findViewById<Button>(R.id.btnCreateDevice)

        // Setup Location Spinner observing
        lifecycleScope.launch {
            viewModel.locations.collect { locations ->
                val displayLocations = mutableListOf<String>()
                displayLocations.add(getString(R.string.unassigned)) // first option is Unassigned

                locations.forEach {
                    displayLocations.add(it.name)
                }

                val adapter = ArrayAdapter(this@AddDeviceActivity, android.R.layout.simple_dropdown_item_1line, displayLocations)
                locationSpinner.setAdapter(adapter)
                
                // Set default selection
                locationSpinner.setText(displayLocations[0], false)
            }
        }

        btnAdd.setOnClickListener {
            val deviceId = inputId.text.toString().trim()
            val selectedLocText = locationSpinner.text.toString()
            val location = if (selectedLocText != getString(R.string.unassigned)) {
                selectedLocText
            } else {
                "Unassigned"
            }
            val type = inputType.text.toString().trim().ifEmpty { "DHT22" }
            val minTemp = inputMinTemp.text.toString().toFloatOrNull() ?: 2.0f
            val maxTemp = inputMaxTemp.text.toString().toFloatOrNull() ?: 8.0f
            val minHum = inputMinHumidity.text.toString().toFloatOrNull() ?: 30.0f
            val maxHum = inputMaxHumidity.text.toString().toFloatOrNull() ?: 60.0f

            if (deviceId.isEmpty()) {
                Toast.makeText(this, R.string.device_binding_enter_id, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.addDevice(deviceId, location, type, minTemp, maxTemp, minHum, maxHum)
        }

        lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                btnAdd.isEnabled = !isLoading
                btnAdd.text = if (isLoading) getString(R.string.device_binding_adding) else getString(R.string.device_binding_button)
            }
        }

        lifecycleScope.launch {
            viewModel.success.collect { isSuccess ->
                if (isSuccess) {
                    Toast.makeText(this@AddDeviceActivity, R.string.device_binding_success, Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }

        lifecycleScope.launch {
            viewModel.error.collect { errorMsg ->
                if (errorMsg != null) {
                    Toast.makeText(this@AddDeviceActivity, errorMsg, Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}