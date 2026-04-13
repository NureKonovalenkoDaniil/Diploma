package com.example.medicationmanagement

import android.content.Context
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.medicationmanagement.model.IoTDevice
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class AddDeviceActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_device)

        val typeField = findViewById<EditText>(R.id.addType)
        val locationField = findViewById<EditText>(R.id.addLocation)
        val parametersField = findViewById<EditText>(R.id.addParams)
        val isActiveCheck = findViewById<CheckBox>(R.id.addIsActive)
        val minTempField = findViewById<EditText>(R.id.addMinTemp)
        val maxTempField = findViewById<EditText>(R.id.addMaxTemp)
        val minHumField = findViewById<EditText>(R.id.addMinHumidity)
        val maxHumField = findViewById<EditText>(R.id.addMaxHumidity)
        val addButton = findViewById<Button>(R.id.addDeviceBtn)

        addButton.setOnClickListener {
            val type = typeField.text.toString()
            val location = locationField.text.toString()
            val parameters = parametersField.text.toString()
            val isActive = isActiveCheck.isChecked
            val minTemp = minTempField.text.toString().toDoubleOrNull() ?: 0.0
            val maxTemp = maxTempField.text.toString().toDoubleOrNull() ?: 0.0
            val minHum = minHumField.text.toString().toDoubleOrNull() ?: 0.0
            val maxHum = maxHumField.text.toString().toDoubleOrNull() ?: 0.0

            val json = JSONObject().apply {
                put("type", type)
                put("location", location)
                put("parameters", parameters)
                put("isActive", isActive)
                put("minTemperature", minTemp)
                put("maxTemperature", maxTemp)
                put("minHumidity", minHum)
                put("maxHumidity", maxHum)
            }

            val token = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE).getString("token", null)

            thread {
                try {
                    val url = URL("http://10.0.2.2:5000/api/iotdevice")
                    val connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "POST"
                    connection.setRequestProperty("Content-Type", "application/json")
                    connection.setRequestProperty("Authorization", "Bearer $token")
                    connection.doOutput = true

                    val writer = OutputStreamWriter(connection.outputStream)
                    writer.write(json.toString())
                    writer.flush()
                    writer.close()

                    val code = connection.responseCode
                    runOnUiThread {
                        if (code == 201 || code == 200) {
                            Toast.makeText(this, "Device added", Toast.LENGTH_SHORT).show()
                            setResult(RESULT_OK)
                            finish()
                        } else {
                            Toast.makeText(this, "Failed to add: $code", Toast.LENGTH_LONG).show()
                        }
                    }

                } catch (e: Exception) {
                    runOnUiThread {
                        Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}