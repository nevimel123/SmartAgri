package com.example.smartagri

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlin.random.Random

class IrrigationActivity : Activity() {

    // UI Components
    private lateinit var tvMoistureLevel: TextView
    private lateinit var tvStatusIrrigation: TextView
    private lateinit var tvRecommendedWater: TextView
    private lateinit var tvWaterAmount: TextView
    private lateinit var tvEstimatedDuration: TextView

    private lateinit var btnDecrease: FloatingActionButton
    private lateinit var btnIncrease: FloatingActionButton
    private lateinit var btn10L: Button
    private lateinit var btn20L: Button
    private lateinit var btn30L: Button
    private lateinit var btnStartIrrigation: Button

    // Bottom navigation
    private lateinit var navHome: LinearLayout
    private lateinit var navWeather: LinearLayout
    private lateinit var navSOS: LinearLayout
    private lateinit var navIrrigation: LinearLayout
    private lateinit var navNotify: LinearLayout
    private lateinit var navAccount: LinearLayout

    // State variables
    private var currentWaterAmount = 8
    private var currentMoisture = 16f
    private val handler = Handler(Looper.getMainLooper())
    private val updateInterval = 5000L // Update every 5 seconds

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_irrigation)

        try {
            initializeViews()
            setupControls()
            setupBottomNavigation()
            startMockDataUpdates()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error initializing: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun initializeViews() {
        // Main UI components
        tvMoistureLevel = findViewById(R.id.tvMoistureLevel)
        tvStatusIrrigation = findViewById(R.id.tvStatusIrrigation)
        tvRecommendedWater = findViewById(R.id.tvRecommendedWater)
        tvWaterAmount = findViewById(R.id.tvWaterAmount)
        tvEstimatedDuration = findViewById(R.id.tvEstimatedDuration)

        // Control buttons
        btnDecrease = findViewById(R.id.btnDecrease)
        btnIncrease = findViewById(R.id.btnIncrease)
        btn10L = findViewById(R.id.btn10L)
        btn20L = findViewById(R.id.btn20L)
        btn30L = findViewById(R.id.btn30L)
        btnStartIrrigation = findViewById(R.id.btnStartIrrigation)

        // Navigation items
        navHome = findViewById(R.id.navHome)
        navWeather = findViewById(R.id.navWeather)
        navSOS = findViewById(R.id.navSOS)
        navIrrigation = findViewById(R.id.navIrrigation)
        navNotify = findViewById(R.id.navNotify)
        navAccount = findViewById(R.id.navAccount)

        // Set initial values
        updateWaterDisplay()
    }

    private fun setupControls() {
        // Decrease water amount
        btnDecrease.setOnClickListener {
            if (currentWaterAmount > 1) {
                currentWaterAmount--
                updateWaterDisplay()
            } else {
                Toast.makeText(this, "Minimum water amount is 1L", Toast.LENGTH_SHORT).show()
            }
        }

        // Increase water amount
        btnIncrease.setOnClickListener {
            if (currentWaterAmount < 100) {
                currentWaterAmount++
                updateWaterDisplay()
            } else {
                Toast.makeText(this, "Maximum water amount is 100L", Toast.LENGTH_SHORT).show()
            }
        }

        // Quick select buttons
        btn10L.setOnClickListener {
            currentWaterAmount = 10
            updateWaterDisplay()
        }

        btn20L.setOnClickListener {
            currentWaterAmount = 20
            updateWaterDisplay()
        }

        btn30L.setOnClickListener {
            currentWaterAmount = 30
            updateWaterDisplay()
        }

        // Start irrigation button
        btnStartIrrigation.setOnClickListener {
            startIrrigation()
        }
    }

    private fun updateWaterDisplay() {
        tvWaterAmount.text = currentWaterAmount.toString()

        // Calculate estimated duration (assume 2L per minute)
        val durationMinutes = (currentWaterAmount / 2.0).toInt()
        tvEstimatedDuration.text = "~$durationMinutes min"
    }

    private fun startIrrigation() {
        // Show confirmation
        Toast.makeText(
            this,
            "Starting irrigation with ${currentWaterAmount}L of water",
            Toast.LENGTH_LONG
        ).show()

        // Disable button to prevent multiple starts
        btnStartIrrigation.isEnabled = false
        btnStartIrrigation.text = "Irrigating..."

        // Simulate irrigation process
        handler.postDelayed({
            btnStartIrrigation.isEnabled = true
            btnStartIrrigation.text = "START IRRIGATION"
            Toast.makeText(this, "Irrigation completed!", Toast.LENGTH_SHORT).show()

            // Simulate moisture increase after irrigation
            currentMoisture = (currentMoisture + 10f).coerceAtMost(100f)
            updateMoistureDisplay()
        }, 3000) // 3 seconds for demo, real would be based on actual time
    }

    private fun startMockDataUpdates() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                updateMockData()
                handler.postDelayed(this, updateInterval)
            }
        }, updateInterval)
    }

    private fun updateMockData() {
        // Simulate realistic moisture changes (-0.3% to +0.1% - gradual decrease)
        val change = (Random.nextFloat() * 0.4f) - 0.3f
        currentMoisture = (currentMoisture + change).coerceIn(5f, 100f)

        updateMoistureDisplay()
    }

    private fun updateMoistureDisplay() {
        // Update moisture percentage
        tvMoistureLevel.text = String.format("%.0f%%", currentMoisture)

        // Update status and recommended water based on moisture level
        when {
            currentMoisture < 15 -> {
                tvStatusIrrigation.text = "Dry"
                tvStatusIrrigation.setTextColor(getColor(R.color.status_dry))

                val recommendedWater = ((30 - currentMoisture) * 0.5).toInt().coerceAtLeast(5)
                tvRecommendedWater.text = "$recommendedWater L"
                currentWaterAmount = recommendedWater
                updateWaterDisplay()
            }
            currentMoisture < 25 -> {
                tvStatusIrrigation.text = "Low"
                tvStatusIrrigation.setTextColor(getColor(R.color.status_yellow))

                val recommendedWater = ((25 - currentMoisture) * 0.5).toInt().coerceAtLeast(3)
                tvRecommendedWater.text = "$recommendedWater L"
                currentWaterAmount = recommendedWater
                updateWaterDisplay()
            }
            currentMoisture < 40 -> {
                tvStatusIrrigation.text = "Good"
                tvStatusIrrigation.setTextColor(getColor(R.color.primary_green))
                tvRecommendedWater.text = "0 L"
            }
            else -> {
                tvStatusIrrigation.text = "Wet"
                tvStatusIrrigation.setTextColor(getColor(R.color.status_blue))
                tvRecommendedWater.text = "0 L"
            }
        }
    }

    private fun setupBottomNavigation() {
        navHome.setOnClickListener {
            try {
                val intent = Intent(this, DashboardActivity::class.java)
                startActivity(intent)
                @Suppress("DEPRECATION")
                overridePendingTransition(0, 0)
                finish()
            } catch (e: Exception) {
                Toast.makeText(this, "Dashboard not available", Toast.LENGTH_SHORT).show()
            }
        }

        navWeather.setOnClickListener {
            try {
                val intent = Intent(this, WeatherActivity::class.java)
                startActivity(intent)
                @Suppress("DEPRECATION")
                overridePendingTransition(0, 0)
                finish()
            } catch (e: Exception) {
                Toast.makeText(this, "Weather not available", Toast.LENGTH_SHORT).show()
            }
        }

        navSOS.setOnClickListener {
            try {
                val intent = Intent(this, SoilMoistureActivity::class.java)
                startActivity(intent)
                @Suppress("DEPRECATION")
                overridePendingTransition(0, 0)
                finish()
            } catch (e: Exception) {
                Toast.makeText(this, "Soil Moisture not available", Toast.LENGTH_SHORT).show()
            }
        }

        navIrrigation.setOnClickListener {
            // Already on this screen
            Toast.makeText(this, "Already on Irrigation screen", Toast.LENGTH_SHORT).show()
        }

        navNotify.setOnClickListener {
            Toast.makeText(this, "Notifications", Toast.LENGTH_SHORT).show()
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }

        navAccount.setOnClickListener {
            try {
                val intent = Intent(this, AccountSettingsActivity::class.java)
                startActivity(intent)
                @Suppress("DEPRECATION")
                overridePendingTransition(0, 0)
                finish()
            } catch (e: Exception) {
                Toast.makeText(this, "Account settings not available", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}