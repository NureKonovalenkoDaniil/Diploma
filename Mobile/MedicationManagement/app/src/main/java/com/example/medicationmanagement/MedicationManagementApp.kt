package com.example.medicationmanagement

import android.app.Application
import com.example.medicationmanagement.ui.theme.AppPreferences

class MedicationManagementApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppPreferences.applyStoredPreferences(this)
    }
}