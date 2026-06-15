package com.example.medicationmanagement

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.medicationmanagement.api.ApiClient
import com.example.medicationmanagement.api.IoTDeviceApi
import kotlinx.coroutines.launch

class AddDeviceActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_device)

        val inputId = findViewById<EditText>(R.id.inputId)
        val inputLocation = findViewById<EditText>(R.id.inputLocation)
        val btnAdd = findViewById<Button>(R.id.btnCreateDevice)

        btnAdd.setOnClickListener {
            val deviceId = inputId.text.toString().trim()
            val location = inputLocation.text.toString().trim()

            if (deviceId.isEmpty()) {
                Toast.makeText(this, R.string.device_binding_enter_id, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            addDevice(deviceId, location, btnAdd)
        }
    }

    private fun addDevice(deviceId: String, location: String, btn: Button) {
        btn.isEnabled = false
        btn.text = getString(R.string.device_binding_adding)

        // Default values for home user device registration
        val deviceData = mapOf(
            "deviceID" to deviceId,
            "location" to location,
            "type" to getString(R.string.device_default_type_thermometer),
            "parameters" to "{}",
            "isActive" to true,
            "minTemperature" to 2.0f,
            "maxTemperature" to 8.0f,
            "minHumidity" to 30.0f,
            "maxHumidity" to 60.0f
        )

        lifecycleScope.launch {
            try {
                val api = ApiClient.createService<IoTDeviceApi>(this@AddDeviceActivity)
                val response = api.createDevice(deviceData)

                if (response.isSuccessful) {
                    Toast.makeText(this@AddDeviceActivity, R.string.device_binding_success, Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@AddDeviceActivity, R.string.device_binding_already_linked, Toast.LENGTH_SHORT).show()
                    btn.isEnabled = true
                    btn.text = getString(R.string.device_binding_button)
                }
            } catch (e: Exception) {
                Toast.makeText(this@AddDeviceActivity, R.string.device_binding_network_error, Toast.LENGTH_SHORT).show()
                btn.isEnabled = true
                btn.text = getString(R.string.device_binding_button)
            }
        }
    }
}