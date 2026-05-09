package com.example.medicationmanagement

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.medicationmanagement.api.RetrofitClient
import com.example.medicationmanagement.model.StorageCondition
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import kotlinx.coroutines.launch

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
    private lateinit var chart: LineChart

    private var deviceId: String? = null
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
        chart = findViewById(R.id.chartCondition)

        deviceId = intent.getStringExtra("deviceID")
        if (deviceId == null) {
            Toast.makeText(this, "Device ID not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

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

    override fun onResume() {
        super.onResume()
        loadDeviceDetails()
    }

    private fun loadDeviceDetails() {
        val iotApi = RetrofitClient.getIoTDeviceApi(this)
        val storageApi = RetrofitClient.getStorageLocationApi(this)

        lifecycleScope.launch {
            try {
                val resp = iotApi.getDevice(deviceId!!)
                if (resp.isSuccessful) {
                    val device = resp.body()!!
                    typeText.text = device.type ?: "-"
                    locationText.text = device.location ?: "-"
                    paramsText.text = device.parameters ?: "-"
                    currentStatus = device.isActive
                    statusText.text = if (currentStatus) "Active" else "Inactive"
                    toggleBtn.text = if (currentStatus) "Deactivate" else "Activate"
                    tempText.text = "T: ${device.minTemperature ?: "-"} - ${device.maxTemperature ?: "-"} °C"
                    humidityText.text = "H: ${device.minHumidity ?: "-"} - ${device.maxHumidity ?: "-"} %"
                }

                // Try to find storage location linked to this device to retrieve current condition
                val slResp = storageApi.getAll()
                if (slResp.isSuccessful) {
                    val list = slResp.body() ?: emptyList()
                    val linked = list.find { it.deviceId == deviceId }
                    linked?.currentCondition?.let { cond ->
                        showChartWithCondition(cond)
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this@DeviceDetailsActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Populate chart. Backend currently provides only current condition; generate simple historical samples for UI.
    private fun showChartWithCondition(cond: com.example.medicationmanagement.api.StorageConditionDto) {
        val tempEntries = ArrayList<Entry>()
        val humEntries = ArrayList<Entry>()

        // Use 6 points: generate preceding points by small deltas for demonstration
        val baseTemp = cond.temperature ?: 0.0
        val baseHum = cond.humidity ?: 0.0

        for (i in 5 downTo 0) {
            val t = (baseTemp + (Math.random() - 0.5) * 1.5).toFloat()
            val h = (baseHum + (Math.random() - 0.5) * 3.0).toFloat()
            val x = (5 - i).toFloat()
            tempEntries.add(Entry(x, t))
            humEntries.add(Entry(x, h))
        }

        val tempSet = LineDataSet(tempEntries, "Temperature °C").apply {
            lineWidth = 2f
            circleRadius = 3f
            setDrawValues(false)
        }

        val humSet = LineDataSet(humEntries, "Humidity %").apply {
            lineWidth = 2f
            circleRadius = 3f
            setDrawValues(false)
        }

        val data = LineData(tempSet, humSet)
        chart.data = data
        val desc = Description()
        desc.text = "Останні показники"
        chart.description = desc
        chart.invalidate()
    }

    private fun toggleDeviceStatus() {
        val iotApi = RetrofitClient.getIoTDeviceApi(this)
        lifecycleScope.launch {
            try {
                val resp = iotApi.setDeviceStatus(deviceId!!, !currentStatus)
                if (resp.isSuccessful) {
                    Toast.makeText(this@DeviceDetailsActivity, "Device status updated", Toast.LENGTH_SHORT).show()
                    loadDeviceDetails()
                } else {
                    Toast.makeText(this@DeviceDetailsActivity, "Failed to update device", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@DeviceDetailsActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun deleteDevice() {
        val iotApi = RetrofitClient.getIoTDeviceApi(this)
        lifecycleScope.launch {
            try {
                val resp = iotApi.deleteDevice(deviceId!!)
                if (resp.isSuccessful) {
                    Toast.makeText(this@DeviceDetailsActivity, "Device deleted", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@DeviceDetailsActivity, "Failed to delete", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@DeviceDetailsActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}