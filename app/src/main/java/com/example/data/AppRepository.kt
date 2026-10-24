package com.example.data

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.os.SystemClock
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject

val Context.dataStore by preferencesDataStore(name = "settings")

class AppRepository(private val context: Context) {
    private val locationClient = LocationServices.getFusedLocationProviderClient(context)
    private val geocoder = Geocoder(context, Locale.forLanguageTag("tr-TR"))
    private val deviceLocationSource = DeviceLocationSource(context)

    companion object {
        val LATITUDE_KEY = doublePreferencesKey("LATITUDE")
        val LONGITUDE_KEY = doublePreferencesKey("LONGITUDE")
        val CITY_KEY = stringPreferencesKey("CITY")
        val AUTO_LOCATION_KEY = booleanPreferencesKey("AUTO_LOCATION_ENABLED")
        val THEME_KEY = stringPreferencesKey("APP_THEME")
        val BACKGROUND_KEY = intPreferencesKey("APP_BACKGROUND")
        val PATTERN_INTENSITY_KEY = androidx.datastore.preferences.core.floatPreferencesKey("APP_PATTERN_INTENSITY")
        val THEME_COLOR_VARIANT_KEY = intPreferencesKey("APP_THEME_COLOR_VARIANT")
        val FAVORITE_HADITHS_KEY = stringPreferencesKey("FAVORITE_HADITHS")
        val MEMORIZED_HADITHS_KEY = stringPreferencesKey("MEMORIZED_HADITHS")
    }

    val latitudeFlow: Flow<Double?> = context.dataStore.data.catch { emit(emptyPreferences()) }.map { it[LATITUDE_KEY] }
    val longitudeFlow: Flow<Double?> = context.dataStore.data.catch { emit(emptyPreferences()) }.map { it[LONGITUDE_KEY] }
    val cityFlow: Flow<String?> = context.dataStore.data.catch { emit(emptyPreferences()) }.map { it[CITY_KEY] }
    val autoLocationFlow: Flow<Boolean> = context.dataStore.data.catch { emit(emptyPreferences()) }.map { it[AUTO_LOCATION_KEY] ?: true }
    val themeFlow: Flow<String?> = context.dataStore.data.catch { emit(emptyPreferences()) }.map { it[THEME_KEY] ?: "CLASSIC_EMERALD" }
    val backgroundFlow: Flow<Int> = context.dataStore.data.catch { emit(emptyPreferences()) }.map { it[BACKGROUND_KEY] ?: 0 }
    val patternIntensityFlow: Flow<Float> = context.dataStore.data.catch { emit(emptyPreferences()) }.map { it[PATTERN_INTENSITY_KEY] ?: 1.0f }
    val themeColorVariantFlow: Flow<Int> = context.dataStore.data.catch { emit(emptyPreferences()) }.map { it[THEME_COLOR_VARIANT_KEY] ?: 0 }

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
                (kotlin.math.abs(oldLat - lat) > 0.002 || kotlin.math.abs(oldLng - lng) > 0.002)) {
                context.getSharedPreferences("prayer_daily_times", Context.MODE_PRIVATE).edit().clear().apply()
                context.getSharedPreferences("countdown_service_prefs", Context.MODE_PRIVATE).edit()
                    .remove("ALL_PRAYERS_DATE").apply()
            }
            prefs[LATITUDE_KEY] = lat
            prefs[LONGITUDE_KEY] = lng
            prefs[CITY_KEY] = city
        }
        com.example.alarm.SunriseAlarmScheduler.refresh(context)
    }

    suspend fun saveAutoLocationEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[AUTO_LOCATION_KEY] = enabled
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

    suspend fun saveThemeColorVariant(variant: Int) {
        context.dataStore.edit { prefs ->
            prefs[THEME_COLOR_VARIANT_KEY] = variant.coerceIn(0, 4)
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(highAccuracy: Boolean = true): LocationResult? {
        return withContext(Dispatchers.IO) {
            try {
                // 1. Try FusedLocationProviderClient with fresh fix request
                val locationTimeout = if (highAccuracy) 8_000L else 5_000L
                var location = withTimeoutOrNull(locationTimeout) {
                    suspendCancellableCoroutine<Location?> { continuation ->
                        val cancellationTokenSource = CancellationTokenSource()
                        val request = CurrentLocationRequest.Builder()
                            .setPriority(if (highAccuracy) Priority.PRIORITY_HIGH_ACCURACY else Priority.PRIORITY_BALANCED_POWER_ACCURACY)
                            .setMaxUpdateAgeMillis(15_000L)
                            .setDurationMillis(locationTimeout - 500L)
                            .build()
                        locationClient.getCurrentLocation(request, cancellationTokenSource.token)
                            .addOnSuccessListener { loc ->
                                if (continuation.isActive) continuation.resume(loc)
                            }
                            .addOnFailureListener {
                                if (continuation.isActive) continuation.resume(null)
                            }
                        continuation.invokeOnCancellation {
                            cancellationTokenSource.cancel()
                        }
                    }
                }

                // 2. If high accuracy timed out, try balanced power accuracy quickly
                if (location == null && highAccuracy) {
                    location = withTimeoutOrNull(4_000L) {
                        suspendCancellableCoroutine<Location?> { continuation ->
                            val cancellationTokenSource = CancellationTokenSource()
                            val request = CurrentLocationRequest.Builder()
                                .setPriority(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
                                .setMaxUpdateAgeMillis(30_000L)
                                .setDurationMillis(3_500L)
                                .build()
                            locationClient.getCurrentLocation(request, cancellationTokenSource.token)
                                .addOnSuccessListener { loc ->
                                    if (continuation.isActive) continuation.resume(loc)
                                }
                                .addOnFailureListener {
                                    if (continuation.isActive) continuation.resume(null)
                                }
                            continuation.invokeOnCancellation {
                                cancellationTokenSource.cancel()
                            }
                        }
                    }
                }

                // 3. Fallback to Android LocationManager (GPS + Network providers)
                if (location == null) {
                    val deviceFix = runCatching { deviceLocationSource.current() }.getOrNull()
                    if (deviceFix != null) {
                        val resolvedCity = resolveCityName(deviceFix.latitude, deviceFix.longitude)
                        return@withContext deviceFix.copy(city = resolvedCity)
                    }
                }

                // 4. Fallback to recent Fused lastLocation
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
                    location = cachedLocation?.takeIf(::isUsableCachedLocation)
                }

                if (location != null) {
                    val city = resolveCityName(location.latitude, location.longitude)
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

    /**
     * Observes continuous device movement so when the user travels to a new district/city,
     * the app automatically receives the new coordinates and resolves the new location name.
     */
    @SuppressLint("MissingPermission")
    fun observeLocationChanges(): Flow<LocationResult> = callbackFlow {
        val fusedRequest = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 45_000L)
            .setMinUpdateIntervalMillis(15_000L)
            .setMinUpdateDistanceMeters(200f)
            .build()

        val fusedCallback = object : LocationCallback() {
            override fun onLocationResult(result: com.google.android.gms.location.LocationResult) {
                val loc = result.lastLocation ?: return
                launch(Dispatchers.IO) {
                    val city = resolveCityName(loc.latitude, loc.longitude)
                    trySend(
                        LocationResult(
                            latitude = loc.latitude,
                            longitude = loc.longitude,
                            city = city,
                            accuracyMeters = loc.accuracy.takeIf { loc.hasAccuracy() },
                            capturedAtMillis = loc.time
                        )
                    )
                }
            }
        }

        val locationManager = context.getSystemService(LocationManager::class.java)
        val managerListener = object : LocationListener {
            override fun onLocationChanged(loc: Location) {
                launch(Dispatchers.IO) {
                    val city = resolveCityName(loc.latitude, loc.longitude)
                    trySend(
                        LocationResult(
                            latitude = loc.latitude,
                            longitude = loc.longitude,
                            city = city,
                            accuracyMeters = loc.accuracy.takeIf { loc.hasAccuracy() },
                            capturedAtMillis = loc.time
                        )
                    )
                }
            }
            override fun onProviderEnabled(provider: String) = Unit
            override fun onProviderDisabled(provider: String) = Unit
            @Deprecated("Legacy callback")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
        }

        runCatching {
            locationClient.requestLocationUpdates(fusedRequest, fusedCallback, Looper.getMainLooper())
        }

        runCatching {
            val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
                .filter { runCatching { locationManager.isProviderEnabled(it) }.getOrDefault(false) }
            providers.forEach { provider ->
                locationManager.requestLocationUpdates(provider, 45_000L, 200f, managerListener, Looper.getMainLooper())
            }
        }

        awaitClose {
            runCatching { locationClient.removeLocationUpdates(fusedCallback) }
            runCatching { locationManager.removeUpdates(managerListener) }
        }
    }

    /**
     * Multi-stage reverse geocoder:
     * 1. Android Geocoder (Turkish locale) -> "İlçe, İl"
     * 2. BigDataCloud Reverse Geocode API (free, no key required, Turkish language)
     * 3. Offline 81 Turkish Provinces coordinate matcher
     */
    suspend fun resolveCityName(lat: Double, lng: Double): String = withContext(Dispatchers.IO) {
        // Stage 1: Android Geocoder
        try {
            val fromAndroid = withTimeoutOrNull(3_000L) {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(lat, lng, 1)
                if (!addresses.isNullOrEmpty()) {
                    val addr = addresses[0]
                    val district = addr.subAdminArea ?: addr.locality ?: addr.subLocality
                    val province = addr.adminArea
                    val parts = listOfNotNull(district, province)
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                        .distinct()
                    if (parts.isNotEmpty()) parts.joinToString(", ") else null
                } else null
            }
            if (!fromAndroid.isNullOrBlank()) return@withContext fromAndroid
        } catch (_: Exception) {}

        // Stage 2: HTTP Reverse Geocoding Fallback (BigDataCloud Turkish endpoint)
        try {
            val fromHttp = withTimeoutOrNull(3_500L) {
                val url = URL("https://api.bigdatacloud.net/data/reverse-geocode-client?latitude=$lat&longitude=$lng&localityLanguage=tr")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 2500
                    readTimeout = 2500
                    requestMethod = "GET"
                }
                if (conn.responseCode == 200) {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(body)
                    val locality = json.optString("locality").takeIf { it.isNotBlank() }
                    val city = json.optString("city").takeIf { it.isNotBlank() }
                    val subdivision = json.optString("principalSubdivision").takeIf { it.isNotBlank() }
                    val district = locality ?: city
                    val parts = listOfNotNull(district, subdivision)
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                        .distinct()
                    if (parts.isNotEmpty()) parts.joinToString(", ") else null
                } else null
            }
            if (!fromHttp.isNullOrBlank()) return@withContext fromHttp
        } catch (_: Exception) {}

        // Stage 3: Offline Nearest Turkish Province (if inside or near Turkey)
        findNearestTurkishProvince(lat, lng)
    }

    suspend fun getLocationFromCityName(cityName: String): LocationResult? {
        return withContext(Dispatchers.IO) {
            // Check offline province table first for instant response on Turkish cities
            val matchedProvince = TURKISH_PROVINCE_COORDS.firstOrNull {
                it.first.equals(cityName.trim(), ignoreCase = true)
            }
            try {
                val geocoded = withTimeoutOrNull(4000L) {
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocationName(cityName, 1)
                    if (!addresses.isNullOrEmpty()) {
                        val address = addresses[0]
                        LocationResult(address.latitude, address.longitude, cityName)
                    } else {
                        null
                    }
                }
                geocoded ?: matchedProvince?.let { (name, coords) ->
                    LocationResult(coords.first, coords.second, name)
                }
            } catch (e: Exception) {
                matchedProvince?.let { (name, coords) ->
                    LocationResult(coords.first, coords.second, name)
                }
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
        com.example.alarm.SunriseAlarmScheduler.refresh(context)
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
        com.example.alarm.SunriseAlarmScheduler.refresh(context)
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
    val accurateEnough = !location.hasAccuracy() || location.accuracy <= 2_000f
    return ageMillis <= 15 * 60_000L && accurateEnough &&
        location.latitude in -90.0..90.0 && location.longitude in -180.0..180.0
}

private val TURKISH_PROVINCE_COORDS: List<Pair<String, Pair<Double, Double>>> = listOf(
    "Adana" to (37.0000 to 35.3213), "Adıyaman" to (37.7648 to 38.2786), "Afyonkarahisar" to (38.7507 to 30.5567),
    "Ağrı" to (39.7191 to 43.0503), "Amasya" to (40.6499 to 35.8353), "Ankara" to (39.9334 to 32.8597),
    "Antalya" to (36.8969 to 30.7133), "Artvin" to (41.1828 to 41.8183), "Aydın" to (37.8560 to 27.8416),
    "Balıkesir" to (39.6484 to 27.8826), "Bilecik" to (40.1451 to 29.9799), "Bingöl" to (38.8854 to 40.4966),
    "Bitlis" to (38.4006 to 42.1095), "Bolu" to (40.7392 to 31.6089), "Burdur" to (37.7203 to 30.2906),
    "Bursa" to (40.1885 to 29.0610), "Çanakkale" to (40.1553 to 26.4142), "Çankırı" to (40.6013 to 33.6134),
    "Çorum" to (40.5506 to 34.9556), "Denizli" to (37.7765 to 29.0864), "Diyarbakır" to (37.9144 to 40.2306),
    "Edirne" to (41.6771 to 26.5557), "Elazığ" to (38.6810 to 39.2264), "Erzincan" to (39.7500 to 39.5000),
    "Erzurum" to (39.9043 to 41.2679), "Eskişehir" to (39.7767 to 30.5206), "Gaziantep" to (37.0662 to 37.3833),
    "Giresun" to (40.9128 to 38.3895), "Gümüşhane" to (40.4386 to 39.5086), "Hakkari" to (37.5833 to 43.7333),
    "Hatay" to (36.4018 to 36.3498), "Isparta" to (37.7648 to 30.5566), "Mersin" to (36.8121 to 34.6415),
    "İstanbul" to (41.0082 to 28.9784), "İzmir" to (38.4237 to 27.1428), "Kars" to (40.6167 to 43.1000),
    "Kastamonu" to (41.3887 to 33.7827), "Kayseri" to (38.7312 to 35.4787), "Kırklareli" to (41.7333 to 27.2167),
    "Kırşehir" to (39.1425 to 34.1709), "Kocaeli" to (40.8533 to 29.8815), "Konya" to (37.8746 to 32.4932),
    "Kütahya" to (39.4167 to 29.9833), "Malatya" to (38.3552 to 38.3095), "Manisa" to (38.6191 to 27.4289),
    "Kahramanmaraş" to (37.5858 to 36.9371), "Mardin" to (37.3212 to 40.7245), "Muğla" to (37.2153 to 28.3636),
    "Muş" to (38.9462 to 41.7539), "Nevşehir" to (38.6939 to 34.6857), "Niğde" to (37.9667 to 34.6833),
    "Ordu" to (40.9839 to 37.8764), "Rize" to (41.0201 to 40.5234), "Sakarya" to (40.7569 to 30.3783),
    "Samsun" to (41.2867 to 36.3300), "Siirt" to (37.9333 to 41.9500), "Sinop" to (42.0231 to 35.1531),
    "Sivas" to (39.7477 to 37.0179), "Tekirdağ" to (40.9833 to 27.5167), "Tokat" to (40.3167 to 36.5500),
    "Trabzon" to (41.0027 to 39.7168), "Tunceli" to (39.1079 to 39.5401), "Şanlıurfa" to (37.1591 to 38.7969),
    "Uşak" to (38.6823 to 29.4082), "Van" to (38.4891 to 43.4089), "Yozgat" to (39.8181 to 34.8147),
    "Zonguldak" to (41.4564 to 31.7987), "Aksaray" to (38.3687 to 34.0370), "Bayburt" to (40.2552 to 40.2249),
    "Karaman" to (37.1759 to 33.2287), "Kırıkkale" to (39.8468 to 33.5153), "Batman" to (37.8812 to 41.1351),
    "Şırnak" to (37.5164 to 42.4611), "Bartın" to (41.6344 to 32.3375), "Ardahan" to (41.1105 to 42.7022),
    "Iğdır" to (39.9167 to 44.0333), "Yalova" to (40.6500 to 29.2667), "Karabük" to (41.2061 to 32.6204),
    "Kilis" to (36.7184 to 37.1212), "Osmaniye" to (37.0742 to 36.2478), "Düzce" to (40.8438 to 31.1565)
)

private fun findNearestTurkishProvince(lat: Double, lng: Double): String {
    if (lat in 35.5..42.5 && lng in 25.5..45.0) {
        var nearestName = "İstanbul"
        var minDist = Float.MAX_VALUE
        val dist = FloatArray(1)
        for ((name, coords) in TURKISH_PROVINCE_COORDS) {
            Location.distanceBetween(lat, lng, coords.first, coords.second, dist)
            if (dist[0] < minDist) {
                minDist = dist[0]
                nearestName = name
            }
        }
        return nearestName
    }
    return String.format(Locale.US, "%.2f°, %.2f°", lat, lng)
}

data class LocationResult(
    val latitude: Double,
    val longitude: Double,
    val city: String,
    val accuracyMeters: Float? = null,
    val capturedAtMillis: Long? = null
)
