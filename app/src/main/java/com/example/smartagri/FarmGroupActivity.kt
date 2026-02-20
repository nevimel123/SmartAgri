package com.example.smartagri

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class FarmGroupActivity : Activity() {

    private lateinit var farmGroupInput: EditText
    private lateinit var farmGroupButton: Button
    private lateinit var backToAccountText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_farm_group)

        farmGroupInput = findViewById(R.id.FarmgroupInput)
        farmGroupButton = findViewById(R.id.FarmgroupButton)
        backToAccountText = findViewById(R.id.backToAccountText)

        farmGroupButton.setOnClickListener {
            joinFarmGroup()
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }

        backToAccountText.setOnClickListener {
            finish()
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }

        loadCurrentFarmGroup()
    }

    private fun loadCurrentFarmGroup() {
        val sharedPref = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val currentFarm = sharedPref.getString("farmGroup", "")
        farmGroupInput.setText(currentFarm)
    }

    private fun joinFarmGroup() {
        val farmCode = farmGroupInput.text.toString().trim()
        if (farmCode.isEmpty()) {
            farmGroupInput.error = "Enter farm group code"
            farmGroupInput.requestFocus()
            return
        }

        val database = FirebaseDatabase.getInstance(
            "https://smartagri-2ef16-default-rtdb.asia-southeast1.firebasedatabase.app/"
        )
        val farmGroupsRef = database.getReference("farmGroups")

        farmGroupsRef.child(farmCode).get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                // Update user farmGroup in database
                val userId = FirebaseAuth.getInstance().currentUser?.uid
                if (userId != null) {
                    database.getReference("users").child(userId).child("farmGroup").setValue(farmCode)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Joined farm group successfully!", Toast.LENGTH_SHORT).show()
                            // Save locally
                            getSharedPreferences("user_prefs", Context.MODE_PRIVATE).edit().putString("farmGroup", farmCode).apply()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Failed to join: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            } else {
                Toast.makeText(this, "Invalid farm group code", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Failed to verify farm code: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
