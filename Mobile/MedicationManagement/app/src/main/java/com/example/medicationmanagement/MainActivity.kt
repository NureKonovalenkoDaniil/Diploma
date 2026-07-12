package com.example.medicationmanagement

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
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

        // Ініціалізація каналу сповіщень та запуск циклу опитування
        createNotificationChannel()
        requestNotificationPermission()
        startPeriodicSync()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    101
                )
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Storage Violations"
            val descriptionText = "Notifications about storage temperature/humidity violations"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("storage_violations_channel", name, importance).apply {
                description = descriptionText
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun startPeriodicSync() {
        lifecycleScope.launch {
            while (isActive) {
                updateNavigationBadgesAndCheckNotifications()
                delay(10000) // Опитування кожні 10 секунд
            }
        }
    }

    private fun showSystemNotification(title: String, message: String, notificationId: Int) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, notificationId, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(this, "storage_violations_channel")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val notificationManager = NotificationManagerCompat.from(this)
        try {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
            ) {
                notificationManager.notify(notificationId, builder.build())
            }
        } catch (_: SecurityException) {
            // Ignore
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
        updateNavigationBadgesAndCheckNotifications()
    }

    fun updateNotificationBadge() {
        updateNavigationBadgesAndCheckNotifications()
    }

    fun updateNavigationBadgesAndCheckNotifications() {
        lifecycleScope.launch {
            // 1. Оновлення баджа сповіщень та показ системних push-повідомлень
            try {
                val api = RetrofitClient.getNotificationApi(this@MainActivity)
                val response = api.getNotifications()
                if (response.isSuccessful) {
                    val notifications = response.body() ?: emptyList()
                    val unreadNotifications = notifications.filter { !it.isRead }
                    val count = unreadNotifications.size

                    val menuItem = navigationView.menu.findItem(R.id.nav_notifications)
                    menuItem?.title = if (count > 0) {
                        "${getString(R.string.notifications)} ($count)"
                    } else {
                        getString(R.string.notifications)
                    }

                    // Перевірка наявності нових сповіщень для виведення в шторку Android
                    val userSubject = com.example.medicationmanagement.utils.RoleHelper.getUserSubject(this@MainActivity) ?: "anonymous"
                    val prefKey = "last_notified_id_$userSubject"
                    val sharedPrefs = getSharedPreferences("app_notifications", Context.MODE_PRIVATE)
                    val lastNotifiedId = sharedPrefs.getInt(prefKey, 0)

                    if (lastNotifiedId == 0) {
                        // Перший запуск або новий користувач: просто ініціалізуємо lastNotifiedId найбільшим поточним ID
                        val maxIdFromServer = unreadNotifications.maxOfOrNull { it.notificationId } ?: 0
                        sharedPrefs.edit().putInt(prefKey, maxIdFromServer).apply()
                    } else {
                        val newNotifications = unreadNotifications.filter { it.notificationId > lastNotifiedId }
                        if (newNotifications.isNotEmpty()) {
                            if (newNotifications.size > 3) {
                                // Замість спаму десятками сповіщень показуємо одне сумарне
                                val maxId = newNotifications.maxOf { it.notificationId }
                                showSystemNotification(
                                    getString(R.string.new_notifications_title, newNotifications.size),
                                    getString(R.string.new_notifications_desc, newNotifications.size),
                                    maxId
                                )
                                sharedPrefs.edit().putInt(prefKey, maxId).apply()
                            } else {
                                var maxId = lastNotifiedId
                                for (notif in newNotifications) {
                                    showSystemNotification(notif.title, notif.message, notif.notificationId)
                                    if (notif.notificationId > maxId) {
                                        maxId = notif.notificationId
                                    }
                                }
                                sharedPrefs.edit().putInt(prefKey, maxId).apply()
                            }
                        }
                    }
                }
            } catch (_: Exception) {
                // Ignore
            }

            // 2. Оновлення баджа активних інцидентів
            try {
                val incidentApi = RetrofitClient.getStorageIncidentApi(this@MainActivity)
                val response = incidentApi.getAll()
                if (response.isSuccessful) {
                    val count = response.body()?.filter { !it.isResolvedCalculated }?.size ?: 0
                    val menuItem = navigationView.menu.findItem(R.id.nav_incidents)
                    menuItem?.title = if (count > 0) {
                        "${getString(R.string.incidents)} ($count)"
                    } else {
                        getString(R.string.incidents)
                    }
                }
            } catch (_: Exception) {
                // Ignore
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
