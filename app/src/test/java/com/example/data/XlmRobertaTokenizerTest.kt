package com.example.data

import java.io.File
import org.json.JSONArray
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class XlmRobertaTokenizerTest {
    private fun asset(name: String): File = listOf(
        File("src/main/assets/quran_search/$name"),
        File("app/src/main/assets/quran_search/$name")
    ).firstOrNull(File::isFile) ?: error("Missing bundled tokenizer asset $name")

    private val tokenizer by lazy {
        XlmRobertaTokenizer(asset("unigram-vocab.bin").readBytes(), asset("unigram-normalizer.bin").readBytes())
    }

    @Test fun officialHuggingFaceGoldenCasesMatchIncludingUnicodeAndTruncation() {
        val stream = javaClass.classLoader!!.getResourceAsStream("quran_search/e5_tokenizer_golden.json")
            ?: error("Missing official tokenizer golden cases")
        val cases = stream.bufferedReader(Charsets.UTF_8).use { JSONArray(it.readText()) }
        assertEquals(44, cases.length())
        for (index in 0 until cases.length()) {
            val case = cases.getJSONObject(index)
            val expected = case.getJSONArray("ids")
            assertArrayEquals("Golden case $index: ${case.getString("text")}",
                LongArray(expected.length()) { expected.getLong(it) },
                tokenizer.encode(case.getString("text"), case.getInt("maxLength")))
        }
    }

    @Test fun defaultEncodingReturnsUnpaddedSpecialTokensForEmptyInput() {
        assertArrayEquals(longArrayOf(0, 2), tokenizer.encode(""))
        assertArrayEquals(longArrayOf(0, 6, 2), tokenizer.encode(" "))
    }

    @Test(expected = IllegalArgumentException::class)
    fun truncationCannotRemoveRequiredSpecialTokens() {
        tokenizer.encode("query: sabır", 1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun corruptedVocabularyHeaderIsRejected() {
        XlmRobertaTokenizer(ByteArray(8), asset("unigram-normalizer.bin").readBytes())
    }
}
