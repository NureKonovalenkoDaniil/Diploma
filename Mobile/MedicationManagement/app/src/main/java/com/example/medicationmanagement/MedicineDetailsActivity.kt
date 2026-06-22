package com.example.medicationmanagement

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.medicationmanagement.api.RetrofitClient
import com.example.medicationmanagement.api.LifecycleApi
import com.example.medicationmanagement.api.MedicineApi
import com.example.medicationmanagement.api.MedicineActionsApi
import com.example.medicationmanagement.api.MoveRequest
import com.example.medicationmanagement.api.StorageLocationApi
import com.example.medicationmanagement.model.Medicine
import com.example.medicationmanagement.api.StorageLocationDto
import com.example.medicationmanagement.utils.RoleHelper
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

class MedicineDetailsActivity : AppCompatActivity() {
    private var medicineID = -1
    private lateinit var adapter: LifecycleEventAdapter
    private lateinit var detailName: TextView
    private lateinit var detailType: TextView
    private lateinit var detailQuantity: TextView
    private lateinit var detailExpiryDate: TextView
    private lateinit var detailStatus: TextView
    private lateinit var detailManufacturer: TextView
    private lateinit var detailBatchNumber: TextView
    private lateinit var detailStorageLocation: TextView
    private lateinit var detailStorageTemperature: TextView
    private lateinit var detailStorageHumidity: TextView
    private lateinit var detailDescription: TextView
    private lateinit var diaryProgressBar: ProgressBar
    private lateinit var diaryEmptyState: TextView
    private lateinit var btnEdit: MaterialButton
    private lateinit var btnDelete: MaterialButton
    private lateinit var btnReceive: MaterialButton
    private lateinit var btnIssue: MaterialButton
    private lateinit var btnMove: MaterialButton
    private lateinit var btnDispose: MaterialButton

    private var currentMedicine: Medicine? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_medicine_details)

        val name = intent.getStringExtra("name")
        val type = intent.getStringExtra("type")
        val category = intent.getStringExtra("category")
        val quantity = intent.getIntExtra("quantity", 0)
        val expiry = intent.getStringExtra("expiryDate")
        medicineID = intent.getIntExtra("medicineID", -1)

        detailName = findViewById(R.id.detailName)
        detailType = findViewById(R.id.detailType)
        detailQuantity = findViewById(R.id.detailQuantity)
        detailExpiryDate = findViewById(R.id.detailExpiryDate)
        detailStatus = findViewById(R.id.detailStatus)
        detailManufacturer = findViewById(R.id.detailManufacturer)
        detailBatchNumber = findViewById(R.id.detailBatchNumber)
        detailStorageLocation = findViewById(R.id.detailStorageLocation)
        detailStorageTemperature = findViewById(R.id.detailStorageTemperature)
        detailStorageHumidity = findViewById(R.id.detailStorageHumidity)
        detailDescription = findViewById(R.id.detailDescription)
        diaryProgressBar = findViewById(R.id.diaryProgressBar)
        diaryEmptyState = findViewById(R.id.diaryEmptyState)
        btnEdit = findViewById(R.id.btnEdit)
        btnDelete = findViewById(R.id.btnDelete)
        btnReceive = findViewById(R.id.btnReceive)
        btnIssue = findViewById(R.id.btnIssue)
        btnMove = findViewById(R.id.btnMove)
        btnDispose = findViewById(R.id.btnDispose)

        bindMedicineHeader(name, type, category, quantity, expiry)
        loadMedicineDetails()
        applyRoleBasedVisibility()

        btnEdit.setOnClickListener {
            val intent = Intent(this, EditMedicineActivity::class.java)
            intent.putExtra("medicineID", medicineID)
            startActivity(intent)
        }

        btnDelete.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.medicine_delete_title))
                .setMessage(getString(R.string.medicine_delete_message))
                .setPositiveButton(getString(R.string.yes)) { _, _ -> deleteMedicine() }
                .setNegativeButton(getString(R.string.no), null)
                .show()
        }

        btnReceive.setOnClickListener {
            showQuantityDialog(
                title = getString(R.string.medicine_action_receive),
                positiveText = getString(R.string.medicine_action_receive),
                allowZero = false
            ) { quantityValue ->
                performQuickAction(quantityValue) { api, id, amount ->
                    api.receive(id, com.example.medicationmanagement.api.QuantityRequest(amount))
                }
            }
        }

        btnIssue.setOnClickListener {
            showQuantityDialog(
                title = getString(R.string.medicine_action_issue),
                positiveText = getString(R.string.medicine_action_issue),
                allowZero = false
            ) { quantityValue ->
                performQuickAction(quantityValue) { api, id, amount ->
                    api.issue(id, com.example.medicationmanagement.api.QuantityRequest(amount))
                }
            }
        }

        btnMove.setOnClickListener {
            if (medicineID == -1) return@setOnClickListener

            lifecycleScope.launch {
                try {
                    val storageApi = RetrofitClient.getStorageLocationApi(this@MedicineDetailsActivity)
                    val resp = storageApi.getAll()
                    if (!resp.isSuccessful) {
                        Toast.makeText(this@MedicineDetailsActivity, R.string.network_error, Toast.LENGTH_SHORT).show()
                        return@launch
                    }

                    val locations = resp.body() ?: emptyList<StorageLocationDto>()
                    if (locations.isEmpty()) {
                        Toast.makeText(this@MedicineDetailsActivity, R.string.no_storage_locations, Toast.LENGTH_SHORT).show()
                        return@launch
                    }

                    val names = locations.map { it.name }.toTypedArray()
                    var selectedIndex = 0

                    MaterialAlertDialogBuilder(this@MedicineDetailsActivity)
                        .setTitle(R.string.select_target_location)
                        .setSingleChoiceItems(names, 0) { _, which -> selectedIndex = which }
                        .setPositiveButton(R.string.move) { _, _ ->
                            val target = locations[selectedIndex]
                            performMoveToLocation(target.locationId)
                        }
                        .setNegativeButton(R.string.cancel, null)
                        .show()

                } catch (e: Exception) {
                    Toast.makeText(this@MedicineDetailsActivity, R.string.network_error, Toast.LENGTH_SHORT).show()
                }
            }
        }

        btnDispose.setOnClickListener {
            showQuantityDialog(
                title = getString(R.string.medicine_action_dispose),
                positiveText = getString(R.string.medicine_action_dispose),
                allowZero = true
            ) { quantityValue ->
                performQuickAction(quantityValue) { api, id, amount ->
                    api.dispose(id, com.example.medicationmanagement.api.QuantityRequest(amount))
                }
            }
        }

        setupDiaryRecyclerView()
        loadDiary()
    }

    private fun bindMedicineHeader(name: String?, type: String?, category: String?, quantity: Int, expiry: String?) {
        detailName.text = name ?: getString(R.string.medicine_details_unknown)
        detailType.text = getString(R.string.medicine_details_type_category, type.orEmpty(), category.orEmpty())
        detailQuantity.text = getString(R.string.medicine_details_quantity, quantity)
        detailExpiryDate.text = getString(R.string.medicine_details_expiry, expiry.orEmpty())
    }

    private fun loadMedicineDetails() {
        if (medicineID == -1) return

        lifecycleScope.launch {
            try {
                val api = RetrofitClient.getMedicineApi(this@MedicineDetailsActivity)
                val response = api.getMedicine(medicineID)
                if (response.isSuccessful) {
                    response.body()?.let { medicine ->
                        currentMedicine = medicine
                        detailStatus.text = getString(R.string.medicine_status_label) + ": ${medicine.status.ifBlank { "-" }}"
                        detailManufacturer.text = getString(R.string.medicine_manufacturer) + ": ${medicine.manufacturer ?: "-"}"
                        detailBatchNumber.text = getString(R.string.medicine_batch_number) + ": ${medicine.batchNumber ?: "-"}"
                        detailStorageLocation.text = getString(R.string.medicine_storage_location) + ": ${medicine.storageLocationName ?: "-"}"
                        detailStorageTemperature.text = getString(R.string.medicine_storage_temp_range) + ": ${medicine.minStorageTemp ?: "-"} - ${medicine.maxStorageTemp ?: "-"} °C"
                        detailStorageHumidity.text = getString(R.string.medicine_storage_humidity_range) + ": ${medicine.minStorageHumidity ?: "-"} - ${medicine.maxStorageHumidity ?: "-"} %"
                        detailDescription.text = getString(R.string.medicine_description) + ": ${medicine.description ?: "-"}"
                    }
                }
            } catch (_: Exception) {
                // Keep header-only fallback if the details request fails.
            }
        }
    }

    private fun applyRoleBasedVisibility() {
        val currentRole = RoleHelper.getCurrentRole(this)
        val canManage = RoleHelper.canManageMedicines(currentRole)

        btnEdit.isVisible = canManage
        btnDelete.isVisible = canManage
        btnMove.isVisible = canManage
        btnDispose.isVisible = canManage
    }

    private fun setupDiaryRecyclerView() {
        val recyclerView = findViewById<RecyclerView>(R.id.diaryRecyclerView)
        adapter = LifecycleEventAdapter(emptyList())
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun loadDiary() {
        if (medicineID == -1) return

        diaryProgressBar.visibility = View.VISIBLE
        diaryEmptyState.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val api = RetrofitClient.getLifecycleApi(this@MedicineDetailsActivity)
                val response = api.getEventsByMedicineId(medicineID)
                
                if (response.isSuccessful) {
                    val events = response.body() ?: emptyList()
                    // Показуємо найновіші зверху
                    val sortedEvents = events.sortedByDescending { event -> event.eventDate }
                    adapter.updateEvents(sortedEvents)
                    diaryEmptyState.isVisible = sortedEvents.isEmpty()
                } else {
                    Toast.makeText(this@MedicineDetailsActivity, R.string.medicine_diary_load_failed, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MedicineDetailsActivity, R.string.network_error, Toast.LENGTH_SHORT).show()
            } finally {
                diaryProgressBar.visibility = View.GONE
            }
        }
    }

    private fun showQuantityDialog(
        title: String,
        positiveText: String,
        allowZero: Boolean,
        onConfirm: (Int) -> Unit
    ) {
        val inputLayout = TextInputLayout(this).apply {
            hint = if (allowZero) getString(R.string.medicine_quantity_hint_dispose) else getString(R.string.medicine_quantity_hint)
        }
        val input = TextInputEditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
        }
        inputLayout.addView(input)

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setView(inputLayout)
            .setNegativeButton(getString(R.string.cancel), null)
            .setPositiveButton(positiveText, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val value = input.text?.toString().orEmpty().trim()
                val quantityValue = value.toIntOrNull()
                val isValid = if (allowZero) {
                    quantityValue != null && quantityValue >= 0
                } else {
                    quantityValue != null && quantityValue > 0
                }

                if (!isValid) {
                    Toast.makeText(this, R.string.medicine_quantity_invalid, Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                onConfirm(quantityValue!!)
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun performQuickAction(
        quantity: Int,
        call: suspend (MedicineActionsApi, Int, Int) -> retrofit2.Response<Medicine>
    ) {
        if (medicineID == -1) return

        lifecycleScope.launch {
            try {
                val api = RetrofitClient.getMedicineActionsApi(this@MedicineDetailsActivity)
                val response = call(api, medicineID, quantity)

                if (response.isSuccessful) {
                    response.body()?.let { updatedMedicine ->
                        currentMedicine = updatedMedicine
                        bindMedicineHeader(
                            updatedMedicine.name,
                            updatedMedicine.type,
                            updatedMedicine.category,
                            updatedMedicine.quantity,
                            currentMedicine?.expiryDate ?: detailExpiryDate.text.toString()
                        )
                    }
                    loadDiary()
                    Toast.makeText(this@MedicineDetailsActivity, R.string.medicine_action_success, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@MedicineDetailsActivity, getString(R.string.medicine_action_failed, response.code()), Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MedicineDetailsActivity, e.message ?: getString(R.string.medicine_action_failed_generic), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun performMoveToLocation(targetLocationId: Int) {
        if (medicineID == -1) return

        lifecycleScope.launch {
            try {
                val api = RetrofitClient.getMedicineActionsApi(this@MedicineDetailsActivity)
                val response = api.move(medicineID, MoveRequest(targetLocationId, null))

                if (response.isSuccessful) {
                    response.body()?.let { updatedMedicine ->
                        currentMedicine = updatedMedicine
                        bindMedicineHeader(
                            updatedMedicine.name,
                            updatedMedicine.type,
                            updatedMedicine.category,
                            updatedMedicine.quantity,
                            currentMedicine?.expiryDate ?: detailExpiryDate.text.toString()
                        )
                    }
                    loadDiary()
                    Toast.makeText(this@MedicineDetailsActivity, R.string.medicine_move_success, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@MedicineDetailsActivity, R.string.medicine_move_failed, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MedicineDetailsActivity, R.string.network_error, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun deleteMedicine() {
        if (medicineID == -1) return

        btnDelete.isEnabled = false

        lifecycleScope.launch {
            try {
                val api = RetrofitClient.getMedicineApi(this@MedicineDetailsActivity)
                val response = api.deleteMedicine(medicineID)

                if (response.isSuccessful) {
                    Toast.makeText(this@MedicineDetailsActivity, R.string.medicine_deleted, Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@MedicineDetailsActivity, R.string.medicine_delete_failed, Toast.LENGTH_SHORT).show()
                    btnDelete.isEnabled = true
                }
            } catch (e: Exception) {
                Toast.makeText(this@MedicineDetailsActivity, R.string.network_error, Toast.LENGTH_SHORT).show()
                btnDelete.isEnabled = true
            }
        }
    }
}
