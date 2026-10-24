package com.example.alarm

import org.junit.Assert.*
import org.junit.Test

class SunriseAlarmUpcomingTest {
    @Test fun queuedSnoozeTakesPrecedenceOverTomorrowMorningPreview() {
        val alarm = SunriseAlarm(id = "alarm")
        val preview = SunriseAlarmOccurrence(alarm.id, 100_000L, 90_000L, "tomorrow")
        val regular = SunriseAlarmDelivery(alarm.id, "regular", 90_000L, 100_000L, "tomorrow")
        val snooze = SunriseAlarmDelivery(alarm.id, "snooze", 12_000L, 10_000L, "today", fromSnooze = true)
        val result = upcomingSunriseAlarmOccurrences(listOf(alarm), listOf(regular, snooze), listOf(preview), 11_000L)
        assertEquals(12_000L, result.getValue(alarm.id).triggerAtMillis)
        assertEquals("today", result.getValue(alarm.id).sourceDate)
    }

    @Test fun pastAndDisabledDeliveriesAreExcludedAndUnschedulableAlarmUsesOnlyRealDatedPreview() {
        val enabled = SunriseAlarm(id = "enabled")
        val disabled = SunriseAlarm(id = "disabled", enabled = false)
        val noData = SunriseAlarm(id = "no-data")
        val preview = SunriseAlarmOccurrence(enabled.id, 100_000L, 90_000L, "tomorrow")
        val pending = listOf(
            SunriseAlarmDelivery(enabled.id, "past", 1_000L, 2_000L, "yesterday"),
            SunriseAlarmDelivery(disabled.id, "disabled", 12_000L, 13_000L, "today")
        )
        val result = upcomingSunriseAlarmOccurrences(listOf(enabled, disabled, noData), pending, listOf(preview), 11_000L)
        assertEquals(mapOf(enabled.id to preview), result)
        assertFalse(result.containsKey(noData.id))
    }
}
