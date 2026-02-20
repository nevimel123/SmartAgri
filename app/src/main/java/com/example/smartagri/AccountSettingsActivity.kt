package com.example.smartagri

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Window
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.Toast

class AccountSettingsActivity : Activity() {

    private var isLoggingOut = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account_settings)

        // Check if user is logged in
        if (com.google.firebase.auth.FirebaseAuth.getInstance().currentUser == null) {
            goToLogin()
            return
        }

        setupMenuItems()
        setupBottomNavigation()
    }

    private fun setupBottomNavigation() {
        // Bottom navigation
        findViewById<LinearLayout>(R.id.navHome)?.setOnClickListener {
            if (!isLoggingOut) {
                val intent = Intent(this, DashboardActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                startActivity(intent)
                @Suppress("DEPRECATION")
                overridePendingTransition(0, 0)
                finish()
            }
        }

        findViewById<LinearLayout>(R.id.navWeather)?.setOnClickListener {
            if (!isLoggingOut) {
                startActivity(Intent(this, WeatherActivity::class.java))
                @Suppress("DEPRECATION")
                overridePendingTransition(0, 0)
            }
        }

        findViewById<LinearLayout>(R.id.navSOS)?.setOnClickListener {
            if (!isLoggingOut) {
                startActivity(Intent(this, SoilMoistureActivity::class.java))
                @Suppress("DEPRECATION")
                overridePendingTransition(0, 0)
            }
        }

        findViewById<LinearLayout>(R.id.navIrrigation)?.setOnClickListener {
            if (!isLoggingOut) {
                startActivity(Intent(this, IrrigationActivity::class.java))
                @Suppress("DEPRECATION")
                overridePendingTransition(0, 0)
            }
        }

        findViewById<LinearLayout>(R.id.navNotify)?.setOnClickListener {
            if (!isLoggingOut) {
                // startActivity(Intent(this, NotificationActivity::class.java))
                // overridePendingTransition(0, 0)
            }
        }

        findViewById<LinearLayout>(R.id.navAccount)?.setOnClickListener {
            // Already on account screen
        }
    }

    private fun setupMenuItems() {
        // Register Device
        findViewById<RelativeLayout>(R.id.itemRegisterDevice)?.setOnClickListener {
            startActivity(Intent(this, AddDeviceActivity::class.java))
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }

        // View Profile
        findViewById<RelativeLayout>(R.id.itemViewProfile)?.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }

        // Terms and Conditions
        findViewById<RelativeLayout>(R.id.itemTerms)?.setOnClickListener {
            startActivity(Intent(this, TermsAndConditionActivity::class.java))
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }

        // Privacy Policy
        findViewById<RelativeLayout>(R.id.itemPrivacy)?.setOnClickListener {
            startActivity(Intent(this, PrivacyPolicyActivity::class.java))
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }

        // Report a Problem
        findViewById<RelativeLayout>(R.id.itemReport)?.setOnClickListener {
            startActivity(Intent(this, ReportActivity::class.java))
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }

        // Change Password
        findViewById<RelativeLayout>(R.id.itemChangePassword)?.setOnClickListener {
            startActivity(Intent(this, ChangePasswordActivity::class.java))
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }

        // Farm Group
        findViewById<RelativeLayout>(R.id.itemFarmGroup)?.setOnClickListener {
            startActivity(Intent(this, FarmGroupActivity::class.java))
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }

        // Logout
        findViewById<RelativeLayout>(R.id.itemLogout)?.setOnClickListener {
            showLogoutDialog()
        }
    }

    /**
     * Show custom logout confirmation dialog
     */
    private fun showLogoutDialog() {
        // Create custom dialog
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_logout)
        dialog.setCancelable(true)

        // Set dialog window properties
        dialog.window?.setBackgroundDrawableResource(android.R.drawable.dialog_holo_light_frame)

        // Get dialog buttons
        val btnLogout = dialog.findViewById<Button>(R.id.btnLogout)
        val btnCancel = dialog.findViewById<Button>(R.id.btnCancel)

        // Logout button click
        btnLogout.setOnClickListener {
            dialog.dismiss()
            performLogout()
        }

        // Cancel button click
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        // Show dialog
        dialog.show()
    }

    /**
     * Perform logout action
     */
    private fun performLogout() {
        isLoggingOut = true

        // Sign out from Firebase
        com.google.firebase.auth.FirebaseAuth.getInstance().signOut()

        // Clear ALL SharedPreferences to ensure clean logout
        // Clear "user_prefs" (used by Login, Register, Dashboard)
        val userPrefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE).edit()
        userPrefs.clear()
        userPrefs.apply()

        // Clear "SmartAgriPrefs" (in case it's used elsewhere)
        val smartAgriPrefs = getSharedPreferences("SmartAgriPrefs", Context.MODE_PRIVATE).edit()
        smartAgriPrefs.clear()
        smartAgriPrefs.apply()

        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()

        // Clear back stack and go to Login
        goToLogin()
    }

    private fun goToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
        finish()
    }

    override fun onBackPressed() {
        if (!isLoggingOut) {
            super.onBackPressed()
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }

    override fun onPause() {
        super.onPause()
        // If logging out, prevent any state saving
        if (isLoggingOut) {
            // Don't save state
        }
    }

    override fun onStop() {
        super.onStop()
        // If logging out, ensure activity is destroyed
        if (isLoggingOut) {
            finish()
        }
    }
}