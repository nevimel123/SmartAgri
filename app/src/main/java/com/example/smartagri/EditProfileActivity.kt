package com.example.smartagri

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class EditProfileActivity : Activity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var btnBack: ImageView
    private lateinit var ivProfileImage: ImageView
    private lateinit var etFirstName: EditText
    private lateinit var etLastName: EditText
    private lateinit var etEmail: EditText
    private lateinit var btnUpdate: Button
    private var progressBar: ProgressBar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
        setContentView(R.layout.activity_edit_profile)

        auth = FirebaseAuth.getInstance()
        initViews()
        setupClickListeners()
        loadCurrentData()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        ivProfileImage = findViewById(R.id.ivProfileImage)
        etFirstName = findViewById(R.id.etFirstName)
        etLastName = findViewById(R.id.etLastName)
        etEmail = findViewById(R.id.etEmail)
        btnUpdate = findViewById(R.id.btnUpdate)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener {
            finish()
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }

        btnUpdate.setOnClickListener {
            updateProfile()
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
        findViewById<LinearLayout>(R.id.navAccount).setOnClickListener {
            startActivity(Intent(this, AccountSettingsActivity::class.java))
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }

    private fun loadCurrentData() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
            finish()
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
            return
        }

        progressBar?.visibility = View.VISIBLE

        // Load from Firebase users node (not Farmer)
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
                progressBar?.visibility = View.GONE
                if (snapshot.exists()) {
                    etFirstName.setText(snapshot.child("firstName").getValue(String::class.java) ?: "")
                    etLastName.setText(snapshot.child("lastName").getValue(String::class.java) ?: "")
                    etEmail.setText(snapshot.child("email").getValue(String::class.java) ?: "")

                    // Display profile placeholder image
                    ivProfileImage.setImageResource(R.drawable.ic_profile_placeholder)
                } else {
                    Toast.makeText(this@EditProfileActivity, "Profile data not found", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                progressBar?.visibility = View.GONE
                Toast.makeText(this@EditProfileActivity, "Failed to load profile: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateProfile() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
            return
        }

        val firstName = etFirstName.text.toString().trim()
        val lastName = etLastName.text.toString().trim()
        val email = etEmail.text.toString().trim()

        // Validation
        if (firstName.isEmpty()) {
            etFirstName.error = "Required"
            return
        }
        if (lastName.isEmpty()) {
            etLastName.error = "Required"
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = "Invalid Email"
            return
        }


        val database = FirebaseDatabase.getInstance(
            "https://smartagri-2ef16-default-rtdb.asia-southeast1.firebasedatabase.app/"
        )
        // ✅ Use "users" path consistently (not "Farmer")
        val userRef = database.getReference("users").child(currentUser.uid)

        val updates = mapOf(
            "firstName" to firstName,
            "lastName" to lastName,
            "email" to email,
        )

        progressBar?.visibility = View.VISIBLE
        btnUpdate.isEnabled = false

        userRef.updateChildren(updates)
            .addOnSuccessListener {
                progressBar?.visibility = View.GONE
                btnUpdate.isEnabled = true

                // ✅ Update SharedPreferences with new data
                val sharedPref = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                with(sharedPref.edit()) {
                    putString("firstName", firstName)
                    putString("lastName", lastName)
                    putString("email", email)
                    apply()
                }

                Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                finish()
                @Suppress("DEPRECATION")
                overridePendingTransition(0, 0)
            }
            .addOnFailureListener { e ->
                progressBar?.visibility = View.GONE
                btnUpdate.isEnabled = true

                if (e is DatabaseException) {
                    Toast.makeText(this, "Permission denied. Cannot update profile.", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "Update failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
    }
}