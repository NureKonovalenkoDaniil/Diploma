package com.example.medicationmanagement

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.medicationmanagement.api.ApiClient
import com.example.medicationmanagement.api.MedicineApi
import com.example.medicationmanagement.model.Medicine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddMedicineActivity : AppCompatActivity() {

    private val calendar = Calendar.getInstance()
    private lateinit var expiryInput: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_medicine)

        val name = findViewById<EditText>(R.id.inputName)
        val type = findViewById<EditText>(R.id.inputType)
        val category = findViewById<EditText>(R.id.inputCategory)
        val quantity = findViewById<EditText>(R.id.inputQuantity)
        val manufacturer = findViewById<EditText>(R.id.inputManufacturer)
        val batchNumber = findViewById<EditText>(R.id.inputBatchNumber)
        val description = findViewById<EditText>(R.id.inputDescription)
        val minTemp = findViewById<EditText>(R.id.inputMinTemp)
        val maxTemp = findViewById<EditText>(R.id.inputMaxTemp)
        val minHumidity = findViewById<EditText>(R.id.inputMinHumidity)
        val maxHumidity = findViewById<EditText>(R.id.inputMaxHumidity)
        val storageLocationId = findViewById<EditText>(R.id.inputStorageLocationId)
        expiryInput = findViewById(R.id.inputExpiry)
        val btnCreate = findViewById<Button>(R.id.btnCreate)

        // DatePicker for Expiry Date
        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, monthOfYear)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            updateDateInView()
        }

        expiryInput.setOnClickListener {
            DatePickerDialog(
                this, dateSetListener,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        btnCreate.setOnClickListener {
            val n = name.text.toString().trim()
            val t = type.text.toString().trim()
            val c = category.text.toString().trim()
            val qStr = quantity.text.toString().trim()
            val e = expiryInput.text.toString().trim()

            if (n.isEmpty() || t.isEmpty() || c.isEmpty() || qStr.isEmpty() || e.isEmpty()) {
                Toast.makeText(this, "Заповніть усі поля", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val q = qStr.toIntOrNull()
            if (q == null || q < 0) {
                Toast.makeText(this, "Некоректна кількість", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            createMedicine(
                name = n,
                type = t,
                category = c,
                quantity = q,
                expiryDate = e,
                manufacturer = manufacturer.text.toString().trim().ifBlank { null },
                batchNumber = batchNumber.text.toString().trim().ifBlank { null },
                description = description.text.toString().trim().ifBlank { null },
                minTemp = minTemp.text.toString().trim().toDoubleOrNull(),
                maxTemp = maxTemp.text.toString().trim().toDoubleOrNull(),
                minHumidity = minHumidity.text.toString().trim().toDoubleOrNull(),
                maxHumidity = maxHumidity.text.toString().trim().toDoubleOrNull(),
                storageLocationId = storageLocationId.text.toString().trim().toIntOrNull()
            )
        }
    }

    private fun updateDateInView() {
        val myFormat = "yyyy-MM-dd"
        val sdf = SimpleDateFormat(myFormat, Locale.getDefault())
        expiryInput.setText(sdf.format(calendar.time))
    }

    private fun createMedicine(
        name: String,
        type: String,
        category: String,
        quantity: Int,
        expiryDate: String,
        manufacturer: String?,
        batchNumber: String?,
        description: String?,
        minTemp: Double?,
        maxTemp: Double?,
        minHumidity: Double?,
        maxHumidity: Double?,
        storageLocationId: Int?
    ) {
        val btnCreate = findViewById<Button>(R.id.btnCreate)
        btnCreate.isEnabled = false
        btnCreate.text = "Створення..."

        lifecycleScope.launch {
            try {
                val api = ApiClient.createService<MedicineApi>(this@AddMedicineActivity)
                val newMedicine = Medicine(
                    medicineID = 0,
                    name = name,
                    type = type,
                    category = category,
                    quantity = quantity,
                    expiryDate = "${expiryDate}T00:00:00",
                    manufacturer = manufacturer,
                    batchNumber = batchNumber,
                    description = description,
                    minStorageTemp = minTemp,
                    maxStorageTemp = maxTemp,
                    minStorageHumidity = minHumidity,
                    maxStorageHumidity = maxHumidity,
                    storageLocationId = storageLocationId
                )
                
                val response = api.createMedicine(newMedicine)

                if (response.isSuccessful) {
                    Toast.makeText(this@AddMedicineActivity, "Препарат успішно додано", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@AddMedicineActivity, "Помилка: ${response.code()}", Toast.LENGTH_SHORT).show()
                    btnCreate.isEnabled = true
                    btnCreate.text = "Create"
                }
            } catch (e: Exception) {
                Toast.makeText(this@AddMedicineActivity, "Помилка мережі", Toast.LENGTH_SHORT).show()
                btnCreate.isEnabled = true
                btnCreate.text = "Create"
            }
        }
    }
}