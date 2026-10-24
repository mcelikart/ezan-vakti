package com.example.alarm

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class SunriseAlarmPlannerTest {
    private fun at(value: String) = Instant.parse(value).toEpochMilli()
    private val today = SunriseDay("2027-01-31", at("2027-01-31T07:15:00Z"))
    private val tomorrow = SunriseDay("2027-02-01", at("2027-02-01T07:16:00Z"))

    @Test fun defaultAlarmTracksThirtyMinutesBeforeDatedSunrise() {
        val alarm = SunriseAlarm(id = "morning")
        val occurrence = SunriseAlarmPlanner.nextOccurrence(alarm, listOf(today), at("2027-01-31T05:00:00Z"))
        assertEquals(30, alarm.minutesBeforeSunrise)
        assertEquals(at("2027-01-31T06:45:00Z"), occurrence?.triggerAtMillis)
        assertEquals(today.sunriseAtMillis, occurrence?.sunriseAtMillis)
        assertEquals("2027-01-31", occurrence?.sourceDate)
    }

    @Test fun passedOrExactlyDueTriggerNeverCreatesAnImmediateCatchupAlarm() {
        val now = at("2027-01-31T06:45:00Z")
        for (time in listOf(now, now + 1, now + 60_000)) {
            val next = SunriseAlarmPlanner.nextOccurrence(SunriseAlarm(id = "a"), listOf(today, tomorrow), time)
            assertEquals(at("2027-02-01T06:46:00Z"), next?.triggerAtMillis)
        }
    }

    @Test fun disabledAlarmsAreOmittedAndSeparateIdsCanRingAtTheSameTime() {
        val alarms = listOf(SunriseAlarm(id = "a"), SunriseAlarm(id = "b"), SunriseAlarm(id = "off", enabled = false))
        val plan = SunriseAlarmPlanner.plan(alarms, listOf(today), at("2027-01-31T00:00:00Z"))
        assertEquals(listOf("a", "b"), plan.map { it.alarmId })
        assertEquals(1, plan.map { it.triggerAtMillis }.distinct().size)
    }

    @Test fun offsetCanCrossMidnightWithoutChangingTheSourceSunriseDate() {
        val day = SunriseDay("2027-02-01", at("2027-02-01T00:15:00Z"))
        val next = SunriseAlarmPlanner.nextOccurrence(SunriseAlarm(id = "a", minutesBeforeSunrise = 60),
            listOf(day), at("2027-01-31T23:00:00Z"))
        assertEquals(at("2027-01-31T23:15:00Z"), next?.triggerAtMillis)
        assertEquals("2027-02-01", next?.sourceDate)
    }

    @Test fun zeroOffsetRingsAtSunriseAndMaximumOffsetUsesThreeHours() {
        val now = at("2027-01-31T00:00:00Z")
        val atSunrise = SunriseAlarmPlanner.nextOccurrence(SunriseAlarm(id = "a", minutesBeforeSunrise = 0), listOf(today), now)
        val earliest = SunriseAlarmPlanner.nextOccurrence(SunriseAlarm(id = "b", minutesBeforeSunrise = 180), listOf(today), now)
        assertEquals(today.sunriseAtMillis, atSunrise?.triggerAtMillis)
        assertEquals(at("2027-01-31T04:15:00Z"), earliest?.triggerAtMillis)
    }

    @Test fun missingFutureDateIsNotFilledUsingTodaysSunrise() {
        assertNull(SunriseAlarmPlanner.nextOccurrence(SunriseAlarm(id = "a"), listOf(today), at("2027-01-31T08:00:00Z")))
        assertTrue(SunriseAlarmPlanner.plan(listOf(SunriseAlarm(id = "a")), emptyList(), 1L).isEmpty())
    }

    @Test fun multipleOffsetsSortByTriggerAndDuplicateAlarmIdsDoNotDuplicateDeliveries() {
        val alarms = listOf(SunriseAlarm(id = "last", minutesBeforeSunrise = 10),
            SunriseAlarm(id = "first", minutesBeforeSunrise = 50), SunriseAlarm(id = "first", minutesBeforeSunrise = 60))
        val plan = SunriseAlarmPlanner.plan(alarms, listOf(tomorrow, today), at("2027-01-31T00:00:00Z"))
        assertEquals(listOf("first", "last"), plan.map { it.alarmId })
        assertEquals(at("2027-01-31T06:25:00Z"), plan.first().triggerAtMillis)
    }

    @Test fun unsupportedOffsetsAreRejected() {
        for (offset in listOf(-1, 181, Int.MAX_VALUE)) {
            assertTrue(runCatching { SunriseAlarm(minutesBeforeSunrise = offset) }.isFailure)
        }
    }
}
