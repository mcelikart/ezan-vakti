package com.example.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class QuranAudioSupportTest {
    private val verse = SurahVerse(
        number = 5,
        arabic = "test",
        pronunciation = "test",
        diyanetMeal = "Diyanet meal",
        elmaliliMeal = "Elmalili meal",
        vakfiMeal = "Vakif meal",
        diyanetTefsir = "",
        bilmenTefsir = "",
        mealsMap = mapOf("tr.yazir" to "<b>Seçili</b> meal")
    )

    @Test
    fun arabicUrlsAreVerseSpecificAndHaveFallback() {
        assertEquals(
            listOf(
                "https://verses.quran.foundation/Alafasy/mp3/002005.mp3",
                "https://everyayah.com/data/Alafasy_128kbps/002005.mp3"
            ),
            QuranAudioSupport.arabicVerseUrls(2, 5)
        )
    }

    @Test
    fun mealTextUsesPreferredEditionAndRemovesMarkup() {
        assertEquals("Seçili meal", QuranAudioSupport.mealText(verse, listOf("tr.yazir")))
    }

    @Test
    fun mealTextFallsBackToDiyanet() {
        assertEquals("Diyanet meal", QuranAudioSupport.mealText(verse, listOf("tr.unknown")))
    }

    @Test
    fun mealSpeechTextCleansFootnotesAndExpandsTurkishAbbreviations() {
        val spokenVerse = verse.copy(
            mealsMap = mapOf(
                "tr.diyanet" to "Hz. Musa [12] kavmine seslendi (bk. Bakara 2/3); sonra yürüdü."
            )
        )

        assertEquals(
            "Hazreti Musa kavmine seslendi; sonra yürüdü.",
            QuranAudioSupport.mealSpeechText(spokenVerse, listOf("tr.diyanet"))
        )
    }
}
