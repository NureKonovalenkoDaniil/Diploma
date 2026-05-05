package com.example.medicationmanagement.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.medicationmanagement.LoginActivity
import com.example.medicationmanagement.R
import com.example.medicationmanagement.ui.theme.AppPreferences
import com.example.medicationmanagement.utils.TokenManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textview.MaterialTextView

class SettingsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val themeGroup = view.findViewById<MaterialButtonToggleGroup>(R.id.themeToggleGroup)
        val languageGroup = view.findViewById<MaterialButtonToggleGroup>(R.id.languageToggleGroup)
        val notificationsSwitch = view.findViewById<MaterialSwitch>(R.id.notificationsSwitch)
        val currentLanguageLabel = view.findViewById<MaterialTextView>(R.id.currentLanguageLabel)
        val logoutButton = view.findViewById<MaterialButton>(R.id.logoutButton)

        when (AppPreferences.getThemeMode(requireContext())) {
            AppPreferences.MODE_LIGHT -> themeGroup.check(R.id.themeLightButton)
            AppPreferences.MODE_DARK -> themeGroup.check(R.id.themeDarkButton)
            else -> themeGroup.check(R.id.themeSystemButton)
        }

        when (AppPreferences.getLanguage(requireContext())) {
            "uk" -> languageGroup.check(R.id.languageUkrButton)
            "en" -> languageGroup.check(R.id.languageEngButton)
            else -> languageGroup.check(R.id.languageSystemButton)
        }

        currentLanguageLabel.text = getString(
            R.string.current_language_value,
            AppPreferences.getLanguage(requireContext()).ifBlank { getString(R.string.language_system) }
        )

        themeGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener

            val themeMode = when (checkedId) {
                R.id.themeLightButton -> AppPreferences.MODE_LIGHT
                R.id.themeDarkButton -> AppPreferences.MODE_DARK
                else -> AppPreferences.MODE_SYSTEM
            }

            AppPreferences.setThemeMode(requireContext(), themeMode)
            Toast.makeText(requireContext(), R.string.theme_applied, Toast.LENGTH_SHORT).show()
        }

        languageGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener

            val languageTag = when (checkedId) {
                R.id.languageUkrButton -> "uk"
                R.id.languageEngButton -> "en"
                else -> ""
            }

            AppPreferences.setLanguage(requireContext(), languageTag)
            currentLanguageLabel.text = getString(
                R.string.current_language_value,
                languageTag.ifBlank { getString(R.string.language_system) }
            )
            Toast.makeText(requireContext(), R.string.language_applied, Toast.LENGTH_SHORT).show()
        }

        notificationsSwitch.isChecked = true
        notificationsSwitch.setOnCheckedChangeListener { _, isChecked ->
            Toast.makeText(
                requireContext(),
                if (isChecked) R.string.notifications_enabled else R.string.notifications_disabled,
                Toast.LENGTH_SHORT
            ).show()
        }

        logoutButton.setOnClickListener {
            TokenManager.getInstance(requireContext()).clearToken()
            startActivity(Intent(requireContext(), LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            requireActivity().finish()
        }
    }
}