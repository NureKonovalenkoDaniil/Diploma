package com.example.medicationmanagement

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.medicationmanagement.ui.MedicinesFragment
import com.example.medicationmanagement.ui.SensorsFragment
import com.example.medicationmanagement.ui.NotificationsFragment
import com.example.medicationmanagement.utils.TokenManager
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)

        // Показати Аптечку при старті
        loadFragment(MedicinesFragment())

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_medicines -> {
                    loadFragment(MedicinesFragment())
                    true
                }
                R.id.nav_devices -> {
                    loadFragment(SensorsFragment())
                    true
                }
                R.id.nav_notifications -> {
                    loadFragment(NotificationsFragment())
                    true
                }
                R.id.nav_settings -> {
                    // Тимчасово: кнопка Settings просто робить Logout для тестування
                    TokenManager.getInstance(this).clearToken()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
