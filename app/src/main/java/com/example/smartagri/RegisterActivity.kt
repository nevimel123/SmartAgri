package com.example.smartagri

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.util.Patterns
import android.widget.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class RegisterActivity : Activity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var addressAdapter: ArrayAdapter<String>

    // Track address selection state
    private var isAddressSelected = false
    private var lastValidAddress = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()

        val firstName = findViewById<EditText>(R.id.firstnameInput)
        val lastName = findViewById<EditText>(R.id.lastnameInput)
        val email = findViewById<EditText>(R.id.emailInput)
        val address = findViewById<AutoCompleteTextView>(R.id.addressInput)
        val password = findViewById<EditText>(R.id.regpasswordInput)
        val confirmPassword = findViewById<EditText>(R.id.confirmpasswordInput)
        val registerButton = findViewById<Button>(R.id.registerButton)
        val loginText = findViewById<TextView>(R.id.loginText)

        loginText.text = "Already have an account? Login"

        // Setup AutoComplete Adapter
        addressAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line)
        address.setAdapter(addressAdapter)
        address.threshold = 3 // show suggestions after 3 characters for better results

        // Listen for item selection from dropdown
        address.setOnItemClickListener { _, _, position, _ ->
            isAddressSelected = true
            lastValidAddress = addressAdapter.getItem(position) ?: ""
            address.error = null // Clear any error when valid selection is made
        }

        // Listen for user input and fetch suggestions
        address.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Mark as not selected if user continues typing after selection
                val currentText = s.toString().trim()
                if (isAddressSelected && currentText != lastValidAddress) {
                    isAddressSelected = false
                }

                if (currentText.length >= 3) {
                    fetchGeoapifySuggestions(currentText)
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        // Validate on focus loss
        address.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val currentText = address.text.toString().trim()
                if (currentText.isNotEmpty() && !isAddressSelected) {
                    address.error = "Please select an address from the dropdown"
                } else if (currentText.isEmpty()) {
                    address.error = "Required"
                }
            }
        }

        registerButton.setOnClickListener {
            if (!validateRegister(firstName, lastName, email, address, password, confirmPassword)) {
                return@setOnClickListener
            }

            registerWithFirebase(
                firstName.text.toString().trim(),
                lastName.text.toString().trim(),
                email.text.toString().trim(),
                address.text.toString().trim(),
                password.text.toString().trim()
            )
        }

        loginText.setOnClickListener {
            goToLogin()
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }

    // --- Fetch suggestions from Geoapify ---
    private fun fetchGeoapifySuggestions(query: String) {
        val apiKey = BuildConfig.GEOAPIFY_API_KEY
        val url = "https://api.geoapify.com/v1/geocode/autocomplete?text=$query&format=json&apiKey=$apiKey"

        val request = Request.Builder().url(url).build()
        val client = OkHttpClient()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("Geoapify", "Failed to fetch suggestions: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.string()?.let { body ->
                    val suggestions = mutableListOf<String>()
                    try {
                        val json = JSONObject(body)
                        val results = json.getJSONArray("results")
                        for (i in 0 until results.length()) {
                            val item = results.getJSONObject(i)
                            val formatted = item.getString("formatted")
                            suggestions.add(formatted)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    // Update adapter on UI thread
                    runOnUiThread {
                        addressAdapter.clear()
                        addressAdapter.addAll(suggestions)
                        addressAdapter.notifyDataSetChanged()
                    }
                }
            }
        })
    }

    // --- Firebase registration ---
    private fun registerWithFirebase(
        firstName: String,
        lastName: String,
        email: String,
        address: String,
        password: String
    ) {
        showLoading(true)
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this@RegisterActivity) { task ->
                showLoading(false)
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid
                    if (userId != null) {
                        saveUserDataToDatabase(userId, firstName, lastName, email, address)
                    } else {
                        Toast.makeText(this, "Registration failed", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(
                        this,
                        task.exception?.message ?: "Registration failed",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun saveUserDataToDatabase(
        userId: String,
        firstName: String,
        lastName: String,
        email: String,
        address: String
    ) {
        val database = FirebaseDatabase.getInstance(
            "https://smartagri-2ef16-default-rtdb.asia-southeast1.firebasedatabase.app/"
        )
        // ✅ Use "users" path consistently
        val usersRef = database.getReference("users").child(userId)

        val userProfile = hashMapOf(
            "firstName" to firstName,
            "lastName" to lastName,
            "email" to email,
            "address" to address,
            "userType" to "farmer",
            "farmGroup" to "",
            "createdAt" to System.currentTimeMillis()
        )

        usersRef.setValue(userProfile)
            .addOnSuccessListener {
                // Don't save to SharedPreferences here - let LoginActivity do it
                auth.signOut()
                Toast.makeText(this, "Registration successful! Please login.", Toast.LENGTH_SHORT).show()
                goToLogin()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(
                    this,
                    "Failed to save user data: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun validateRegister(
        firstName: EditText,
        lastName: EditText,
        email: EditText,
        address: AutoCompleteTextView,
        password: EditText,
        confirmPassword: EditText
    ): Boolean {
        var isValid = true

        if (firstName.text.toString().trim().isEmpty()) {
            firstName.error = "Required"
            isValid = false
        }

        if (lastName.text.toString().trim().isEmpty()) {
            lastName.error = "Required"
            isValid = false
        }

        if (email.text.toString().trim().isEmpty()) {
            email.error = "Required"
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email.text.toString()).matches()) {
            email.error = "Invalid email format"
            isValid = false
        }

        val addressText = address.text.toString().trim()
        if (addressText.isEmpty()) {
            address.error = "Required"
            isValid = false
        } else if (!isAddressSelected) {
            address.error = "Please select a valid address from the dropdown"
            Toast.makeText(this, "Please select your address from the suggestions", Toast.LENGTH_LONG).show()
            isValid = false
        }

        if (password.text.toString().length < 6) {
            password.error = "At least 6 characters"
            isValid = false
        }

        if (password.text.toString() != confirmPassword.text.toString()) {
            confirmPassword.error = "Passwords do not match"
            isValid = false
        }

        return isValid
    }

    private fun showLoading(isLoading: Boolean) {
        val registerButton = findViewById<Button>(R.id.registerButton)
        registerButton.isEnabled = !isLoading
    }

    private fun goToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}