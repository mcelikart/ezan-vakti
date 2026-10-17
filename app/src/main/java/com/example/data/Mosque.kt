package com.example.data

import com.squareup.moshi.JsonClass
import java.util.Locale
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * A single place of worship returned by the Overpass API.
 *
 * `lat`/`lon` are only present on nodes; ways and relations carry a `center`
 * instead, which is why every accessor goes through [latitude]/[longitude].
 */
@JsonClass(generateAdapter = true)
data class Mosque(
    val type: String? = null,
    val id: Long? = null,
    val lat: Double? = null,
    val lon: Double? = null,
    val center: MosqueCenter? = null,
    val tags: Map<String, String>? = null
) {
    val latitude: Double get() = lat ?: center?.lat ?: 0.0
    val longitude: Double get() = lon ?: center?.lon ?: 0.0

    val isValid: Boolean get() = (lat != null && lon != null || center != null) &&
        latitude in -90.0..90.0 && longitude in -180.0..180.0 &&
        !(latitude == 0.0 && longitude == 0.0)

    private fun tag(vararg keys: String): String =
        keys.firstNotNullOfOrNull { tags?.get(it) }?.trim().orEmpty()

    /** A small prayer room, distinguished from a full mosque. */
    val isSmallPrayerRoom: Boolean
        get() {
            val t = tags.orEmpty()
            val text = listOf("name", "amenity", "building", "description", "note")
                .mapNotNull { t[it] }.joinToString(" ").lowercase(TR)
            return t["amenity"] == "prayer_room" ||
                t["building"] in setOf("moscid", "mescit") ||
                listOf("mescid", "mescit", "musalla", "prayer room", "prayer_room")
                    .any { text.contains(it) }
        }

    val displayName: String
        get() = tag("name", "name:tr", "name:en", "official_name").ifBlank {
            if (isSmallPrayerRoom) "Mescit" else "Cami"
        }

    val address: String
        get() {
            val t = tags.orEmpty()
            val street = listOfNotNull(
                t["addr:street"]?.takeIf { it.isNotBlank() },
                t["addr:housenumber"]?.takeIf { it.isNotBlank() }
            ).joinToString(" No: ")
            val parts = listOfNotNull(
                street.takeIf { it.isNotBlank() },
                listOfNotNull(t["addr:neighbourhood"], t["addr:suburb"], t["addr:city"], t["addr:province"])
                    .firstOrNull { !it.isNullOrBlank() }
            )
            return parts.joinToString(", ").ifBlank {
                if (isSmallPrayerRoom) "Yakınlarda mescit" else "Yakınlarda cami"
            }
        }

    fun distanceMetersFrom(originLat: Double, originLon: Double): Double {
        if (!isValid) return 0.0
        return haversineMeters(originLat, originLon, latitude, longitude)
    }

    /** Compass bearing to this mosque, in degrees clockwise from north. */
    fun bearingFrom(originLat: Double, originLon: Double): Double {
        if (!isValid) return 0.0
        val phi1 = Math.toRadians(originLat)
        val phi2 = Math.toRadians(latitude)
        val dLambda = Math.toRadians(longitude - originLon)
        val y = sin(dLambda) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(dLambda)
        val deg = Math.toDegrees(atan2(y, x))
        return if (deg < 0) deg + 360.0 else deg
    }

    /** Rough walking estimate at 4.8 km/h. */
    fun walkingMinutesFrom(originLat: Double, originLon: Double): Int {
        val meters = distanceMetersFrom(originLat, originLon)
        if (meters <= 0) return 1
        return maxOf(1, Math.round(meters / 80.0).toInt())
    }

    companion object {
        /**
         * Turkish casing rules: `I` lowercases to `ı` and `İ` lowercases to `i`
         * plus a combining dot. Locale.forLanguageTag is used because the
         * Locale(String, String) constructor is deprecated from Java 19 onward and
         * Locale.of is not available at minSdk 24.
         */
        val TR: Locale = Locale.forLanguageTag("tr-TR")
    }
}

/**
 * Case folds a string for Turkish text search.
 *
 * `String.contains(…, ignoreCase = true)` cannot do this job. Java folds with
 * Unicode's default rules, where `İ` (U+0130) and `I` are not variants of `i`,
 * so a search for "izmir" misses "İzmir Camii" and a search for "kirmizi" misses
 * "Kırmızı". Turkish needs `i`/`İ` and `ı`/`I` treated as two letters, and both
 * halves of each pair must land on a single canonical form.
 *
 * The two-step fold is what makes that work: lowercasing in a Turkish locale
 * turns `İ` into `i` plus a combining dot above, and stripping that dot is what
 * makes the dotted and dotless spellings comparable.
 */
fun String.turkishFold(): String =
    lowercase(Mosque.TR)
        .replace("i̇", "i")   // "i" + U+0307 COMBINING DOT ABOVE
        .replace('ı', 'i')   // dotless i shares the canonical form with i

@JsonClass(generateAdapter = true)
data class MosqueCenter(val lat: Double, val lon: Double)

@JsonClass(generateAdapter = true)
data class OverpassEnvelope(val elements: List<Mosque>? = null, val remark: String? = null)

internal fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val earthRadius = 6371000.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).pow(2) +
        cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
    return 2 * earthRadius * asin(sqrt(a).coerceIn(0.0, 1.0))
}
