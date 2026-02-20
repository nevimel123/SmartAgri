package com.example.smartagri

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Window
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.*

class DashboardActivity : Activity() {

    // UI Components
    private lateinit var tvGreeting: TextView
    private lateinit var tvDate: TextView
    private lateinit var cardGrowthPhase: CardView
    private lateinit var tvSelectGrowthPhase: TextView
    private lateinit var cardDeviceA: CardView
    private lateinit var cardDeviceB: CardView
    private lateinit var progressDeviceA: ProgressBar
    private lateinit var progressDeviceB: ProgressBar

    // Time period buttons
    private lateinit var btnWeekly: TextView
    private lateinit var btnMonthly: TextView
    private lateinit var btnYearly: TextView

    // Bottom navigation
    private lateinit var navHome: LinearLayout
    private lateinit var navWeather: LinearLayout
    private lateinit var navSOS: LinearLayout
    private lateinit var navIrrigation: LinearLayout
    private lateinit var navNotify: LinearLayout
    private lateinit var navAccount: LinearLayout

    // Firebase
    private lateinit var auth: FirebaseAuth

    // Growth Phase State
    private var selectedPhase: GrowthPhase? = null


    enum class GrowthPhase(
        val displayName: String,
        val emoji: String,
        val description: String,
        val minMoisture: Int,   // % — lower threshold (trigger irrigation)
        val maxMoisture: Int,   // % — upper threshold (stop irrigation)
        val tipColor: Int       // resource color id placeholder (resolved at runtime)
    ) {
        VEGETATIVE(
            displayName   = "Vegetative Stage",
            emoji         = "🌱",
            description   = "Active leaf & stem growth. Roots need consistent moisture.",
            minMoisture   = 60,
            maxMoisture   = 80,
            tipColor      = R.color.green_primary
        ),
        BUD_FORMATION(
            displayName   = "Bud Formation",
            emoji         = "🌿",
            description   = "Buds forming. Slight moisture reduction promotes bud set.",
            minMoisture   = 50,
            maxMoisture   = 70,
            tipColor      = R.color.green_primary
        ),
        FLOWERING(
            displayName   = "Flowering",
            emoji         = "🌸",
            description   = "Critical pollination window. Avoid water stress & excess.",
            minMoisture   = 55,
            maxMoisture   = 75,
            tipColor      = R.color.pink_primary
        ),
        POST_FLOWERING(
            displayName   = "Post-Flowering",
            emoji         = "🌾",
            description   = "Fruit/grain fill. Reduce water gradually toward harvest.",
            minMoisture   = 40,
            maxMoisture   = 60,
            tipColor      = R.color.orange_primary
        )
    }

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        auth = FirebaseAuth.getInstance()

        if (auth.currentUser == null) {
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            return
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        initializeViews()
        setupDate()
        setupClickListeners()
        setupAnalytics()
        setupBottomNavigation()
        restoreSavedPhase()
    }

    override fun onResume() {
        super.onResume()
        loadUserData()
    }

    // ─── Init ─────────────────────────────────────────────────────────────────

    private fun initializeViews() {
        tvGreeting        = findViewById(R.id.tvGreeting)
        tvDate            = findViewById(R.id.tvDate)
        cardGrowthPhase   = findViewById(R.id.cardGrowthPhase)
        tvSelectGrowthPhase = findViewById(R.id.tvSelectGrowthPhase)
        cardDeviceA       = findViewById(R.id.cardDeviceA)
        cardDeviceB       = findViewById(R.id.cardDeviceB)
        progressDeviceA   = findViewById(R.id.progressDeviceA)
        progressDeviceB   = findViewById(R.id.progressDeviceB)
        btnWeekly         = findViewById(R.id.btnWeekly)
        btnMonthly        = findViewById(R.id.btnMonthly)
        btnYearly         = findViewById(R.id.btnYearly)
        navHome           = findViewById(R.id.navHome)
        navWeather        = findViewById(R.id.navWeather)
        navSOS            = findViewById(R.id.navSOS)
        navIrrigation     = findViewById(R.id.navIrrigation)
        navNotify         = findViewById(R.id.navNotify)
        navAccount        = findViewById(R.id.navAccount)
    }

    private fun setupDate() {
        val dateFormat = SimpleDateFormat("EEEE, MMMM d yyyy", Locale.getDefault())
        tvDate.text = dateFormat.format(Date())
    }

    // ─── Growth Phase ─────────────────────────────────────────────────────────

    /** Restore previously selected phase from SharedPreferences */
    private fun restoreSavedPhase() {
        val prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val savedPhase = prefs.getString("growth_phase", null)
        if (savedPhase != null) {
            try {
                selectedPhase = GrowthPhase.valueOf(savedPhase)
                updateGrowthPhaseUI(selectedPhase!!)
            } catch (_: IllegalArgumentException) { /* ignore stale value */ }
        }
    }

    /** Open the bottom-sheet phase picker */
    private fun showGrowthPhaseDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_growth_phase)
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
            setGravity(Gravity.BOTTOM)
            attributes = attributes.also { it.windowAnimations = android.R.style.Animation_InputMethod }
        }

        // Wire up each phase row
        GrowthPhase.values().forEach { phase ->
            val rowId = when (phase) {
                GrowthPhase.VEGETATIVE    -> R.id.rowVegetative
                GrowthPhase.BUD_FORMATION -> R.id.rowBudFormation
                GrowthPhase.FLOWERING     -> R.id.rowFlowering
                GrowthPhase.POST_FLOWERING-> R.id.rowPostFlowering
            }
            val row = dialog.findViewById<LinearLayout>(rowId)

            // Highlight the currently active phase
            if (phase == selectedPhase) {
                row.setBackgroundResource(R.drawable.bg_phase_selected)
            }

            row.setOnClickListener {
                onPhaseSelected(phase)
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun onPhaseSelected(phase: GrowthPhase) {
        selectedPhase = phase

        // Persist selection
        getSharedPreferences("user_prefs", Context.MODE_PRIVATE).edit()
            .putString("growth_phase", phase.name)
            .apply()

        updateGrowthPhaseUI(phase)

        showToast("${phase.emoji} ${phase.displayName} selected")
    }

    private fun updateGrowthPhaseUI(phase: GrowthPhase) {
        // Update the card label to show the selected phase
        tvSelectGrowthPhase.text = "${phase.emoji}  ${phase.displayName}"

        // Optionally pass thresholds to IrrigationActivity via SharedPreferences
        // so the irrigation screen can enforce them automatically
        getSharedPreferences("user_prefs", Context.MODE_PRIVATE).edit()
            .putInt("threshold_min", phase.minMoisture)
            .putInt("threshold_max", phase.maxMoisture)
            .apply()
    }

    // ─── Click Listeners ──────────────────────────────────────────────────────

    private fun setupClickListeners() {
        cardGrowthPhase.setOnClickListener {
            showGrowthPhaseDialog()
        }

        cardDeviceA.setOnClickListener {
            showToast("Device A — Moisture: 42%")
            checkMoistureAlert(42)
        }

        cardDeviceB.setOnClickListener {
            showToast("Device B — Moisture: 38%")
            checkMoistureAlert(38)
        }

        btnWeekly.setOnClickListener  { selectTimePeriod(TimePeriod.WEEKLY)  }
        btnMonthly.setOnClickListener { selectTimePeriod(TimePeriod.MONTHLY) }
        btnYearly.setOnClickListener  { selectTimePeriod(TimePeriod.YEARLY)  }
    }

    /**
     * Compare a real-time moisture reading against the active phase thresholds
     * and warn the farmer if out of range.
     */
    private fun checkMoistureAlert(currentMoisture: Int) {
        val phase = selectedPhase ?: return   // no phase set — skip check

        val message = when {
            currentMoisture < phase.minMoisture ->
                "⚠️ Moisture ${currentMoisture}% is BELOW the ${phase.displayName} minimum (${phase.minMoisture}%). Consider irrigating."
            currentMoisture > phase.maxMoisture ->
                "⚠️ Moisture ${currentMoisture}% EXCEEDS the ${phase.displayName} maximum (${phase.maxMoisture}%). Reduce irrigation."
            else ->
                "✅ Moisture ${currentMoisture}% is within the optimal range for ${phase.displayName} (${phase.minMoisture}–${phase.maxMoisture}%)."
        }

        // Show as a longer toast so the farmer can read it
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    // ─── Bottom Navigation ────────────────────────────────────────────────────

    private fun setupBottomNavigation() {
        navHome.setOnClickListener { showToast("Home") }
        navWeather.setOnClickListener {
            startActivity(Intent(this, WeatherActivity::class.java))
            @Suppress("DEPRECATION") overridePendingTransition(0, 0)
        }
        navSOS.setOnClickListener {
            startActivity(Intent(this, SoilMoistureActivity::class.java))
            @Suppress("DEPRECATION") overridePendingTransition(0, 0)
        }
        navIrrigation.setOnClickListener {
            startActivity(Intent(this, IrrigationActivity::class.java))
            @Suppress("DEPRECATION") overridePendingTransition(0, 0)
        }
        navNotify.setOnClickListener {
            showToast("Notifications")
            @Suppress("DEPRECATION") overridePendingTransition(0, 0)
        }
        navAccount.setOnClickListener {
            startActivity(Intent(this, AccountSettingsActivity::class.java))
            @Suppress("DEPRECATION") overridePendingTransition(0, 0)
        }
    }

    // ─── Analytics ────────────────────────────────────────────────────────────

    private fun setupAnalytics() {
        progressDeviceA.progress = 55
        progressDeviceB.progress = 45
        selectTimePeriod(TimePeriod.WEEKLY)
    }

    private fun selectTimePeriod(period: TimePeriod) {
        resetTimePeriodButtons()
        val btn = when (period) {
            TimePeriod.WEEKLY  -> btnWeekly
            TimePeriod.MONTHLY -> btnMonthly
            TimePeriod.YEARLY  -> btnYearly
        }
        btn.setBackgroundResource(R.drawable.bg_tab_selected)
        btn.setTextColor(ContextCompat.getColor(this, R.color.white))
        btn.setTypeface(null, android.graphics.Typeface.BOLD)
    }

    private fun resetTimePeriodButtons() {
        listOf(btnWeekly, btnMonthly, btnYearly).forEach { button ->
            button.setBackgroundResource(R.drawable.bg_tab_unselected)
            button.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
            button.setTypeface(null, android.graphics.Typeface.NORMAL)
        }
    }

    // ─── Firebase / User ──────────────────────────────────────────────────────

    private fun loadUserData() {
        val currentUser = auth.currentUser ?: run { goToLogin(); return }
        val database = FirebaseDatabase.getInstance(
            "https://smartagri-2ef16-default-rtdb.asia-southeast1.firebasedatabase.app/"
        )
        database.getReference("users").child(currentUser.uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val firstName = snapshot.child("firstName").getValue(String::class.java) ?: "User"
                        val lastName  = snapshot.child("lastName") .getValue(String::class.java) ?: ""
                        val email     = snapshot.child("email")    .getValue(String::class.java) ?: ""
                        val address   = snapshot.child("address")  .getValue(String::class.java) ?: ""
                        val userType  = snapshot.child("userType") .getValue(String::class.java) ?: "farmer"
                        val farmGroup = snapshot.child("farmGroup").getValue(String::class.java) ?: ""

                        getSharedPreferences("user_prefs", Context.MODE_PRIVATE).edit().apply {
                            putString("firstName", firstName)
                            putString("lastName",  lastName)
                            putString("email",     email)
                            putString("address",   address)
                            putString("userType",  userType)
                            putString("farmGroup", farmGroup)
                            apply()
                        }
                        setupGreeting(firstName)
                    } else {
                        showToast("User data not found")
                        goToLogin()
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    showToast("Failed to load user data: ${error.message}")
                }
            })
    }

    private fun setupGreeting(firstName: String) {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val greeting = when {
            hour < 12 -> "Good Morning"
            hour < 17 -> "Good Afternoon"
            else      -> "Good Evening"
        }
        tvGreeting.text = "$greeting, $firstName!"
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private fun showToast(message: String) =
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    private fun goToLogin() {
        startActivity(Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }

    enum class TimePeriod { WEEKLY, MONTHLY, YEARLY }
}