package com.example.ui

import org.junit.Assert.*
import org.junit.Test

class PrayerAlertIdentityTest {
    private val prayer = 1_800_000_000_000L

    @Test fun warningsOnlyStartTenMinutesBeforeThePrayerAndNeverEarly() {
        assertFalse(PrayerAlertIdentity.isTimely(prayer, true, prayer - 600_001L))
        assertTrue(PrayerAlertIdentity.isTimely(prayer, true, prayer - 600_000L))
        assertTrue(PrayerAlertIdentity.isTimely(prayer, true, prayer - 300_001L))
        assertFalse(PrayerAlertIdentity.isTimely(prayer, true, prayer - 300_000L))
        assertFalse(PrayerAlertIdentity.isTimely(prayer, true, prayer))
    }

    @Test fun exactAlarmsRejectFutureAndExpiredDeliveries() {
        assertFalse(PrayerAlertIdentity.isTimely(prayer, false, prayer - 1L))
        assertTrue(PrayerAlertIdentity.isTimely(prayer, false, prayer))
        assertTrue(PrayerAlertIdentity.isTimely(prayer, false, prayer + 299_999L))
        assertFalse(PrayerAlertIdentity.isTimely(prayer, false, prayer + 300_000L))
        assertFalse(PrayerAlertIdentity.isTimely(0L, false, 0L))
    }

    @Test fun dedupeCoversTimeCorrectionsWithinTheSamePrayerDayButKeepsWarningSeparate() {
        assertEquals(PrayerAlertIdentity.key("Öğle", prayer, false),
            PrayerAlertIdentity.key("Öğle", prayer + 60_000L, false))
        assertNotEquals(PrayerAlertIdentity.key("Öğle", prayer, false),
            PrayerAlertIdentity.key("Öğle", prayer, true))
        assertNotEquals(PrayerAlertIdentity.key("Öğle", prayer, false),
            PrayerAlertIdentity.key("Öğle", prayer + 86_400_000L, false))
    }
}
