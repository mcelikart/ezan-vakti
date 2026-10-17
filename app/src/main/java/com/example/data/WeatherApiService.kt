package com.example.data

import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApiService {
    @GET("forecast")
    suspend fun getCurrentWeather(
        @Query("latitude") lat: Double,
        @Query("longitude") lng: Double,
        @Query("current") current: String = "temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,wind_speed_10m",
        @Query("daily") daily: String = "weather_code,temperature_2m_max,temperature_2m_min,uv_index_max,precipitation_probability_max",
        @Query("hourly") hourly: String = "temperature_2m,weather_code",
        @Query("timezone") timezone: String = "auto"
    ): WeatherResponse
}
