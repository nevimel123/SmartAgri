package com.example.smartagri

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast

class ChangePasswordActivity : Activity() {

    private lateinit var currentPasswordInput: EditText
    private lateinit var newPasswordInput: EditText
    private lateinit var confirmNewPasswordInput: EditText
    private lateinit var changePasswordButton: Button
    private lateinit var backToSettingsText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_password)

        initializeViews()
        setupClickListeners()
    }

    private fun initializeViews() {
        currentPasswordInput = findViewById(R.id.currentpasswordInput)
        newPasswordInput = findViewById(R.id.changenewpasswordInput)
        confirmNewPasswordInput = findViewById(R.id.changeconfirmnewpasswordInput)
        changePasswordButton = findViewById(R.id.changePasswordButton)
        backToSettingsText = findViewById(R.id.backToSettingsText)
    }

    private fun setupClickListeners() {
        changePasswordButton.setOnClickListener {
            handleChangePassword()
        }

        backToSettingsText.setOnClickListener {
            navigateBack()
        }
    }

    private fun handleChangePassword() {
        val currentPassword = currentPasswordInput.text.toString().trim()
        val newPassword = newPasswordInput.text.toString().trim()
        val confirmPassword = confirmNewPasswordInput.text.toString().trim()

        // Validation
        if (currentPassword.isEmpty()) {
            Toast.makeText(this, "Please enter your current password", Toast.LENGTH_SHORT).show()
            currentPasswordInput.requestFocus()
            return
        }

        if (newPassword.isEmpty()) {
            Toast.makeText(this, "Please enter a new password", Toast.LENGTH_SHORT).show()
            newPasswordInput.requestFocus()
            return
        }

        if (newPassword.length < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
            newPasswordInput.requestFocus()
            return
        }

        if (confirmPassword.isEmpty()) {
            Toast.makeText(this, "Please confirm your new password", Toast.LENGTH_SHORT).show()
            confirmNewPasswordInput.requestFocus()
            return
        }

        if (newPassword != confirmPassword) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            confirmNewPasswordInput.requestFocus()
            return
        }

        if (currentPassword == newPassword) {
            Toast.makeText(this, "New password must be different from current password", Toast.LENGTH_SHORT).show()
            newPasswordInput.requestFocus()
            return
        }

        // TODO: Add your actual password change logic here
        // This would typically involve:
        // 1. Verifying the current password with your backend/database
        // 2. Updating the password in your backend/database
        // 3. Handling success/error responses

        // For now, just show success message
        Toast.makeText(this, "Password changed successfully!", Toast.LENGTH_LONG).show()

        // Clear the input fields
        currentPasswordInput.setText("")
        newPasswordInput.setText("")
        confirmNewPasswordInput.setText("")

        // Navigate back to settings after a short delay
        android.os.Handler().postDelayed({
            navigateBack()
        }, 1500)
    }

    private fun navigateBack() {
        val intent = Intent(this, AccountSettingsActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
        finish()
    }

    override fun onBackPressed() {
        navigateBack()
    }
}