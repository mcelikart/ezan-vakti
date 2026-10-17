package com.example.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WeatherResponse(
    val current: CurrentWeather?,
    val daily: DailyWeather?,
    val hourly: HourlyWeather?
)

@JsonClass(generateAdapter = true)
data class CurrentWeather(
    val temperature_2m: Double?,
    val apparent_temperature: Double?,
    val relative_humidity_2m: Int?,
    val wind_speed_10m: Double?,
    val weather_code: Int?
)

@JsonClass(generateAdapter = true)
data class DailyWeather(
    val time: List<String>?,
    val weather_code: List<Int>?,
    val temperature_2m_max: List<Double>?,
    val temperature_2m_min: List<Double>?,
    val uv_index_max: List<Double>?,
    val precipitation_probability_max: List<Int>?
)

@JsonClass(generateAdapter = true)
data class HourlyWeather(
    val time: List<String>?,
    val temperature_2m: List<Double>?,
    val weather_code: List<Int>?
)
