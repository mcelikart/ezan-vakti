package com.example.ui

import android.content.Context
import com.example.data.Timings

object NotificationHelper {
    @Suppress("UNUSED_PARAMETER")
    fun scheduleNotificationsForToday(context: Context, timings: Timings) {
        // The repository has saved the date-specific timings before this call.
        // Use the same alarm owner as the background service, including on refresh.
        PrayerAlarmScheduler.scheduleAllPrayerAlarms(context)
    }
}
