package com.example.medicationmanagement

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import java.util.*

class SettingsFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        val btnUa = view.findViewById<Button>(R.id.btnLangUa)
        val btnEn = view.findViewById<Button>(R.id.btnLangEn)

        btnUa.setOnClickListener { setLocale("uk") }
        btnEn.setOnClickListener { setLocale("en") }

        return view
    }

    private fun setLocale(lang: String) {
        val locale = Locale(lang)
        Locale.setDefault(locale)

        val config = Configuration()
        config.setLocale(locale)
        requireActivity().baseContext.resources.updateConfiguration(config, requireActivity().resources.displayMetrics)

        val prefs = requireActivity().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("lang", lang).apply()

        requireActivity().recreate()
    }
}
