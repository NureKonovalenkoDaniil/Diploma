package com.example.medicationmanagement

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.medicationmanagement.model.AuditLog
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class LogsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AuditLogAdapter
    private lateinit var searchField: EditText
    private var originalList: List<AuditLog> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_logs, container, false)

        recyclerView = view.findViewById(R.id.logRecyclerView)
        searchField = view.findViewById(R.id.logSearchField)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = AuditLogAdapter(emptyList())
        recyclerView.adapter = adapter

        return view
    }

    override fun onResume() {
        super.onResume()
        loadLogs()
    }

    private fun loadLogs() {
        val token = requireActivity().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE).getString("token", null)
        if (token == null) {
            Toast.makeText(requireContext(), "No token found", Toast.LENGTH_SHORT).show()
            return
        }

        thread {
            try {
                val url = URL("http://10.0.2.2:5000/api/auditlog")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.setRequestProperty("Authorization", "Bearer $token")

                val result = conn.inputStream.bufferedReader().use { it.readText() }
                val jsonArray = JSONArray(result)

                val logs = mutableListOf<AuditLog>()
                for (i in 0 until jsonArray.length()) {
                    val item = jsonArray.getJSONObject(i)
                    logs.add(
                        AuditLog(
                            Id = item.getInt("id"),
                            Action = item.getString("action"),
                            User = item.getString("user"),
                            Timestamp = item.getString("timestamp"),
                            Details = if (item.has("details") && !item.isNull("details")) item.getString("details") else null
                        )
                    )
                }

                requireActivity().runOnUiThread {
                    originalList = logs
                    adapter.updateData(logs)

                    searchField.addTextChangedListener(object : TextWatcher {
                        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                            val query = s.toString().lowercase()
                            val filtered = originalList.filter {
                                it.Action.lowercase().contains(query) ||
                                        it.User.lowercase().contains(query) ||
                                        (it.Details?.lowercase()?.contains(query) ?: false)
                            }
                            adapter.updateData(filtered)
                        }

                        override fun afterTextChanged(s: Editable?) {}
                        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    })
                }
            } catch (e: Exception) {
                requireActivity().runOnUiThread {
                    Toast.makeText(requireContext(), "Failed to load logs: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}