package com.example.data

import org.junit.Assert.*
import org.junit.Test

class PrayerLessonsTest {
    @Test fun fiveObligatoryPrayersHaveSeventeenRakats() {
        assertEquals(listOf(2, 4, 4, 3, 4), DailyPrayer.entries.map { it.rakats })
        assertEquals(17, DailyPrayer.entries.sumOf { it.rakats })
    }

    @Test fun everyRakatContainsTwoProstrationsAndOneBowing() {
        DailyPrayer.entries.forEach { prayer ->
            val steps = PrayerLessons.steps(prayer)
            assertEquals(steps.size, steps.map { it.id }.distinct().size)
            for (rakat in 1..prayer.rakats) {
                val one = steps.filter { it.rakat == rakat }
                assertEquals(2, one.count { it.posture == PrayerPosture.PROSTRATION })
                assertEquals(1, one.count { it.posture == PrayerPosture.BOWING })
                assertEquals(1, one.count { it.posture == PrayerPosture.STANDING })
                assertEquals(1, one.count { it.posture == PrayerPosture.UPRIGHT })
            }
        }
    }

    @Test fun sittingAndRecitationRulesFollowIndividualHanafiFard() {
        DailyPrayer.entries.forEach { prayer ->
            val steps = PrayerLessons.steps(prayer)
            assertEquals("intention", steps.first().id)
            assertEquals("salam", steps.last().id)
            assertEquals(1, steps.count { it.posture == PrayerPosture.TAKBIR })
            assertEquals(1, steps.count { "subhaneke" in it.recitations })
            assertEquals(1, steps.count { "salawat" in it.recitations })
            assertEquals(prayer.rakats, steps.count { "fatiha" in it.recitations })
            assertEquals(listOf(1, 2), steps.filter { "kevser" in it.recitations || "ihlas" in it.recitations }.map { it.rakat })
            assertEquals(if (prayer.rakats == 2) listOf(2) else listOf(2, prayer.rakats),
                steps.filter { "tahiyyat" in it.recitations }.map { it.rakat })
            steps.flatMap { it.recitations }.forEach { assertTrue(PrayerLessons.recitations.containsKey(it)) }
        }
    }
    @Test fun allReadingsAreCompleteOfflineAndNonempty() {
        PrayerLessons.recitations.values.forEach {
            assertTrue(it.arabic.isNotBlank())
            assertTrue(it.reading.isNotBlank())
            assertFalse(it.arabic.contains("..."))
        }
    }
}
