package com.example.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import java.util.Locale

private const val TAG = "MosqueDirections"

fun openNearbyMosqueSearch(context: Context, latitude: Double, longitude: Double) {
    if (latitude !in -90.0..90.0 || longitude !in -180.0..180.0) return
    val center = String.format(Locale.US, "%.6f,%.6f", latitude, longitude)
    for (url in listOf("geo:$center?q=cami", "https://www.google.com/maps/search/cami/@$center,14z")) {
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
 *
 * Tried in order, because no single URI scheme is universally supported:
 *  1. `google.navigation:` — Google Maps, and the only one that reliably honours
 *     `dirflg=w` for walking on every Android version.
 *  2. A `geo:` URI — the platform's documented "show on a map" contract, which
 *     lets the user pick from any installed maps app.
 *
 * If neither resolves the app is missing a maps client, which is worth telling the
 * user about rather than failing silently.
 */
fun openWalkingDirections(
    context: Context,
    latitude: Double,
    longitude: Double,
    label: String
) {
    if (latitude !in -90.0..90.0 || longitude !in -180.0..180.0) {
        Toast.makeText(context, "Bu konum için yönlendirme açılamadı.", Toast.LENGTH_SHORT).show()
        return
    }

    val destination = String.format(Locale.US, "%f,%f", latitude, longitude)
    val candidates = listOf(
        Intent(
            Intent.ACTION_VIEW,
            Uri.parse("google.navigation:q=$destination&mode=w")
        ),
        Intent(
            Intent.ACTION_VIEW,
            Uri.parse(
                "geo:$destination?q=${Uri.encode(destination)}(${Uri.encode(label)})"
            )
        ),
        Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$destination&travelmode=walking"))
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
