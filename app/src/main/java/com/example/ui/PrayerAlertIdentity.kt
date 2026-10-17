package com.example.ui

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PrayerAlertIdentity {
    fun key(name: String, prayerTime: Long, warning: Boolean): String {
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(prayerTime))
        return "$date|$name|$warning"
    }

    fun isTimely(prayerTime: Long, warning: Boolean, now: Long): Boolean {
        val trigger = prayerTime - if (warning) 600_000L else 0L
        return now >= trigger && now - trigger < 5 * 60_000L
    }
}
