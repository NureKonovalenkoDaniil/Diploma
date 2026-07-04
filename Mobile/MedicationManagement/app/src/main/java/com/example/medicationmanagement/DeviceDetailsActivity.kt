package com.example.medicationmanagement

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.medicationmanagement.api.RetrofitClient
import com.example.medicationmanagement.databinding.ActivityDeviceDetailsBinding
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import kotlinx.coroutines.launch

class DeviceDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDeviceDetailsBinding
    private var deviceId: String? = null
    private var currentStatus = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeviceDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        deviceId = intent.getStringExtra("deviceID")
        if (deviceId == null) {
            Toast.makeText(this, R.string.device_not_found, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.toggleDeviceBtn.setOnClickListener {
            toggleDeviceStatus()
        }

        binding.editDeviceBtn.setOnClickListener {
            val intent = Intent(this, EditDeviceActivity::class.java)
            intent.putExtra("deviceID", deviceId)
            startActivity(intent)
        }

        binding.deleteDeviceBtn.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle(R.string.confirm_delete)
                .setMessage(R.string.confirm_delete_device_message)
                .setPositiveButton(R.string.yes) { _, _ -> deleteDevice() }
                .setNegativeButton(R.string.no, null)
                .show()
        }
    }

    override fun onResume() {
        super.onResume()
        loadDeviceDetails()
    }

    private fun loadDeviceDetails() {
        val iotApi = RetrofitClient.getIoTDeviceApi(this)
        val conditionApi = RetrofitClient.getStorageConditionApi(this)

        lifecycleScope.launch {
            try {
                val resp = iotApi.getDevice(deviceId!!)
                if (resp.isSuccessful) {
                    val device = resp.body()!!
                    binding.deviceTypeText.text = device.type.ifBlank { "-" }
                    binding.deviceLocationText.text = device.location.ifBlank { "-" }
                    currentStatus = device.isActive
                    binding.deviceStatusText.text = if (currentStatus) getString(R.string.device_status_active) else getString(R.string.device_status_inactive)
                    binding.toggleDeviceBtn.text = if (currentStatus) getString(R.string.device_action_deactivate) else getString(R.string.device_action_activate)
                    
                    binding.deviceTempText.text = getString(R.string.device_temp_range) + ": ${device.minTemperature} - ${device.maxTemperature} °C"
                    binding.deviceHumidityText.text = getString(R.string.device_humidity_range) + ": ${device.minHumidity} - ${device.maxHumidity} %"
                }

                val conditionResp = conditionApi.getByDeviceId(deviceId!!)
                if (conditionResp.isSuccessful) {
                    val conditions = conditionResp.body() ?: emptyList()
                    showChartWithConditions(conditions)
                } else {
                    binding.chartCondition.clear()
                    binding.chartCondition.setNoDataText(getString(R.string.no_chart_data))
                    binding.chartCondition.invalidate()
                }
            } catch (e: Exception) {
                Toast.makeText(this@DeviceDetailsActivity, "${getString(R.string.network_error)}: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showChartWithConditions(conditions: List<com.example.medicationmanagement.api.StorageConditionDto>) {
        if (conditions.isEmpty()) {
            binding.chartCondition.clear()
            binding.chartCondition.setNoDataText(getString(R.string.no_chart_data))
            binding.chartCondition.invalidate()
            return
        }

        val tempEntries = ArrayList<Entry>()
        val humEntries = ArrayList<Entry>()

        conditions.takeLast(20).forEachIndexed { index, cond ->
            tempEntries.add(Entry(index.toFloat(), cond.temperature.toFloat()))
            humEntries.add(Entry(index.toFloat(), cond.humidity.toFloat()))
        }

        val primaryColor = ContextCompat.getColor(this, R.color.brand_primary)
        val secondaryColor = ContextCompat.getColor(this, R.color.brand_secondary)

        val tempSet = LineDataSet(tempEntries, "Temperature °C")
        tempSet.lineWidth = 2f
        tempSet.circleRadius = 3f
        tempSet.setDrawValues(false)
        tempSet.color = primaryColor
        tempSet.setCircleColor(primaryColor)

        val humSet = LineDataSet(humEntries, "Humidity %")
        humSet.lineWidth = 2f
        humSet.circleRadius = 3f
        humSet.setDrawValues(false)
        humSet.color = secondaryColor
        humSet.setCircleColor(secondaryColor)

        val data = LineData(tempSet, humSet)
        binding.chartCondition.data = data
        val desc = Description()
        desc.text = getString(R.string.chart_description_latest)
        binding.chartCondition.description = desc
        binding.chartCondition.setNoDataText(getString(R.string.no_chart_data))
        binding.chartCondition.invalidate()
    }

    private fun toggleDeviceStatus() {
        val iotApi = RetrofitClient.getIoTDeviceApi(this)
        lifecycleScope.launch {
            try {
                val resp = iotApi.setDeviceStatus(deviceId!!, !currentStatus)
                if (resp.isSuccessful) {
                    Toast.makeText(this@DeviceDetailsActivity, R.string.device_status_updated, Toast.LENGTH_SHORT).show()
                    loadDeviceDetails()
                } else {
                    Toast.makeText(this@DeviceDetailsActivity, R.string.device_update_failed, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@DeviceDetailsActivity, "${getString(R.string.network_error)}: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun deleteDevice() {
        val iotApi = RetrofitClient.getIoTDeviceApi(this)
        lifecycleScope.launch {
            try {
                val resp = iotApi.deleteDevice(deviceId!!)
                if (resp.isSuccessful) {
                    Toast.makeText(this@DeviceDetailsActivity, R.string.device_deleted, Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@DeviceDetailsActivity, R.string.device_delete_failed, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@DeviceDetailsActivity, "${getString(R.string.network_error)}: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
