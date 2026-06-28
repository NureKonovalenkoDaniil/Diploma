package com.example.medicationmanagement

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.medicationmanagement.api.RetrofitClient
import com.example.medicationmanagement.ui.AuditLogFragment
import com.example.medicationmanagement.ui.IncidentsFragment
import com.example.medicationmanagement.ui.MedicinesFragment
import com.example.medicationmanagement.ui.NotificationsFragment
import com.example.medicationmanagement.ui.ProfileFragment
import com.example.medicationmanagement.ui.SensorsFragment
import com.example.medicationmanagement.ui.SettingsFragment
import com.example.medicationmanagement.ui.StorageLocationsFragment
import com.example.medicationmanagement.ui.UsersFragment
import com.example.medicationmanagement.ui.theme.AppPreferences
import com.example.medicationmanagement.utils.RoleHelper
import com.example.medicationmanagement.utils.TokenManager
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var topAppBar: MaterialToolbar
    private lateinit var toggle: ActionBarDrawerToggle

    override fun onCreate(savedInstanceState: Bundle?) {
        AppPreferences.applyStoredPreferences(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)
        topAppBar = findViewById(R.id.topAppBar)

        setSupportActionBar(topAppBar)

        // ActionBarDrawerToggle — hamburger icon
        toggle = ActionBarDrawerToggle(
            this, drawerLayout, topAppBar,
            R.string.drawer_open, R.string.drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // Role-based visibility
        val userRole = RoleHelper.getCurrentRole(this)
        val isAdmin = RoleHelper.isAdmin(userRole)
        navigationView.menu.findItem(R.id.nav_users)?.isVisible = isAdmin
        navigationView.menu.findItem(R.id.nav_audit_log)?.isVisible = isAdmin

        // Show user email in drawer header
        val headerView = navigationView.getHeaderView(0)
        val emailTextView = headerView?.findViewById<android.widget.TextView>(R.id.textViewUserEmail)
        emailTextView?.text = TokenManager.getInstance(this).getUserEmail() ?: getString(R.string.nav_header_default_email)

        // Navigation item selection
        navigationView.setNavigationItemSelectedListener { item ->
            handleNavigation(item)
            drawerLayout.closeDrawers()
            true
        }

        // Default fragment
        if (savedInstanceState == null) {
            loadFragment(MedicinesFragment())
            updateToolbarTitle(R.string.medicines)
            navigationView.setCheckedItem(R.id.nav_medicines)
        }
    }

    private fun handleNavigation(item: MenuItem) {
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
            R.id.nav_incidents -> {
                loadFragment(IncidentsFragment())
                updateToolbarTitle(R.string.incidents)
            }
            R.id.nav_notifications -> {
                loadFragment(NotificationsFragment())
                updateToolbarTitle(R.string.notifications)
            }
            R.id.nav_profile -> {
                loadFragment(ProfileFragment())
                updateToolbarTitle(R.string.profile)
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
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (toggle.onOptionsItemSelected(item)) return true
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
                    val menuItem = navigationView.menu.findItem(R.id.nav_notifications)
                    menuItem?.title = if (count > 0) {
                        "${getString(R.string.notifications)} ($count)"
                    } else {
                        getString(R.string.notifications)
                    }
                }
            } catch (_: Exception) {
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
