package com.example.medicationmanagement

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.medicationmanagement.api.ApiClient
import com.example.medicationmanagement.api.LifecycleApi
import com.example.medicationmanagement.api.MedicineApi
import kotlinx.coroutines.launch

class MedicineDetailsActivity : AppCompatActivity() {
    private var medicineID = -1
    private lateinit var adapter: LifecycleEventAdapter

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
        findViewById<TextView>(R.id.detailType).text = "$type | $category"
        findViewById<TextView>(R.id.detailQuantity).text = "В наявності: $quantity шт."
        findViewById<TextView>(R.id.detailExpiryDate).text = "Термін придатності: $expiry"

        val btnEdit = findViewById<Button>(R.id.btnEdit)
        btnEdit.setOnClickListener {
            val intent = Intent(this, EditMedicineActivity::class.java)
            intent.putExtra("medicineID", medicineID)
            startActivity(intent)
        }

        val btnDelete = findViewById<Button>(R.id.btnDelete)
        btnDelete.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Видалити препарат")
                .setMessage("Ви впевнені, що хочете видалити цей препарат з аптечки?")
                .setPositiveButton("Так") { _, _ -> deleteMedicine() }
                .setNegativeButton("Ні", null)
                .show()
        }

        setupDiaryRecyclerView()
        loadDiary()
    }

    private fun setupDiaryRecyclerView() {
        val recyclerView = findViewById<RecyclerView>(R.id.diaryRecyclerView)
        adapter = LifecycleEventAdapter(emptyList())
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun loadDiary() {
        if (medicineID == -1) return

        val progressBar = findViewById<ProgressBar>(R.id.diaryProgressBar)
        progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val api = ApiClient.createService<LifecycleApi>(this@MedicineDetailsActivity)
                val response = api.getEventsByMedicineId(medicineID)
                
                if (response.isSuccessful) {
                    val events = response.body() ?: emptyList()
                    // Показуємо найновіші зверху
                    adapter.updateEvents(events.sortedByDescending { it.eventDate })
                } else {
                    Toast.makeText(this@MedicineDetailsActivity, "Не вдалося завантажити щоденник", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MedicineDetailsActivity, "Помилка мережі", Toast.LENGTH_SHORT).show()
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun deleteMedicine() {
        if (medicineID == -1) return

        val btnDelete = findViewById<Button>(R.id.btnDelete)
        btnDelete.isEnabled = false

        lifecycleScope.launch {
            try {
                val api = ApiClient.createService<MedicineApi>(this@MedicineDetailsActivity)
                val response = api.deleteMedicine(medicineID)

                if (response.isSuccessful) {
                    Toast.makeText(this@MedicineDetailsActivity, "Успішно видалено", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@MedicineDetailsActivity, "Помилка видалення", Toast.LENGTH_SHORT).show()
                    btnDelete.isEnabled = true
                }
            } catch (e: Exception) {
                Toast.makeText(this@MedicineDetailsActivity, "Помилка мережі", Toast.LENGTH_SHORT).show()
                btnDelete.isEnabled = true
            }
        }
    }
}