package com.example.medicationmanagement

import android.content.Context
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class EditDeviceActivity : AppCompatActivity() {

    private lateinit var typeInput: EditText
    private lateinit var locationInput: EditText
    private lateinit var parametersInput: EditText
    private lateinit var minTempInput: EditText
    private lateinit var maxTempInput: EditText
    private lateinit var minHumidityInput: EditText
    private lateinit var maxHumidityInput: EditText
    private lateinit var saveBtn: Button

    private var deviceId = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_device)

        typeInput = findViewById(R.id.editDeviceType)
        locationInput = findViewById(R.id.editDeviceLocation)
        parametersInput = findViewById(R.id.editDeviceParams)
        minTempInput = findViewById(R.id.editMinTemp)
        maxTempInput = findViewById(R.id.editMaxTemp)
        minHumidityInput = findViewById(R.id.editMinHumidity)
        maxHumidityInput = findViewById(R.id.editMaxHumidity)
        saveBtn = findViewById(R.id.saveDeviceBtn)

        deviceId = intent.getIntExtra("deviceID", -1)
        if (deviceId == -1) {
            Toast.makeText(this, "Device ID not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        loadDeviceDetails()

        saveBtn.setOnClickListener {
            sendPatchUpdate()
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

                val json = org.json.JSONObject(conn.inputStream.bufferedReader().readText())

                runOnUiThread {
                    typeInput.setText(json.getString("type"))
                    locationInput.setText(json.getString("location"))
                    parametersInput.setText(json.getString("parameters"))
                    minTempInput.setText(json.getDouble("minTemperature").toString())
                    maxTempInput.setText(json.getDouble("maxTemperature").toString())
                    minHumidityInput.setText(json.getDouble("minHumidity").toString())
                    maxHumidityInput.setText(json.getDouble("maxHumidity").toString())
                }

            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Error loading: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun sendPatchUpdate() {
        val token = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE).getString("token", null) ?: return

        val patch = JSONArray()
        patch.put(createPatch("replace", "/type", typeInput.text.toString()))
        patch.put(createPatch("replace", "/location", locationInput.text.toString()))
        patch.put(createPatch("replace", "/parameters", parametersInput.text.toString()))
        patch.put(createPatch("replace", "/minTemperature", minTempInput.text.toString().toDoubleOrNull() ?: 0.0))
        patch.put(createPatch("replace", "/maxTemperature", maxTempInput.text.toString().toDoubleOrNull() ?: 0.0))
        patch.put(createPatch("replace", "/minHumidity", minHumidityInput.text.toString().toDoubleOrNull() ?: 0.0))
        patch.put(createPatch("replace", "/maxHumidity", maxHumidityInput.text.toString().toDoubleOrNull() ?: 0.0))

        thread {
            try {
                val url = URL("http://10.0.2.2:5000/api/iotdevice/$deviceId")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "PATCH"
                conn.setRequestProperty("Authorization", "Bearer $token")
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true

                conn.outputStream.bufferedWriter().use {
                    it.write(patch.toString())
                }

                if (conn.responseCode == 200) {
                    runOnUiThread {
                        Toast.makeText(this, "Device updated", Toast.LENGTH_SHORT).show()
                        finish()
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

    private fun createPatch(op: String, path: String, value: Any): JSONObject {
        return JSONObject().apply {
            put("op", op)
            put("path", path)
            put("value", value)
        }
    }
}