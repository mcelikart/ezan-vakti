package com.example.ui

import com.example.ui.audio.MealSpeechEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Guards the Turkish meal narration pipeline.
 *
 * The regression these tests exist for: meal editions include `ar.alafasy`
 * ("Arapça Orijinal Metin"), which lives in the same map as the translations.
 * Taking the first non-blank entry in the user's edition order fed Arabic script
 * to the Turkish voice, which read it back as unintelligible Latin gibberish.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MealSpeechTextTest {

    private fun verse(meals: Map<String, String>, diyanet: String = "") = SurahVerse(
        number = 1,
        arabic = "بِسْمِ اللَّهِ",
        pronunciation = "Bismillah",
        diyanetMeal = diyanet,
        elmaliliMeal = "",
        vakfiMeal = "",
        diyanetTefsir = "",
        bilmenTefsir = "",
        selectedMeal = diyanet,
        mealsMap = meals
    )

    @Test
    fun arabicEditionIsNeverSpokenByTheTurkishEngine() {
        val v = verse(
            meals = mapOf(
                "ar.alafasy" to "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                "tr.diyanet" to "Bismillahirrahmanirrahim."
            ),
            diyanet = "Bismillahirrahmanirrahim."
        )
        val text = QuranAudioSupport.mealSpeechText(v, listOf("ar.alafasy", "tr.diyanet"))
        assertEquals("Bismillahirrahmanirrahim.", text)
        assertTrue(QuranAudioSupport.isSpeakable(text))
    }

    @Test
    fun arabicFirstInEditionOrderStillYieldsTurkish() {
        val v = verse(
            meals = mapOf("ar.alafasy" to "وَإِنَّكَ لَعَلَىٰ خُلُقٍ عَظِيمٍ"),
            diyanet = "Ve innekene leala khuluqin azim."
        )
        val text = QuranAudioSupport.mealSpeechText(v, listOf("ar.alafasy"))
        assertEquals("Ve innekene leala khuluqin azim.", text)
    }

    @Test
    fun arabicIsRejectedEvenWhenKeyedAsTurkish() {
        val v = verse(meals = mapOf("tr.diyanet" to "بسم الله"), diyanet = "بسم الله")
        assertEquals("", QuranAudioSupport.mealSpeechText(v, listOf("tr.diyanet")))
    }

    @Test
    fun recitationAbbreviationsAreExpanded() {
        val v = verse(
            meals = mapOf("tr.diyanet" to "Peygamberimiz (s.a.v.) buyurdu (bk. 12) ki: (Hz.) merhaba."),
            diyanet = ""
        )
        val text = QuranAudioSupport.mealSpeechText(v, listOf("tr.diyanet"))
        assertTrue("abbreviation not expanded: $text", text.contains("sallallahu aleyhi ve sellem"))
        assertTrue("parenthetical not removed: $text", !text.contains("(bk."))
        assertTrue("number bracket not removed: $text", !text.contains("(12)"))
    }

    @Test
    fun textAlwaysEndsWithTerminalPunctuation() {
        val v = verse(meals = mapOf("tr.diyanet" to "Hamd Allah'a"), diyanet = "")
        val text = QuranAudioSupport.mealSpeechText(v, listOf("tr.diyanet"))
        assertTrue("missing terminal punctuation: $text", text.endsWith("."))
    }

    @Test
    fun spokenTextIsAlwaysLatinProse() {
        val meals = mapOf(
            "tr.diyanet" to "İnnama yemû'l-muttakîne. (bk. 3) Rabbena âti-na (s.a.v.) fî'd-dunyâ hasanah.",
            "ar.alafasy" to "إِنَّمَا الْمُتَّقُونَ"
        )
        val text = QuranAudioSupport.mealSpeechText(
            verse(meals = meals, diyanet = meals.getValue("tr.diyanet")),
            listOf("ar.alafasy", "tr.diyanet")
        )
        assertTrue("not speakable: $text", QuranAudioSupport.isSpeakable(text))
        assertTrue("contains Arabic: $text", text.none { it.code in 0x0600..0x06FF })
    }

    @Test
    fun chunkingNeverMergesTwoSentences() {
        val text = "Birinci cümle burada. İkinci cümle burada. Üçüncü cümle burada. Dördüncü cümle."
        val chunks = MealSpeechEngine.splitIntoChunks(text, maxChars = 30)
        assertTrue(chunks.size >= 4)
        // Every chunk must end on terminal punctuation, otherwise the engine has
        // no boundary to drop its intonation at and the reading sounds flat.
        chunks.forEach { assertTrue("chunk without terminal punctuation: $it", it.trimEnd().last() in ".!?…") }
    }

    @Test
    fun normaliseRemovesDiacriticsButArabicIsRejectedEarlier() {
        val stripped = MealSpeechEngine.normalise("بِسْمِ اللَّهِ الرَّحْمَٰنِ")
        assertTrue("diacritics were not stripped: $stripped", stripped.none { it.code in 0x064B..0x0652 })
        assertTrue("guard should still reject it: $stripped", !QuranAudioSupport.isSpeakable(stripped))
    }
}
