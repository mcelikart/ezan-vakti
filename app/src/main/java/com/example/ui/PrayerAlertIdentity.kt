package com.example.ui

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PrayerAlertIdentity {
    const val WARNING_LEAD_MILLIS = 10 * 60_000L
    const val DELIVERY_WINDOW_MILLIS = 5 * 60_000L

    fun key(name: String, prayerTime: Long, warning: Boolean): String {
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(prayerTime))
        return "$date|$name|$warning"
    }

    fun isTimely(prayerTime: Long, warning: Boolean, now: Long): Boolean {
        if (prayerTime <= 0L) return false
        val trigger = prayerTime - if (warning) WARNING_LEAD_MILLIS else 0L
        val delay = now - trigger
        return now >= trigger && delay >= 0L && delay < DELIVERY_WINDOW_MILLIS
    }
}
