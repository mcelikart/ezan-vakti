package com.example.data

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Explicit opt-in: public services must not make the normal suite flaky. */
class MosqueLiveServiceTest {
    @Test fun realRetrofitQueryDecodesAnkaraMosques() = runBlocking {
        assumeTrue(System.getenv("EZAN_LIVE_NETWORK") == "1")
        val result = RetrofitClient.mosqueRepository.findNearby(39.8, 32.55, 3000, forceRefresh = true)
        assertTrue("No real mosque decoded", result.isNotEmpty())
        assertTrue(result.all { it.isValid && it.distanceMetersFrom(39.8, 32.55) <= 3000 })
        assertTrue(result.any { it.displayName.contains("Cam") })
        println("Live Ankara search: ${result.size} places, nearest=${result.first().displayName}")
    }
}
