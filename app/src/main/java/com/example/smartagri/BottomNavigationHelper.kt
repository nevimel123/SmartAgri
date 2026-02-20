package com.example.smartagri

import android.app.Activity
import android.content.Intent
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat

object BottomNavigationHelper {

    fun setup(activity: Activity, active: String) {
        val map = mapOf(
            "home" to activity.findViewById<LinearLayout>(R.id.navHome),
            "weather" to activity.findViewById<LinearLayout>(R.id.navWeather),
            "sos" to activity.findViewById<LinearLayout>(R.id.navSOS),
            "irrigation" to activity.findViewById<LinearLayout>(R.id.navIrrigation),
            "notify" to activity.findViewById<LinearLayout>(R.id.navNotify),
            "account" to activity.findViewById<LinearLayout>(R.id.navAccount)
        )

        val defaultColor = ContextCompat.getColor(activity, R.color.text_secondary)
        val selectedColor = ContextCompat.getColor(activity, R.color.green_primary)

        map.forEach { (key, nav) ->
            val icon = nav.getChildAt(0) as ImageView
            val text = nav.getChildAt(1) as TextView

            if (key == active) {
                icon.setColorFilter(selectedColor)
                text.setTextColor(selectedColor)
                text.setTypeface(null, android.graphics.Typeface.BOLD)
            } else {
                icon.setColorFilter(defaultColor)
                text.setTextColor(defaultColor)
                text.setTypeface(null, android.graphics.Typeface.NORMAL)
            }

            nav.setOnClickListener {
                if (key == active) return@setOnClickListener
                when (key) {
                    "home" -> activity.startActivity(Intent(activity, DashboardActivity::class.java))
                    "weather" -> activity.startActivity(Intent(activity, WeatherActivity::class.java))
                    "sos" -> {} // implement SOS activity
                    "irrigation" -> {} // implement Irrigation activity
                    "notify" -> {} // implement Notify activity
                    "account" -> activity.startActivity(Intent(activity, AccountSettingsActivity::class.java))
                }
                activity.finish()
            }
        }
    }
}
