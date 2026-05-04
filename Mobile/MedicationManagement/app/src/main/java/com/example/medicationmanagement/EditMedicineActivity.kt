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
        expiryInput = findViewById(R.id.editExpiry)
        val saveBtn = findViewById<Button>(R.id.saveBtn)

        medicineID = intent.getIntExtra("medicineID", -1)
        if (medicineID == -1) {
            Toast.makeText(this, "Invalid medicine ID", Toast.LENGTH_SHORT).show()
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
        loadData(name, type, category, quantity, expiryInput)

        saveBtn.setOnClickListener {
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

            saveChanges(n, t, c, q, e, saveBtn)
        }
    }

    private fun updateDateInView() {
        val myFormat = "yyyy-MM-dd"
        val sdf = SimpleDateFormat(myFormat, Locale.getDefault())
        expiryInput.setText(sdf.format(calendar.time))
    }

    private fun loadData(name: EditText, type: EditText, category: EditText, quantity: EditText, expiry: EditText) {
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

    private fun saveChanges(name: String, type: String, category: String, quantity: Int, expiryDate: String, btn: Button) {
        btn.isEnabled = false
        btn.text = "Збереження..."

        val patchBody = listOf(
            mapOf("op" to "replace", "path" to "/name", "value" to name),
            mapOf("op" to "replace", "path" to "/type", "value" to type),
            mapOf("op" to "replace", "path" to "/category", "value" to category),
            mapOf("op" to "replace", "path" to "/quantity", "value" to quantity),
            mapOf("op" to "replace", "path" to "/expiryDate", "value" to "${expiryDate}T00:00:00")
        )

        lifecycleScope.launch {
            try {
                val api = ApiClient.createService<MedicineApi>(this@EditMedicineActivity)
                val response = api.updateMedicine(medicineID, patchBody)

                if (response.isSuccessful) {
                    Toast.makeText(this@EditMedicineActivity, "Успішно оновлено", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@EditMedicineActivity, "Помилка оновлення", Toast.LENGTH_SHORT).show()
                    btn.isEnabled = true
                    btn.text = "Save"
                }
            } catch (e: Exception) {
                Toast.makeText(this@EditMedicineActivity, "Помилка мережі", Toast.LENGTH_SHORT).show()
                btn.isEnabled = true
                btn.text = "Save"
            }
        }
    }
}