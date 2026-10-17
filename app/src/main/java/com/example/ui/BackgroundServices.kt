package com.example.ui

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat

object BackgroundServices {
    private const val WATCHDOG_DELAY = 15 * 60_000L
    fun startCountdown(context: Context) {
        try {
            ContextCompat.startForegroundService(context, Intent(context, CountdownService::class.java))
        } catch (error: IllegalStateException) {
            Log.w("BackgroundServices", "Background start deferred until next allowed start", error)
        } catch (error: SecurityException) {
            Log.w("BackgroundServices", "Foreground permission unavailable", error)
        }
    }

    fun scheduleRecovery(context: Context) {
        scheduleRecovery(context, 30_000L)
    }

    fun armWatchdog(context: Context) {
        scheduleRecovery(context, WATCHDOG_DELAY)
    }

    private fun scheduleRecovery(context: Context, delay: Long) {
        val alarm = context.getSystemService(AlarmManager::class.java)
        val pending = PendingIntent.getBroadcast(context, 9999,
            Intent(context, PrayerNotificationReceiver::class.java).setAction(PrayerNotificationReceiver.ACTION_RESTART),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val at = System.currentTimeMillis() + delay
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarm.canScheduleExactAlarms()) {
                alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
            } else {
                alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
            }
        } catch (error: SecurityException) {
            alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
        }
    }
}
