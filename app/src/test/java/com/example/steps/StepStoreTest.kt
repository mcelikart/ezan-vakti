package com.example.steps

import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class StepStoreTest {
    private lateinit var store: StepStore
    private fun sample(total: Long, seconds: Long, boot: Int = 1) = CounterSample(total, boot, seconds * 1_000_000_000)
    @Before fun setup() {
        store = StepStore.get(ApplicationProvider.getApplicationContext())
        store.writableDatabase.delete("daily_steps", null, null)
        store.writableDatabase.delete("body_profiles", null, null)
        store.resetBaseline()
    }
    @Test fun midnightAndMonthRolloverKeepSeparateReports() {
        store.record(sample(100, 100), "2026-09-30")
        store.record(sample(120, 120), "2026-09-30")
        store.record(sample(127, 130), "2026-10-01")
        store.record(sample(127, 130), "2026-10-01")
        assertEquals(20L, store.day("2026-09-30"))
        assertEquals(7L, store.month("2026-10").sumOf { it.steps })
    }
    @Test fun detectorSplitsMidnightBatchWithoutDoubleCounting() {
        store.record(sample(100, 100), "2026-09-30")
        store.observeDetector(sample(1, 101), "2026-09-30")
        store.observeDetector(sample(1, 102), "2026-10-01")
        store.observeDetector(sample(1, 103), "2026-10-01")
        store.record(sample(103, 103), "2026-10-01")
        store.observeDetector(sample(1, 102), "2026-10-01")
        store.record(sample(103, 103), "2026-10-01")
        assertEquals(1L, store.day("2026-09-30"))
        assertEquals(2L, store.day("2026-10-01"))
        assertEquals(3L, store.total())
    }
    @Test fun detectorFalsePositivesCannotExceedCounterTotal() {
        store.record(sample(100, 100), "2026-09-21")
        for (second in 101L..105L) store.observeDetector(sample(1, second), "2026-09-21")
        store.record(sample(103, 105), "2026-09-21")
        assertEquals(3L, store.total())
    }
    @Test fun restartAndRebootKeepHistory() {
        store.record(sample(10, 100), "2026-09-21")
        store.record(sample(30, 120), "2026-09-21")
        store.close()
        store.record(sample(35, 130), "2026-09-21")
        store.record(sample(7, 10, 2), "2026-09-21")
        assertEquals(32L, store.day("2026-09-21"))
    }
    @Test fun pauseDoesNotImportUntrackedInterval() {
        store.record(sample(10, 100), "2026-09-21")
        store.record(sample(30, 120), "2026-09-21")
        store.resetBaseline()
        store.record(sample(500, 200), "2026-09-21")
        store.record(sample(507, 210), "2026-09-21")
        assertEquals(27L, store.day("2026-09-21"))
    }
    @Test fun detectorFallbackRejectsDuplicateAndImpossibleFrequency() {
        store.record(sample(1, 1), "2026-09-21", true)
        store.record(sample(1, 1), "2026-09-21", true)
        store.record(CounterSample(1, 1, 1_050_000_000), "2026-09-21", true)
        store.record(sample(1, 2), "2026-09-21", true)
        assertEquals(2L, store.day("2026-09-21"))
    }
    @Test fun resetCandidateSurvivesProcessRestart() {
        store.record(sample(100, 100), "2026-09-21")
        store.record(sample(102, 101), "2026-09-21")
        store.record(sample(0, 102), "2026-09-21")
        store.close()
        store.record(sample(2, 103), "2026-09-21")
        assertEquals(4L, store.total())
    }
    @Test fun changingWeightDoesNotRewritePreviousDays() {
        store.saveProfile(WalkingProfile(175.0, 70.0, 70.0), "2026-09-20")
        store.saveProfile(WalkingProfile(175.0, 80.0, 75.0), "2026-09-21")
        assertEquals(70.0, store.profile("2026-09-20")!!.weightKg, 0.001)
        assertEquals(80.0, store.profile("2026-09-21")!!.weightKg, 0.001)
        assertEquals(70.0, store.profile("2026-08-01")!!.calibratedStepCm!!, 0.001)
    }
    @Test fun versionOneMigrationPreservesStepsAndCheckpoint() {
        val db = SQLiteDatabase.create(null)
        try {
            db.execSQL("CREATE TABLE daily_steps(day TEXT PRIMARY KEY, steps INTEGER NOT NULL)")
            db.execSQL("CREATE TABLE counter_state(id INTEGER PRIMARY KEY, total INTEGER, boot INTEGER, stamp INTEGER, detector INTEGER)")
            db.execSQL("INSERT INTO daily_steps VALUES ('2026-09-21', 7654)")
            db.execSQL("INSERT INTO counter_state VALUES (1, 7654, 1, 1000000000, 0)")
            store.onUpgrade(db, 1, 2)
            db.rawQuery("SELECT steps FROM daily_steps", null).use { it.moveToFirst(); assertEquals(7654L, it.getLong(0)) }
            db.rawQuery("SELECT total, reset_total FROM counter_state", null).use {
                it.moveToFirst(); assertEquals(7654L, it.getLong(0)); assertTrue(it.isNull(1))
            }
        } finally { db.close() }
    }
}
