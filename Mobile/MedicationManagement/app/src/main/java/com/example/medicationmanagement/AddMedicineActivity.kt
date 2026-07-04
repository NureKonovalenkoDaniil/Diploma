package com.example.medicationmanagement

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.medicationmanagement.api.RetrofitClient
import com.example.medicationmanagement.api.StorageLocationDto
import com.example.medicationmanagement.model.Medicine
import com.example.medicationmanagement.ui.AddMedicineViewModel
import com.example.medicationmanagement.ui.AddMedicineViewModelFactory
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddMedicineActivity : AppCompatActivity() {

    private lateinit var viewModel: AddMedicineViewModel
    private val calendar = Calendar.getInstance()
    private lateinit var expiryInput: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_medicine)

        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        val factory = AddMedicineViewModelFactory(this)
        viewModel = ViewModelProvider(this, factory)[AddMedicineViewModel::class.java]

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
        val storageLocationSpinner = findViewById<AutoCompleteTextView>(R.id.inputStorageLocationSpinner)
        expiryInput = findViewById(R.id.inputExpiry)
        val btnCreate = findViewById<Button>(R.id.btnCreate)

        var selectedLocationId: Int? = null
        var locationsList: List<StorageLocationDto> = emptyList()

        // Fetch storage locations
        lifecycleScope.launch {
            try {
                val api = RetrofitClient.getStorageLocationApi(this@AddMedicineActivity)
                val response = api.getAll()
                if (response.isSuccessful) {
                    locationsList = response.body() ?: emptyList()
                    val locationNames = mutableListOf("Без локації")
                    locationNames.addAll(locationsList.map { it.name })

                    val adapter = ArrayAdapter(
                        this@AddMedicineActivity,
                        android.R.layout.simple_dropdown_item_1line,
                        locationNames
                    )
                    storageLocationSpinner.setAdapter(adapter)
                    storageLocationSpinner.setOnItemClickListener { _, _, position, _ ->
                        selectedLocationId = if (position == 0) null else locationsList[position - 1].locationId
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this@AddMedicineActivity, "Помилка завантаження локацій", Toast.LENGTH_SHORT).show()
            }
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

        btnCreate.setOnClickListener {
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

            val formattedDate = formatToIsoDate(e)
            if (formattedDate == null) {
                Toast.makeText(this, "Некоректний формат дати терміну придатності. Спробуйте РРРР-ММ-ДД або ДД-ММ-РРРР", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val newMedicine = Medicine(
                medicineID = 0,
                name = n,
                type = t,
                category = c,
                quantity = q,
                expiryDate = "${formattedDate}T00:00:00",
                manufacturer = manufacturer.text.toString().trim().ifBlank { null },
                batchNumber = batchNumber.text.toString().trim().ifBlank { null },
                description = description.text.toString().trim().ifBlank { null },
                minStorageTemp = minTemp.text.toString().trim().toDoubleOrNull(),
                maxStorageTemp = maxTemp.text.toString().trim().toDoubleOrNull(),
                minStorageHumidity = minHumidity.text.toString().trim().toDoubleOrNull(),
                maxStorageHumidity = maxHumidity.text.toString().trim().toDoubleOrNull(),
                storageLocationId = selectedLocationId
            )

            viewModel.createMedicine(newMedicine)
        }

        setupObservers(btnCreate)
    }

    private fun setupObservers(btnCreate: Button) {
        lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                btnCreate.isEnabled = !isLoading
                btnCreate.text = if (isLoading) getString(R.string.medicine_creating) else getString(R.string.create)
            }
        }

        lifecycleScope.launch {
            viewModel.success.collect { isSuccess ->
                if (isSuccess) {
                    Toast.makeText(this@AddMedicineActivity, R.string.medicine_created_success, Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }

        lifecycleScope.launch {
            viewModel.error.collect { errorMsg ->
                if (errorMsg != null) {
                    Toast.makeText(this@AddMedicineActivity, errorMsg, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun updateDateInView() {
        val myFormat = "yyyy-MM-dd"
        val sdf = SimpleDateFormat(myFormat, Locale.getDefault())
        expiryInput.setText(sdf.format(calendar.time))
    }

    private fun formatToIsoDate(input: String): String? {
        val formats = listOf(
            "yyyy-MM-dd",
            "dd-MM-yyyy",
            "dd.MM.yyyy",
            "yyyy.MM.dd",
            "dd/MM/yyyy",
            "yyyy/MM/dd"
        )
        for (format in formats) {
            try {
                val sdf = SimpleDateFormat(format, Locale.US)
                sdf.isLenient = false
                val date = sdf.parse(input)
                if (date != null) {
                    val isoFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    return isoFormat.format(date)
                }
            } catch (ex: Exception) {
                // Ignore and try next format
            }
        }
        return null
    }
}