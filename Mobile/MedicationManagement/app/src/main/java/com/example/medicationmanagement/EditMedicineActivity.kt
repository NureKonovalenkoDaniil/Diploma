package com.example.medicationmanagement

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class EditMedicineActivity : AppCompatActivity() {
    private var medicineID: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_medicine)

        val name = findViewById<EditText>(R.id.editName)
        val type = findViewById<EditText>(R.id.editType)
        val category = findViewById<EditText>(R.id.editCategory)
        val quantity = findViewById<EditText>(R.id.editQuantity)
        val expiry = findViewById<EditText>(R.id.editExpiry)
        val save = findViewById<Button>(R.id.saveBtn)

        medicineID = intent.getIntExtra("medicineID", -1)
        if (medicineID == -1) {
            Toast.makeText(this, "Invalid medicine ID", Toast.LENGTH_SHORT).show()
            finish()
        }

        val token = getSharedPreferences("app_prefs", MODE_PRIVATE).getString("token", "") ?: ""

        // GET поточні дані
        Thread {
            try {
                val conn = URL("http://10.0.2.2:5000/api/medicine/$medicineID")
                    .openConnection() as HttpURLConnection
                conn.setRequestProperty("Authorization", "Bearer $token")
                conn.requestMethod = "GET"

                if (conn.responseCode == 200) {
                    val data = JSONObject(conn.inputStream.bufferedReader().readText())
                    runOnUiThread {
                        name.setText(data.getString("name"))
                        type.setText(data.getString("type"))
                        category.setText(data.getString("category"))
                        quantity.setText(data.getInt("quantity").toString())
                        expiry.setText(data.getString("expiryDate").substring(0, 10))
                    }
                }
                conn.disconnect()
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Error loading data", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()

        save.setOnClickListener {
            val patchDoc = JSONArray().apply {
                put(jsonPatch("replace", "/name", name.text.toString()))
                put(jsonPatch("replace", "/type", type.text.toString()))
                put(jsonPatch("replace", "/category", category.text.toString()))
                put(jsonPatch("replace", "/quantity", quantity.text.toString().toInt()))
                put(jsonPatch("replace", "/expiryDate", expiry.text.toString()))
            }

            Thread {
                try {
                    val conn = URL("http://10.0.2.2:5000/api/medicine/$medicineID")
                        .openConnection() as HttpURLConnection
                    conn.setRequestProperty("Authorization", "Bearer $token")
                    conn.setRequestProperty("Content-Type", "application/json")
                    conn.requestMethod = "PATCH"
                    conn.doOutput = true
                    conn.outputStream.use { it.write(patchDoc.toString().toByteArray()) }

                    if (conn.responseCode == 200) {
                        runOnUiThread {
                            Toast.makeText(this, "Updated", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, DashboardActivity::class.java))
                            finish()
                        }
                    } else {
                        runOnUiThread {
                            Toast.makeText(this, "Failed to update", Toast.LENGTH_SHORT).show()
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

    private fun jsonPatch(op: String, path: String, value: Any): JSONObject {
        return JSONObject().apply {
            put("op", op)
            put("path", path)
            put("value", value)
        }
    }
}