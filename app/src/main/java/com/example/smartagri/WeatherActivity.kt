package com.example.smartagri

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.location.Geocoder
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.example.smartagri.model.WeatherApi
import com.example.smartagri.model.WeatherResponse
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.*

class WeatherActivity : Activity() {

    private val API_KEY = BuildConfig.WEATHER_API_KEY

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance(
        "https://smartagri-2ef16-default-rtdb.asia-southeast1.firebasedatabase.app/"
    )

    private lateinit var tvTime: TextView
    private lateinit var tvTemperature: TextView
    private lateinit var tvCondition: TextView
    private lateinit var tvFeelsLike: TextView
    private lateinit var tvWind: TextView
    private lateinit var tvHumidity: TextView
    private lateinit var tvVisibility: TextView
    private lateinit var tvPressure: TextView
    private lateinit var tvUVIndex: TextView
    private lateinit var tvDewPoint: TextView

    private lateinit var navHome: LinearLayout
    private lateinit var navWeather: LinearLayout
    private lateinit var navSOS: LinearLayout
    private lateinit var navIrrigation: LinearLayout
    private lateinit var navNotify: LinearLayout
    private lateinit var navAccount: LinearLayout
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_weather)

        initViews()
        setupBottomNavigation()
        updateTime()

        // ✅ Fetch user's address from Firebase
        loadUserAddressAndFetchWeather()
    }

    override fun onResume() {
        super.onResume()
        // ✅ Refresh weather data when returning to this activity
        loadUserAddressAndFetchWeather()
    }

    private val timeRunnable = object : Runnable {
        override fun run() {
            val currentTime = SimpleDateFormat("h:mm:ss a", Locale.getDefault()).format(Date())
            tvTime.text = currentTime
            handler.postDelayed(this, 1000)
        }
    }

    /** ✅ Load user's address from Firebase and fetch weather */
    private fun loadUserAddressAndFetchWeather() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            return
        }

        val userId = currentUser.uid
        val userRef = database.getReference("users").child(userId)

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val address = snapshot.child("address").getValue(String::class.java) ?: "Cebu, Philippines"

                    // Update SharedPreferences with fresh address
                    val sharedPref = getSharedPreferences("user_prefs", Context.MODE_PRIVATE).edit()
                    sharedPref.putString("address", address)
                    sharedPref.apply()

                    // Fetch weather for this address
                    fetchWeatherFromAddress(address)
                } else {
                    Toast.makeText(this@WeatherActivity, "User data not found", Toast.LENGTH_SHORT).show()
                    fetchWeatherFromAddress("Cebu, Philippines") // Fallback
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@WeatherActivity, "Failed to load user data", Toast.LENGTH_SHORT).show()
                fetchWeatherFromAddress("Cebu, Philippines") // Fallback
            }
        })
    }

    /** 🔹 Convert address to lat/lon and fetch weather */
    private fun fetchWeatherFromAddress(address: String) {
        try {
            val geocoder = Geocoder(this)
            @Suppress("DEPRECATION")
            val locations = geocoder.getFromLocationName(address, 1)
            if (locations.isNullOrEmpty()) {
                Toast.makeText(this, "Unable to locate address. Showing default location.", Toast.LENGTH_SHORT).show()
                fetchWeatherByCoordinates(10.3157, 123.8854) // Cebu fallback
            } else {
                val loc = locations[0]
                fetchWeatherByCoordinates(loc.latitude, loc.longitude)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error fetching location. Showing default.", Toast.LENGTH_SHORT).show()
            fetchWeatherByCoordinates(10.3157, 123.8854) // Cebu fallback
        }
    }

    /** 🔹 Fetch weather using latitude & longitude */
    private fun fetchWeatherByCoordinates(lat: Double, lon: Double) {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
        val client = OkHttpClient.Builder().addInterceptor(logging).build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.openweathermap.org/data/2.5/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val api = retrofit.create(WeatherApi::class.java)

        api.getWeatherByCoordinates(lat, lon, API_KEY).enqueue(object : Callback<WeatherResponse> {
            override fun onResponse(call: Call<WeatherResponse>, response: Response<WeatherResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val data = response.body()!!
                    updateUI(data)
                    saveWeatherToFirebase(data)
                } else {
                    Toast.makeText(this@WeatherActivity, "Failed to get weather data", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                Toast.makeText(this@WeatherActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun saveWeatherToFirebase(data: WeatherResponse) {
        val userId = auth.currentUser?.uid ?: return
        val sharedPref = getSharedPreferences("weather_cache", Context.MODE_PRIVATE)
        val lastWeatherJson = sharedPref.getString("last_weather", "")
        val gson = com.google.gson.Gson()
        val currentWeatherJson = gson.toJson(data)

        if (currentWeatherJson == lastWeatherJson) return

        // ✅ Save weather data inside users/{userId}/weatherData
        val weatherRef = database.getReference("users")
            .child(userId)
            .child("weatherData")
            .child(System.currentTimeMillis().toString())

        val weatherData = hashMapOf(
            "temperature" to data.main.temp,
            "feelsLike" to data.main.feelsLike,
            "humidity" to data.main.humidity,
            "pressure" to data.main.pressure,
            "windSpeed" to data.wind.speed,
            "windDirection" to data.wind.deg,
            "condition" to data.weather[0].description,
            "visibility" to data.visibility,
            "timestamp" to System.currentTimeMillis()
        )

        weatherRef.setValue(weatherData)
            .addOnSuccessListener {
                sharedPref.edit().putString("last_weather", currentWeatherJson).apply()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this@WeatherActivity, "Failed to save weather data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateUI(data: WeatherResponse) {
        tvTemperature.text = "${data.main.temp.toInt()}°"
        tvCondition.text = data.weather[0].description.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }
        tvFeelsLike.text = "Feels like ${data.main.feelsLike.toInt()}°"
        tvWind.text = "${data.wind.speed.toInt()} mph ${getWindDirection(data.wind.deg)}"
        tvHumidity.text = "${data.main.humidity}%"
        tvVisibility.text = "${(data.visibility / 1609.34).toInt()}mi"
        tvPressure.text = "${(data.main.pressure * 0.02953).toInt()} IN"
        tvUVIndex.text = "N/A"
        tvDewPoint.text = "${calculateDewPoint(data.main.temp, data.main.humidity).toInt()}°"
    }

    private fun updateTime() = handler.post(timeRunnable)

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(timeRunnable)
    }

    private fun getWindDirection(degrees: Int): String {
        val directions = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
        return directions[((degrees + 22.5) / 45.0).toInt() % 8]
    }

    private fun calculateDewPoint(tempF: Double, humidity: Int): Double {
        val tempC = (tempF - 32) * 5 / 9
        val a = 17.27
        val b = 237.7
        val alpha = ((a * tempC) / (b + tempC)) + Math.log(humidity / 100.0)
        val dewPointC = (b * alpha) / (a - alpha)
        return (dewPointC * 9 / 5) + 32
    }

    private fun initViews() {
        tvTime = findViewById(R.id.tvTime)
        tvTemperature = findViewById(R.id.tvTemperature)
        tvCondition = findViewById(R.id.tvCondition)
        tvFeelsLike = findViewById(R.id.tvFeelsLike)
        tvWind = findViewById(R.id.tvWind)
        tvHumidity = findViewById(R.id.tvHumidity)
        tvVisibility = findViewById(R.id.tvVisibility)
        tvPressure = findViewById(R.id.tvPressure)
        tvUVIndex = findViewById(R.id.tvUVIndex)
        tvDewPoint = findViewById(R.id.tvDewPoint)

        navHome = findViewById(R.id.navHome)
        navWeather = findViewById(R.id.navWeather)
        navSOS = findViewById(R.id.navSOS)
        navIrrigation = findViewById(R.id.navIrrigation)
        navNotify = findViewById(R.id.navNotify)
        navAccount = findViewById(R.id.navAccount)
    }

    private fun setupBottomNavigation() {
        navHome.setOnClickListener {
            startActivity(Intent(this, DashboardActivity::class.java))
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
        navWeather.setOnClickListener {
            Toast.makeText(this, "Weather", Toast.LENGTH_SHORT).show()
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
        navSOS.setOnClickListener {
            startActivity(Intent(this, SoilMoistureActivity::class.java))
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
        navIrrigation.setOnClickListener {
            startActivity(Intent(this, IrrigationActivity::class.java))
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
        navNotify.setOnClickListener {
            startActivity(Intent(this, NotificationActivity::class.java))
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
        navAccount.setOnClickListener {
            startActivity(Intent(this, AccountSettingsActivity::class.java))
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }
}