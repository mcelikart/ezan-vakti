package com.example.ui.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MealSpeechEngineTest {

    @Test fun stripsQuranicTashkeel() {
        val withMarks = "الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ"
        val result = MealSpeechEngine.normalise(withMarks)
        // Harakat, sukun, shadda, tanwin, dagger alif and tatweel must all go,
        // otherwise Turkish engines either skip or garble the word.
        assertTrue("still has tashkeel: $result", result.none { it.code in 0x064B..0x0652 || it.code == 0x0640 })
    }

    @Test fun keepsTurkishLettersIntact() {
        val text = "Bismillahirrahmanirrahim. Allah'ın rahmeti"
        assertEquals(text, MealSpeechEngine.normalise(text))
    }

    @Test fun collapsesWhitespaceAndFixesPunctuationSpacing() {
        assertEquals("Merhaba, dünya.", MealSpeechEngine.normalise("Merhaba ,  dünya."))
    }

    @Test fun shortTextIsASingleChunk() {
        val text = "Kısa bir ayet metni."
        assertEquals(listOf(text), MealSpeechEngine.splitIntoChunks(text))
    }

    @Test fun longTextIsSplitOnSentenceBoundaries() {
        val sentence = "Bu oldukça uzun bir cümle olsun ve Türkçe seslendirme motoru tarafından cümle sonlarında bölünsün. "
        val text = sentence.repeat(6).trim()
        val chunks = MealSpeechEngine.splitIntoChunks(text, maxChars = 220)
        assertTrue("expected multiple chunks, got ${chunks.size}", chunks.size > 1)
        assertTrue("a chunk exceeded the cap: ${chunks.map { it.length }}", chunks.all { it.length <= 220 })
        // No word may be lost or invented by the split.
        assertEquals(
            text.split(Regex("\\s+")),
            chunks.joinToString(" ").split(Regex("\\s+"))
        )
    }

    @Test fun oversizedSentenceIsBrokenOnClauses() {
        val clause = "bir çok uzun alt cümle parçası, "
        val text = clause.repeat(20).trim()
        val chunks = MealSpeechEngine.splitIntoChunks(text, maxChars = 120)
        assertTrue("expected clause split, got ${chunks.size}", chunks.size > 1)
        assertTrue(chunks.all { it.length <= 140 })
        assertTrue("text lost: ${chunks.size}", chunks.joinToString(" ").replace(Regex("\\s+"), " ") == text)
    }

    @Test fun blankInputProducesNoChunks() {
        assertTrue(MealSpeechEngine.splitIntoChunks("   ").isEmpty())
        assertTrue(MealSpeechEngine.splitIntoChunks("").isEmpty())
    }
}
