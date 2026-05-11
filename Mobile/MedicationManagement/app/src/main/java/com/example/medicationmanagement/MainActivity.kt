package com.example.medicationmanagement

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.drawerlayout.widget.DrawerLayout
import android.widget.TextView
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
import com.example.medicationmanagement.utils.TokenManager
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var topAppBar: MaterialToolbar
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        AppPreferences.applyStoredPreferences(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        topAppBar = findViewById(R.id.topAppBar)
        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)
        setSupportActionBar(topAppBar)

        // Set user email in nav header
        val headerView = navigationView.getHeaderView(0)
        val userEmailTextView = headerView.findViewById<TextView>(R.id.textViewUserEmail)
        val userEmail = TokenManager.getInstance(this).getUserEmail()
        if (userEmail != null) {
            userEmailTextView.text = userEmail
        }

        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, topAppBar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // Show/hide admin menu items based on role
        val userRole = RoleHelper.getCurrentRole(this)
        val canViewUsers = RoleHelper.canViewUsers(userRole)
        val canViewAuditLog = RoleHelper.canViewAuditLog(userRole)
        navigationView.menu.findItem(R.id.nav_users)?.isVisible = canViewUsers
        navigationView.menu.findItem(R.id.nav_audit_log)?.isVisible = canViewAuditLog

        if (savedInstanceState == null) {
            loadFragment(MedicinesFragment())
            updateToolbarTitle(R.string.medicines)
            navigationView.setCheckedItem(R.id.nav_medicines)
        }

        navigationView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_medicines -> {
                    loadFragment(MedicinesFragment())
                    updateToolbarTitle(R.string.medicines)
                }
                R.id.nav_devices -> {
                    loadFragment(SensorsFragment())
                    updateToolbarTitle(R.string.devices)
                }
                R.id.nav_locations -> {
                    loadFragment(StorageLocationsFragment())
                    updateToolbarTitle(R.string.locations)
                }
                R.id.nav_notifications -> {
                    loadFragment(NotificationsFragment())
                    updateToolbarTitle(R.string.notifications)
                }
                R.id.nav_users -> {
                    loadFragment(UsersFragment())
                    updateToolbarTitle(R.string.users)
                }
                R.id.nav_audit_log -> {
                    loadFragment(AuditLogFragment())
                    updateToolbarTitle(R.string.log_audit)
                }
                R.id.nav_settings -> {
                    loadFragment(SettingsFragment())
                    updateToolbarTitle(R.string.settings)
                }
            }
            drawerLayout.closeDrawers()
            true
        }
    }

    override fun onResume() {
        super.onResume()
        updateNotificationBadge()
    }

    fun updateNotificationBadge() {
        // Notification badges for NavigationView are more complex to implement
        // and are skipped for now to ensure stability.
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
