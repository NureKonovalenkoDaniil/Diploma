package com.example.medicationmanagement

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class AddMedicineActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_medicine)

        val name = findViewById<EditText>(R.id.inputName)
        val type = findViewById<EditText>(R.id.inputType)
        val category = findViewById<EditText>(R.id.inputCategory)
        val quantity = findViewById<EditText>(R.id.inputQuantity)
        val expiry = findViewById<EditText>(R.id.inputExpiry)
        val btn = findViewById<Button>(R.id.btnCreate)

        btn.setOnClickListener {
            val n = name.text.toString().trim()
            val t = type.text.toString().trim()
            val c = category.text.toString().trim()
            val q = quantity.text.toString().trim()
            val e = expiry.text.toString().trim()

            if (n.isEmpty() || t.isEmpty() || c.isEmpty() || q.isEmpty() || e.isEmpty()) {
                Toast.makeText(this, "Fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            createMedicine(n, t, c, q.toInt(), e)
        }
    }

    private fun createMedicine(name: String, type: String, category: String, quantity: Int, expiryDate: String) {
        Thread {
            try {
                val token = getSharedPreferences("app_prefs", MODE_PRIVATE)
                    .getString("token", "") ?: ""

                val url = URL("http://10.0.2.2:5000/api/medicine")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Authorization", "Bearer $token")
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true

                val json = JSONObject().apply {
                    put("name", name)
                    put("type", type)
                    put("category", category)
                    put("quantity", quantity)
                    put("expiryDate", expiryDate)
                }

                conn.outputStream.use { it.write(json.toString().toByteArray()) }

                val responseCode = conn.responseCode
                if (responseCode == 200 || responseCode == 201) {
                    runOnUiThread {
                        Toast.makeText(this, "Created successfully", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, DashboardActivity::class.java))
                        finish()
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this, "Error: $responseCode", Toast.LENGTH_SHORT).show()
                    }
                }

                conn.disconnect()

            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }
}