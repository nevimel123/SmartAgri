package com.example.smartagri

import android.app.Activity
import android.os.Bundle
import android.util.Patterns
import android.widget.*

class ForgotPasswordActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        val emailInput = findViewById<EditText>(R.id.forgotemailInput)
        val resetButton = findViewById<Button>(R.id.resetPasswordButton)
        val backToLogin = findViewById<TextView>(R.id.backToLoginText)

        backToLogin.text = "Back to Login"

        resetButton.setOnClickListener {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
            val email = emailInput.text.toString().trim()

            if (email.isEmpty()) {
                emailInput.error = "Email is required"
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailInput.error = "Invalid email address"
                return@setOnClickListener
            }

            Toast.makeText(
                this,
                "Password reset link sent to $email",
                Toast.LENGTH_LONG
            ).show()
        }

        backToLogin.setOnClickListener {
            finish()
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)

        }
    }
}
