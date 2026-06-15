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
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class EditMedicineActivity : AppCompatActivity() {
    private var medicineID: Int = -1
    private val calendar = Calendar.getInstance()
    private lateinit var expiryInput: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_medicine)

        val name = findViewById<EditText>(R.id.editName)
        val type = findViewById<EditText>(R.id.editType)
        val category = findViewById<EditText>(R.id.editCategory)
        val quantity = findViewById<EditText>(R.id.editQuantity)
        val manufacturer = findViewById<EditText>(R.id.editManufacturer)
        val batchNumber = findViewById<EditText>(R.id.editBatchNumber)
        val description = findViewById<EditText>(R.id.editDescription)
        val minTemp = findViewById<EditText>(R.id.editMinTemp)
        val maxTemp = findViewById<EditText>(R.id.editMaxTemp)
        val minHumidity = findViewById<EditText>(R.id.editMinHumidity)
        val maxHumidity = findViewById<EditText>(R.id.editMaxHumidity)
        val storageLocationId = findViewById<EditText>(R.id.editStorageLocationId)
        expiryInput = findViewById(R.id.editExpiry)
        val saveBtn = findViewById<Button>(R.id.saveBtn)

        medicineID = intent.getIntExtra("medicineID", -1)
        if (medicineID == -1) {
            Toast.makeText(this, R.string.medicine_invalid_id, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

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

        // Завантаження даних
        loadData(name, type, category, quantity, manufacturer, batchNumber, description, minTemp, maxTemp, minHumidity, maxHumidity, storageLocationId, expiryInput)

        saveBtn.setOnClickListener {
            val n = name.text.toString().trim()
            val t = type.text.toString().trim()
            val c = category.text.toString().trim()
            val qStr = quantity.text.toString().trim()
            val e = expiryInput.text.toString().trim()

            if (n.isEmpty() || t.isEmpty() || c.isEmpty() || qStr.isEmpty() || e.isEmpty()) {
                Toast.makeText(this, R.string.medicine_fill_all_fields, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val q = qStr.toIntOrNull()
            if (q == null || q < 0) {
                Toast.makeText(this, R.string.medicine_invalid_quantity, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            saveChanges(
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
                storageLocationId = storageLocationId.text.toString().trim().toIntOrNull(),
                btn = saveBtn
            )
        }
    }

    private fun updateDateInView() {
        val myFormat = "yyyy-MM-dd"
        val sdf = SimpleDateFormat(myFormat, Locale.getDefault())
        expiryInput.setText(sdf.format(calendar.time))
    }

    private fun loadData(
        name: EditText,
        type: EditText,
        category: EditText,
        quantity: EditText,
        manufacturer: EditText,
        batchNumber: EditText,
        description: EditText,
        minTemp: EditText,
        maxTemp: EditText,
        minHumidity: EditText,
        maxHumidity: EditText,
        storageLocationId: EditText,
        expiry: EditText
    ) {
        lifecycleScope.launch {
            try {
                val api = ApiClient.createService<MedicineApi>(this@EditMedicineActivity)
                val response = api.getMedicine(medicineID)

                if (response.isSuccessful) {
                    val data = response.body()
                    if (data != null) {
                        name.setText(data.name)
                        type.setText(data.type)
                        category.setText(data.category)
                        quantity.setText(data.quantity.toString())
                        manufacturer.setText(data.manufacturer.orEmpty())
                        batchNumber.setText(data.batchNumber.orEmpty())
                        description.setText(data.description.orEmpty())
                        minTemp.setText(data.minStorageTemp?.toString().orEmpty())
                        maxTemp.setText(data.maxStorageTemp?.toString().orEmpty())
                        minHumidity.setText(data.minStorageHumidity?.toString().orEmpty())
                        maxHumidity.setText(data.maxStorageHumidity?.toString().orEmpty())
                        storageLocationId.setText(data.storageLocationId?.toString().orEmpty())
                        
                        // Парсимо дату з ISO в yyyy-MM-dd
                        try {
                            val isoParser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                            val dateObj = isoParser.parse(data.expiryDate)
                            if (dateObj != null) {
                                calendar.time = dateObj
                                updateDateInView()
                            } else {
                                expiry.setText(data.expiryDate.take(10))
                            }
                        } catch (e: Exception) {
                            expiry.setText(data.expiryDate.take(10))
                        }
                    }
                } else {
                    Toast.makeText(this@EditMedicineActivity, "Помилка завантаження", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@EditMedicineActivity, "Помилка мережі", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveChanges(
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
        storageLocationId: Int?,
        btn: Button
    ) {
        btn.isEnabled = false
        btn.text = "Збереження..."

        val patchBody = listOf(
            mapOf("op" to "replace", "path" to "/name", "value" to name),
            mapOf("op" to "replace", "path" to "/type", "value" to type),
            mapOf("op" to "replace", "path" to "/category", "value" to category),
            mapOf("op" to "replace", "path" to "/quantity", "value" to quantity),
            mapOf("op" to "replace", "path" to "/expiryDate", "value" to "${expiryDate}T00:00:00"),
            mapOf("op" to "replace", "path" to "/manufacturer", "value" to (manufacturer ?: "")),
            mapOf("op" to "replace", "path" to "/batchNumber", "value" to (batchNumber ?: "")),
            mapOf("op" to "replace", "path" to "/description", "value" to (description ?: "")),
            mapOf("op" to "replace", "path" to "/minStorageTemp", "value" to minTemp),
            mapOf("op" to "replace", "path" to "/maxStorageTemp", "value" to maxTemp),
            mapOf("op" to "replace", "path" to "/minStorageHumidity", "value" to minHumidity),
            mapOf("op" to "replace", "path" to "/maxStorageHumidity", "value" to maxHumidity),
            mapOf("op" to "replace", "path" to "/storageLocationId", "value" to storageLocationId)
        )

        lifecycleScope.launch {
            try {
                val api = ApiClient.createService<MedicineApi>(this@EditMedicineActivity)
                val response = api.updateMedicine(medicineID, patchBody)

                if (response.isSuccessful) {
                    Toast.makeText(this@EditMedicineActivity, R.string.medicine_updated_success, Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@EditMedicineActivity, R.string.medicine_update_failed, Toast.LENGTH_SHORT).show()
                    btn.isEnabled = true
                    btn.text = getString(R.string.edit_medicine)
                }
            } catch (e: Exception) {
                Toast.makeText(this@EditMedicineActivity, R.string.medicine_update_network_error, Toast.LENGTH_SHORT).show()
                btn.isEnabled = true
                btn.text = getString(R.string.edit_medicine)
            }
        }
    }
}