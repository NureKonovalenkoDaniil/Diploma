package com.example.medicationmanagement

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.medicationmanagement.ui.AddDeviceViewModel
import com.example.medicationmanagement.ui.AddDeviceViewModelFactory
import kotlinx.coroutines.launch

class AddDeviceActivity : AppCompatActivity() {

    private lateinit var viewModel: AddDeviceViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_device)

        val factory = AddDeviceViewModelFactory(this)
        viewModel = ViewModelProvider(this, factory)[AddDeviceViewModel::class.java]

        val inputId = findViewById<EditText>(R.id.inputId)
        val inputLocation = findViewById<EditText>(R.id.inputLocation)
        val btnAdd = findViewById<Button>(R.id.btnCreateDevice)

        // Restore values if present
        if (viewModel.deviceId.isNotEmpty()) {
            inputId.setText(viewModel.deviceId)
        }
        if (viewModel.location.isNotEmpty()) {
            inputLocation.setText(viewModel.location)
        }

        btnAdd.setOnClickListener {
            val deviceId = inputId.text.toString().trim()
            val location = inputLocation.text.toString().trim()

            if (deviceId.isEmpty()) {
                Toast.makeText(this, R.string.device_binding_enter_id, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.addDevice(deviceId, location, getString(R.string.device_default_type_thermometer))
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