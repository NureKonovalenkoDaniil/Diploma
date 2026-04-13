package com.example.medicationmanagement

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.medicationmanagement.model.IoTDevice
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class DeviceDetailsActivity : AppCompatActivity() {

    private lateinit var typeText: TextView
    private lateinit var locationText: TextView
    private lateinit var paramsText: TextView
    private lateinit var statusText: TextView
    private lateinit var tempText: TextView
    private lateinit var humidityText: TextView
    private lateinit var toggleBtn: Button
    private lateinit var editBtn: Button
    private lateinit var deleteBtn: Button

    private var deviceId = -1
    private var currentStatus = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_details)

        typeText = findViewById(R.id.deviceTypeText)
        locationText = findViewById(R.id.deviceLocationText)
        paramsText = findViewById(R.id.deviceParamsText)
        statusText = findViewById(R.id.deviceStatusText)
        tempText = findViewById(R.id.deviceTempText)
        humidityText = findViewById(R.id.deviceHumidityText)
        toggleBtn = findViewById(R.id.toggleDeviceBtn)
        editBtn = findViewById(R.id.editDeviceBtn)
        deleteBtn = findViewById(R.id.deleteDeviceBtn)

        deviceId = intent.getIntExtra("deviceID", -1)
        if (deviceId == -1) {
            Toast.makeText(this, "Device ID not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        loadDeviceDetails()

        toggleBtn.setOnClickListener {
            toggleDeviceStatus()
        }

        editBtn.setOnClickListener {
            val intent = Intent(this, EditDeviceActivity::class.java)
            intent.putExtra("deviceID", deviceId)
            startActivity(intent)
        }

        deleteBtn.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Confirm Delete")
                .setMessage("Are you sure you want to delete this device?")
                .setPositiveButton("Yes") { _, _ -> deleteDevice() }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun loadDeviceDetails() {
        val token = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE).getString("token", null) ?: return

        thread {
            try {
                val url = URL("http://10.0.2.2:5000/api/iotdevice/$deviceId")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.setRequestProperty("Authorization", "Bearer $token")

                val response = conn.inputStream.bufferedReader().readText()
                val obj = JSONObject(response)

                runOnUiThread {
                    typeText.text = obj.getString("type")
                    locationText.text = obj.getString("location")
                    paramsText.text = obj.getString("parameters")
                    currentStatus = obj.getBoolean("isActive")
                    statusText.text = if (currentStatus) "Active" else "Inactive"
                    toggleBtn.text = if (currentStatus) "Deactivate" else "Activate"
                    tempText.text = "T: ${obj.getDouble("minTemperature")} - ${obj.getDouble("maxTemperature")} °C"
                    humidityText.text = "H: ${obj.getDouble("minHumidity")} - ${obj.getDouble("maxHumidity")} %"
                }

            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun toggleDeviceStatus() {
        val token = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE).getString("token", null) ?: return

        val newStatus = !currentStatus
        val patch = listOf(mapOf("op" to "replace", "path" to "/isActive", "value" to newStatus))

        thread {
            try {
                val url = URL("http://10.0.2.2:5000/api/iotdevice/setstatus/$deviceId?isActive=$newStatus")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "PATCH"
                conn.setRequestProperty("Authorization", "Bearer $token")
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true

                conn.outputStream.bufferedWriter().use {
                    it.write(org.json.JSONArray(patch).toString())
                }

                if (conn.responseCode == 200) {
                    runOnUiThread {
                        Toast.makeText(this, "Device status updated", Toast.LENGTH_SHORT).show()
                        loadDeviceDetails()
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this, "Failed to update device", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun deleteDevice() {
        val token = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE).getString("token", null) ?: return

        thread {
            try {
                val url = URL("http://10.0.2.2:5000/api/iotdevice/$deviceId")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "DELETE"
                conn.setRequestProperty("Authorization", "Bearer $token")

                if (conn.responseCode == 200) {
                    runOnUiThread {
                        Toast.makeText(this, "Device deleted", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this, "Failed to delete", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}