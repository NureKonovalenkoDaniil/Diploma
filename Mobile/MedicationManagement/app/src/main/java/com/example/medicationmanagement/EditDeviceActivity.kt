package com.example.medicationmanagement

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.medicationmanagement.ui.EditDeviceViewModel
import com.example.medicationmanagement.ui.EditDeviceViewModelFactory
import kotlinx.coroutines.launch

class EditDeviceActivity : AppCompatActivity() {

    private lateinit var viewModel: EditDeviceViewModel

    private lateinit var typeInput: EditText
    private lateinit var locationInput: EditText
    private lateinit var parametersInput: EditText
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

        typeInput = findViewById(R.id.editDeviceType)
        locationInput = findViewById(R.id.editDeviceLocation)
        parametersInput = findViewById(R.id.editDeviceParams)
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
        lifecycleScope.launch {
            viewModel.device.collect { device ->
                if (device != null) {
                    typeInput.setText(device.type)
                    locationInput.setText(device.location)
                    parametersInput.setText(device.parameters)
                    minTempInput.setText(device.minTemperature.toString())
                    maxTempInput.setText(device.maxTemperature.toString())
                    minHumidityInput.setText(device.minHumidity.toString())
                    maxHumidityInput.setText(device.maxHumidity.toString())
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
                    Toast.makeText(this@EditDeviceActivity, "Device updated", Toast.LENGTH_SHORT).show()
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

    private fun sendPatchUpdate() {
        val patch = listOf(
            mapOf("op" to "replace", "path" to "/type", "value" to typeInput.text.toString()),
            mapOf("op" to "replace", "path" to "/location", "value" to locationInput.text.toString()),
            mapOf("op" to "replace", "path" to "/parameters", "value" to parametersInput.text.toString()),
            mapOf("op" to "replace", "path" to "/minTemperature", "value" to (minTempInput.text.toString().toDoubleOrNull() ?: 0.0)),
            mapOf("op" to "replace", "path" to "/maxTemperature", "value" to (maxTempInput.text.toString().toDoubleOrNull() ?: 0.0)),
            mapOf("op" to "replace", "path" to "/minHumidity", "value" to (minHumidityInput.text.toString().toDoubleOrNull() ?: 0.0)),
            mapOf("op" to "replace", "path" to "/maxHumidity", "value" to (maxHumidityInput.text.toString().toDoubleOrNull() ?: 0.0))
        )
        viewModel.updateDevice(deviceId!!, patch)
    }
}