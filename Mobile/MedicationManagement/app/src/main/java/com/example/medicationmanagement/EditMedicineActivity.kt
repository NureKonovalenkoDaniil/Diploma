package com.example.medicationmanagement

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.medicationmanagement.ui.EditMedicineViewModel
import com.example.medicationmanagement.ui.EditMedicineViewModelFactory
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class EditMedicineActivity : AppCompatActivity() {

    private lateinit var viewModel: EditMedicineViewModel
    private var medicineID: Int = -1
    private val calendar = Calendar.getInstance()
    private lateinit var expiryInput: EditText

    private lateinit var name: EditText
    private lateinit var type: EditText
    private lateinit var category: EditText
    private lateinit var quantity: EditText
    private lateinit var manufacturer: EditText
    private lateinit var batchNumber: EditText
    private lateinit var description: EditText
    private lateinit var minTemp: EditText
    private lateinit var maxTemp: EditText
    private lateinit var minHumidity: EditText
    private lateinit var maxHumidity: EditText
    private lateinit var storageLocationId: EditText
    private lateinit var saveBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_medicine)

        val factory = EditMedicineViewModelFactory(this)
        viewModel = ViewModelProvider(this, factory)[EditMedicineViewModel::class.java]

        name = findViewById(R.id.editName)
        type = findViewById(R.id.editType)
        category = findViewById(R.id.editCategory)
        quantity = findViewById(R.id.editQuantity)
        manufacturer = findViewById(R.id.editManufacturer)
        batchNumber = findViewById(R.id.editBatchNumber)
        description = findViewById(R.id.editDescription)
        minTemp = findViewById(R.id.editMinTemp)
        maxTemp = findViewById(R.id.editMaxTemp)
        minHumidity = findViewById(R.id.editMinHumidity)
        maxHumidity = findViewById(R.id.editMaxHumidity)
        storageLocationId = findViewById(R.id.editStorageLocationId)
        expiryInput = findViewById(R.id.editExpiry)
        saveBtn = findViewById(R.id.saveBtn)

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

        viewModel.loadMedicine(medicineID)

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
                storageLocationId = storageLocationId.text.toString().trim().toIntOrNull()
            )
        }

        setupObservers()
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.medicine.collect { data ->
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
                    
                    try {
                        val isoParser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                        val dateObj = isoParser.parse(data.expiryDate)
                        if (dateObj != null) {
                            calendar.time = dateObj
                            updateDateInView()
                        } else {
                            expiryInput.setText(data.expiryDate.take(10))
                        }
                    } catch (e: Exception) {
                        expiryInput.setText(data.expiryDate.take(10))
                    }
                }
            }
        }

        lifecycleScope.launch {
            viewModel.isSaving.collect { isSaving ->
                saveBtn.isEnabled = !isSaving
                saveBtn.text = if (isSaving) "Збереження..." else getString(R.string.save_changes)
            }
        }

        lifecycleScope.launch {
            viewModel.success.collect { isSuccess ->
                if (isSuccess) {
                    Toast.makeText(this@EditMedicineActivity, R.string.medicine_updated_success, Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }

        lifecycleScope.launch {
            viewModel.error.collect { errorMsg ->
                if (errorMsg != null) {
                    Toast.makeText(this@EditMedicineActivity, errorMsg, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun updateDateInView() {
        val myFormat = "yyyy-MM-dd"
        val sdf = SimpleDateFormat(myFormat, Locale.getDefault())
        expiryInput.setText(sdf.format(calendar.time))
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
        storageLocationId: Int?
    ) {
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
        viewModel.updateMedicine(medicineID, patchBody)
    }
}