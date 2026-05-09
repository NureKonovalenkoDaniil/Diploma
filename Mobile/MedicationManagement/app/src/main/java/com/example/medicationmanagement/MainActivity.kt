package com.example.medicationmanagement

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.medicationmanagement.R
import com.example.medicationmanagement.api.NotificationApi
import com.example.medicationmanagement.ui.AuditLogFragment
import com.example.medicationmanagement.ui.MedicinesFragment
import com.example.medicationmanagement.ui.NotificationsFragment
import com.example.medicationmanagement.ui.StorageLocationsFragment
import com.example.medicationmanagement.ui.SensorsFragment
import com.example.medicationmanagement.ui.SettingsFragment
import com.example.medicationmanagement.ui.UsersFragment
import com.example.medicationmanagement.ui.theme.AppPreferences
import com.example.medicationmanagement.utils.RoleHelper
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var topAppBar: MaterialToolbar
    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        AppPreferences.applyStoredPreferences(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        topAppBar = findViewById(R.id.topAppBar)
        bottomNav = findViewById(R.id.bottomNavigation)
        setSupportActionBar(topAppBar)

        // Show/hide admin menu items based on role
        val userRole = RoleHelper.getCurrentRole(this)
        val isAdmin = RoleHelper.isAdmin(userRole)
        bottomNav.menu.findItem(R.id.nav_users)?.isVisible = isAdmin
        bottomNav.menu.findItem(R.id.nav_audit_log)?.isVisible = isAdmin

        loadFragment(MedicinesFragment())
        updateToolbarTitle(R.string.medicines)

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_medicines -> {
                    loadFragment(MedicinesFragment())
                    updateToolbarTitle(R.string.medicines)
                    true
                }
                R.id.nav_devices -> {
                    loadFragment(SensorsFragment())
                    updateToolbarTitle(R.string.devices)
                    true
                }
                R.id.nav_locations -> {
                    loadFragment(StorageLocationsFragment())
                    updateToolbarTitle(R.string.locations)
                    true
                }
                R.id.nav_notifications -> {
                    loadFragment(NotificationsFragment())
                    // When opening notifications, clear the badge temporarily
                    // It will be updated accurately on next resume
                    bottomNav.removeBadge(R.id.nav_notifications)
                    updateToolbarTitle(R.string.notifications)
                    true
                }
                R.id.nav_users -> {
                    loadFragment(UsersFragment())
                    updateToolbarTitle(R.string.users)
                    true
                }
                R.id.nav_audit_log -> {
                    loadFragment(AuditLogFragment())
                    updateToolbarTitle(R.string.log_audit)
                    true
                }
                R.id.nav_settings -> {
                    loadFragment(SettingsFragment())
                    updateToolbarTitle(R.string.settings)
                    true
                }
                else -> false
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateNotificationBadge()
    }

    fun updateNotificationBadge() {
        lifecycleScope.launch {
            try {
                val api = com.example.medicationmanagement.api.RetrofitClient.getNotificationApi(this@MainActivity)
                val response = api.getNotifications()
                
                if (response.isSuccessful) {
                    val unreadCount = response.body()?.count { !it.isRead } ?: 0
                    if (unreadCount > 0) {
                        val badge = bottomNav.getOrCreateBadge(R.id.nav_notifications)
                        badge.isVisible = true
                        badge.number = unreadCount
                        badge.backgroundColor = getColor(android.R.color.holo_red_dark)
                    } else {
                        bottomNav.removeBadge(R.id.nav_notifications)
                    }
                }
            } catch (e: Exception) {
                // Ignore network errors here to avoid spamming the user
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    private fun updateToolbarTitle(titleResId: Int) {
        topAppBar.title = getString(titleResId)
    }
}
