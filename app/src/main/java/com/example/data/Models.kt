package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AladhanResponse(
    val code: Int,
    val status: String,
    val data: TimingsData
)

@JsonClass(generateAdapter = true)
data class TimingsData(
    val timings: Timings,
    val date: DateInfo,
    val meta: MetaInfo
)

@JsonClass(generateAdapter = true)
data class Timings(
    val Fajr: String,
    val Sunrise: String,
    val Dhuhr: String,
    val Asr: String,
    val Sunset: String,
    val Maghrib: String,
    val Isha: String,
    val Imsak: String,
    val Midnight: String
)

@JsonClass(generateAdapter = true)
data class DateInfo(
    val readable: String,
    val timestamp: String,
    val hijri: HijriDate? = null
)

@JsonClass(generateAdapter = true)
data class HijriDate(
    val date: String,
    val format: String,
    val day: String,
    val weekday: HijriWeekday?,
    val month: HijriMonth?,
    val year: String,
    val holidays: List<String>? = emptyList()
)

@JsonClass(generateAdapter = true)
data class HijriWeekday(
    val en: String,
    val ar: String? = null
)

@JsonClass(generateAdapter = true)
data class HijriMonth(
    val number: Int,
    val en: String,
    val ar: String? = null
)

@JsonClass(generateAdapter = true)
data class MetaInfo(
    val timezone: String
)

@JsonClass(generateAdapter = true)
data class CalendarResponse(
    val code: Int,
    val status: String,
    val data: List<TimingsData>
)
