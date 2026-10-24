package com.example.alarm

import android.content.Context
import com.example.data.AppRepository
import com.example.data.PrayerTimeCache
import com.example.data.RetrofitClient
import com.example.data.dataStore
import java.util.Calendar
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withTimeoutOrNull

/** Reuses the app's prayer source to refill dated sunrise data before it runs out. */
object SunriseAlarmDataRefresh {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private const val RETRY_INTERVAL = 15 * 60_000L

    fun request(context: Context) {
        val app = context.applicationContext
        scope.launch { refreshIfNeeded(app) }
    }

    suspend fun refreshIfNeeded(context: Context, now: Long = System.currentTimeMillis()) {
        if (!SunriseAlarmStore(context).list().any { it.enabled } || !mutex.tryLock()) return
        try {
            withTimeoutOrNull(8_000L) {
                val settings = context.dataStore.data.first()
                val latitude = settings[AppRepository.LATITUDE_KEY] ?: return@withTimeoutOrNull
                val longitude = settings[AppRepository.LONGITUDE_KEY] ?: return@withTimeoutOrNull
                if (!latitude.isFinite() || !longitude.isFinite() || latitude !in -90.0..90.0 || longitude !in -180.0..180.0) return@withTimeoutOrNull
                val months = missingMonths(context, now)
                if (months.isEmpty()) return@withTimeoutOrNull
                val prefs = context.getSharedPreferences("sunrise_alarm_data_refresh", Context.MODE_PRIVATE)
                val locationKey = "$latitude,$longitude"
                val elapsed = now - prefs.getLong("attempt_at", 0L)
                if (prefs.getString("location", "") == locationKey && elapsed in 0 until RETRY_INTERVAL) return@withTimeoutOrNull
                prefs.edit().putLong("attempt_at", now).putString("location", locationKey).apply()
                for ((year, month) in months) {
                    val response = RetrofitClient.apiService.getCalendarByCoordinates(year, month, latitude, longitude)
                    // A location change during the request must never install the old city's data.
                    val latest = context.dataStore.data.first()
                    if (latest[AppRepository.LATITUDE_KEY] != latitude || latest[AppRepository.LONGITUDE_KEY] != longitude) return@withTimeoutOrNull
                    response.data.forEach { PrayerTimeCache.save(context, it) }
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            // Existing dated alarms remain armed if the prayer source is unavailable.
        } finally {
            mutex.unlock()
            SunriseAlarmScheduler.refresh(context)
        }
    }

    /** Calendar fields, rather than fixed 24-hour steps, also cover DST/month rollover. */
    internal fun missingMonths(context: Context, now: Long): List<Pair<Int, Int>> {
        val known = SunriseAlarmPlanner.readSunriseDays(context, now).mapTo(mutableSetOf()) { it.date }
        val today = Calendar.getInstance().apply { timeInMillis = now }
        return (0..7).mapNotNull { offset ->
            val day = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, offset) }
            if (PrayerTimeCache.date(day.timeInMillis) in known) null
            else day.get(Calendar.YEAR) to day.get(Calendar.MONTH) + 1
        }.distinct()
    }
}
