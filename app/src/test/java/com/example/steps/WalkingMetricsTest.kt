package com.example.steps

import org.junit.Assert.*
import org.junit.Test
import java.util.TimeZone

class WalkingMetricsTest {
    @Test fun calibratedDistanceAndNetCaloriesHaveCorrectUnits() {
        val estimate = WalkingMetrics.estimate(1000, WalkingProfile(175.0, 70.0, 75.0))!!
        assertEquals(750.0, estimate.meters, 0.001)
        assertEquals(26.25, estimate.activeKcal, 0.001)
    }
    @Test fun missingAndInvalidProfileNeverInventCalories() {
        assertNull(WalkingMetrics.estimate(1000, null))
        assertNull(WalkingMetrics.estimate(1000, WalkingProfile(Double.NaN, 70.0)))
        assertNull(WalkingMetrics.estimate(1000, WalkingProfile(175.0, Double.POSITIVE_INFINITY)))
        assertNull(WalkingMetrics.estimate(-1, WalkingProfile(175.0, 70.0)))
        assertFalse(WalkingProfile(175.0, 70.0, 200.0).valid)
    }
    @Test fun zeroStepsHaveZeroActiveCalories() {
        assertEquals(0.0, WalkingMetrics.estimate(0, WalkingProfile(175.0, 70.0))!!.activeKcal, 0.001)
    }
    @Test fun calibrationOverridesHeightBasedEstimate() {
        val step = WalkingMetrics.calibratedStepCm(100.0, 140)!!
        assertEquals(71.42857, step, 0.001)
        assertEquals(100.0, WalkingMetrics.estimate(140, WalkingProfile(190.0, 70.0, step))!!.meters, 0.001)
        assertNull(WalkingMetrics.calibratedStepCm(100.0, 0))
        assertNull(WalkingMetrics.calibratedStepCm(100.0, 10))
    }
    @Test fun weekIsMondayToSundayAcrossYearBoundary() {
        val week = WalkingDates.range("2027-01-01", ReportPeriod.WEEK)
        assertEquals("2026-12-28", week.start)
        assertEquals("2027-01-03", week.end)
        assertEquals(7, week.days.size)
    }
    @Test fun monthHandlesLeapYear() {
        assertEquals(29, WalkingDates.range("2028-02-17", ReportPeriod.MONTH).days.size)
        assertEquals(28, WalkingDates.range("2027-02-17", ReportPeriod.MONTH).days.size)
    }
    @Test fun daylightSavingDoesNotSkipOrRepeatCalendarDays() {
        val original = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Europe/Berlin"))
            val week = WalkingDates.range("2026-03-29", ReportPeriod.WEEK)
            assertEquals((23..29).map { "2026-03-$it" }, week.days)
        } finally { TimeZone.setDefault(original) }
    }
}
