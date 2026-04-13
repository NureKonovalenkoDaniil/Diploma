package com.example.medicationmanagement

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.medicationmanagement.model.Medicine
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

class DashboardActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private val medicines = mutableListOf<Medicine>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        val addButton = findViewById<Button>(R.id.addMedicineButton)

        recyclerView = findViewById(R.id.medicineRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = MedicineAdapter(medicines)

        addButton.setOnClickListener {
            startActivity(Intent(this, AddMedicineActivity::class.java))
            finish()
        }

        loadMedicines()
    }

    private fun loadMedicines() {
        Thread {
            try {
                val token = getSharedPreferences("app_prefs", MODE_PRIVATE)
                    .getString("token", "") ?: ""

                val url = URL("http://10.0.2.2:5000/api/medicine")
                val conn = url.openConnection() as HttpURLConnection
                conn.setRequestProperty("Authorization", "Bearer $token")
                conn.requestMethod = "GET"

                val responseCode = conn.responseCode
                if (responseCode == 200) {
                    val json = conn.inputStream.bufferedReader().readText()
                    val arr = JSONArray(json)

                    medicines.clear()
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        medicines.add(
                            Medicine(
                                obj.getInt("medicineID"),
                                obj.getString("name"),
                                obj.getString("type"),
                                obj.getString("expiryDate"),
                                obj.getInt("quantity"),
                                obj.getString("category")
                            )
                        )
                    }

                    runOnUiThread {
                        recyclerView.adapter = MedicineAdapter(medicines)
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this, "Failed to load medicines", Toast.LENGTH_SHORT).show()
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