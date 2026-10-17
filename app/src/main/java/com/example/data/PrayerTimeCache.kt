package com.example.data

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object PrayerTimeCache {
    fun date(time: Long = System.currentTimeMillis()) = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(time))

    fun save(context: Context, data: TimingsData) {
        val timestamp = data.date.timestamp.toLongOrNull()?.times(1000) ?: return
        val t = data.timings
        val sunrise = t.Sunrise.take(5).split(":")
        val hour = sunrise.getOrNull(0)?.toIntOrNull()
        val minute = sunrise.getOrNull(1)?.toIntOrNull()
        val sabah = if (hour != null && minute != null) String.format(Locale.US, "%02d:%02d", (hour + 23) % 24, minute) else t.Sunrise.take(5)
        val prayers = listOf("İmsak" to t.Fajr, "Sabah" to sabah, "Güneş" to t.Sunrise, "Öğle" to t.Dhuhr,
            "İkindi" to t.Asr, "Akşam" to t.Maghrib, "Yatsı" to t.Isha)
            .joinToString("|") { "${it.first},${it.second.take(5)}" }
        context.getSharedPreferences("prayer_daily_times", Context.MODE_PRIVATE).edit()
            .putString(date(timestamp), prayers).apply()
    }

    fun read(context: Context, day: Calendar = Calendar.getInstance()): String {
        val key = date(day.timeInMillis)
        val cached = context.getSharedPreferences("prayer_daily_times", Context.MODE_PRIVATE).getString(key, null)
        if (cached != null) return cached
        val prefs = context.getSharedPreferences("countdown_service_prefs", Context.MODE_PRIVATE)
        return if (prefs.getString("ALL_PRAYERS_DATE", "") == key) prefs.getString("ALL_PRAYERS", "") ?: "" else ""
    }
}
