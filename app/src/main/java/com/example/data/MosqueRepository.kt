package com.example.data

import retrofit2.http.GET
import retrofit2.http.Query
import java.util.Locale
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface OverpassService {
    @GET("interpreter")
    suspend fun query(@Query("data") overpassQuery: String): OverpassEnvelope
}

/**
 * Fetches places of worship from OpenStreetMap via the Overpass API.
 *
 * The query covers the three tagging conventions that actually appear in OSM for
 * Muslim places of worship, and returns element centres so ways and relations
 * become usable as map points.
 */
class MosqueRepository(
    private val service: OverpassService,
    private val fallback: OverpassService? = null,
    private val extraFallbacks: List<OverpassService> = emptyList()
) {
    private data class Cached(val createdAt: Long, val mosques: List<Mosque>)
    private val mutex = Mutex()
    private val cache = LinkedHashMap<String, Cached>()
    private val retryAfter = mutableMapOf<OverpassService, Long>()
    private val rateLimitedUntil = mutableMapOf<OverpassService, Long>()

    /**
     * @param radiusMeters Overpass' `around` is a straight-line radius in metres.
     *   The value is clamped because very large radii time the public endpoint.
     */
    suspend fun findNearby(
        latitude: Double,
        longitude: Double,
        radiusMeters: Int = 3000,
        forceRefresh: Boolean = false
    ): List<Mosque> {
        require(latitude in -90.0..90.0 && longitude in -180.0..180.0) { "Invalid coordinates" }
        val radius = radiusMeters.coerceIn(200, 10_000)
        val query = buildQuery(latitude, longitude, radius)
        return mutex.withLock {
            val now = System.nanoTime()
            val cached = cache[query]
            if (!forceRefresh && cached != null && now - cached.createdAt < 300_000_000_000L) return@withLock cached.mosques
            var response: OverpassEnvelope? = null
            var failure: Exception = IOException("Cami servisi geçici olarak kullanılamıyor.")
            val providers = listOfNotNull(service, fallback) + extraFallbacks
            for (provider in providers) {
                if (now < (rateLimitedUntil[provider] ?: 0L)) continue
                if (!forceRefresh && now < (retryAfter[provider] ?: 0L)) continue
                try {
                    val result = provider.query(query)
                    if (result.elements == null || !result.remark.isNullOrBlank()) throw IOException("Incomplete Overpass response")
                    response = result
                    break
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    failure = error
                    retryAfter[provider] = System.nanoTime() + 30_000_000_000L
                    if (error is retrofit2.HttpException && error.code() in listOf(429, 406)) {
                        rateLimitedUntil[provider] = retryAfter.getValue(provider)
                    }
                }
            }
            val result = (response ?: throw failure).elements.orEmpty()
                .filter { it.isValid }
                .filter { it.tags?.get("religion").let { religion -> religion == null || religion == "muslim" } }
                .filter { it.distanceMetersFrom(latitude, longitude) <= radius }
                .distinctBy { "${it.latitude},${it.longitude},${it.displayName}" }
                .sortedBy { it.distanceMetersFrom(latitude, longitude) }
            cache[query] = Cached(System.nanoTime(), result)
            if (cache.size > 8) cache.remove(cache.keys.first())
            result
        }
    }

    companion object {
        /**
         * Kept as a function of its inputs so the query is unit-testable without a
         * network call. `out center` is what makes ways/relations usable.
         */
        fun buildQuery(latitude: Double, longitude: Double, radiusMeters: Int): String {
            val lat = String.format(Locale.US, "%.6f", latitude)
            val lon = String.format(Locale.US, "%.6f", longitude)
            val around = "(around:$radiusMeters,$lat,$lon)"
            // nwr applies each filter to nodes, ways and relations in one statement.
            return """[out:json][timeout:12][maxsize:67108864];(nwr["amenity"="place_of_worship"]["religion"="muslim"]$around;nwr["amenity"="mosque"]$around;nwr["amenity"="prayer_room"]$around;nwr["building"~"^(mosque|moscid|mescit|mescid)$"]$around;);out center tags;"""
        }
    }
}
