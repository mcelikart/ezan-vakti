package com.example.data

import com.squareup.moshi.JsonClass
import java.util.Calendar
import java.util.Random
import java.util.TimeZone

/**
 * Data model for mosques retrieved from Ezan Vakti Pro's curated content service:
 * `https://service-content.ezanvaktipro.com/v3.0/mosque/list`
 */
@JsonClass(generateAdapter = true)
data class EvpMosque(
    val id: Long? = null,
    val name: String? = null,
    val lat: Double? = null,
    val lon: Double? = null,
    val distance: Double? = null
) {
    /**
     * Converts this Ezan Vakti Pro mosque into our canonical domain [Mosque].
     */
    fun toMosque(): Mosque {
        val cleanName = name.orEmpty().trim()
        val isMescit = cleanName.lowercase(Mosque.TR).contains("mescit") ||
            cleanName.lowercase(Mosque.TR).contains("mescid") ||
            cleanName.lowercase(Mosque.TR).contains("prayer room")

        return Mosque(
            type = "node",
            id = id,
            lat = lat,
            lon = lon,
            name = cleanName,
            distance = distance,
            tags = mapOf(
                "amenity" to if (isMescit) "prayer_room" else "place_of_worship",
                "religion" to "muslim",
                "name" to cleanName
            )
        )
    }
}

/**
 * Dynamic HMAC/XOR time-seeded token generator reverse-engineered from Ezan Vakti Pro (`kr.r0`).
 *
 * It produces valid `xApiKey` authentication headers matching the ASP.NET verification
 * logic of `service-content.ezanvaktipro.com`.
 */
object EvpAuthTokenGenerator {
    private const val KEY_A = "Maviay"
    private const val KEY_C = "."
    private const val KEY_D = "-"

    fun xorEncrypt(str: String, key: String): String {
        val chars = key.toCharArray()
        val sb = StringBuilder(str.length)
        for (i in str.indices) {
            val k = chars[i % chars.size].code
            val c = str[i].code
            sb.append((c xor k).toChar())
        }
        return sb.toString()
    }

    internal fun toBase64(str: String): String {
        val bytes = str.toByteArray(Charsets.UTF_8)
        return android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
    }

    /**
     * Generates a time-seeded authentication token.
     * @param timestampMillis GMT timestamp in milliseconds, or 0 to use current time.
     */
    fun generateToken(timestampMillis: Long = 0L): String {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"))
        if (timestampMillis > 0) {
            calendar.timeInMillis = timestampMillis
        }
        val rand = Random()
        val r11 = rand.nextInt(9) + 1
        val r18 = rand.nextInt(9) + 1
        val r21 = rand.nextInt(9) + 1

        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val second = calendar.get(Calendar.SECOND)

        val r19 = (hour + 24) * year * day * month
        val calcMin = (minute + 60) * hour * day
        val calcSec = (second + 60) * minute * hour

        val part1 = toBase64(xorEncrypt("$year", "$KEY_C$KEY_A"))
        val part2 = toBase64(xorEncrypt("$month", "$KEY_C$KEY_A"))
        val part3 = toBase64(xorEncrypt("$day", "$KEY_C$KEY_A"))
        val part4 = "$r11" + toBase64(xorEncrypt("$r19", "$KEY_C$KEY_A 41 $r11"))
        val part5 = "$r18" + toBase64(xorEncrypt("$calcMin", "$KEY_C$KEY_A 41 $r11"))
        val part6 = "$r21" + toBase64(xorEncrypt("$calcSec", "$KEY_C$KEY_A 41 $r11"))

        val joined = "$part1$KEY_D$part2$KEY_D$part3$KEY_D$part4$KEY_D$part5$KEY_D$part6"
        return toBase64(xorEncrypt(joined, "$KEY_A$KEY_C 41"))
    }
}
