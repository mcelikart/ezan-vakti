package com.example.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import com.example.data.Mosque
import java.util.Locale

private const val TAG = "MosqueDirections"

fun openNearbyMosqueSearch(context: Context, latitude: Double, longitude: Double) {
    if (latitude !in -90.0..90.0 || longitude !in -180.0..180.0) return
    val center = String.format(Locale.US, "%.6f,%.6f", latitude, longitude)
    for (url in listOf("geo:$center?q=cami", "https://www.google.com/maps/search/cami/@$center,15z")) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return
        } catch (_: ActivityNotFoundException) {
            // Try the browser when no maps application handles geo URIs.
        }
    }
    Toast.makeText(context, "Harita açabilecek bir uygulama bulunamadı.", Toast.LENGTH_SHORT).show()
}

/**
 * Opens walking directions to a mosque in whatever navigation app is installed.
 */
fun openWalkingDirections(
    context: Context,
    latitude: Double,
    longitude: Double,
    label: String
) {
    openDirectionsWithMode(context, latitude, longitude, label, walking = true)
}

/**
 * Opens driving directions to a mosque in whatever navigation app is installed.
 */
fun openDrivingDirections(
    context: Context,
    latitude: Double,
    longitude: Double,
    label: String
) {
    openDirectionsWithMode(context, latitude, longitude, label, walking = false)
}

private fun openDirectionsWithMode(
    context: Context,
    latitude: Double,
    longitude: Double,
    label: String,
    walking: Boolean
) {
    if (latitude !in -90.0..90.0 || longitude !in -180.0..180.0) {
        Toast.makeText(context, "Bu konum için yönlendirme açılamadı.", Toast.LENGTH_SHORT).show()
        return
    }

    val destination = String.format(Locale.US, "%.6f,%.6f", latitude, longitude)
    val navMode = if (walking) "w" else "d"
    val webMode = if (walking) "walking" else "driving"
    val candidates = listOf(
        Intent(
            Intent.ACTION_VIEW,
            Uri.parse("google.navigation:q=$destination&mode=$navMode")
        ),
        Intent(
            Intent.ACTION_VIEW,
            Uri.parse("geo:$destination?q=${Uri.encode(destination)}(${Uri.encode(label)})")
        ),
        Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$destination&travelmode=$webMode")
        )
    )

    for (intent in candidates) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
            return
        } catch (e: ActivityNotFoundException) {
            Log.d(TAG, "No handler for ${intent.data?.scheme}", e)
        }
    }

    Toast.makeText(
        context,
        "Yönlendirme için bir harita uygulaması bulunamadı.",
        Toast.LENGTH_LONG
    ).show()
}

/**
 * Shares the mosque name, address, distance, and Google Maps link.
 */
fun shareMosqueLocation(
    context: Context,
    mosque: Mosque,
    distanceLabel: String
) {
    val coords = String.format(Locale.US, "%.6f,%.6f", mosque.latitude, mosque.longitude)
    val mapsUrl = "https://www.google.com/maps/search/?api=1&query=$coords"
    val shareText = buildString {
        append("🕌 ${mosque.displayName}\n")
        if (mosque.address.isNotBlank()) append("📍 ${mosque.address}\n")
        append("📏 Mesafe: $distanceLabel\n")
        append("🗺️ Harita: $mapsUrl")
    }
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, shareText)
    }
    runCatching {
        context.startActivity(Intent.createChooser(sendIntent, "Cami Konumunu Paylaş").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
