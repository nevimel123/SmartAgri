package com.example.smartagri.model

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

data class GeoapifySuggestion(
    val features: List<Feature>
)

data class Feature(
    val properties: Properties
)

data class Properties(
    val formatted: String,
    val lat: Double,
    val lon: Double
)

interface GeoapifyApi {
    @GET("v1/geocode/autocomplete")
    fun getAddressSuggestions(
        @Query("text") query: String,
        @Query("apiKey") apiKey: String,
        @Query("limit") limit: Int = 5
    ): Call<GeoapifySuggestion>
}
