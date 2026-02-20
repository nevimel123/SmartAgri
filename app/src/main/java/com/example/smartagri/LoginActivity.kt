package com.example.smartagri

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class LoginActivity : Activity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Check if user is already logged in
        if (auth.currentUser != null) {
            Log.d("LoginActivity", "User already logged in, navigating to dashboard")
            navigateToDashboard()
            return
        }

        val emailInput = findViewById<EditText>(R.id.emailInput)
        val passwordInput = findViewById<EditText>(R.id.passwordInput)
        val loginButton = findViewById<Button>(R.id.loginButton)
        val forgotPassword = findViewById<TextView>(R.id.forgotPassword)
        val registerText = findViewById<TextView>(R.id.textregister)

        loginButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (!validateLogin(email, password, emailInput, passwordInput)) return@setOnClickListener

            // Login with Firebase
            loginWithFirebase(email, password)
        }

        forgotPassword.setOnClickListener {
            val email = emailInput.text.toString().trim()
            if (email.isEmpty()) {
                emailInput.error = "Enter your email first"
                emailInput.requestFocus()
                startActivity(Intent(this, ForgotPasswordActivity::class.java))
                @Suppress("DEPRECATION")
                overridePendingTransition(0, 0)
            }
            resetPassword(email)
        }

        registerText.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }

    private fun loginWithFirebase(email: String, password: String) {
        showLoading(true)

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d("LoginActivity", "Login successful")
                    // Login success
                    Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()
                    @Suppress("DEPRECATION")
                    overridePendingTransition(0, 0)

                    // Load user data and navigate
                    loadUserDataAndNavigate()
                } else {
                    showLoading(false)
                    Log.e("LoginActivity", "Login failed: ${task.exception?.message}")
                    // Login failed
                    val errorMessage = when {
                        task.exception?.message?.contains("no user record") == true ->
                            "No account found. Please register first."
                        task.exception?.message?.contains("password is invalid") == true ->
                            "Incorrect password. Please try again."
                        task.exception?.message?.contains("network") == true ->
                            "Network error. Please check your connection."
                        else -> "Login failed: ${task.exception?.message}"
                    }
                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun loadUserDataAndNavigate() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.e("LoginActivity", "User ID is null")
            showLoading(false)
            navigateToDashboard()
            return
        }

        Log.d("LoginActivity", "Loading user data for userId: $userId")

        // Use the correct regional database URL
        val database = FirebaseDatabase.getInstance("https://smartagri-2ef16-default-rtdb.asia-southeast1.firebasedatabase.app/")

        database.getReference("users").child(userId).get()
            .addOnSuccessListener { snapshot ->
                Log.d("LoginActivity", "User data loaded successfully")

                if (snapshot.exists()) {
                    // Save to SharedPreferences for offline access
                    val sharedPref = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                    val editor = sharedPref.edit()

                    val firstName = snapshot.child("firstName").value?.toString() ?: ""
                    val lastName = snapshot.child("lastName").value?.toString() ?: ""
                    val email = snapshot.child("email").value?.toString() ?: ""
                    val address = snapshot.child("address").value?.toString() ?: ""

                    editor.putString("firstName", firstName)
                    editor.putString("lastName", lastName)
                    editor.putString("email", email)
                    editor.putString("address", address)
                    editor.apply()

                    Log.d("LoginActivity", "User data saved to SharedPreferences")
                } else {
                    Log.w("LoginActivity", "User data does not exist in database")
                }

                navigateToDashboard()
            }
            .addOnFailureListener { exception ->
                Log.e("LoginActivity", "Failed to load user data: ${exception.message}", exception)
                // Still navigate even if user data fetch fails
                Toast.makeText(this, "Warning: Could not load user profile", Toast.LENGTH_SHORT).show()
                navigateToDashboard()
            }
    }

    private fun resetPassword(email: String) {
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Please enter a valid email", Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true)

        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                showLoading(false)
                if (task.isSuccessful) {
                    Toast.makeText(
                        this,
                        "Password reset email sent to $email",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(
                        this,
                        "Failed to send reset email: ${task.exception?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun navigateToDashboard() {
        Log.d("LoginActivity", "Navigating to Dashboard")
        val intent = Intent(this, DashboardActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
        finish()
    }

    private fun validateLogin(
        email: String,
        password: String,
        emailInput: EditText,
        passwordInput: EditText
    ): Boolean {

        if (email.isEmpty()) {
            emailInput.error = "Email is required"
            emailInput.requestFocus()
            return false
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.error = "Invalid email format"
            emailInput.requestFocus()
            return false
        }

        if (password.isEmpty()) {
            passwordInput.error = "Password is required"
            passwordInput.requestFocus()
            return false
        }

        if (password.length < 6) {
            passwordInput.error = "Password must be at least 6 characters"
            passwordInput.requestFocus()
            return false
        }

        return true
    }

    private fun showLoading(isLoading: Boolean) {
        val loginButton = findViewById<Button>(R.id.loginButton)
        loginButton.isEnabled = !isLoading
    }
}