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

/** One bounded, cancellable foreground fix. Never uses the selected prayer city. */
class DeviceLocationSource(context: Context) {
    private val manager = context.getSystemService(LocationManager::class.java)

    @SuppressLint("MissingPermission")
    suspend fun current(): LocationResult? {
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .filter { runCatching { manager.isProviderEnabled(it) }.getOrDefault(false) }
        if (providers.isEmpty()) return null
        var best: Location? = providers.mapNotNull { runCatching { manager.getLastKnownLocation(it) }.getOrNull() }
            .filter { SystemClock.elapsedRealtimeNanos() - it.elapsedRealtimeNanos in 0..60_000_000_000L }
            .filter { it.hasAccuracy() && it.accuracy <= 100f }
            .minByOrNull { it.accuracy }
        val fresh = withTimeoutOrNull(15_000) {
            suspendCancellableCoroutine<Location?> { continuation ->
                val listener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        if (!location.hasAccuracy()) return
                        if (best == null || location.accuracy < best!!.accuracy) best = location
                        if (location.accuracy <= 50f && continuation.isActive) {
                            manager.removeUpdates(this)
                            continuation.resume(location)
                        }
                    }
                    override fun onProviderEnabled(provider: String) = Unit
                    override fun onProviderDisabled(provider: String) = Unit
                    @Deprecated("Legacy location callback")
                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
                }
                var registered = false
                providers.forEach { provider ->
                    if (runCatching { manager.requestLocationUpdates(provider, 1_000L, 0f, listener, Looper.getMainLooper()) }.isSuccess) registered = true
                }
                continuation.invokeOnCancellation { manager.removeUpdates(listener) }
                if (!registered) continuation.resume(null)
            }
        }
        val fix = fresh ?: best ?: return null
        return LocationResult(fix.latitude, fix.longitude, "Konumunuz", fix.accuracy, fix.time)
    }
}
