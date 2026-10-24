package com.example.alarm

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import java.time.Instant
import java.time.LocalDate
import java.util.TimeZone
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SunriseAlarmDataRefreshTest {
    private lateinit var context: Context
    private lateinit var originalZone: TimeZone

    @Before fun setup() {
        originalZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        context = ApplicationProvider.getApplicationContext()
        listOf("prayer_daily_times", "countdown_service_prefs").forEach {
            context.getSharedPreferences(it, Context.MODE_PRIVATE).edit().clear().commit()
        }
    }

    @After fun restoreZone() = TimeZone.setDefault(originalZone)

    private fun now(date: String): Long {
        // missingMonths accepts its wall clock explicitly; no global Android clock is involved.
        return Instant.parse("${date}T05:00:00Z").toEpochMilli()
    }

    private fun cache(date: String, timings: String = "İmsak,05:20|Güneş,07:15|Öğle,12:30") {
        assertTrue(context.getSharedPreferences("prayer_daily_times", Context.MODE_PRIVATE).edit()
            .putString(date, timings).commit())
    }

    private fun cacheRange(first: String, offsets: IntRange) {
        val start = LocalDate.parse(first)
        offsets.forEach { cache(start.plusDays(it.toLong()).toString()) }
    }

    @Test fun missingDaysWithinOneMonthRequestThatMonthOnlyOnce() {
        assertEquals(listOf(2027 to 3), SunriseAlarmDataRefresh.missingMonths(context, now("2027-03-10")))
    }

    @Test fun eightDayWindowRequestsBothMonthsInDateOrderAtMonthBoundary() {
        assertEquals(listOf(2027 to 1, 2027 to 2), SunriseAlarmDataRefresh.missingMonths(context, now("2027-01-27")))
    }

    @Test fun yearBoundaryKeepsTheYearPairedWithItsActualMonth() {
        assertEquals(listOf(2027 to 12, 2028 to 1), SunriseAlarmDataRefresh.missingMonths(context, now("2027-12-29")))
    }

    @Test fun completeDatedCacheForTodayThroughSevenDaysNeedsNoRefresh() {
        cacheRange("2027-01-27", 0..7)
        assertTrue(SunriseAlarmDataRefresh.missingMonths(context, now("2027-01-27")).isEmpty())
    }

    @Test fun seventhDayIsIncludedButAlreadyCompleteCurrentMonthIsNotRefetched() {
        cacheRange("2027-01-25", 0..6)
        assertEquals(listOf(2027 to 2), SunriseAlarmDataRefresh.missingMonths(context, now("2027-01-25")))
    }

    @Test fun malformedOrMissingSunriseDoesNotMakeADatedCacheEntryComplete() {
        val timestamp = now("2027-01-27")
        cacheRange("2027-01-27", 0..7)
        listOf("Güneş,25:00", "Güneş,07:60", "Güneş,bozuk", "Güneş,", "İmsak,05:20", "").forEach { invalid ->
            cache("2027-02-01", invalid)
            assertEquals("Invalid sunrise: $invalid", listOf(2027 to 2), SunriseAlarmDataRefresh.missingMonths(context, timestamp))
        }
        cache("2027-02-01")
        assertTrue(SunriseAlarmDataRefresh.missingMonths(context, timestamp).isEmpty())
    }

    @Test fun correctlyDatedCountdownFallbackCountsForTodayOnly() {
        val timestamp = now("2027-01-27")
        cacheRange("2027-01-27", 1..7)
        context.getSharedPreferences("countdown_service_prefs", Context.MODE_PRIVATE).edit()
            .putString("ALL_PRAYERS_DATE", "2027-01-27")
            .putString("ALL_PRAYERS", "Güneş,07:15").commit()
        assertTrue(SunriseAlarmDataRefresh.missingMonths(context, timestamp).isEmpty())
        context.getSharedPreferences("countdown_service_prefs", Context.MODE_PRIVATE).edit()
            .putString("ALL_PRAYERS_DATE", "2027-01-26").commit()
        assertEquals(listOf(2027 to 1), SunriseAlarmDataRefresh.missingMonths(context, timestamp))
    }
}
