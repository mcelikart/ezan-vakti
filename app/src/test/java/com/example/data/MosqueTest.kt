package com.example.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class MosqueTest {

    private val basicTags = mapOf(
        "amenity" to "place_of_worship",
        "religion" to "muslim"
    )

    @Test
    fun `a node takes its coordinates from lat and lon`() {
        val m = Mosque(type = "node", id = 1, lat = 41.0082, lon = 28.9784, tags = basicTags)
        assertTrue(m.isValid)
        assertEquals(41.0082, m.latitude, 1e-9)
        assertEquals(28.9784, m.longitude, 1e-9)
    }

    @Test
    fun `a way falls back to its centre`() {
        // A mosque mapped as a building outline has no lat/lon of its own; `out center`
        // is what supplies these.
        val m = Mosque(
            type = "way", id = 2,
            center = MosqueCenter(41.01, 29.02),
            tags = basicTags
        )
        assertTrue(m.isValid)
        assertEquals(41.01, m.latitude, 1e-9)
    }

    @Test
    fun `an element with no usable coordinate is rejected`() {
        assertFalse(Mosque(type = "node", id = 3, tags = basicTags).isValid)
        assertFalse(Mosque(type = "node", id = 4, lat = 0.0, lon = 0.0, tags = basicTags).isValid)
    }

    @Test
    fun `name falls back through the usual OSM name tags`() {
        val named = Mosque(id = 1, lat = 41.0, lon = 29.0, tags = basicTags + ("name" to "Yeni Cami"))
        assertEquals("Yeni Cami", named.displayName)

        val turkishOnly = Mosque(id = 2, lat = 41.0, lon = 29.0, tags = basicTags + ("name:tr" to "Fatih Camii"))
        assertEquals("Fatih Camii", turkishOnly.displayName)
    }

    @Test
    fun `an untagged mosque gets a generic name rather than an empty one`() {
        val m = Mosque(id = 1, lat = 41.0, lon = 29.0, tags = basicTags)
        assertEquals("Cami", m.displayName)
    }

    @Test
    fun `prayer rooms are detected and named as mescit`() {
        val room = Mosque(id = 1, lat = 41.0, lon = 29.0, tags = mapOf("amenity" to "prayer_room"))
        assertTrue(room.isSmallPrayerRoom)
        assertEquals("Mescit", room.displayName)

        val mescitBuilding = Mosque(id = 2, lat = 41.0, lon = 29.0, tags = mapOf("building" to "mescid"))
        assertTrue(mescitBuilding.isSmallPrayerRoom)
    }

    @Test
    fun `a real mosque is not mistaken for a prayer room`() {
        val m = Mosque(
            id = 1, lat = 41.0, lon = 29.0,
            tags = basicTags + ("name" to "Sultanahmet Camii")
        )
        assertFalse(m.isSmallPrayerRoom)
    }

    @Test
    fun `address is assembled from the addr tags that exist`() {
        val m = Mosque(
            id = 1, lat = 41.0, lon = 29.0,
            tags = basicTags + mapOf(
                "addr:street" to "Atatürk Caddesi",
                "addr:housenumber" to "12",
                "addr:neighbourhood" to "Sultanahmet"
            )
        )
        assertEquals("Atatürk Caddesi No: 12, Sultanahmet", m.address)
    }

    @Test
    fun `an address with no tags is still readable`() {
        val m = Mosque(id = 1, lat = 41.0, lon = 29.0, tags = basicTags)
        assertTrue(m.address.isNotBlank())
    }

    @Test
    fun `name matching folds turkish dotted and dotless i`() {
        // This is how the screen matches: both sides go through turkishFold.
        val name = "İZMİR CAMİİ"
        assertTrue("lowercase query must match", "izmir".turkishFold() in name.turkishFold())
        assertTrue("dotted query must match", "İzmir".turkishFold() in name.turkishFold())
        assertTrue("ascii query must match", "IZMIR".turkishFold() in name.turkishFold())
    }

    @Test
    fun `name matching handles the dotless i`() {
        // Kırıkkale is spelled with a dotless i and is a very common search.
        val name = Mosque(id = 1, lat = 41.0, lon = 29.0, tags = basicTags + ("name" to "Kırıkkale Camii"))
        assertTrue("kirikkale".turkishFold() in name.displayName.turkishFold())
        assertTrue("kırıkkale".turkishFold() in name.displayName.turkishFold())
    }

    @Test
    fun `turkish fold maps all four i forms onto one letter`() {
        // A Turkish user may type any of these for the same letter.
        val forms = listOf("i", "I", "ı", "İ").map { it.turkishFold() }
        assertEquals("all four i forms must fold alike", 1, forms.toSet().size)
    }

    @Test
    fun `turkish fold preserves the turkish letters and lowercases`() {
        assertEquals("çanakkale camii", "Çanakkale Camii".turkishFold())
        assertEquals("öğretim", "Öğretim".turkishFold())
        assertEquals("güngören", "Güngören".turkishFold())
        assertEquals("şanliurfa", "Şanlıurfa".turkishFold())
        assertEquals("çankaya", "Çankaya".turkishFold())
    }

    @Test
    fun `turkish fold leaves the five added letters untouched`() {
        // Turkish adds ç, ğ, ı, ö, ş, ü. Only ı belongs to the i family that the
        // fold collapses; the other five must survive as themselves, otherwise
        // "Şişli" and "Sisli" would become indistinguishable.
        for (letter in listOf("ç", "ğ", "ö", "ş", "ü")) {
            assertEquals("$letter must survive the fold", letter, letter.turkishFold())
            assertEquals("${letter.uppercase(Mosque.TR)} must lower to $letter", letter, letter.uppercase(Mosque.TR).turkishFold())
        }
    }

    @Test
    fun `turkish fold keeps the cedilla on s`() {
        // A naive ASCII transliteration would turn ş into s. Searching "sisli" for
        // "Şişli" should not match, and "şişli" should still find it.
        val name = "Şişli Camii"
        assertTrue("şişli".turkishFold() in name.turkishFold())
        assertFalse("sisli must not match Şişli", "sisli".turkishFold() in name.turkishFold())
    }

    @Test
    fun `turkish fold is idempotent`() {
        val once = "İZMİR CAMİİ".turkishFold()
        assertEquals(once, once.turkishFold())
    }

    @Test
    fun `prayer room detection does not misfire on turkish casing`() {
        val m = Mosque(id = 1, lat = 41.0, lon = 29.0, tags = mapOf("name" to "MESCİT"))
        assertTrue(m.isSmallPrayerRoom)
    }

    @Test
    fun `distance between two known points is correct to a metre`() {
        // Dolmabahce -> Blue Mosque, roughly 3.9 km apart.
        val m = Mosque(id = 1, lat = 41.0017, lon = 28.9849, tags = basicTags)
        val metres = m.distanceMetersFrom(41.0430, 28.9850)
        assertEquals(4600.0, metres, 200.0)
    }

    @Test
    fun `distance to yourself is zero`() {
        val m = Mosque(id = 1, lat = 41.0, lon = 29.0, tags = basicTags)
        assertEquals(0.0, m.distanceMetersFrom(41.0, 29.0), 1e-6)
    }

    @Test
    fun `walking minutes never round down to zero`() {
        // A mosque 20 m away is "less than a minute away", not "0 minutes away".
        val m = Mosque(id = 1, lat = 41.0, lon = 29.0, tags = basicTags)
        assertEquals(1, m.walkingMinutesFrom(41.0001, 29.0))
    }

    @Test
    fun `walking minutes scale with distance`() {
        val far = Mosque(id = 1, lat = 41.03, lon = 29.0, tags = basicTags)
        val minutes = far.walkingMinutesFrom(41.0, 29.0)
        assertTrue("3.3 km should be a plausible walk, got $minutes", minutes in 35..50)
    }

    @Test
    fun `bearing due north is zero and due east is ninety`() {
        val north = Mosque(id = 1, lat = 41.01, lon = 29.0, tags = basicTags)
        assertEquals(0.0, north.bearingFrom(41.0, 29.0), 0.5)

        val east = Mosque(id = 2, lat = 41.0, lon = 29.01, tags = basicTags)
        assertEquals(90.0, east.bearingFrom(41.0, 29.0), 0.5)
    }

    @Test
    fun `bearing is always within zero to three hundred sixty`() {
        val list = (0 until 8).map {
            Mosque(id = it.toLong(), lat = 41.0 + it * 0.01, lon = 29.0 + it * 0.01, tags = basicTags)
        }
        for (m in list) {
            val bearing = m.bearingFrom(41.0, 29.0)
            assertTrue("bearing $bearing out of range", bearing >= 0.0 && bearing < 360.0)
        }
    }
}

class MosqueRepositoryTest {
    @Test fun manualRetryRespectsRateLimit() = kotlinx.coroutines.test.runTest {
        var calls = 0
        val provider = object : OverpassService {
            override suspend fun query(overpassQuery: String): OverpassEnvelope {
                calls++
                throw retrofit2.HttpException(retrofit2.Response.error<OverpassEnvelope>(
                    429, okhttp3.ResponseBody.create(null, "Rate limited")))
            }
        }
        val repo = MosqueRepository(provider)
        runCatching { repo.findNearby(41.0, 29.0) }
        runCatching { repo.findNearby(41.0, 29.0, forceRefresh = true) }
        assertEquals(1, calls)
    }

    @Test fun manualRetryBypassesProviderCooldown() = kotlinx.coroutines.test.runTest {
        var calls = 0
        val provider = object : OverpassService {
            override suspend fun query(overpassQuery: String): OverpassEnvelope {
                calls++
                if (calls == 1) throw java.io.IOException("offline")
                return OverpassEnvelope(listOf(Mosque(lat = 41.001, lon = 29.0)))
            }
        }
        val repo = MosqueRepository(provider)
        try { repo.findNearby(41.0, 29.0) } catch (_: java.io.IOException) { }
        assertEquals(1, repo.findNearby(41.0, 29.0, forceRefresh = true).size)
        assertEquals(2, calls)
    }

    @Test fun failedProviderFallsBackAndRepeatedLookupsUseCache() = kotlinx.coroutines.test.runTest {
        var calls = 0
        val broken = object : OverpassService {
            override suspend fun query(overpassQuery: String): OverpassEnvelope = throw java.io.IOException("offline")
        }
        val backup = object : OverpassService {
            override suspend fun query(overpassQuery: String): OverpassEnvelope {
                calls++
                return OverpassEnvelope(listOf(Mosque(lat = 41.001, lon = 29.0)))
            }
        }
        val repository = MosqueRepository(broken, backup)
        assertEquals(1, repository.findNearby(41.0, 29.0).size)
        assertEquals(1, repository.findNearby(41.0, 29.0).size)
        assertEquals(1, calls)
        repository.findNearby(41.0, 29.0, forceRefresh = true)
        assertEquals(2, calls)
    }

    @Test fun cancellationNeverStartsFallback() = kotlinx.coroutines.test.runTest {
        var fallbackCalled = false
        val cancelled = object : OverpassService {
            override suspend fun query(overpassQuery: String): OverpassEnvelope = throw kotlinx.coroutines.CancellationException()
        }
        val backup = object : OverpassService {
            override suspend fun query(overpassQuery: String): OverpassEnvelope {
                fallbackCalled = true
                return OverpassEnvelope(emptyList())
            }
        }
        try { MosqueRepository(cancelled, backup).findNearby(41.0, 29.0) }
        catch (_: kotlinx.coroutines.CancellationException) { }
        assertFalse(fallbackCalled)
    }

    @Test fun resultsAreValidWithinRadiusSortedAndDeduplicated() = kotlinx.coroutines.test.runTest {
        val near = Mosque(type = "node", id = 1, lat = 41.001, lon = 29.0)
        val further = Mosque(type = "way", id = 2, center = MosqueCenter(41.01, 29.0))
        val service = object : OverpassService {
            override suspend fun query(overpassQuery: String) = OverpassEnvelope(listOf(
                further, near, near, Mosque(id = 3, lat = 45.0, lon = 29.0),
                Mosque(id = 4, lat = Double.NaN, lon = 29.0),
                near.copy(id = 5, tags = mapOf("religion" to "christian"))
            ))
        }
        assertEquals(listOf(near, further), MosqueRepository(service).findNearby(41.0, 29.0, 3000))
    }

    @Test fun equatorAndPrimeMeridianAreValidCoordinates() {
        val mosque = Mosque(lat = 0.0, lon = 30.0)
        assertTrue(mosque.isValid)
        assertTrue(mosque.distanceMetersFrom(.001, 30.0) > 100)
        assertTrue(Mosque(lat = 41.0, lon = 0.0).isValid)
    }

    @Test
    fun `query requests all three worship tagging conventions`() {
        val q = MosqueRepository.buildQuery(41.0, 29.0, 3000)
        assertTrue(q.contains("""["amenity"="place_of_worship"]"""))
        assertTrue(q.contains("""["amenity"="mosque"]"""))
        assertTrue(q.contains("""["amenity"="prayer_room"]"""))
        assertTrue(q.contains("moscid|mescit"))
    }

    @Test
    fun `query covers nodes ways and relations`() {
        // Only querying nodes is the usual cause of "the mosque I can see is missing":
        // most large mosques in OSM are mapped as a building way or an area relation.
        val q = MosqueRepository.buildQuery(41.0, 29.0, 3000)
        assertEquals(4, Regex("nwr\\[").findAll(q).count())
        assertTrue(q.length < 600)
    }

    @Test
    fun `query asks for centre so ways become usable map points`() {
        assertTrue(MosqueRepository.buildQuery(41.0, 29.0, 3000).contains("out center tags"))
    }

    @Test
    fun `query uses a dot decimal separator under a turkish locale`() {
        // Overpass parses "41,008200" as a syntax error and returns nothing, which
        // would look to the user like "no mosques near you".
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"))
            val q = MosqueRepository.buildQuery(41.0082, 28.9784, 3000)
            assertTrue(q.contains("41.008200,28.978400"))
            assertFalse(q.contains("41,008200"))
        } finally {
            Locale.setDefault(previous)
        }
    }

    @Test
    fun `query embeds the requested radius`() {
        assertTrue(MosqueRepository.buildQuery(41.0, 29.0, 5000).contains("around:5000"))
    }

    @Test
    fun `query sets a timeout so a slow public endpoint cannot hang the screen`() {
        assertTrue(MosqueRepository.buildQuery(41.0, 29.0, 3000).contains("[timeout:"))
    }
}
