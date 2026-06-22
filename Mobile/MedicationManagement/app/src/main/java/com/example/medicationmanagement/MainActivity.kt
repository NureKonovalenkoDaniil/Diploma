package com.example.medicationmanagement

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.medicationmanagement.R
import com.example.medicationmanagement.api.RetrofitClient
import com.example.medicationmanagement.ui.AuditLogFragment
import com.example.medicationmanagement.ui.MedicinesFragment
import com.example.medicationmanagement.ui.NotificationsFragment
import com.example.medicationmanagement.ui.StorageLocationsFragment
import com.example.medicationmanagement.ui.SensorsFragment
import com.example.medicationmanagement.ui.SettingsFragment
import com.example.medicationmanagement.ui.IncidentsFragment
import com.example.medicationmanagement.ui.UsersFragment
import com.example.medicationmanagement.ui.theme.AppPreferences
import com.example.medicationmanagement.utils.RoleHelper
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var topAppBar: MaterialToolbar
    private lateinit var bottomNavigation: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        AppPreferences.applyStoredPreferences(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        topAppBar = findViewById(R.id.topAppBar)
        bottomNavigation = findViewById(R.id.bottomNavigation)
        setSupportActionBar(topAppBar)

        if (savedInstanceState == null) {
            loadFragment(MedicinesFragment())
            updateToolbarTitle(R.string.medicines)
            bottomNavigation.selectedItemId = R.id.nav_medicines
        }

        val backCallback = object : androidx.activity.OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                bottomNavigation.selectedItemId = R.id.nav_medicines
            }
        }
        onBackPressedDispatcher.addCallback(this, backCallback)

        bottomNavigation.setOnItemSelectedListener { item ->
            backCallback.isEnabled = (item.itemId != R.id.nav_medicines)
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
                R.id.nav_incidents -> {
                    loadFragment(IncidentsFragment())
                    updateToolbarTitle(R.string.incidents)
                    true
                }
                R.id.nav_notifications -> {
                    loadFragment(NotificationsFragment())
                    updateToolbarTitle(R.string.notifications)
                    true
                }
                else -> false
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_options_menu, menu)
        val userRole = RoleHelper.getCurrentRole(this)
        val isAdmin = RoleHelper.isAdmin(userRole)
        menu?.findItem(R.id.action_users)?.isVisible = isAdmin
        menu?.findItem(R.id.action_audit_log)?.isVisible = isAdmin
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_users -> {
                loadFragment(UsersFragment())
                updateToolbarTitle(R.string.users)
                return true
            }
            R.id.action_audit_log -> {
                loadFragment(AuditLogFragment())
                updateToolbarTitle(R.string.log_audit)
                return true
            }
            R.id.action_settings -> {
                loadFragment(SettingsFragment())
                updateToolbarTitle(R.string.settings)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        super.onResume()
        updateNotificationBadge()
    }

    fun updateNotificationBadge() {
        lifecycleScope.launch {
            try {
                val api = RetrofitClient.getNotificationApi(this@MainActivity)
                val response = api.getNotifications()
                if (response.isSuccessful) {
                    val count = response.body()?.filter { !it.isRead }?.size ?: 0
                    val badge = bottomNavigation.getOrCreateBadge(R.id.nav_notifications)
                    if (count > 0) {
                        badge.isVisible = true
                        badge.number = count
                    } else {
                        badge.isVisible = false
                    }
                }
            } catch (e: Exception) {
                // Ignore background errors
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
