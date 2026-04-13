package com.example.medicationmanagement

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.net.HttpURLConnection
import java.net.URL

class MedicineDetailsActivity : AppCompatActivity() {
    private var medicineID = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_medicine_details)

        val name = intent.getStringExtra("name")
        val type = intent.getStringExtra("type")
        val category = intent.getStringExtra("category")
        val quantity = intent.getIntExtra("quantity", 0)
        val expiry = intent.getStringExtra("expiryDate")
        medicineID = intent.getIntExtra("medicineID", -1)

        findViewById<TextView>(R.id.detailName).text = name
        findViewById<TextView>(R.id.detailType).text = "Type: $type"
        findViewById<TextView>(R.id.detailCategory).text = "Category: $category"
        findViewById<TextView>(R.id.detailQuantity).text = "Quantity: $quantity"
        findViewById<TextView>(R.id.detailExpiryDate).text = "Expiry Date: $expiry"

        val btnEdit = findViewById<Button>(R.id.btnEdit)
        btnEdit.setOnClickListener {
            val intent = Intent(this, EditMedicineActivity::class.java)
            intent.putExtra("medicineID", medicineID)
            startActivity(intent)
        }

        val btnDelete = findViewById<Button>(R.id.btnDelete)
        btnDelete.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Delete Medicine")
                .setMessage("Are you sure you want to delete this medicine?")
                .setPositiveButton("Yes") { _, _ -> deleteMedicine() }
                .setNegativeButton("No", null)
                .show()
        }
    }

    private fun deleteMedicine() {
        if (medicineID == -1) return

        Thread {
            try {
                val token = getSharedPreferences("app_prefs", MODE_PRIVATE)
                    .getString("token", "") ?: ""

                val url = URL("http://10.0.2.2:5000/api/medicine/$medicineID")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "DELETE"
                conn.setRequestProperty("Authorization", "Bearer $token")

                val responseCode = conn.responseCode
                runOnUiThread {
                    if (responseCode == 200 || responseCode == 204) {
                        Toast.makeText(this, "Deleted successfully", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, DashboardActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this, "Error deleting (code $responseCode)", Toast.LENGTH_LONG).show()
                    }
                }

                conn.disconnect()
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Exception: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }
}