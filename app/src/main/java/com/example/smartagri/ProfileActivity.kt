package com.example.smartagri

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ProfileActivity : Activity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var btnBack: ImageView
    private lateinit var ivProfileImage: ImageView
    private lateinit var tvFirstName: TextView
    private lateinit var tvLastName: TextView
    private lateinit var tvUsername: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvAddress: TextView
    private lateinit var btnEditProfile: Button
    private var progressBar: ProgressBar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        auth = FirebaseAuth.getInstance()

        initViews()
        setupClickListeners()
        loadProfileData()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        ivProfileImage = findViewById(R.id.ivProfileImage)
        tvFirstName = findViewById(R.id.tvFirstName)
        tvLastName = findViewById(R.id.tvLastName)
        tvUsername = findViewById(R.id.tvUsername)
        tvEmail = findViewById(R.id.tvEmail)
        tvAddress = findViewById(R.id.tvAddress)
        btnEditProfile = findViewById(R.id.btnEditProfile)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener {
            finish()
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }

        btnEditProfile.setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }

        setupBottomNavigation()
    }

    private fun setupBottomNavigation() {
        findViewById<LinearLayout>(R.id.navHome).setOnClickListener {
            val intent = Intent(this, DashboardActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
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

    override fun onResume() {
        super.onResume()
        // Reload profile data after returning from EditProfileActivity
        loadProfileData()
    }

    private fun loadProfileData() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            return
        }

        showLoading(true)

        // Fetch fresh data from Firebase
        fetchFromFirebase(currentUser.uid)
    }

    private fun fetchFromFirebase(userId: String) {
        val database = FirebaseDatabase.getInstance(
            "https://smartagri-2ef16-default-rtdb.asia-southeast1.firebasedatabase.app/"
        )
        // ✅ Use "users" path consistently (not "Farmer")
        val userRef = database.getReference("users").child(userId)

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                showLoading(false)
                if (snapshot.exists()) {
                    val firstName = snapshot.child("firstName").getValue(String::class.java) ?: ""
                    val lastName = snapshot.child("lastName").getValue(String::class.java) ?: ""
                    val email = snapshot.child("email").getValue(String::class.java) ?: ""
                    val address = snapshot.child("address").getValue(String::class.java) ?: ""

                    tvFirstName.text = firstName
                    tvLastName.text = lastName
                    tvEmail.text = email
                    tvAddress.text = address
                    tvUsername.text = generateUsername(firstName, lastName)

                    // Save latest data to SharedPreferences
                    saveToSharedPreferences(firstName, lastName, email, address)
                } else {
                    Toast.makeText(this@ProfileActivity, "Profile data not found", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                showLoading(false)
                Toast.makeText(this@ProfileActivity, "Failed to load profile: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun generateUsername(firstName: String, lastName: String): String {
        return if (firstName.isNotEmpty() && lastName.isNotEmpty()) {
            "${firstName[0].lowercaseChar()}${lastName.lowercase()}"
        } else firstName.lowercase().ifEmpty { "user" }
    }

    private fun saveToSharedPreferences(firstName: String, lastName: String, email: String, address: String) {
        val sharedPref = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("firstName", firstName)
            putString("lastName", lastName)
            putString("email", email)
            putString("address", address)
            apply()
        }
    }

    private fun showLoading(isLoading: Boolean) {
        progressBar?.visibility = if (isLoading) View.VISIBLE else View.GONE
        btnEditProfile.isEnabled = !isLoading
    }
}