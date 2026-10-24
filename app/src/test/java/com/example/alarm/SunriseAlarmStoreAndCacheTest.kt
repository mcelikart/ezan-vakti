package com.example.alarm

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SunriseAlarmStoreAndCacheTest {
    private lateinit var context: Context
    private lateinit var originalZone: TimeZone
    private fun at(value: String) = Instant.parse(value).toEpochMilli()

    @Before fun setup() {
        originalZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        context = ApplicationProvider.getApplicationContext()
        listOf(SunriseAlarmStore.PREFERENCES, "prayer_daily_times", "countdown_service_prefs").forEach {
            context.getSharedPreferences(it, Context.MODE_PRIVATE).edit().clear().commit()
        }
    }

    @After fun restoreZone() = TimeZone.setDefault(originalZone)

    private fun cache(date: String, times: String) {
        context.getSharedPreferences("prayer_daily_times", Context.MODE_PRIVATE).edit().putString(date, times).commit()
    }

    @Test fun multipleAlarmsPersistAcrossInstancesCanBeEditedDisabledAndDeleted() {
        val first = SunriseAlarm(id = "first")
        val second = SunriseAlarm(id = "second", minutesBeforeSunrise = 45)
        SunriseAlarmStore(context).save(first)
        SunriseAlarmStore(context).save(second)
        val restored = SunriseAlarmStore(context)
        assertEquals(listOf(first, second), restored.list())
        restored.save(first.copy(enabled = false, minutesBeforeSunrise = 15))
        assertEquals(2, restored.list().size)
        assertFalse(requireNotNull(restored.find("first")).enabled)
        restored.delete("first")
        assertNull(SunriseAlarmStore(context).find("first"))
        assertEquals(listOf(second), restored.list())
    }

    @Test fun corruptAlarmEntriesCannotInvalidateOtherSavedAlarms() {
        context.getSharedPreferences(SunriseAlarmStore.PREFERENCES, 0).edit().putString(SunriseAlarmStore.ALARMS,
            """[{"id":"good","minutesBeforeSunrise":30},{"id":"bad","minutesBeforeSunrise":-1},{"minutesBeforeSunrise":20},{"id":"good","minutesBeforeSunrise":45}]""").commit()
        assertEquals(listOf(SunriseAlarm(id = "good")), SunriseAlarmStore(context).list())
    }

    @Test fun savedDefaultSunriseLabelMigratesToThePrayerAlarmName() {
        context.getSharedPreferences(SunriseAlarmStore.PREFERENCES, 0).edit()
            .putString(SunriseAlarmStore.ALARMS,
                """[{"id":"legacy","minutesBeforeSunrise":30,"enabled":true,"label":"${SunriseAlarm.LEGACY_DEFAULT_LABEL}"}]""")
            .commit()

        assertEquals(SunriseAlarm.DEFAULT_LABEL, SunriseAlarmStore(context).list().single().label)
    }

    @Test fun noAlarmIsInventedWhenOnlyUndatedOrYesterdaySunriseExists() {
        cache("2027-01-30", "Güneş,07:10")
        context.getSharedPreferences("countdown_service_prefs", 0).edit().putString("ALL_PRAYERS", "Güneş,07:15").commit()
        val now = at("2027-01-31T00:00:00Z")
        assertTrue(SunriseAlarmPlanner.readSunriseDays(context, now).isEmpty())
        assertNull(SunriseAlarmPlanner.nextOccurrence(context, SunriseAlarm(id = "a"), now))
    }

    @Test fun nextDatedSunriseIsUsedWithNoExtrapolationAcrossMissingDays() {
        cache("2027-01-31", "Güneş,07:15")
        cache("2027-02-02", "Güneş,07:17")
        val next = SunriseAlarmPlanner.nextOccurrence(context, SunriseAlarm(id = "a"), at("2027-01-31T08:00:00Z"))
        assertEquals("2027-02-02", next?.sourceDate)
        assertEquals(at("2027-02-02T06:47:00Z"), next?.triggerAtMillis)
    }

    @Test fun queuedOccurrenceIsRejectedAfterDisableOffsetLocationOrTimezoneChange() {
        cache("2027-01-31", "Güneş,07:15")
        val alarm = SunriseAlarm(id = "a")
        val occurrence = requireNotNull(SunriseAlarmPlanner.nextOccurrence(context, alarm, at("2027-01-31T00:00:00Z")))
        assertTrue(SunriseAlarmPlanner.isCurrentOccurrence(context, alarm, occurrence))
        assertFalse(SunriseAlarmPlanner.isCurrentOccurrence(context, alarm.copy(enabled = false), occurrence))
        assertFalse(SunriseAlarmPlanner.isCurrentOccurrence(context, alarm.copy(minutesBeforeSunrise = 45), occurrence))
        cache("2027-01-31", "Güneş,07:16")
        assertFalse(SunriseAlarmPlanner.isCurrentOccurrence(context, alarm, occurrence))
        cache("2027-01-31", "Güneş,07:15")
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Istanbul"))
        assertFalse(SunriseAlarmPlanner.isCurrentOccurrence(context, alarm, occurrence))
    }

    @Test fun invalidAndNonexistentDstWallClockTimesAreIgnored() {
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Berlin"))
        cache("2027-03-28", "Güneş,02:30")
        cache("2027-03-29", "Güneş,25:00")
        cache("2027-03-30", "Güneş,07:15")
        val days = SunriseAlarmPlanner.readSunriseDays(context, at("2027-03-28T00:00:00Z"))
        assertEquals(listOf("2027-03-30"), days.map { it.date })
        assertEquals(at("2027-03-30T05:15:00Z"), days.single().sunriseAtMillis)
    }

    @Test fun datedCountdownCacheFallbackIsAccepted() {
        context.getSharedPreferences("countdown_service_prefs", 0).edit()
            .putString("ALL_PRAYERS_DATE", "2027-01-31").putString("ALL_PRAYERS", "Güneş,07:15").commit()
        val next = SunriseAlarmPlanner.nextOccurrence(context, SunriseAlarm(id = "a"), at("2027-01-31T00:00:00Z"))
        assertEquals(at("2027-01-31T06:45:00Z"), next?.triggerAtMillis)
    }

    @Test fun monthYearAndDstRolloverScanCalendarDatesRatherThanTwentyFourHourIntervals() {
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Berlin"))
        cache("2027-10-30", "Güneş,07:59")
        cache("2027-10-31", "Güneş,07:01")
        cache("2027-11-01", "Güneş,07:02")
        val days = SunriseAlarmPlanner.readSunriseDays(context, at("2027-10-30T12:00:00Z"))
        assertEquals(listOf("2027-10-30", "2027-10-31", "2027-11-01"), days.map { it.date })
        assertEquals(at("2027-10-31T06:01:00Z"), days[1].sunriseAtMillis)
        cache("2027-12-31", "Güneş,08:00")
        cache("2028-01-01", "Güneş,08:01")
        val next = SunriseAlarmPlanner.nextOccurrence(context, SunriseAlarm(id = "a"), at("2027-12-31T12:00:00Z"))
        assertEquals("2028-01-01", next?.sourceDate)
    }
}
