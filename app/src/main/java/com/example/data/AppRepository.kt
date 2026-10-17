package com.example.data

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.os.SystemClock
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

import kotlinx.coroutines.flow.catch
import androidx.datastore.preferences.core.emptyPreferences

val Context.dataStore by preferencesDataStore(name = "settings")

class AppRepository(private val context: Context) {
    private val locationClient = LocationServices.getFusedLocationProviderClient(context)
    private val geocoder = Geocoder(context, Locale.getDefault())

    companion object {
        val LATITUDE_KEY = doublePreferencesKey("LATITUDE")
        val LONGITUDE_KEY = doublePreferencesKey("LONGITUDE")
        val CITY_KEY = stringPreferencesKey("CITY")
        val THEME_KEY = stringPreferencesKey("APP_THEME")
        val BACKGROUND_KEY = intPreferencesKey("APP_BACKGROUND")
        val PATTERN_INTENSITY_KEY = androidx.datastore.preferences.core.floatPreferencesKey("APP_PATTERN_INTENSITY")
        val FAVORITE_HADITHS_KEY = stringPreferencesKey("FAVORITE_HADITHS")
        val MEMORIZED_HADITHS_KEY = stringPreferencesKey("MEMORIZED_HADITHS")
    }

    val latitudeFlow: Flow<Double?> = context.dataStore.data.catch { emit(emptyPreferences()) }.map { it[LATITUDE_KEY] }
    val longitudeFlow: Flow<Double?> = context.dataStore.data.catch { emit(emptyPreferences()) }.map { it[LONGITUDE_KEY] }
    val cityFlow: Flow<String?> = context.dataStore.data.catch { emit(emptyPreferences()) }.map { it[CITY_KEY] }
    val themeFlow: Flow<String?> = context.dataStore.data.catch { emit(emptyPreferences()) }.map { it[THEME_KEY] ?: "CLASSIC_EMERALD" }
    val backgroundFlow: Flow<Int> = context.dataStore.data.catch { emit(emptyPreferences()) }.map { it[BACKGROUND_KEY] ?: 0 }
    val patternIntensityFlow: Flow<Float> = context.dataStore.data.catch { emit(emptyPreferences()) }.map { it[PATTERN_INTENSITY_KEY] ?: 1.0f }
    
    val favoriteHadithsFlow: Flow<Set<String>> = context.dataStore.data.catch { emit(emptyPreferences()) }.map { 
        it[FAVORITE_HADITHS_KEY]?.split(",")?.filter { id -> id.isNotEmpty() }?.toSet() ?: emptySet() 
    }
    
    val memorizedHadithsFlow: Flow<Set<String>> = context.dataStore.data.catch { emit(emptyPreferences()) }.map { 
        it[MEMORIZED_HADITHS_KEY]?.split(",")?.filter { id -> id.isNotEmpty() }?.toSet() ?: emptySet() 
    }

    suspend fun toggleFavoriteHadith(hadithId: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[FAVORITE_HADITHS_KEY]?.split(",")?.filter { it.isNotEmpty() }?.toMutableSet() ?: mutableSetOf()
            if (current.contains(hadithId)) {
                current.remove(hadithId)
            } else {
                current.add(hadithId)
            }
            prefs[FAVORITE_HADITHS_KEY] = current.joinToString(",")
        }
    }

    suspend fun toggleMemorizedHadith(hadithId: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[MEMORIZED_HADITHS_KEY]?.split(",")?.filter { it.isNotEmpty() }?.toMutableSet() ?: mutableSetOf()
            if (current.contains(hadithId)) {
                current.remove(hadithId)
            } else {
                current.add(hadithId)
            }
            prefs[MEMORIZED_HADITHS_KEY] = current.joinToString(",")
        }
    }

    suspend fun saveLocation(lat: Double, lng: Double, city: String) {
        context.dataStore.edit { prefs ->
            val oldLat = prefs[LATITUDE_KEY]
            val oldLng = prefs[LONGITUDE_KEY]
            if (oldLat != null && oldLng != null &&
                (kotlin.math.abs(oldLat - lat) > 0.003 || kotlin.math.abs(oldLng - lng) > 0.003)) {
                context.getSharedPreferences("prayer_daily_times", Context.MODE_PRIVATE).edit().clear().apply()
                context.getSharedPreferences("countdown_service_prefs", Context.MODE_PRIVATE).edit()
                    .remove("ALL_PRAYERS_DATE").apply()
            }
            prefs[LATITUDE_KEY] = lat
            prefs[LONGITUDE_KEY] = lng
            prefs[CITY_KEY] = city
        }
    }

    suspend fun saveTheme(themeName: String) {
        context.dataStore.edit { prefs ->
            prefs[THEME_KEY] = themeName
        }
    }

    suspend fun saveBackground(bgId: Int) {
        context.dataStore.edit { prefs ->
            prefs[BACKGROUND_KEY] = bgId
        }
    }

    suspend fun savePatternIntensity(intensity: Float) {
        context.dataStore.edit { prefs ->
            prefs[PATTERN_INTENSITY_KEY] = intensity
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(highAccuracy: Boolean = false): LocationResult? {
        return withContext(Dispatchers.IO) {
            try {
                val locationTimeout = if (highAccuracy) 13_000L else 5_000L
                var location = withTimeoutOrNull(locationTimeout) {
                    suspendCancellableCoroutine<Location?> { continuation ->
                        val cancellationTokenSource = CancellationTokenSource()
                        val locationTask = if (highAccuracy) {
                            val request = CurrentLocationRequest.Builder()
                                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                                .setMaxUpdateAgeMillis(5_000L)
                                .setDurationMillis(12_000L)
                                .build()
                            locationClient.getCurrentLocation(request, cancellationTokenSource.token)
                        } else {
                            locationClient.getCurrentLocation(
                                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                                cancellationTokenSource.token
                            )
                        }
                        locationTask.addOnSuccessListener { loc ->
                            if (continuation.isActive) continuation.resume(loc)
                        }.addOnFailureListener {
                            if (continuation.isActive) continuation.resume(null)
                        }
                        continuation.invokeOnCancellation {
                            cancellationTokenSource.cancel()
                        }
                    }
                }

                if (location == null) {
                    val cachedLocation = withTimeoutOrNull(2000L) {
                        suspendCancellableCoroutine<Location?> { continuation ->
                            locationClient.lastLocation
                                .addOnSuccessListener { loc ->
                                    if (continuation.isActive) continuation.resume(loc)
                                }
                                .addOnFailureListener {
                                    if (continuation.isActive) continuation.resume(null)
                            }
                        }
                    }
                    location = if (highAccuracy) cachedLocation?.takeIf(::isUsableCachedLocation) else cachedLocation
                }

                if (location != null) {
                    var city = "Bilinmeyen Konum"
                    try {
                        withTimeoutOrNull(3000L) {
                            @Suppress("DEPRECATION")
                            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                            if (!addresses.isNullOrEmpty()) {
                                val address = addresses[0]
                                val parts = listOfNotNull(
                                    address.subLocality, 
                                    address.locality, 
                                    address.subAdminArea, 
                                    address.adminArea
                                )
                                if (parts.isNotEmpty()) {
                                    city = parts.distinct().joinToString(", ")
                                }
                            }
                        }
                    } catch (e: Exception) {
                        // Ignore geocoder errors / timeouts
                    }
                    
                    LocationResult(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        city = city,
                        accuracyMeters = location.accuracy.takeIf { location.hasAccuracy() },
                        capturedAtMillis = location.time
                    )
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun getLocationFromCityName(cityName: String): LocationResult? {
        return withContext(Dispatchers.IO) {
            try {
                withTimeoutOrNull(4000L) {
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocationName(cityName, 1)
                    if (!addresses.isNullOrEmpty()) {
                        val address = addresses[0]
                        LocationResult(address.latitude, address.longitude, cityName)
                    } else {
                        null
                    }
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun getLocationsFromQuery(query: String): List<LocationResult> {
        return withContext(Dispatchers.IO) {
            try {
                withTimeoutOrNull(4000L) {
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocationName(query, 5)
                    addresses?.map { address ->
                        val parts = listOfNotNull(
                            address.subLocality, 
                            address.locality, 
                            address.subAdminArea, 
                            address.adminArea,
                            address.countryName
                        )
                        val fullAddress = if (parts.isNotEmpty()) parts.distinct().joinToString(", ") else query
                        LocationResult(address.latitude, address.longitude, fullAddress)
                    } ?: emptyList()
                } ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    private val moshi = com.squareup.moshi.Moshi.Builder()
        .add(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
        .build()
    private val timingsAdapter = moshi.adapter(TimingsData::class.java)
    
    private val calendarListAdapter = moshi.adapter<List<TimingsData>>(
        com.squareup.moshi.Types.newParameterizedType(List::class.java, TimingsData::class.java)
    )

    fun getCachedTodayTimings(): TimingsData? {
        return try {
            val prefs = context.getSharedPreferences("prayer_timings_cache", Context.MODE_PRIVATE)
            val json = prefs.getString("today_timings", null)
            if (json != null) timingsAdapter.fromJson(json) else null
        } catch (e: Exception) {
            null
        }
    }

    fun saveCachedTodayTimings(timings: TimingsData) {
        PrayerTimeCache.save(context, timings)
        try {
            val prefs = context.getSharedPreferences("prayer_timings_cache", Context.MODE_PRIVATE)
            val json = timingsAdapter.toJson(timings)
            prefs.edit().putString("today_timings", json).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getCachedMonthlyCalendar(): List<TimingsData>? {
        return try {
            val prefs = context.getSharedPreferences("prayer_timings_cache", Context.MODE_PRIVATE)
            val json = prefs.getString("monthly_calendar", null)
            if (json != null) calendarListAdapter.fromJson(json) else null
        } catch (e: Exception) {
            null
        }
    }

    fun saveCachedMonthlyCalendar(data: List<TimingsData>) {
        data.forEach { PrayerTimeCache.save(context, it) }
        try {
            val prefs = context.getSharedPreferences("prayer_timings_cache", Context.MODE_PRIVATE)
            val json = calendarListAdapter.toJson(data)
            prefs.edit().putString("monthly_calendar", json).apply()
        } catch (e: Exception) {}
    }

    suspend fun getCurrentWeather(lat: Double, lng: Double): WeatherResponse? {
        return try {
            RetrofitClient.weatherApiService.getCurrentWeather(lat, lng)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getTodayTimings(lat: Double, lng: Double): TimingsData {
        val format = java.text.SimpleDateFormat("dd-MM-yyyy", java.util.Locale.getDefault())
        val dateStr = format.format(java.util.Date())
        val response = RetrofitClient.apiService.getTimingsByCoordinates(dateStr, lat, lng)
        val data = response.data
        saveCachedTodayTimings(data)
        return data
    }

    suspend fun getMonthlyCalendar(lat: Double, lng: Double, month: Int, year: Int): List<TimingsData> {
        val response = RetrofitClient.apiService.getCalendarByCoordinates(year, month, lat, lng)
        saveCachedMonthlyCalendar(response.data)
        return response.data
    }

}

private fun isUsableCachedLocation(location: Location): Boolean {
    val ageMillis = if (location.elapsedRealtimeNanos > 0L) {
        (SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos).coerceAtLeast(0L) / 1_000_000L
    } else {
        (System.currentTimeMillis() - location.time).coerceAtLeast(0L)
    }
    val accurateEnough = !location.hasAccuracy() || location.accuracy <= 1_000f
    return ageMillis <= 10 * 60_000L && accurateEnough &&
        location.latitude in -90.0..90.0 && location.longitude in -180.0..180.0
}

data class LocationResult(
    val latitude: Double,
    val longitude: Double,
    val city: String,
    val accuracyMeters: Float? = null,
    val capturedAtMillis: Long? = null
)
