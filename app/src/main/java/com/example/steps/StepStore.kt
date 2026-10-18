package com.example.steps

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class DailySteps(val date: String, val steps: Long, val profile: WalkingProfile? = null) {
    val estimate: WalkingEstimate? get() = WalkingMetrics.estimate(steps, profile)
}

data class WalkingSessionRecord(
    val id: Long,
    val title: String,
    val startTimeMillis: Long,
    val durationSec: Int,
    val steps: Long,
    val meters: Double,
    val kcal: Double
)

class StepStore private constructor(context: Context) : SQLiteOpenHelper(
    context, java.io.File(context.noBackupFilesDir, "walking.db").absolutePath, null, 2
) {
    private val revision = MutableStateFlow(0L)
    val changes = revision.asStateFlow()

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE daily_steps (day TEXT PRIMARY KEY, steps INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE counter_state (id INTEGER PRIMARY KEY CHECK(id=1), total INTEGER NOT NULL, boot INTEGER NOT NULL, stamp INTEGER NOT NULL, detector INTEGER NOT NULL, reset_total INTEGER, reset_stamp INTEGER)")
        createV2Tables(db)
    }

    private fun createV2Tables(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS detector_samples (boot INTEGER NOT NULL, stamp INTEGER NOT NULL, day TEXT NOT NULL, PRIMARY KEY(boot, stamp))")
        db.execSQL("CREATE TABLE IF NOT EXISTS body_profiles (day TEXT PRIMARY KEY, height REAL NOT NULL, weight REAL NOT NULL, step_cm REAL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS hourly_steps (day TEXT NOT NULL, hour INTEGER NOT NULL, steps INTEGER NOT NULL, PRIMARY KEY(day, hour))")
        db.execSQL("CREATE TABLE IF NOT EXISTS walking_sessions (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT NOT NULL, start_time INTEGER NOT NULL, duration_sec INTEGER NOT NULL, steps INTEGER NOT NULL, meters REAL NOT NULL, kcal REAL NOT NULL)")
    }

    private fun ensureExtendedTables(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS hourly_steps (day TEXT NOT NULL, hour INTEGER NOT NULL, steps INTEGER NOT NULL, PRIMARY KEY(day, hour))")
        db.execSQL("CREATE TABLE IF NOT EXISTS walking_sessions (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT NOT NULL, start_time INTEGER NOT NULL, duration_sec INTEGER NOT NULL, steps INTEGER NOT NULL, meters REAL NOT NULL, kcal REAL NOT NULL)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE counter_state ADD COLUMN reset_total INTEGER")
            db.execSQL("ALTER TABLE counter_state ADD COLUMN reset_stamp INTEGER")
            createV2Tables(db)
        }
    }

    @Synchronized
    fun observeDetector(sample: CounterSample, date: String) {
        val db = writableDatabase
        val checkpoint = db.rawQuery("SELECT stamp FROM counter_state WHERE id=1 AND boot=?", arrayOf(sample.boot.toString()))
            .use { if (it.moveToFirst()) it.getLong(0) else -1L }
        if (sample.timestamp <= checkpoint) return
        val latest = db.rawQuery("SELECT MAX(stamp) FROM detector_samples WHERE boot=?", arrayOf(sample.boot.toString()))
            .use { if (it.moveToFirst() && !it.isNull(0)) it.getLong(0) else -1L }
        if (latest >= 0 && sample.timestamp - latest < 120_000_000L) return
        db.execSQL("INSERT OR IGNORE INTO detector_samples VALUES (?, ?, ?)", arrayOf<Any>(sample.boot, sample.timestamp, date))
        db.delete("detector_samples", "boot != ? OR stamp < ?", arrayOf(sample.boot.toString(), (sample.timestamp - 3_600_000_000_000L).toString()))
    }

    @Synchronized
    fun record(sample: CounterSample, date: String, detector: Boolean = false) {
        val db = writableDatabase
        ensureExtendedTables(db)
        db.beginTransaction()
        try {
            var sameSensor = true
            var candidate: CounterSample? = null
            val previous = db.rawQuery("SELECT total, boot, stamp, detector, reset_total, reset_stamp FROM counter_state WHERE id=1", null).use {
                if (it.moveToFirst()) {
                    sameSensor = (it.getInt(3) == 1) == detector
                    if (!it.isNull(4)) candidate = CounterSample(it.getLong(4), it.getInt(1), it.getLong(5))
                    CounterSample(it.getLong(0), it.getInt(1), it.getLong(2))
                } else null
            }
            val decision = if (detector) {
                if (sample.total != 1L || (previous != null && sameSensor && previous.boot == sample.boot &&
                    sample.timestamp - previous.timestamp < 120_000_000L)) return
                CounterDecision(1, sample)
            } else StepCounterMath.evaluate(if (sameSensor) previous else null, sample, if (sameSensor) candidate else null)
            val checkpoint = decision.checkpoint ?: return
            val allocations = linkedMapOf<String, Long>()
            if (decision.steps > 0) {
                var remaining = decision.steps
                if (!detector) {
                    // Detector timestamps only distribute the authoritative counter delta; they never add to it.
                    db.rawQuery("SELECT day FROM detector_samples WHERE boot=? AND stamp<=? ORDER BY stamp DESC LIMIT ?",
                        arrayOf(sample.boot.toString(), sample.timestamp.toString(), remaining.toString())).use {
                        while (it.moveToNext()) {
                            val key = it.getString(0)
                            allocations[key] = (allocations[key] ?: 0) + 1
                            remaining--
                        }
                    }
                }
                if (remaining > 0) allocations[date] = (allocations[date] ?: 0) + remaining
            }
            val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            for ((day, steps) in allocations) {
                db.execSQL("INSERT OR IGNORE INTO daily_steps(day, steps) VALUES (?, 0)", arrayOf(day))
                db.execSQL("UPDATE daily_steps SET steps=steps+? WHERE day=?", arrayOf<Any>(steps, day))
                db.execSQL("INSERT OR IGNORE INTO hourly_steps(day, hour, steps) VALUES (?, ?, 0)", arrayOf<Any>(day, currentHour))
                db.execSQL("UPDATE hourly_steps SET steps=steps+? WHERE day=? AND hour=?", arrayOf<Any>(steps, day, currentHour))
            }
            db.execSQL("INSERT OR REPLACE INTO counter_state VALUES (1, ?, ?, ?, ?, ?, ?)",
                arrayOf<Any?>(checkpoint.total, checkpoint.boot, checkpoint.timestamp, if (detector) 1 else 0,
                    decision.resetCandidate?.total, decision.resetCandidate?.timestamp))
            db.delete("detector_samples", "boot != ? OR stamp <= ?", arrayOf(checkpoint.boot.toString(), checkpoint.timestamp.toString()))
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        revision.value += 1
    }

    @Synchronized
    fun resetBaseline() {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete("counter_state", null, null)
            db.delete("detector_samples", null, null)
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }

    fun day(date: String): Long = readableDatabase.rawQuery(
        "SELECT steps FROM daily_steps WHERE day=?", arrayOf(date)
    ).use { if (it.moveToFirst()) it.getLong(0) else 0L }

    fun total(): Long = readableDatabase.rawQuery("SELECT COALESCE(SUM(steps), 0) FROM daily_steps", null)
        .use { it.moveToFirst(); it.getLong(0) }

    fun bestDay(): DailySteps? = readableDatabase.rawQuery(
        "SELECT day, steps FROM daily_steps WHERE steps > 0 ORDER BY steps DESC, day DESC LIMIT 1", null
    ).use {
        if (it.moveToFirst()) {
            val d = it.getString(0)
            DailySteps(d, it.getLong(1), profile(d))
        } else null
    }

    fun currentStreak(todayKey: String = dayKey(), minSteps: Long = 1L): Int {
        var streak = 0
        var cursorDay = todayKey
        for (i in 0..365) {
            val s = day(cursorDay)
            if (s >= minSteps) {
                streak++
                cursorDay = WalkingDates.shift(cursorDay, ReportPeriod.DAY, -1)
            } else if (i == 0) {
                // If today hasn't reached threshold yet, check from yesterday
                cursorDay = WalkingDates.shift(cursorDay, ReportPeriod.DAY, -1)
            } else {
                break
            }
        }
        return streak
    }

    fun hourlySteps(date: String): Map<Int, Long> {
        val db = writableDatabase
        ensureExtendedTables(db)
        return db.rawQuery(
            "SELECT hour, steps FROM hourly_steps WHERE day=? ORDER BY hour", arrayOf(date)
        ).use { cursor ->
            buildMap {
                while (cursor.moveToNext()) {
                    put(cursor.getInt(0), cursor.getLong(1))
                }
            }
        }
    }

    @Synchronized
    fun saveSession(title: String, startTimeMillis: Long, durationSec: Int, steps: Long, meters: Double, kcal: Double) {
        val db = writableDatabase
        ensureExtendedTables(db)
        db.execSQL(
            "INSERT INTO walking_sessions(title, start_time, duration_sec, steps, meters, kcal) VALUES (?, ?, ?, ?, ?, ?)",
            arrayOf<Any>(title, startTimeMillis, durationSec.coerceAtLeast(1), steps.coerceAtLeast(0L), meters.coerceAtLeast(0.0), kcal.coerceAtLeast(0.0))
        )
        revision.value += 1
    }

    fun sessions(limit: Int = 10): List<WalkingSessionRecord> {
        val db = writableDatabase
        ensureExtendedTables(db)
        return db.rawQuery(
            "SELECT id, title, start_time, duration_sec, steps, meters, kcal FROM walking_sessions ORDER BY start_time DESC LIMIT ?",
            arrayOf(limit.toString())
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        WalkingSessionRecord(
                            id = cursor.getLong(0),
                            title = cursor.getString(1),
                            startTimeMillis = cursor.getLong(2),
                            durationSec = cursor.getInt(3),
                            steps = cursor.getLong(4),
                            meters = cursor.getDouble(5),
                            kcal = cursor.getDouble(6)
                        )
                    )
                }
            }
        }
    }

    @Synchronized
    fun deleteSession(id: Long) {
        val db = writableDatabase
        ensureExtendedTables(db)
        db.delete("walking_sessions", "id=?", arrayOf(id.toString()))
        revision.value += 1
    }

    fun profile(date: String = dayKey()): WalkingProfile? = readableDatabase.rawQuery(
        "SELECT height, weight, step_cm FROM body_profiles WHERE day<=? ORDER BY day DESC LIMIT 1", arrayOf(date)
    ).use {
        if (it.moveToFirst()) WalkingProfile(it.getDouble(0), it.getDouble(1), if (it.isNull(2)) null else it.getDouble(2)) else null
    }

    @Synchronized
    fun saveProfile(profile: WalkingProfile, date: String = dayKey()) {
        require(profile.valid)
        // The first profile supplies an explicit estimate for older records. Later changes apply from their date.
        val first = readableDatabase.rawQuery("SELECT COUNT(*) FROM body_profiles", null).use { it.moveToFirst(); it.getInt(0) == 0 }
        writableDatabase.execSQL("INSERT OR REPLACE INTO body_profiles VALUES (?, ?, ?, ?)",
            arrayOf<Any?>(if (first) "0000-01-01" else date, profile.heightCm, profile.weightKg, profile.calibratedStepCm))
        revision.value += 1
    }

    fun range(start: String, end: String): List<DailySteps> = readableDatabase.rawQuery(
        "SELECT day, steps FROM daily_steps WHERE day>=? AND day<=? ORDER BY day", arrayOf(start, end)
    ).use { cursor ->
        buildList {
            while (cursor.moveToNext()) {
                val date = cursor.getString(0)
                add(DailySteps(date, cursor.getLong(1), profile(date)))
            }
        }
    }

    fun month(month: String): List<DailySteps> = range("$month-01", "$month-31")

    companion object {
        @Volatile private var instance: StepStore? = null
        fun get(context: Context): StepStore = instance ?: synchronized(this) {
            instance ?: StepStore(context.applicationContext).also { instance = it }
        }
        fun dayKey(time: Long = System.currentTimeMillis()): String =
            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(time))
    }
}
