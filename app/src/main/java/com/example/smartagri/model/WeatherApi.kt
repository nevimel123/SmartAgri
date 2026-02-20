package com.example.smartagri.model

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApi {
    @GET("weather")
    fun getWeatherByCoordinates(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "imperial"
    ): Call<WeatherResponse>


    /**
     * Optional: Get UV Index data
     * Note: This requires latitude and longitude
     * You can get lat/lon from the weather response: response.coord.lat, response.coord.lon
     */
    @GET("uvi")
    fun getUVIndex(
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double,
        @Query("appid") apiKey: String
    ): Call<UVResponse>
}

data class UVResponse(
    val value: Double, // UV Index value
    val date: Long
)