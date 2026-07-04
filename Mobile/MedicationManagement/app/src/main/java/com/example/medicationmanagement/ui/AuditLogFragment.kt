package com.example.medicationmanagement.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.medicationmanagement.R
import com.example.medicationmanagement.ui.adapter.AuditLogAdapter
import com.google.android.material.button.MaterialButton
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AuditLogFragment : Fragment() {
    private val viewModel: AuditLogViewModel by viewModels {
        AuditLogViewModelFactory(requireContext())
    }

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyState: TextView
    private lateinit var adapter: AuditLogAdapter

    private lateinit var inputUser: TextInputEditText
    private lateinit var inputAction: TextInputEditText
    private lateinit var btnDateFrom: MaterialButton
    private lateinit var btnDateTo: MaterialButton
    private lateinit var btnApply: MaterialButton
    private lateinit var btnReset: MaterialButton

    private var selectedDateFrom: String? = null
    private var selectedDateTo: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_audit_log, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.audit_log_list)
        progressBar = view.findViewById(R.id.audit_log_loading)
        emptyState = view.findViewById(R.id.audit_log_empty)

        inputUser = view.findViewById(R.id.input_user)
        inputAction = view.findViewById(R.id.input_action)
        btnDateFrom = view.findViewById(R.id.btn_date_from)
        btnDateTo = view.findViewById(R.id.btn_date_to)
        btnApply = view.findViewById(R.id.btn_apply)
        btnReset = view.findViewById(R.id.btn_reset)

        adapter = AuditLogAdapter()
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        // Setup Date Pickers
        btnDateFrom.setOnClickListener {
            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(getString(R.string.fromDate))
                .build()
            datePicker.addOnPositiveButtonClickListener { selection ->
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                selectedDateFrom = sdf.format(Date(selection))
                btnDateFrom.text = selectedDateFrom
            }
            datePicker.show(parentFragmentManager, "DATE_PICKER_FROM")
        }

        btnDateTo.setOnClickListener {
            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(getString(R.string.toDate))
                .build()
            datePicker.addOnPositiveButtonClickListener { selection ->
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                selectedDateTo = sdf.format(Date(selection))
                btnDateTo.text = selectedDateTo
            }
            datePicker.show(parentFragmentManager, "DATE_PICKER_TO")
        }

        // Apply filters click
        btnApply.setOnClickListener {
            val user = inputUser.text?.toString()?.trim()
            val action = inputAction.text?.toString()?.trim()
            viewModel.fetchLogs(
                from = selectedDateFrom,
                to = selectedDateTo,
                user = if (user.isNullOrBlank()) null else user,
                action = if (action.isNullOrBlank()) null else action
            )
        }

        // Reset filters click
        btnReset.setOnClickListener {
            inputUser.text = null
            inputAction.text = null
            selectedDateFrom = null
            selectedDateTo = null
            btnDateFrom.setText(R.string.fromDate)
            btnDateTo.setText(R.string.toDate)
            viewModel.fetchLogs()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.logs.collect { logs ->
                adapter.updateLogs(logs)
                emptyState.visibility = if (logs.isEmpty()) View.VISIBLE else View.GONE
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.error.collect { error ->
                if (error != null) {
                    Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
                }
            }
        }

        viewModel.fetchLogs()
    }
}
