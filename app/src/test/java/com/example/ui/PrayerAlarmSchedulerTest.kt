package com.example.ui

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Instant
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PrayerAlarmSchedulerTest {
    private lateinit var context: Context
    private lateinit var manager: AlarmManager
    private lateinit var originalZone: TimeZone

    @Before fun setup() {
        originalZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        context = ApplicationProvider.getApplicationContext()
        manager = context.getSystemService(AlarmManager::class.java)
        listOf("alarm_prefs", "prayer_daily_times", "countdown_service_prefs", "prayer_alarm_scheduler")
            .forEach { context.getSharedPreferences(it, 0).edit().clear().commit() }
    }

    @After fun restoreZone() = TimeZone.setDefault(originalZone)

    private fun at(iso: String) = Instant.parse(iso).toEpochMilli()

    private fun cache(isoDate: String, prayers: String) {
        context.getSharedPreferences("prayer_daily_times", 0).edit()
            .putString(isoDate, prayers).commit()
    }

    private fun triggers() = shadowOf(manager).scheduledAlarms.map { it.triggerAtTime }.sorted()

    @Test fun changedLocationTimesReplaceOldAlarmsAndRejectTheirQueuedBroadcasts() {
        val now = at("2027-01-31T10:00:00Z")
        val oldTime = at("2027-02-01T12:30:00Z")
        val newTime = at("2027-02-01T12:45:00Z")
        cache("2027-02-01", "Öğle,12:30")
        PrayerAlarmScheduler.scheduleAllPrayerAlarms(context, now)
        assertTrue(PrayerAlarmScheduler.isCurrentSchedule(context, "Öğle", oldTime))

        cache("2027-02-01", "Öğle,12:45")
        PrayerAlarmScheduler.scheduleAllPrayerAlarms(context, now)
        assertEquals(listOf(newTime - 600_000L, newTime), triggers())
        assertFalse(PrayerAlarmScheduler.isCurrentSchedule(context, "Öğle", oldTime))
        assertTrue(PrayerAlarmScheduler.isCurrentSchedule(context, "Öğle", newTime))
    }

    @Test fun removingPrayerOrEntireCachedDayCancelsTheirPendingAlarms() {
        val now = at("2027-01-31T10:00:00Z")
        cache("2027-02-01", "Öğle,12:30|İkindi,15:30")
        PrayerAlarmScheduler.scheduleAllPrayerAlarms(context, now)
        assertEquals(4, triggers().size)

        cache("2027-02-01", "İkindi,15:30")
        PrayerAlarmScheduler.scheduleAllPrayerAlarms(context, now)
        assertEquals(listOf(at("2027-02-01T15:20:00Z"), at("2027-02-01T15:30:00Z")), triggers())
        assertFalse(PrayerAlarmScheduler.isCurrentSchedule(context, "Öğle", at("2027-02-01T12:30:00Z")))

        context.getSharedPreferences("prayer_daily_times", 0).edit().clear().commit()
        PrayerAlarmScheduler.scheduleAllPrayerAlarms(context, now)
        assertTrue(triggers().isEmpty())
    }

    @Test fun warningForPrayerAfterMidnightUsesTomorrowsDateAndCanFireToday() {
        val now = at("2027-01-31T23:54:00Z")
        val prayer = at("2027-02-01T00:05:00Z")
        cache("2027-01-31", "İmsak,05:00")
        cache("2027-02-01", "İmsak,00:05")
        PrayerAlarmScheduler.scheduleAllPrayerAlarms(context, now)
        assertEquals(listOf(at("2027-01-31T23:55:00Z"), prayer), triggers())
        assertTrue(PrayerAlarmScheduler.isCurrentSchedule(context, "İmsak", prayer))
        assertTrue(PrayerAlertIdentity.isTimely(prayer, true, now + 60_000L))
    }

    @Test fun dateRolloverDoesNotLeaveYesterdayOrMissedWarningSlotsAlive() {
        cache("2027-02-01", "İmsak,00:05|Öğle,12:30")
        cache("2027-02-02", "Öğle,12:31")
        PrayerAlarmScheduler.scheduleAllPrayerAlarms(context, at("2027-01-31T23:54:00Z"))
        assertEquals(4, triggers().size)

        PrayerAlarmScheduler.scheduleAllPrayerAlarms(context, at("2027-02-01T00:10:00Z"))
        assertEquals(listOf(at("2027-02-01T12:20:00Z"), at("2027-02-01T12:30:00Z"),
            at("2027-02-02T12:21:00Z"), at("2027-02-02T12:31:00Z")), triggers())
    }

    @Test fun timezoneChangeRebuildsLocalTimesAndRejectsOldTimestamp() {
        val now = at("2027-01-31T10:00:00Z")
        val oldTime = at("2027-02-01T12:30:00Z")
        cache("2027-02-01", "Öğle,12:30")
        PrayerAlarmScheduler.scheduleAllPrayerAlarms(context, now)
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Istanbul"))
        PrayerAlarmScheduler.scheduleAllPrayerAlarms(context, now)
        val localPrayer = at("2027-02-01T09:30:00Z")
        assertEquals(listOf(localPrayer - 600_000L, localPrayer), triggers())
        assertFalse(PrayerAlarmScheduler.isCurrentSchedule(context, "Öğle", oldTime))
        assertTrue(PrayerAlarmScheduler.isCurrentSchedule(context, "Öğle", localPrayer))
    }

    @Test fun migrationCancelsLegacyAlarmsEvenForDatesMissingFromCache() {
        val name = "Yatsı"
        val oldDay = 31
        for (warning in listOf(false, true)) {
            val code = (name.hashCode() * 100 + oldDay * 2 + if (warning) 2 else 1) and 0x7FFFFFFF
            val intent = Intent(context, PrayerNotificationReceiver::class.java).apply {
                action = if (warning) PrayerAlarmScheduler.ACTION_WARNING_PRAYER_ALARM
                    else PrayerAlarmScheduler.ACTION_EXACT_PRAYER_ALARM
            }
            val pending = PendingIntent.getBroadcast(context, code, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            manager.setExact(AlarmManager.RTC_WAKEUP, at("2027-01-31T22:00:00Z"), pending)
        }
        assertEquals(2, triggers().size)
        PrayerAlarmScheduler.scheduleAllPrayerAlarms(context, at("2027-01-31T10:00:00Z"))
        assertTrue(triggers().isEmpty())
    }

    @Test fun reschedulingAtPrayerTimePreservesTheExactAlarmAndItsPendingToken() {
        val prayer = at("2027-01-31T12:30:00Z")
        cache("2027-01-31", "Öğle,12:30")
        PrayerAlarmScheduler.scheduleAllPrayerAlarms(context, prayer - 1_000L)
        val exact = shadowOf(manager).scheduledAlarms.single().operation

        PrayerAlarmScheduler.scheduleAllPrayerAlarms(context, prayer)
        assertEquals(listOf(prayer), triggers())
        assertEquals(exact, shadowOf(manager).scheduledAlarms.single().operation)
        // A token invalidated with PendingIntent.cancel() would throw here.
        requireNotNull(exact).send()
    }

    @Test fun midnightReschedulingPreservesYesterdaysStillTimelyExactAlongsideTwoNewDays() {
        val yesterdayPrayer = at("2027-01-31T23:59:00Z")
        cache("2027-01-31", "Yatsı,23:59")
        cache("2027-02-01", "İmsak,05:00")
        cache("2027-02-02", "İmsak,05:01")
        PrayerAlarmScheduler.scheduleAllPrayerAlarms(context, yesterdayPrayer - 1_000L)
        val due = shadowOf(manager).scheduledAlarms.single { it.triggerAtTime == yesterdayPrayer }.operation

        PrayerAlarmScheduler.scheduleAllPrayerAlarms(context, at("2027-02-01T00:00:00Z"))
        assertEquals(listOf(yesterdayPrayer, at("2027-02-01T04:50:00Z"), at("2027-02-01T05:00:00Z"),
            at("2027-02-02T04:51:00Z"), at("2027-02-02T05:01:00Z")), triggers())
        assertEquals(due, shadowOf(manager).scheduledAlarms.single { it.triggerAtTime == yesterdayPrayer }.operation)
        requireNotNull(due).send()

        PrayerAlarmScheduler.scheduleAllPrayerAlarms(context, yesterdayPrayer + 300_000L)
        assertEquals(4, triggers().size)
        assertFalse(triggers().contains(yesterdayPrayer))
    }

    @Test fun firstMigrationAtPrayerTimeAlsoPreservesAValidLegacyDueAlarm() {
        val prayer = at("2027-01-31T12:30:00Z")
        cache("2027-01-31", "Öğle,12:30")
        val code = ("Öğle".hashCode() * 100 + 31 * 2 + 1) and 0x7FFFFFFF
        val intent = Intent(context, PrayerNotificationReceiver::class.java).apply {
            action = PrayerAlarmScheduler.ACTION_EXACT_PRAYER_ALARM
            putExtra(PrayerAlarmScheduler.EXTRA_PRAYER_NAME, "Öğle")
            putExtra(PrayerAlarmScheduler.EXTRA_PRAYER_TIME_MILLIS, prayer)
        }
        val due = PendingIntent.getBroadcast(context, code, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        manager.setExact(AlarmManager.RTC_WAKEUP, prayer, due)

        PrayerAlarmScheduler.scheduleAllPrayerAlarms(context, prayer)
        assertEquals(listOf(prayer), triggers())
        requireNotNull(due).send()
    }

    @Test fun invalidTimesAreNotNormalizedIntoUnrelatedDatesOrHours() {
        cache("2027-02-01", "İmsak,24:05|Öğle,12:61|İkindi,bad|Sabah,06:30|Akşam,18:45")
        PrayerAlarmScheduler.scheduleAllPrayerAlarms(context, at("2027-01-31T10:00:00Z"))
        assertEquals(listOf(at("2027-02-01T18:35:00Z"), at("2027-02-01T18:45:00Z")), triggers())
        assertFalse(PrayerAlarmScheduler.isCurrentSchedule(context, "İmsak", at("2027-02-02T00:05:00Z")))
    }

    @Test fun restartDuringWarningWindowDoesNotScheduleAMissedWarningImmediately() {
        cache("2027-01-31", "Öğle,12:30")
        PrayerAlarmScheduler.scheduleAllPrayerAlarms(context, at("2027-01-31T12:22:00Z"))
        assertEquals(listOf(at("2027-01-31T12:30:00Z")), triggers())
    }
}
