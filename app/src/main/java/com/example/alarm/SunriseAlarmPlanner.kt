package com.example.alarm

import android.content.Context
import com.example.data.PrayerTimeCache
import java.util.Calendar

/** Uses only date-specific prayer data already held on this device. */
object SunriseAlarmPlanner {
    private const val MINUTE_MILLIS = 60_000L
    private const val LOOK_AHEAD_DAYS = 32

    fun nextOccurrence(
        context: Context,
        alarm: SunriseAlarm,
        now: Long = System.currentTimeMillis()
    ): SunriseAlarmOccurrence? = nextOccurrence(alarm, readSunriseDays(context, now), now)

    fun allNext(
        context: Context,
        alarms: List<SunriseAlarm>,
        now: Long = System.currentTimeMillis()
    ): List<SunriseAlarmOccurrence> = plan(alarms, readSunriseDays(context, now), now)

    fun plan(alarms: List<SunriseAlarm>, days: List<SunriseDay>, now: Long): List<SunriseAlarmOccurrence> =
        alarms.distinctBy { it.id }.mapNotNull { nextOccurrence(it, days, now) }
            .sortedWith(compareBy<SunriseAlarmOccurrence> { it.triggerAtMillis }.thenBy { it.alarmId })

    fun nextOccurrence(alarm: SunriseAlarm, days: List<SunriseDay>, now: Long): SunriseAlarmOccurrence? {
        if (!alarm.enabled) return null
        return days.asSequence().filter { it.sunriseAtMillis > 0L }.map { day ->
            SunriseAlarmOccurrence(
                alarmId = alarm.id,
                sunriseAtMillis = day.sunriseAtMillis,
                triggerAtMillis = day.sunriseAtMillis - alarm.minutesBeforeSunrise * MINUTE_MILLIS,
                sourceDate = day.date
            )
        }.filter { it.triggerAtMillis > now }.minByOrNull { it.triggerAtMillis }
    }

    /** Validate queued regular alarms against current settings and location/date data. */
    fun isCurrentOccurrence(context: Context, alarm: SunriseAlarm, occurrence: SunriseAlarmOccurrence): Boolean {
        if (!alarm.enabled || occurrence.alarmId != alarm.id) return false
        if (occurrence.triggerAtMillis != occurrence.sunriseAtMillis - alarm.minutesBeforeSunrise * MINUTE_MILLIS) return false
        val day = Calendar.getInstance().apply { timeInMillis = occurrence.sunriseAtMillis }
        if (PrayerTimeCache.date(day.timeInMillis) != occurrence.sourceDate) return false
        return readSunrise(context, day)?.sunriseAtMillis == occurrence.sunriseAtMillis
    }

    fun readSunriseDays(context: Context, now: Long = System.currentTimeMillis()): List<SunriseDay> {
        val today = Calendar.getInstance().apply { timeInMillis = now }
        return (0 until LOOK_AHEAD_DAYS).mapNotNull { offset ->
            val day = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, offset) }
            readSunrise(context, day)
        }
    }

    private fun readSunrise(context: Context, day: Calendar): SunriseDay? {
        for (entry in PrayerTimeCache.read(context, day).split('|')) {
            val parts = entry.split(',')
            if (parts.size != 2 || parts[0] != "Güneş") continue
            val fields = parts[1].split(':')
            if (fields.size != 2) continue
            val hour = fields[0].toIntOrNull() ?: continue
            val minute = fields[1].toIntOrNull() ?: continue
            if (hour !in 0..23 || minute !in 0..59) continue
            val target = (day.clone() as Calendar).apply {
                isLenient = false
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            // Reject nonexistent DST wall-clock times instead of silently moving the alarm.
            val timestamp = runCatching { target.timeInMillis }.getOrNull() ?: continue
            return SunriseDay(PrayerTimeCache.date(day.timeInMillis), timestamp)
        }
        return null
    }
}
