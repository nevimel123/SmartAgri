package com.example.smartagri.model

import com.google.gson.annotations.SerializedName

data class WeatherResponse(
    val main: Main,
    val weather: List<Weather>,
    val wind: Wind,
    val rain: Rain? = null,
    val visibility: Int = 10000, // in meters
    val dt: Long // timestamp
)

data class Main(
    val temp: Double,
    val humidity: Int,
    @SerializedName("feels_like") val feelsLike: Double,
    val pressure: Int, // in hPa
    @SerializedName("temp_min") val tempMin: Double,
    @SerializedName("temp_max") val tempMax: Double
)

data class Weather(
    val main: String,
    val description: String,
    val icon: String
)

data class Wind(
    val speed: Double,
    val deg: Int = 0 // wind direction in degrees
)

data class Rain(
    @SerializedName("1h") val oneHour: Double? = 0.0
)