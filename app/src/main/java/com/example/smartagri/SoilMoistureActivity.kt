package com.example.smartagri

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.LinearLayout
import android.widget.TextView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.chip.ChipGroup
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

class SoilMoistureActivity : Activity() {

    private lateinit var tvMoisturePercentage: TextView
    private lateinit var tvOptimal: TextView
    private lateinit var tvLastUpdated: TextView
    private lateinit var tvTemperature: TextView
    private lateinit var tvStatus: TextView
    private lateinit var soilMoistureChart: LineChart
    private lateinit var chipGroupPeriod: ChipGroup

    private val handler = Handler(Looper.getMainLooper())
    private var currentMoisture = 16f
    private val updateInterval = 5000L // Update every 5 seconds for demo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_soil_moisture)

        try {
            initializeViews()
            setupChart()
            setupChipListeners()
            setupBottomNavigation()
            startMockDataUpdates()
        } catch (e: Exception) {
            e.printStackTrace()
            // Log error but don't crash - some features might not be available
        }
    }

    private fun initializeViews() {
        tvMoisturePercentage = findViewById(R.id.tvMoisturePercentage)
        tvOptimal = findViewById(R.id.tvOptimal)
        tvLastUpdated = findViewById(R.id.tvLastUpdated)
        tvTemperature = findViewById(R.id.tvTemperature)
        tvStatus = findViewById(R.id.tvStatus)
        soilMoistureChart = findViewById(R.id.soilMoistureChart)
        chipGroupPeriod = findViewById(R.id.chipGroupPeriod)
    }

    private fun setupChart() {
        // Generate initial mock data
        val entries = generateMockData("hourly")

        val dataSet = LineDataSet(entries, "Soil Moisture %").apply {
            color = getColor(R.color.primary_green)
            setCircleColor(getColor(R.color.primary_green))
            lineWidth = 2f
            circleRadius = 4f
            setDrawCircleHole(false)
            valueTextSize = 9f
            setDrawFilled(true)
            fillColor = getColor(R.color.primary_green)
            fillAlpha = 50
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }

        val lineData = LineData(dataSet)
        soilMoistureChart.apply {
            data = lineData
            description.isEnabled = false
            setTouchEnabled(true)
            setPinchZoom(true)

            // X-Axis configuration
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                        val calendar = Calendar.getInstance()
                        calendar.add(Calendar.HOUR, -24 + value.toInt())
                        return timeFormat.format(calendar.time)
                    }
                }
            }

            // Y-Axis configuration
            axisLeft.apply {
                setDrawGridLines(true)
                axisMinimum = 0f
                axisMaximum = 100f
            }

            axisRight.isEnabled = false
            legend.isEnabled = true

            animateX(1000)
            invalidate()
        }
    }

    private fun generateMockData(period: String): List<Entry> {
        val entries = mutableListOf<Entry>()

        when (period) {
            "hourly" -> {
                // Last 24 hours, hourly data
                for (i in 0..23) {
                    val moisture = 15f + Random.nextFloat() * 10f // Random between 15-25%
                    entries.add(Entry(i.toFloat(), moisture))
                }
            }
            "daily" -> {
                // Last 7 days, daily data
                for (i in 0..6) {
                    val moisture = 14f + Random.nextFloat() * 12f // Random between 14-26%
                    entries.add(Entry(i.toFloat(), moisture))
                }
            }
            "weekly" -> {
                // Last 4 weeks, weekly data
                for (i in 0..3) {
                    val moisture = 13f + Random.nextFloat() * 14f // Random between 13-27%
                    entries.add(Entry(i.toFloat(), moisture))
                }
            }
            "monthly" -> {
                // Last 12 months, monthly data
                for (i in 0..11) {
                    val moisture = 12f + Random.nextFloat() * 16f // Random between 12-28%
                    entries.add(Entry(i.toFloat(), moisture))
                }
            }
        }

        return entries
    }

    private fun setupChipListeners() {
        chipGroupPeriod.setOnCheckedChangeListener { group, checkedId ->
            val period = when (checkedId) {
                R.id.chipHourly -> "hourly"
                R.id.chipDaily -> "daily"
                R.id.chipWeekly -> "weekly"
                R.id.chipMonthly -> "monthly"
                else -> "hourly"
            }

            updateChart(period)
        }
    }

    private fun updateChart(period: String) {
        val entries = generateMockData(period)
        val dataSet = LineDataSet(entries, "Soil Moisture %").apply {
            color = getColor(R.color.primary_green)
            setCircleColor(getColor(R.color.primary_green))
            lineWidth = 2f
            circleRadius = 4f
            setDrawCircleHole(false)
            valueTextSize = 9f
            setDrawFilled(true)
            fillColor = getColor(R.color.primary_green)
            fillAlpha = 50
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }

        soilMoistureChart.data = LineData(dataSet)

        // Update X-axis formatter based on period
        soilMoistureChart.xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val calendar = Calendar.getInstance()
                return when (period) {
                    "hourly" -> {
                        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                        calendar.add(Calendar.HOUR, -24 + value.toInt())
                        timeFormat.format(calendar.time)
                    }
                    "daily" -> {
                        val dateFormat = SimpleDateFormat("EEE", Locale.getDefault())
                        calendar.add(Calendar.DAY_OF_YEAR, -7 + value.toInt())
                        dateFormat.format(calendar.time)
                    }
                    "weekly" -> {
                        "Week ${value.toInt() + 1}"
                    }
                    "monthly" -> {
                        val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())
                        calendar.add(Calendar.MONTH, -12 + value.toInt())
                        monthFormat.format(calendar.time)
                    }
                    else -> value.toInt().toString()
                }
            }
        }

        soilMoistureChart.animateX(500)
        soilMoistureChart.invalidate()
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
        // Simulate realistic moisture changes (-0.5% to +0.5%)
        val change = (Random.nextFloat() - 0.5f)
        currentMoisture = (currentMoisture + change).coerceIn(10f, 30f)

        // Update moisture percentage
        tvMoisturePercentage.text = String.format("%.1f%%", currentMoisture)

        // Update status based on moisture level
        val status = when {
            currentMoisture < 15 -> {
                tvStatus.text = "Dry"
                tvStatus.setTextColor(getColor(R.color.status_red))
                tvOptimal.text = "Needs Water"
                tvOptimal.setTextColor(getColor(R.color.status_red))
                "Dry"
            }
            currentMoisture > 25 -> {
                tvStatus.text = "Wet"
                tvStatus.setTextColor(getColor(R.color.status_blue))
                tvOptimal.text = "Too Wet"
                tvOptimal.setTextColor(getColor(R.color.status_blue))
                "Wet"
            }
            else -> {
                tvStatus.text = "Good"
                tvStatus.setTextColor(getColor(R.color.primary_green))
                tvOptimal.text = "Optimal"
                tvOptimal.setTextColor(getColor(R.color.primary_green))
                "Good"
            }
        }

        // Update temperature (simulate realistic temperature)
        val temperature = 22f + Random.nextFloat() * 5f
        tvTemperature.text = String.format("%.1f°C", temperature)

        // Update last updated time
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val currentTime = timeFormat.format(Date())
        tvLastUpdated.text = "Last updated: just now"
    }

    private fun setupBottomNavigation() {
        findViewById<LinearLayout>(R.id.navHome)?.setOnClickListener {
            try {
                startActivity(Intent(this, DashboardActivity::class.java))
                @Suppress("DEPRECATION")
                overridePendingTransition(0, 0)
                finish()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        findViewById<LinearLayout>(R.id.navWeather)?.setOnClickListener {
            try {
                startActivity(Intent(this, WeatherActivity::class.java))
                @Suppress("DEPRECATION")
                overridePendingTransition(0, 0)
                finish()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        findViewById<LinearLayout>(R.id.navSOS)?.setOnClickListener {
            try {
                startActivity(Intent(this, "Soil"::class.java))
                @Suppress("DEPRECATION")
                overridePendingTransition(0, 0)
                finish()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        findViewById<LinearLayout>(R.id.navIrrigation)?.setOnClickListener {
            startActivity(Intent(this, IrrigationActivity::class.java))
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
            finish()
        }

        findViewById<LinearLayout>(R.id.navNotify)?.setOnClickListener {
            try {
                startActivity(Intent(this, NotificationActivity::class.java))
                @Suppress("DEPRECATION")
                overridePendingTransition(0, 0)
                finish()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        findViewById<LinearLayout>(R.id.navAccount)?.setOnClickListener {
            try {
                startActivity(Intent(this, AccountSettingsActivity::class.java))
                @Suppress("DEPRECATION")
                overridePendingTransition(0, 0)
                finish()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}