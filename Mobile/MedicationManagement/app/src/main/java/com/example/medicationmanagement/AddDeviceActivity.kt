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
                Toast.makeText(this, "Введіть ID датчика", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            addDevice(deviceId, location, btnAdd)
        }
    }

    private fun addDevice(deviceId: String, location: String, btn: Button) {
        btn.isEnabled = false
        btn.text = "Додавання..."

        // Default values for home user device registration
        val deviceData = mapOf(
            "deviceID" to deviceId,
            "location" to location,
            "type" to "Термометр",
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
                    Toast.makeText(this@AddDeviceActivity, "Датчик успішно додано", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@AddDeviceActivity, "Помилка (Можливо, датчик вже прив'язаний)", Toast.LENGTH_SHORT).show()
                    btn.isEnabled = true
                    btn.text = "Додати"
                }
            } catch (e: Exception) {
                Toast.makeText(this@AddDeviceActivity, "Помилка мережі", Toast.LENGTH_SHORT).show()
                btn.isEnabled = true
                btn.text = "Додати"
            }
        }
    }
}