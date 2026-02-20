package com.example.smartagri

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import com.google.android.material.textfield.TextInputEditText

class AddDeviceActivity : Activity() {

    private lateinit var etDeviceId: TextInputEditText
    private lateinit var etDeviceName: TextInputEditText
    private lateinit var etDeviceLocation: TextInputEditText
    private lateinit var btnAddDevice: Button
    private lateinit var btnCancel: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_device)

        initializeViews()
        setupClickListeners()
        setupBottomNavigation()
    }

    private fun initializeViews() {
        etDeviceId = findViewById(R.id.etDeviceId)
        etDeviceName = findViewById(R.id.etDeviceName)
        etDeviceLocation = findViewById(R.id.etDeviceLocation)
        btnAddDevice = findViewById(R.id.btnAddDevice)
        btnCancel = findViewById(R.id.btnCancel)
    }

    private fun setupClickListeners() {
        btnAddDevice.setOnClickListener {
            handleAddDevice()
        }

        btnCancel.setOnClickListener {
            navigateToAccountSettings()
        }
    }

    private fun setupBottomNavigation() {
        // Home
        findViewById<LinearLayout>(R.id.navHome).setOnClickListener {
            val intent = Intent(this, DashboardActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
            finish()
        }

        // Weather
        findViewById<LinearLayout>(R.id.navWeather).setOnClickListener {
            startActivity(Intent(this, WeatherActivity::class.java))
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)        }

        // SOS
        findViewById<LinearLayout>(R.id.navSOS).setOnClickListener {
            startActivity(Intent(this, SoilMoistureActivity::class.java))
            overridePendingTransition(0, 0) // No animation
        }

        // Irrigation
        findViewById<LinearLayout>(R.id.navIrrigation).setOnClickListener {
            startActivity(Intent(this, IrrigationActivity::class.java))
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)        }

        // Notify
        findViewById<LinearLayout>(R.id.navNotify).setOnClickListener {
            // startActivity(Intent(this, NotificationActivity::class.java))
            // overridePendingTransition(0, 0) // No animation
        }

        // Account
        findViewById<LinearLayout>(R.id.navAccount).setOnClickListener {
            navigateToAccountSettings()
        }
    }

    private fun handleAddDevice() {
        val deviceId = etDeviceId.text.toString().trim()
        val deviceName = etDeviceName.text.toString().trim()
        val deviceLocation = etDeviceLocation.text.toString().trim()

        // Validation
        if (deviceId.isEmpty()) {
            Toast.makeText(this, "Please enter Device ID", Toast.LENGTH_SHORT).show()
            etDeviceId.requestFocus()
            return
        }

        if (!deviceId.startsWith("AGR-CTRL-")) {
            Toast.makeText(this, "Device ID must start with AGR-CTRL-", Toast.LENGTH_SHORT).show()
            etDeviceId.requestFocus()
            return
        }

        if (deviceName.isEmpty()) {
            Toast.makeText(this, "Please enter Device Name", Toast.LENGTH_SHORT).show()
            etDeviceName.requestFocus()
            return
        }

        if (deviceLocation.isEmpty()) {
            Toast.makeText(this, "Please enter Device Location", Toast.LENGTH_SHORT).show()
            etDeviceLocation.requestFocus()
            return
        }

        // Save device to preferences
        saveDeviceToPreferences(deviceId, deviceName, deviceLocation)

        Toast.makeText(this, "Device added successfully!", Toast.LENGTH_LONG).show()

        // Navigate back after a short delay
        android.os.Handler().postDelayed({
            navigateToAccountSettings()
        }, 1500)
    }

    private fun saveDeviceToPreferences(deviceId: String, deviceName: String, deviceLocation: String) {
        val sharedPreferences = getSharedPreferences("SmartAgriPrefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        // Save device information
        editor.putString("lastDeviceId", deviceId)
        editor.putString("lastDeviceName", deviceName)
        editor.putString("lastDeviceLocation", deviceLocation)

        editor.apply()
    }

    private fun navigateToAccountSettings() {
        val intent = Intent(this, AccountSettingsActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
        finish()
    }

    override fun onBackPressed() {
        navigateToAccountSettings()
    }
}