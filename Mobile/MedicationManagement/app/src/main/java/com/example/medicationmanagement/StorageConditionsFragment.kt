package com.example.medicationmanagement

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.medicationmanagement.model.StorageCondition
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread
import android.text.TextWatcher

class StorageConditionsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: StorageConditionAdapter
    private lateinit var searchField: EditText
    private var originalList: List<StorageCondition> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_conditions, container, false)

        recyclerView = view.findViewById(R.id.conditionRecyclerView)
        searchField = view.findViewById(R.id.conditionSearchField)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = StorageConditionAdapter(emptyList())
        recyclerView.adapter = adapter

        searchField.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().lowercase()
                val filtered = originalList.filter {
                    it.deviceID.toString().contains(query) ||
                            it.temperature.toString().contains(query) ||
                            it.humidity.toString().contains(query)
                }
                adapter.updateData(filtered)
            }

            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        })

        loadConditions()
        return view
    }

    private fun loadConditions() {
        val token = requireActivity().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE).getString("token", null)
        if (token == null) return

        thread {
            try {
                val url = URL("http://10.0.2.2:5000/api/storagecondition")
                val conn = url.openConnection() as HttpURLConnection
                conn.setRequestProperty("Authorization", "Bearer $token")
                conn.requestMethod = "GET"

                val response = conn.inputStream.bufferedReader().use { it.readText() }
                val jsonArray = JSONArray(response)

                val list = mutableListOf<StorageCondition>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    list.add(
                        StorageCondition(
                            deviceID = obj.getInt("deviceID"),
                            temperature = obj.getDouble("temperature"),
                            humidity = obj.getDouble("humidity"),
                            timestamp = obj.getString("timestamp")
                        )
                    )
                }

                requireActivity().runOnUiThread {
                    originalList = list
                    adapter.updateData(list)
                    searchField.addTextChangedListener(object : TextWatcher {
                        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                            val query = s.toString().lowercase()
                            val filtered = originalList.filter {
                                it.deviceID.toString().contains(query) ||
                                        it.temperature.toString().contains(query) ||
                                        it.humidity.toString().contains(query)
                            }
                            adapter.updateData(filtered)
                        }

                        override fun afterTextChanged(s: Editable?) {}
                        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    })
                }

            } catch (e: Exception) {
                requireActivity().runOnUiThread {
                    Toast.makeText(requireContext(), "Failed to load: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}