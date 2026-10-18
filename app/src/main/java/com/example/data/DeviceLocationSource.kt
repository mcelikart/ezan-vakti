package com.example.data

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.os.SystemClock
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Fast, resilient foreground location fix for the Nearest Mosque module.
 * Uses recent cached locations immediately when accurate enough, and requests
 * a bounded live update from GPS/Network providers without hanging for 15s indoors.
 */
class DeviceLocationSource(private val context: Context) {
    private val manager = context.getSystemService(LocationManager::class.java)

    @SuppressLint("MissingPermission")
    suspend fun current(): LocationResult? {
        val allProviders = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        )
        val enabledProviders = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER
        ).filter { runCatching { manager.isProviderEnabled(it) }.getOrDefault(false) }

        val nowElapsedNanos = SystemClock.elapsedRealtimeNanos()

        // 1. Check all providers (including passive) for a usable cached location up to 60 minutes old
        val cachedCandidates = allProviders.mapNotNull { provider ->
            runCatching { manager.getLastKnownLocation(provider) }.getOrNull()
        }.filter { loc ->
            loc.latitude in -90.0..90.0 && loc.longitude in -180.0..180.0 &&
                !(loc.latitude == 0.0 && loc.longitude == 0.0)
        }

        var best: Location? = cachedCandidates
            .filter { nowElapsedNanos - it.elapsedRealtimeNanos in 0..3_600_000_000_000L }
            .filter { !it.hasAccuracy() || it.accuracy <= 2500f }
            .minByOrNull { if (it.hasAccuracy()) it.accuracy else 800f }
            ?: cachedCandidates.minByOrNull { if (it.hasAccuracy()) it.accuracy else 1500f }

        // If we already have a very freshcached location (< 90s old and <= 150m accuracy), return immediately
        if (best != null &&
            nowElapsedNanos - best!!.elapsedRealtimeNanos in 0..90_000_000_000L &&
            (!best!!.hasAccuracy() || best!!.accuracy <= 150f)
        ) {
            return toLocationResult(best!!)
        }

        // 2. If active providers are enabled, wait up to 4.5s (if we have a fallback) or 7s for a live fix
        if (enabledProviders.isNotEmpty()) {
            val timeoutMs = if (best != null) 4_000L else 7_000L
            val fresh = withTimeoutOrNull(timeoutMs) {
                suspendCancellableCoroutine<Location?> { continuation ->
                    val listener = object : LocationListener {
                        override fun onLocationChanged(location: Location) {
                            if (location.latitude == 0.0 && location.longitude == 0.0) return
                            val acc = if (location.hasAccuracy()) location.accuracy else 200f
                            val bestAcc = best?.let { if (it.hasAccuracy()) it.accuracy else 500f } ?: Float.MAX_VALUE
                            if (best == null || acc <= bestAcc + 50f) {
                                best = location
                            }
                            if (acc <= 200f && continuation.isActive) {
                                runCatching { manager.removeUpdates(this) }
                                continuation.resume(location)
                            }
                        }
                        override fun onProviderEnabled(provider: String) = Unit
                        override fun onProviderDisabled(provider: String) = Unit
                        @Deprecated("Legacy location callback")
                        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
                    }
                    var registered = false
                    enabledProviders.forEach { provider ->
                        if (runCatching {
                                manager.requestLocationUpdates(provider, 800L, 0f, listener, Looper.getMainLooper())
                            }.isSuccess
                        ) {
                            registered = true
                        }
                    }
                    continuation.invokeOnCancellation {
                        runCatching { manager.removeUpdates(listener) }
                    }
                    if (!registered && continuation.isActive) {
                        continuation.resume(null)
                    }
                }
            }
            val resolved = fresh ?: best
            if (resolved != null) {
                return toLocationResult(resolved)
            }
        }

        return best?.let { toLocationResult(it) }
    }

    private fun toLocationResult(fix: Location): LocationResult {
        val acc = if (fix.hasAccuracy()) fix.accuracy else null
        return LocationResult(fix.latitude, fix.longitude, "Mevcut Konumunuz", acc, fix.time)
    }
}
