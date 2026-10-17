package com.example.ui

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.util.Calendar

object PrayerAlarmScheduler {

    val PRAYER_NAMES = listOf("İmsak", "Güneş", "Öğle", "İkindi", "Akşam", "Yatsı")

    const val ACTION_EXACT_PRAYER_ALARM = "com.example.ACTION_EXACT_PRAYER_ALARM"
    const val ACTION_WARNING_PRAYER_ALARM = "com.example.ACTION_WARNING_PRAYER_ALARM"

    const val EXTRA_PRAYER_NAME = "EXTRA_PRAYER_NAME"
    const val EXTRA_PRAYER_TIME_MILLIS = "EXTRA_PRAYER_TIME_MILLIS"
    const val EXTRA_SOUND_ID = "EXTRA_SOUND_ID"
    const val EXTRA_IS_WARNING = "EXTRA_IS_WARNING"

    fun scheduleAllPrayerAlarms(context: Context) {
        val alarmPrefs = context.getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val now = System.currentTimeMillis()

        // Schedule for today and tomorrow
        for (dayOffset in 0..1) {
            val dayCal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, dayOffset)
            }
            val dayOfYear = dayCal.get(Calendar.DAY_OF_YEAR)

            val parsedList = com.example.data.PrayerTimeCache.read(context, dayCal).split("|").mapNotNull {
                val parts = it.split(",")
                if (parts.size == 2 && parts[0] in PRAYER_NAMES) parts[0] to parts[1] else null
            }

            for ((prayerName, timeStr) in parsedList) {
                val timeParts = timeStr.split(":")
                if (timeParts.size != 2) continue

                val hour = timeParts[0].toIntOrNull() ?: continue
                val minute = timeParts[1].toIntOrNull() ?: continue
                if (hour !in 0..23 || minute !in 0..59) continue

                val targetCal = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, dayOffset)
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                val exactTimeMillis = targetCal.timeInMillis

                // 1. Exact Prayer Alarm
                val exactEnabled = alarmPrefs.getBoolean("key_exact_enabled_$prayerName", true)
                val exactSoundId = alarmPrefs.getInt("key_exact_sound_$prayerName", 2)

                run {
                    val requestCode = (prayerName.hashCode() * 100 + dayOfYear * 2 + 1) and 0x7FFFFFFF
                    val intent = Intent(context, PrayerNotificationReceiver::class.java).apply {
                        action = ACTION_EXACT_PRAYER_ALARM
                        putExtra(EXTRA_PRAYER_NAME, prayerName)
                        putExtra(EXTRA_PRAYER_TIME_MILLIS, exactTimeMillis)
                        putExtra(EXTRA_SOUND_ID, exactSoundId)
                        putExtra(EXTRA_IS_WARNING, false)
                    }

                    val pendingIntent = PendingIntent.getBroadcast(
                        context,
                        requestCode,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    try {
                        if (!exactEnabled) {
                            alarmManager.cancel(pendingIntent)
                            return@run
                        }
                        if (exactTimeMillis <= now) return@run
                        scheduleExactOrAlarmClock(alarmManager, context, exactTimeMillis, pendingIntent)
                        Log.d("PrayerAlarmScheduler", "Scheduled EXACT alarm for $prayerName at $exactTimeMillis")
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                // 2. 10-Minute Warning Alarm
                val warningEnabled = alarmPrefs.getBoolean("key_10min_enabled_$prayerName", true)
                val warningSoundId = alarmPrefs.getInt("key_10min_sound_$prayerName", 5)
                val warningTimeMillis = exactTimeMillis - (10 * 60 * 1000)

                run {
                    val requestCode = (prayerName.hashCode() * 100 + dayOfYear * 2 + 2) and 0x7FFFFFFF
                    val intent = Intent(context, PrayerNotificationReceiver::class.java).apply {
                        action = ACTION_WARNING_PRAYER_ALARM
                        putExtra(EXTRA_PRAYER_NAME, prayerName)
                        putExtra(EXTRA_PRAYER_TIME_MILLIS, exactTimeMillis)
                        putExtra(EXTRA_SOUND_ID, warningSoundId)
                        putExtra(EXTRA_IS_WARNING, true)
                    }

                    val pendingIntent = PendingIntent.getBroadcast(
                        context,
                        requestCode,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    try {
                        if (!warningEnabled) {
                            alarmManager.cancel(pendingIntent)
                            return@run
                        }
                        if (warningTimeMillis <= now) return@run
                        scheduleExactOrAlarmClock(alarmManager, context, warningTimeMillis, pendingIntent)
                        Log.d("PrayerAlarmScheduler", "Scheduled 10MIN alarm for $prayerName at $warningTimeMillis")
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    private fun scheduleExactOrAlarmClock(
        alarmManager: AlarmManager,
        context: Context,
        triggerAtMillis: Long,
        operation: PendingIntent
    ) {
        val showIntent = Intent(context, com.example.MainActivity::class.java)
        val showPendingIntent = PendingIntent.getActivity(
            context,
            0,
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()) {
            val clockInfo = AlarmManager.AlarmClockInfo(triggerAtMillis, showPendingIntent)
            alarmManager.setAlarmClock(clockInfo, operation)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, operation)
        }
    }
}
