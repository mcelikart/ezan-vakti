package com.example.ui

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import com.example.data.PrayerTimeCache
import java.util.Calendar
import java.util.TimeZone

object PrayerAlarmScheduler {

    val PRAYER_NAMES = listOf("İmsak", "Güneş", "Öğle", "İkindi", "Akşam", "Yatsı")

    const val ACTION_EXACT_PRAYER_ALARM = "com.example.ACTION_EXACT_PRAYER_ALARM"
    const val ACTION_WARNING_PRAYER_ALARM = "com.example.ACTION_WARNING_PRAYER_ALARM"

    const val EXTRA_PRAYER_NAME = "EXTRA_PRAYER_NAME"
    const val EXTRA_PRAYER_TIME_MILLIS = "EXTRA_PRAYER_TIME_MILLIS"
    const val EXTRA_SOUND_ID = "EXTRA_SOUND_ID"
    const val EXTRA_IS_WARNING = "EXTRA_IS_WARNING"

    private const val ALARM_SLOT_BASE = 41_000
    private const val DAY_BUCKETS = 3
    private const val MIGRATION_PREFS = "prayer_alarm_scheduler"
    private const val LEGACY_CANCELLED = "legacy_nearby_alarms_cancelled_v4"

    private data class PlannedAlarm(val name: String, val prayerTime: Long, val warning: Boolean) {
        val triggerTime: Long get() = prayerTime - if (warning) PrayerAlertIdentity.WARNING_LEAD_MILLIS else 0L
    }

    @Synchronized
    fun scheduleAllPrayerAlarms(context: Context, now: Long = System.currentTimeMillis()) {
        val alarmPrefs = context.getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val registry = context.getSharedPreferences(MIGRATION_PREFS, Context.MODE_PRIVATE)
        val today = Calendar.getInstance().apply { timeInMillis = now }
        val planned = linkedMapOf<Int, PlannedAlarm>()
        for (dayOffset in 0..1) {
            val day = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, dayOffset) }
            for ((name, prayerTime) in prayerTimes(context, day)) {
                for (warning in listOf(false, true)) {
                    val prefix = if (warning) "key_10min" else "key_exact"
                    if (!alarmPrefs.getBoolean("${prefix}_enabled_$name", true)) continue
                    val slot = alarmSlot(day, PRAYER_NAMES.indexOf(name), warning)
                    planned[slot] = PlannedAlarm(name, prayerTime, warning)
                }
            }
        }

        cancelNearbyLegacyAlarms(context, alarmManager, today, now)
        val registryEdit = registry.edit()
        for (bucket in 0 until DAY_BUCKETS) {
            for ((prayerIndex, name) in PRAYER_NAMES.withIndex()) {
                for (warning in listOf(false, true)) {
                    val slot = alarmSlot(bucket, prayerIndex, warning)
                    val previousTime = registry.getLong("scheduled_time_$slot", 0L)
                    if (previousTime <= 0L) continue
                    val prefix = if (warning) "key_10min" else "key_exact"
                    val stillTimely = PrayerAlertIdentity.isTimely(previousTime, warning, now)
                    val unchangedFuture = planned[slot]?.let {
                        it.prayerTime == previousTime && it.triggerTime > now
                    } == true
                    // Keep a due alarm's PendingIntent alive while AlarmManager dispatches it.
                    // A third date bucket also protects yesterday's last prayer after midnight.
                    val validDue = stillTimely && alarmPrefs.getBoolean("${prefix}_enabled_$name", true) &&
                        isCurrentSchedule(context, name, previousTime)
                    if (!unchangedFuture && !validDue) {
                        cancelIfPresent(context, alarmManager, slot, alarmIntent(context, warning, slot))
                        registryEdit.remove("scheduled_time_$slot")
                    }
                }
            }
        }

        for ((slot, alarm) in planned) {
            // Rescheduling at T+0 must neither cancel nor recreate an alarm already being sent.
            // If no alarm was armed, a service restart must not invent a missed alert either.
            if (alarm.triggerTime <= now) continue
            val prefix = if (alarm.warning) "key_10min" else "key_exact"
            val intent = alarmIntent(context, alarm.warning, slot).apply {
                putExtra(EXTRA_PRAYER_NAME, alarm.name)
                putExtra(EXTRA_PRAYER_TIME_MILLIS, alarm.prayerTime)
                putExtra(EXTRA_SOUND_ID, alarmPrefs.getInt("${prefix}_sound_${alarm.name}", if (alarm.warning) 5 else 2))
                putExtra(EXTRA_IS_WARNING, alarm.warning)
            }
            val pending = PendingIntent.getBroadcast(context, slot, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            try {
                scheduleExactOrAlarmClock(alarmManager, context, alarm.triggerTime, pending)
                registryEdit.putLong("scheduled_time_$slot", alarm.prayerTime)
            } catch (error: Exception) {
                Log.w("PrayerAlarmScheduler", "Could not schedule ${alarm.name} prayer alarm", error)
            }
        }
        registryEdit.apply()
    }

    /** A queued broadcast must still match the current date's cached prayer time. */
    fun isCurrentSchedule(context: Context, name: String, prayerTimeMillis: Long): Boolean {
        if (name !in PRAYER_NAMES || prayerTimeMillis <= 0L) return false
        val day = Calendar.getInstance().apply { timeInMillis = prayerTimeMillis }
        return prayerTimes(context, day)[name] == prayerTimeMillis
    }

    private fun prayerTimes(context: Context, day: Calendar): Map<String, Long> {
        val result = linkedMapOf<String, Long>()
        for (entry in PrayerTimeCache.read(context, day).split("|")) {
            val parts = entry.split(",")
            if (parts.size != 2 || parts[0] !in PRAYER_NAMES) continue
            val time = parts[1].split(":")
            if (time.size != 2) continue
            val hour = time[0].toIntOrNull() ?: continue
            val minute = time[1].toIntOrNull() ?: continue
            if (hour !in 0..23 || minute !in 0..59) continue
            val target = (day.clone() as Calendar).apply {
                isLenient = false
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            // Invalid local times at a daylight-saving transition must not roll over.
            val timestamp = runCatching { target.timeInMillis }.getOrNull() ?: continue
            result[parts[0]] = timestamp
        }
        return result
    }

    private fun alarmSlot(day: Calendar, prayerIndex: Int, warning: Boolean): Int {
        // Convert the local date fields to a UTC day number; elapsed local-day milliseconds
        // are unsuitable here because a timezone or daylight-saving change can alter them.
        val utcDate = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(day.get(Calendar.YEAR), day.get(Calendar.MONTH), day.get(Calendar.DAY_OF_MONTH))
        }
        val bucket = Math.floorMod(utcDate.timeInMillis / 86_400_000L, DAY_BUCKETS.toLong()).toInt()
        return alarmSlot(bucket, prayerIndex, warning)
    }

    private fun alarmSlot(bucket: Int, prayerIndex: Int, warning: Boolean) =
        ALARM_SLOT_BASE + bucket * PRAYER_NAMES.size * 2 + prayerIndex * 2 + if (warning) 1 else 0

    private fun alarmIntent(context: Context, warning: Boolean, slot: Int) =
        Intent(context, PrayerNotificationReceiver::class.java).apply {
            action = if (warning) ACTION_WARNING_PRAYER_ALARM else ACTION_EXACT_PRAYER_ALARM
            data = Uri.parse("ezanvakti://prayer-alarm/v4/$slot")
        }

    private fun cancelIfPresent(context: Context, alarmManager: AlarmManager, code: Int, intent: Intent) {
        val pending = PendingIntent.getBroadcast(context, code, intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE) ?: return
        alarmManager.cancel(pending)
        // Do not invalidate the token: AlarmManager may already have queued its broadcast.
        // The receiver independently rejects broadcasts for removed or changed prayer times.
    }

    private fun cancelNearbyLegacyAlarms(context: Context, alarmManager: AlarmManager, today: Calendar, now: Long) {
        val prefs = context.getSharedPreferences(MIGRATION_PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean(LEGACY_CANCELLED, false)) return
        val alarmPrefs = context.getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)
        // Previous releases armed only today and tomorrow. Probe nearby calendar dates,
        // keeping startup bounded; receiver validation makes any distant old alarm harmless.
        for (offset in -1..1) {
            val day = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, offset) }
            val times = prayerTimes(context, day)
            for (name in PRAYER_NAMES) {
                for (warning in listOf(false, true)) {
                    val prefix = if (warning) "key_10min" else "key_exact"
                    val currentTime = times[name]
                    if (currentTime != null && alarmPrefs.getBoolean("${prefix}_enabled_$name", true) &&
                        PrayerAlertIdentity.isTimely(currentTime, warning, now)) continue
                    val code = (name.hashCode() * 100 + day.get(Calendar.DAY_OF_YEAR) * 2 + if (warning) 2 else 1) and 0x7FFFFFFF
                    val intent = Intent(context, PrayerNotificationReceiver::class.java).apply {
                        action = if (warning) ACTION_WARNING_PRAYER_ALARM else ACTION_EXACT_PRAYER_ALARM
                    }
                    cancelIfPresent(context, alarmManager, code, intent)
                }
            }
        }
        prefs.edit().putBoolean(LEGACY_CANCELLED, true).apply()
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
