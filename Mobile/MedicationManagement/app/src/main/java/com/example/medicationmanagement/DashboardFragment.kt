package com.example.medicationmanagement

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.medicationmanagement.model.Medicine
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class DashboardFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MedicineAdapter
    private lateinit var addButton: Button
    private lateinit var searchField: EditText
    private var originalList: List<Medicine> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_dashboard, container, false)

        recyclerView = view.findViewById(R.id.medicineRecyclerView)
        addButton = view.findViewById(R.id.addMedicineButton)
        searchField = view.findViewById(R.id.searchField)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = MedicineAdapter(emptyList())
        recyclerView.adapter = adapter

        addButton.setOnClickListener {
            startActivity(Intent(requireContext(), AddMedicineActivity::class.java))
        }

        searchField.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().lowercase()
                val filtered = originalList.filter {
                    it.name.lowercase().contains(query) ||
                            it.type.lowercase().contains(query) ||
                            it.category.lowercase().contains(query)
                }
                adapter.updateMedicines(filtered)
            }
        })

        loadMedicines()

        return view
    }

    private fun loadMedicines() {
        val sharedPref = requireActivity().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val token = sharedPref.getString("token", null)

        if (token == null) {
            Toast.makeText(requireContext(), "No token found. Please log in again.", Toast.LENGTH_LONG).show()
            return
        }

        thread {
            try {
                val url = URL("http://10.0.2.2:5000/api/medicine")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Authorization", "Bearer $token")

                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = StringBuilder()
                var line: String? = reader.readLine()
                while (line != null) {
                    response.append(line)
                    line = reader.readLine()
                }
                reader.close()

                val medicines = parseMedicines(JSONArray(response.toString()))
                originalList = medicines

                requireActivity().runOnUiThread {
                    adapter.updateMedicines(medicines)
                }

            } catch (e: Exception) {
                requireActivity().runOnUiThread {
                    Toast.makeText(requireContext(), "Failed to load medicines: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun parseMedicines(jsonArray: JSONArray): List<Medicine> {
        val list = mutableListOf<Medicine>()
        for (i in 0 until jsonArray.length()) {
            val item = jsonArray.getJSONObject(i)
            val medicine = Medicine(
                medicineID = item.getInt("medicineID"),
                name = item.getString("name"),
                type = item.getString("type"),
                expiryDate = item.getString("expiryDate"),
                quantity = item.getInt("quantity"),
                category = item.getString("category")
            )
            list.add(medicine)
        }
        return list
    }
}