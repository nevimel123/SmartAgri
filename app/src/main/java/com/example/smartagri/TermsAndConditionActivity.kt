package com.example.smartagri

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout

class TermsAndConditionActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_terms_and_condition)

        setupBottomNavigation()
    }

    private fun setupBottomNavigation() {
        findViewById<LinearLayout>(R.id.navHome).setOnClickListener {
            val intent = Intent(this, DashboardActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }

        findViewById<LinearLayout>(R.id.navWeather).setOnClickListener {
            startActivity(Intent(this, WeatherActivity::class.java))
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }

        findViewById<LinearLayout>(R.id.navSOS).setOnClickListener {
            startActivity(Intent(this, SoilMoistureActivity::class.java))
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }

        findViewById<LinearLayout>(R.id.navIrrigation).setOnClickListener {
            startActivity(Intent(this, IrrigationActivity::class.java))
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }

        findViewById<LinearLayout>(R.id.navNotify).setOnClickListener {
            // startActivity(Intent(this, NotificationActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.navAccount).setOnClickListener {
            startActivity(Intent(this, AccountSettingsActivity::class.java))
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }
}