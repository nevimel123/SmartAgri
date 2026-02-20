package com.example.smartagri

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import com.google.android.material.textfield.TextInputEditText

class ReportActivity : Activity() {

    private lateinit var etDeviceId: TextInputEditText
    private lateinit var actvReportType: AutoCompleteTextView
    private lateinit var etDescription: TextInputEditText
    private lateinit var btnSubmit: Button

    private val reportTypes = arrayOf(
        "Request Maintenance",
        "Sensor Malfunction",
        "Connection Issue",
        "Hardware Problem",
        "Software Bug",
        "Other"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report)

        initializeViews()
        setupReportTypeDropdown()
        setupClickListeners()
        setupBottomNavigation()
    }

    private fun initializeViews() {
        etDeviceId = findViewById(R.id.etDeviceId)
        actvReportType = findViewById(R.id.actvReportType)
        etDescription = findViewById(R.id.etDescription)
        btnSubmit = findViewById(R.id.btnSubmit)
    }

    private fun setupReportTypeDropdown() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, reportTypes)
        actvReportType.setAdapter(adapter)

        // Set default value
        actvReportType.setText(reportTypes[0], false)
    }

    private fun setupClickListeners() {
        btnSubmit.setOnClickListener {
            handleSubmitReport()
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
            overridePendingTransition(0, 0)
        }

        // SOS
        findViewById<LinearLayout>(R.id.navSOS).setOnClickListener {
            startActivity(Intent(this, SoilMoistureActivity::class.java))
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)        }

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

    private fun handleSubmitReport() {
        val deviceId = etDeviceId.text.toString().trim()
        val reportType = actvReportType.text.toString().trim()
        val description = etDescription.text.toString().trim()

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

        if (reportType.isEmpty()) {
            Toast.makeText(this, "Please select Report Type", Toast.LENGTH_SHORT).show()
            actvReportType.requestFocus()
            return
        }

        if (description.isEmpty()) {
            Toast.makeText(this, "Please enter Description", Toast.LENGTH_SHORT).show()
            etDescription.requestFocus()
            return
        }

        if (description.length < 10) {
            Toast.makeText(this, "Description must be at least 10 characters", Toast.LENGTH_SHORT).show()
            etDescription.requestFocus()
            return
        }

        // Save report to preferences
        saveReportToPreferences(deviceId, reportType, description)

        Toast.makeText(this, "Report submitted successfully! Our team will review it soon.", Toast.LENGTH_LONG).show()

        // Clear form fields
        etDeviceId.setText("")
        actvReportType.setText(reportTypes[0], false)
        etDescription.setText("")

        // Navigate back after a short delay
        android.os.Handler().postDelayed({
            navigateToAccountSettings()
        }, 2000)
    }

    private fun saveReportToPreferences(deviceId: String, reportType: String, description: String) {
        val sharedPreferences = getSharedPreferences("SmartAgriPrefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        // Save last report information
        val timestamp = System.currentTimeMillis()
        editor.putString("lastReportDeviceId", deviceId)
        editor.putString("lastReportType", reportType)
        editor.putString("lastReportDescription", description)
        editor.putLong("lastReportTimestamp", timestamp)

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