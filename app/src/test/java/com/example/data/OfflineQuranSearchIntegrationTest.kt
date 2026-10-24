package com.example.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Loads exactly the Android asset/runtime path rather than a test-only index. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [24, 36])
class OfflineQuranSearchIntegrationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @After fun releaseModel() = OfflineQuranSearch.release()

    @Test fun shortTurkishSubjectLoadsBundledModelAndFindsAuthenticAyahs() = runBlocking {
        val results = OfflineQuranSearch.search(context, "hasetlik")
        val references = results.map { "${it.surahId}:${it.verseNumberInt}" }
        assertTrue("Missing envy verses: $references", "113:5" in references && "4:54" in references)
        val felak = results.single { it.surahId == 113 && it.verseNumberInt == 5 }
        assertEquals("Haset ettiği zaman hasetçinin şerrinden.", felak.translation)
        assertTrue(results.all { it.arabic.isNotBlank() && it.translation.isNotBlank() })
        assertTrue(OfflineQuranSearch.search(context, "En iyi ekran kartı hangisi?").isEmpty())
    }
}
