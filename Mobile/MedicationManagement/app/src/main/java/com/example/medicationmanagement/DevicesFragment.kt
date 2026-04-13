package com.example.medicationmanagement

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.medicationmanagement.model.IoTDevice
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class DevicesFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var searchField: EditText
    private lateinit var adapter: DeviceAdapter
    private lateinit var addButton: View
    private var originalList: List<IoTDevice> = emptyList()

    private val ADD_DEVICE_REQUEST_CODE = 200

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_devices, container, false)

        recyclerView = view.findViewById(R.id.deviceRecyclerView)
        searchField = view.findViewById(R.id.deviceSearchField)
        addButton = view.findViewById(R.id.addDeviceButton)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = DeviceAdapter(emptyList())
        recyclerView.adapter = adapter

        searchField.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().lowercase()
                val filtered = originalList.filter {
                    it.type.lowercase().contains(query) ||
                            it.location.lowercase().contains(query) ||
                            it.parameters.lowercase().contains(query)
                }
                adapter.updateDevices(filtered)
            }
        })

        addButton.setOnClickListener {
            val intent = Intent(requireContext(), AddDeviceActivity::class.java)
            startActivityForResult(intent, ADD_DEVICE_REQUEST_CODE)
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        loadDevices()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ADD_DEVICE_REQUEST_CODE && resultCode == AppCompatActivity.RESULT_OK) {
            loadDevices()
        }
    }

    private fun loadDevices() {
        val token = requireActivity().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE).getString("token", null)
        if (token == null) {
            Toast.makeText(requireContext(), "No token found", Toast.LENGTH_SHORT).show()
            return
        }

        thread {
            try {
                val url = URL("http://10.0.2.2:5000/api/iotdevice")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.setRequestProperty("Authorization", "Bearer $token")

                val result = conn.inputStream.bufferedReader().use { it.readText() }
                val jsonArray = JSONArray(result)

                val devices = mutableListOf<IoTDevice>()
                for (i in 0 until jsonArray.length()) {
                    val item = jsonArray.getJSONObject(i)
                    devices.add(
                        IoTDevice(
                            deviceID = item.getInt("deviceID"),
                            location = item.getString("location"),
                            type = item.getString("type"),
                            parameters = item.getString("parameters"),
                            isActive = item.getBoolean("isActive"),
                            minTemperature = item.getDouble("minTemperature"),
                            maxTemperature = item.getDouble("maxTemperature"),
                            minHumidity = item.getDouble("minHumidity"),
                            maxHumidity = item.getDouble("maxHumidity")
                        )
                    )
                }

                requireActivity().runOnUiThread {
                    originalList = devices
                    adapter.updateDevices(devices)
                }
            } catch (e: Exception) {
                requireActivity().runOnUiThread {
                    Toast.makeText(requireContext(), "Failed to load: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}