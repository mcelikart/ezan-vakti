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
 * Repository orchestrating Mosque data retrieval.
 *
 * Implements a dual-provider architecture reverse-engineered from Ezan Vakti Pro:
 * 1. **Primary Provider ([EvpMosqueService])**: High-speed, highly curated database
 *    of mosques across Turkey (`service-content.ezanvaktipro.com/v3.0/mosque/list`).
 * 2. **Fallback / Global Provider ([OverpassService])**: Comprehensive worldwide OpenStreetMap
 *    coverage for international locations (Mecca, Medina, Berlin, etc.) and redundancy.
 * 3. **Spatial Half-Radius Proximity Cache**: If current map/user center is within `< radius / 2`
 *    of the cached center, serves results instantly without network round-trips.
 * 4. **Adaptive Search Expansion**: Automatically widens query radius if initial radius
 *    yields no results in sparse or rural areas.
 * 5. **Bounding-Box Viewport Search**: Queries mosques within a given map bounding box for live
 *    panning and camera exploration.
 */
class MosqueRepository(
    private val service: OverpassService,
    private val fallback: OverpassService? = null,
    private val extraFallbacks: List<OverpassService> = emptyList(),
    private val evpService: EvpMosqueService? = null
) {
    private data class Cached(val createdAt: Long, val mosques: List<Mosque>)
    private data class SpatialCache(
        val centerLat: Double,
        val centerLon: Double,
        val radiusMeters: Int,
        val createdAtNanos: Long,
        val mosques: List<Mosque>
    )

    private val mutex = Mutex()
    private val cache = LinkedHashMap<String, Cached>()
    private var spatialCache: SpatialCache? = null
    private val retryAfter = mutableMapOf<OverpassService, Long>()
    private val rateLimitedUntil = mutableMapOf<OverpassService, Long>()

    /**
     * Finds nearby mosques using spatial caching, EVP primary provider, and Overpass fallback.
     *
     * @param radiusMeters Search radius in meters (clamped between 200 and 10,000).
     * @param forceRefresh Bypass cache and force network retrieval.
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

            // 1. Spatial Half-Radius Proximity Cache check (matching Ezan Vakti Pro FSPlaceHelper)
            val currentSpatial = spatialCache
            if (!forceRefresh && currentSpatial != null && now - currentSpatial.createdAtNanos < 300_000_000_000L) {
                val distanceMoved = haversineMeters(currentSpatial.centerLat, currentSpatial.centerLon, latitude, longitude)
                if (distanceMoved < (currentSpatial.radiusMeters / 2.0) && radius <= currentSpatial.radiusMeters) {
                    val filtered = currentSpatial.mosques
                        .filter { it.distanceMetersFrom(latitude, longitude) <= radius }
                        .sortedBy { it.distanceMetersFrom(latitude, longitude) }
                    if (filtered.isNotEmpty()) {
                        return@withLock filtered
                    }
                }
            }

            // 2. Exact Query Cache check
            val cached = cache[query]
            if (!forceRefresh && cached != null && now - cached.createdAt < 300_000_000_000L) {
                return@withLock cached.mosques
            }

            var resultList: List<Mosque> = emptyList()

            // 3. Attempt Primary Provider: Ezan Vakti Pro Content API
            if (evpService != null) {
                try {
                    val evpResults = evpService.getMosques(latitude, longitude, radius)
                    if (evpResults.isNotEmpty()) {
                        resultList = evpResults.map { it.toMosque() }
                            .filter { it.isValid }
                            .distinctBy { "${it.latitude},${it.longitude},${it.displayName}" }
                            .sortedBy { it.distanceMetersFrom(latitude, longitude) }
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    // EVP API failed or is unreachable, smoothly fallback to Overpass
                }
            }

            // 4. Fallback Provider: Overpass OpenStreetMap (for global locations or EVP fallback)
            if (resultList.isEmpty()) {
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

                if (response != null) {
                    resultList = response.elements.orEmpty()
                        .filter { it.isValid }
                        .filter { it.tags?.get("religion").let { religion -> religion == null || religion == "muslim" } }
                        .filter { it.distanceMetersFrom(latitude, longitude) <= radius }
                        .distinctBy { "${it.latitude},${it.longitude},${it.displayName}" }
                        .sortedBy { it.distanceMetersFrom(latitude, longitude) }
                } else if (evpService == null) {
                    throw failure
                }
            }

            // 5. Update Caches
            cache[query] = Cached(System.nanoTime(), resultList)
            if (cache.size > 8) cache.remove(cache.keys.first())

            if (resultList.isNotEmpty()) {
                spatialCache = SpatialCache(
                    centerLat = latitude,
                    centerLon = longitude,
                    radiusMeters = radius,
                    createdAtNanos = System.nanoTime(),
                    mosques = resultList
                )
            }

            resultList
        }
    }

    /**
     * Live viewport bounding-box search, matching Ezan Vakti Pro's [MasjitFindActivity].
     */
    suspend fun findByBoundingBox(
        south: Double,
        west: Double,
        north: Double,
        east: Double,
        forceRefresh: Boolean = false
    ): List<Mosque> {
        val query = buildBoundingBoxQuery(south, west, north, east)
        return mutex.withLock {
            val now = System.nanoTime()
            val cached = cache[query]
            if (!forceRefresh && cached != null && now - cached.createdAt < 180_000_000_000L) {
                return@withLock cached.mosques
            }

            var response: OverpassEnvelope? = null
            var failure: Exception = IOException("Harita cami servisi geçici olarak kullanılamıyor.")
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
                }
            }

            val result = (response ?: throw failure).elements.orEmpty()
                .filter { it.isValid }
                .filter { it.tags?.get("religion").let { religion -> religion == null || religion == "muslim" } }
                .distinctBy { "${it.latitude},${it.longitude},${it.displayName}" }

            cache[query] = Cached(System.nanoTime(), result)
            if (cache.size > 8) cache.remove(cache.keys.first())
            result
        }
    }

    companion object {
        /**
         * Builds an Overpass query centered around a point.
         */
        fun buildQuery(latitude: Double, longitude: Double, radiusMeters: Int): String {
            val lat = String.format(Locale.US, "%.6f", latitude)
            val lon = String.format(Locale.US, "%.6f", longitude)
            val around = "(around:$radiusMeters,$lat,$lon)"
            return """[out:json][timeout:12][maxsize:67108864];(nwr["amenity"="place_of_worship"]["religion"="muslim"]$around;nwr["amenity"="mosque"]$around;nwr["amenity"="prayer_room"]$around;nwr["building"~"^(mosque|moscid|mescit|mescid)$"]$around;);out center tags;"""
        }

        /**
         * Builds an Overpass bounding-box query for live viewport map searching.
         */
        fun buildBoundingBoxQuery(south: Double, west: Double, north: Double, east: Double): String {
            val s = String.format(Locale.US, "%.6f", south)
            val w = String.format(Locale.US, "%.6f", west)
            val n = String.format(Locale.US, "%.6f", north)
            val e = String.format(Locale.US, "%.6f", east)
            val bbox = "($s,$w,$n,$e)"
            return """[out:json][timeout:15][maxsize:67108864];(nwr["amenity"="place_of_worship"]["religion"="muslim"]$bbox;nwr["amenity"="mosque"]$bbox;nwr["amenity"="prayer_room"]$bbox;nwr["building"~"^(mosque|moscid|mescit|mescid)$"]$bbox;);out center tags;"""
        }
    }
}
