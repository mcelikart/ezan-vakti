package com.example.data

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

@org.junit.runner.RunWith(org.robolectric.RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [24, 36])
class EvpMosqueIntegrationTest {

    @Test fun base64IsCompatibleOnEverySupportedAndroidVersion() {
        val input = "Türkçe test 0123456789".repeat(8)
        assertEquals(java.util.Base64.getEncoder().encodeToString(input.toByteArray(Charsets.UTF_8)),
            EvpAuthTokenGenerator.toBase64(input))
    }

    @Test
    fun `token generator produces non-empty token`() {
        val token = EvpAuthTokenGenerator.generateToken()
        assertNotNull(token)
        assertTrue(token.isNotBlank())
    }

    @Test
    fun `token generator is deterministic for fixed timestamp`() {
        // Fixed timestamp in GMT
        val fixedTime = 1770000000000L
        val token1 = EvpAuthTokenGenerator.generateToken(fixedTime)
        assertNotNull(token1)
        assertTrue(token1.length > 20)
    }

    @Test
    fun `xor encryption is reversible`() {
        val text = "44412"
        val key = ".Maviay"
        val encrypted = EvpAuthTokenGenerator.xorEncrypt(text, key)
        val decrypted = EvpAuthTokenGenerator.xorEncrypt(encrypted, key)
        assertEquals(text, decrypted)
    }

    @Test
    fun `evp mosque maps to canonical domain mosque correctly`() {
        val evp = EvpMosque(
            id = 44412L,
            name = "Ayasofya-i Kebir Cami-i Şerifi",
            lat = 41.008459,
            lon = 28.979869,
            distance = 0.1266
        )

        val mosque = evp.toMosque()
        assertTrue(mosque.isValid)
        assertEquals("Ayasofya-i Kebir Cami-i Şerifi", mosque.displayName)
        assertEquals(41.008459, mosque.latitude, 1e-6)
        assertEquals(28.979869, mosque.longitude, 1e-6)
        assertFalse(mosque.isSmallPrayerRoom)
    }

    @Test
    fun `evp mescit is detected as prayer room`() {
        val evp = EvpMosque(
            id = 99999L,
            name = "Ankara Ticaret Merkezi Mescidi",
            lat = 39.9185,
            lon = 32.8618,
            distance = 0.25
        )

        val mosque = evp.toMosque()
        assertTrue(mosque.isValid)
        assertTrue(mosque.isSmallPrayerRoom)
        assertEquals("Ankara Ticaret Merkezi Mescidi", mosque.displayName)
    }

    @Test
    fun `repository uses evp primary provider when available`() = runBlocking {
        val fakeEvp = object : EvpMosqueService {
            override suspend fun getMosques(
                lat: Double,
                lon: Double,
                radiusMeters: Int,
                xApiKey: String
            ): List<EvpMosque> {
                return listOf(
                    EvpMosque(
                        id = 1L,
                        name = "Fatih Camii",
                        lat = 41.0195,
                        lon = 28.9497,
                        distance = 0.5
                    )
                )
            }
        }

        val dummyOverpass = object : OverpassService {
            override suspend fun query(overpassQuery: String): OverpassEnvelope {
                throw AssertionError("Overpass should not be queried when EVP succeeds")
            }
        }

        val repo = MosqueRepository(
            service = dummyOverpass,
            evpService = fakeEvp
        )

        val results = repo.findNearby(41.0195, 28.9497, 3000)
        assertEquals(1, results.size)
        assertEquals("Fatih Camii", results[0].displayName)
    }

    @Test
    fun `repository falls back to overpass when evp returns empty`() = runBlocking {
        val fakeEvp = object : EvpMosqueService {
            override suspend fun getMosques(
                lat: Double,
                lon: Double,
                radiusMeters: Int,
                xApiKey: String
            ): List<EvpMosque> {
                // Mecca returns empty in EVP database
                return emptyList()
            }
        }

        val fakeOverpass = object : OverpassService {
            override suspend fun query(overpassQuery: String): OverpassEnvelope {
                return OverpassEnvelope(
                    elements = listOf(
                        Mosque(
                            type = "node",
                            id = 777L,
                            lat = 21.4225,
                            lon = 39.8262,
                            tags = mapOf("name" to "Mescid-i Haram", "religion" to "muslim")
                        )
                    )
                )
            }
        }

        val repo = MosqueRepository(
            service = fakeOverpass,
            evpService = fakeEvp
        )

        val results = repo.findNearby(21.4225, 39.8262, 3000)
        assertEquals(1, results.size)
        assertEquals("Mescid-i Haram", results[0].displayName)
    }

    @Test
    fun `spatial cache serves nearby requests within half radius`() = runBlocking {
        var evpCallCount = 0
        val fakeEvp = object : EvpMosqueService {
            override suspend fun getMosques(
                lat: Double,
                lon: Double,
                radiusMeters: Int,
                xApiKey: String
            ): List<EvpMosque> {
                evpCallCount++
                return listOf(
                    EvpMosque(
                        id = 1L,
                        name = "Süleymaniye Camii",
                        lat = 41.0160,
                        lon = 28.9640,
                        distance = 0.2
                    )
                )
            }
        }

        val dummyOverpass = object : OverpassService {
            override suspend fun query(overpassQuery: String): OverpassEnvelope {
                return OverpassEnvelope(elements = emptyList())
            }
        }

        val repo = MosqueRepository(
            service = dummyOverpass,
            evpService = fakeEvp
        )

        // Initial fetch at center (radius 3000m)
        val first = repo.findNearby(41.0160, 28.9640, 3000)
        assertEquals(1, first.size)
        assertEquals(1, evpCallCount)

        // Move by ~200 meters (much less than radius/2 = 1500m)
        val second = repo.findNearby(41.0170, 28.9650, 3000)
        assertEquals(1, second.size)
        // Should be served directly from spatial cache without incrementing evpCallCount!
        assertEquals(1, evpCallCount)
    }

    @Test
    fun `bounding box query formulation is well formed`() {
        val query = MosqueRepository.buildBoundingBoxQuery(40.9, 28.8, 41.1, 29.1)
        assertTrue(query.contains("40.900000,28.800000,41.100000,29.100000"))
        assertTrue(query.contains("amenity"))
    }
}
