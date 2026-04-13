package com.example.medicationmanagement

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)

        // Показати DashboardFragment при старті
        loadFragment(DashboardFragment())

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_medicines -> {
                    loadFragment(DashboardFragment())
                    true
                }
                R.id.nav_devices -> {
                    loadFragment(DevicesFragment())
                    true
                }
                R.id.nav_conditions -> {
                    loadFragment(StorageConditionsFragment()) // створимо пізніше
                    true
                }
                R.id.nav_logs -> {
                    loadFragment(LogsFragment()) // створимо пізніше
                    true
                }
                R.id.nav_settings -> {
                    loadFragment(SettingsFragment()) // уже реалізовано
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
