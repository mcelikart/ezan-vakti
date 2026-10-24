package com.example.data

import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.security.MessageDigest
import kotlin.math.abs
import kotlin.math.sqrt
import org.json.JSONArray
import org.json.JSONObject
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test

/** Exercises the shipped offline model and corpus, without an Android or network service. */
class QuranSemanticSearchTest {
    @Test fun bundledCorpusContainsEveryVerseExactlyOnce() {
        assertEquals(6236, verses.size)
        assertEquals(6236, verses.map { it.reference() }.toSet().size)
        assertEquals((1..114).toSet(), verses.map { it.surahId }.toSet())
        for ((surah, count) in SURAH_VERSE_COUNTS.withIndex()) {
            val actual = verses.filter { it.surahId == surah + 1 }
            assertEquals("Surah ${surah + 1}", (1..count).toList(), actual.map { it.verseNumber })
            assertEquals(1, actual.map { it.surahName }.distinct().size)
        }
        assertTrue(verses.all { it.surahName.isNotBlank() && it.arabic.isNotBlank() && it.translation.isNotBlank() })
    }

    @Test fun bundledVectorIndexCoversEveryVerseAndMatchesItsManifest() {
        val manifest = JSONObject(asset("semantic-index.json").readText())
        val data = ByteBuffer.wrap(asset("vectors.bin").readBytes()).order(ByteOrder.LITTLE_ENDIAN)
        val magic = ByteArray(4).also { data.get(it) }.toString(Charsets.US_ASCII)
        assertEquals("QSV2", magic)
        val dimensions = data.int
        val rows = data.int
        assertEquals(QuranSentenceEncoder.DIMENSIONS, dimensions)
        assertEquals(dimensions, manifest.getInt("dimensions"))
        assertEquals(rows, manifest.getInt("rows"))
        assertEquals(6236, manifest.getInt("verses"))
        assertEquals("intfloat/multilingual-e5-small", manifest.getString("model"))
        assertEquals(rows.toLong() * (8 + 4 * dimensions), data.remaining().toLong())
        val covered = mutableSetOf<Int>()
        repeat(rows) { row ->
            val verseIndex = data.int
            val weight = data.float
            assertTrue("Invalid verse reference in row $row", verseIndex in verses.indices)
            assertTrue("Invalid weight in row $row", weight.isFinite() && weight > 0f && weight <= 1f)
            covered += verseIndex
            var normSquared = 0.0
            repeat(dimensions) {
                val value = data.float
                assertTrue("Non-finite vector in row $row", value.isFinite())
                normSquared += value.toDouble() * value
            }
            assertTrue("Unnormalised vector in row $row: $normSquared", abs(normSquared - 1.0) < 0.005)
        }
        assertEquals(verses.indices.toSet(), covered)
        val hashes = manifest.getJSONObject("sha256")
        for (name in hashes.keys()) {
            val digest = MessageDigest.getInstance("SHA-256").digest(asset(name).readBytes())
                .joinToString("") { "%02x".format(it.toInt() and 0xff) }
            assertEquals("Asset digest for $name", hashes.getString(name), digest)
        }
    }

    @Test fun realE5QueryEmbeddingsMatchPythonOnnxReference() {
        // Generated independently by scripts/prepare-semantic-index.py using the
        // official tokenizer, query prefix, mean pooling and L2 normalisation.
        assertEquals(5, goldenCases.length())
        repeat(goldenCases.length()) { index ->
            val case = goldenCases.getJSONObject(index)
            val query = case.getString("query")
            val expectedJson = case.getJSONArray("queryEmbedding")
            val expected = FloatArray(expectedJson.length()) { expectedJson.getDouble(it).toFloat() }
            val actual = encoded(query)
            assertEquals(QuranSentenceEncoder.DIMENSIONS, actual.size)
            assertTrue("Non-finite embedding for $query", actual.all { it.isFinite() })
            assertTrue("Embedding is not normalised for $query", abs(dot(actual, actual) - 1.0) < 0.0001)
            assertTrue("Kotlin/Python encoder mismatch for $query", cosine(actual, expected) > 0.999)
        }
    }

    @Test fun naturalLanguageQuestionsRetrieveRelevantAuthenticVerses() {
        val cases = linkedMapOf(
            "İnsanların arkamdan konuşması ve kusurlarımı araştırması" to "49:12",
            "Borç verdiğim kişi ödeme yapamıyor, ona nasıl davranmalıyım?" to "2:280",
            "Anne ve babam yaşlanınca onlara karşı sorumluluklarım" to "17:23",
            "Öfkeliyken kendimi tutmak ve birini bağışlamak" to "3:134",
            "Aldığım bir haberin doğru olup olmadığını araştırmak" to "49:6",
            "Başka inançtan birini zorla dine sokmak" to "2:256"
        )
        for ((query, expectedReference) in cases) {
            val result = index.searchText(query, ::encoded, limit = 10)
            assertTrue("Expected $expectedReference for '$query', found ${result.map { it.first.reference() }}",
                result.any { it.first.reference() == expectedReference })
            assertEquals("Duplicate verses for $query", result.size, result.map { it.first.reference() }.distinct().size)
            assertTrue("Scores must be descending for $query", result.zipWithNext().all { (a, b) -> a.second >= b.second })
            for ((verse, score) in result) {
                assertSame("Search must return the authentic bundled verse", verseByReference.getValue(verse.reference()), verse)
                assertTrue(score.isFinite() && score >= QuranSemanticIndex.MINIMUM_SIMILARITY)
            }
            assertEquals("Ranking must be deterministic for $query", result, index.searchText(query, ::encoded, limit = 10))
        }
    }

    @Test fun commonTurkishConceptsAndDerivedWordsRetrieveRelatedVerses() {
        val cases = linkedMapOf(
            "hasetçilik" to setOf("113:5", "4:54", "2:109", "4:32"),
            "kibirlenmek" to setOf("31:18", "31:19", "17:37"),
            "hasetlik" to setOf("113:5", "4:54", "2:109", "4:32"),
            "haset" to setOf("113:5", "4:54", "2:109", "4:32"),
            "hased" to setOf("113:5", "4:54", "2:109", "4:32"),
            "çekememezlik" to setOf("113:5", "4:54", "2:109", "4:32"),
            "gıybetçilik" to setOf("49:12", "104:1", "68:11"),
            "dedikoduculuk" to setOf("49:12", "104:1", "68:11"),
            "adalet" to setOf("4:135", "5:8", "4:58"),
            "kul hakkı" to setOf("4:29", "2:188", "4:58"),
            "zekat" to setOf("9:60", "2:43"),
            "israf" to setOf("7:31", "17:26", "17:27"),
            "emanet" to setOf("4:58", "23:8"),
            "tevekkül" to setOf("3:159", "65:3"),
            "tesettür" to setOf("24:31", "33:59")
        )
        for ((query, references) in cases) {
            val results = index.searchText(query, ::encoded, limit = 10)
            assertTrue("No relevant concept for '$query': ${results.map { it.first.reference() }}",
                results.any { it.first.reference() in references })
        }
    }

    @Test fun unrelatedEverydayRequestsDoNotBecomeRandomVerseFragments() {
        val queries = listOf(
            "iPhone 17 fiyatı", "Bugün İstanbul hava durumu nasıl?", "En iyi ekran kartı hangisi?",
            "Pilav nasıl yapılır?", "Python kodu yazabilir misin?", "Fenerbahçe Galatasaray maçı kaç kaç?",
            "asdfghjkl zxcvbnm", "qwerty", "Merhaba nasılsın?", "ali ata bak",
            "Şarjım çok çabuk bitiyor ne yapmalıyım?", "Kedim mamasını yemiyor",
            "Otobüs bileti nereden alınır?", "Kuantum bilgisayarların hata düzeltmesi"
        )
        for (query in queries) {
            assertTrue("Unexpected scripture for '$query'", index.searchText(query, ::encoded).isEmpty())
        }
    }

    @Test fun explicitSearchFocusPreservesTheQuestionNegation() {
        assertEquals("oruç", QuranSearchQuery.prepare("Namaz değil oruç hakkında ayetler arıyorum."))
        assertEquals("yetim hakkı", QuranSearchQuery.prepare("Anne babaya iyilik değil, yetim hakkı"))
        assertEquals("borçluyu affetmek", QuranSearchQuery.prepare("Faiz hakkında değil, borçluyu affetmek hakkında"))
        assertEquals("hasetlik", QuranSearchQuery.prepare("hasetlik hakkında ayetler"))
        assertEquals("Allah beni affetmez diye düşünüyorum", QuranSearchQuery.prepare("Allah beni affetmez diye düşünüyorum."))
        assertEquals("İnsan zayıf değil güçlü olabilir mi", QuranSearchQuery.prepare("İnsan zayıf değil güçlü olabilir mi?"))
        assertEquals("Allah merhametli değil mi", QuranSearchQuery.prepare("Allah merhametli değil mi hakkında ayetler"))
        assertEquals("Zengin olmak kötü değil mi", QuranSearchQuery.prepare("Zengin olmak kötü değil mi hakkında ayetler"))
        assertEquals("haset", QuranSearchQuery.derivativeFallback("hasetçilik"))
        assertEquals("kibir", QuranSearchQuery.derivativeFallback("kibirlenmek"))
        assertEquals(null, QuranSearchQuery.derivativeFallback("adalet"))
        assertEquals(null, QuranSearchQuery.derivativeFallback("Pilav nasıl yapılır"))
    }

    @Test fun severalSemanticRowsForOneVerseReturnOneResultWithStableTies() {
        val sample = sampleVerses()
        val testIndex = QuranSemanticIndex(sample, vectorBytes(
            Row(0, 0.5f, unit(0)), Row(1, 1f, unit(0)), Row(0, 1f, unit(0))
        ), centroidBytes())
        val results = testIndex.search(unit(0))
        assertEquals(listOf(sample[0], sample[1]), results.map { it.first })
        assertEquals(listOf(1f, 1f), results.map { it.second })
        assertEquals(results, testIndex.search(unit(0)))
        assertEquals(1, testIndex.search(unit(0), limit = 1).size)
    }

    @Test fun lowSimilarityReturnsNoInventedOrFallbackVerse() {
        val testIndex = QuranSemanticIndex(sampleVerses(), vectorBytes(
            Row(0, 1f, unit(0)), Row(1, 1f, unit(1))
        ), centroidBytes())
        assertTrue(testIndex.search(unit(2)).isEmpty())
        assertTrue(testIndex.search(FloatArray(DIMENSIONS)).isEmpty())
    }

    @Test fun corpusMeanIsRemovedFromQueryBeforeCosineRetrieval() {
        val sample = sampleVerses()
        val center = FloatArray(DIMENSIONS).also { it[0] = 0.8f }
        val query = FloatArray(DIMENSIONS).also { it[0] = 0.8f; it[1] = 0.6f }
        val testIndex = QuranSemanticIndex(sample, vectorBytes(
            Row(0, 1f, unit(1)), Row(1, 1f, unit(0))
        ), centroidBytes(center))
        val results = testIndex.search(query)
        assertEquals(listOf(sample[0]), results.map { it.first })
        assertEquals(1f, results.single().second, 0.0001f)
    }

    @Test fun conceptAnchorFormatRejectsMissingMappingsAndInvalidVectors() {
        val passages = vectorBytes(Row(0, 1f, unit(0)), Row(1, 1f, unit(1)))
        val concepts = listOf(QuranSearchConcept(intArrayOf(0)), QuranSearchConcept(intArrayOf(1)))
        fun anchorBytes(firstConcept: Int = 0, firstVector: FloatArray = unit(2)): ByteArray = ByteBuffer
            .allocate(16 + 2 * (4 + DIMENSIONS * 4)).order(ByteOrder.LITTLE_ENDIAN).apply {
                put("QCA1".toByteArray(Charsets.US_ASCII)); putInt(DIMENSIONS); putInt(2); putInt(2)
                putInt(firstConcept); firstVector.forEach { putFloat(it) }
                putInt(1); unit(3).forEach { putFloat(it) }
            }.array()
        val valid = anchorBytes()
        val withAnchors = QuranSemanticIndex(sampleVerses(), passages, centroidBytes(), concepts, valid)
        assertEquals(listOf(sampleVerses()[0]), withAnchors.search(unit(2), queryText = "kavram").map { it.first })
        assertTrue(withAnchors.search(unit(4), queryText = "bilinmeyen").isEmpty())
        assertRejected { QuranSemanticIndex(sampleVerses(), passages, centroidBytes(), concepts) }
        assertRejected { QuranSemanticIndex(sampleVerses(), passages, centroidBytes(), concepts, byteArrayOf()) }
        assertRejected { QuranSemanticIndex(sampleVerses(), passages, centroidBytes(), concepts, valid.copyOf(valid.size - 1)) }
        assertRejected { QuranSemanticIndex(sampleVerses(), passages, centroidBytes(), concepts, valid + byteArrayOf(0)) }
        assertRejected { QuranSemanticIndex(sampleVerses(), passages, centroidBytes(), concepts, anchorBytes(firstConcept = 2)) }
        assertRejected { QuranSemanticIndex(sampleVerses(), passages, centroidBytes(), concepts, anchorBytes(firstConcept = 1)) }
        assertRejected { QuranSemanticIndex(sampleVerses(), passages, centroidBytes(), concepts, anchorBytes(firstVector = FloatArray(DIMENSIONS))) }
        assertRejected { QuranSemanticIndex(sampleVerses(), passages, centroidBytes(), concepts,
            anchorBytes(firstVector = unit(2).also { it[10] = Float.NaN })) }
    }

    @Test fun corruptOrIncompatibleIndexesAreRejected() {
        val valid = vectorBytes(Row(0, 1f, unit(0)), Row(1, 1f, unit(1)))
        val center = centroidBytes()
        assertRejected { QuranSemanticIndex(sampleVerses(), valid.copyOf().also { it[0] = 0 }, center) }
        assertRejected { QuranSemanticIndex(sampleVerses(), valid.copyOf(valid.size - 1), center) }
        assertRejected { QuranSemanticIndex(sampleVerses(), valid + byteArrayOf(0), center) }
        assertRejected { QuranSemanticIndex(sampleVerses(), valid.copyOf().also { littleEndian(it).putInt(4, 512) }, center) }
        assertRejected { QuranSemanticIndex(sampleVerses(), vectorBytes(Row(0, 1f, unit(0))), center) }
        assertRejected { QuranSemanticIndex(sampleVerses(), vectorBytes(Row(0, 1f, unit(0)), Row(2, 1f, unit(1))), center) }
        for (weight in floatArrayOf(0f, -1f, 1.01f, Float.NaN)) {
            assertRejected { QuranSemanticIndex(sampleVerses(), vectorBytes(Row(0, weight, unit(0)), Row(1, 1f, unit(1))), center) }
        }
        assertRejected { QuranSemanticIndex(sampleVerses(), vectorBytes(Row(0, 1f, unit(0)), Row(0, 1f, unit(1))), center) }
        assertRejected { QuranSemanticIndex(sampleVerses(), valid.copyOf().also { littleEndian(it).putFloat(20, Float.NaN) }, center) }
        assertRejected { QuranSemanticIndex(sampleVerses(), valid, center.copyOf().also { it[0] = 0 }) }
        assertRejected { QuranSemanticIndex(sampleVerses(), valid, center.copyOf(center.size - 1)) }
        assertRejected { QuranSemanticIndex(sampleVerses(), valid, center.copyOf().also { littleEndian(it).putInt(4, 512) }) }
        assertRejected { QuranSemanticIndex(sampleVerses(), valid, center.copyOf().also { littleEndian(it).putFloat(8, Float.POSITIVE_INFINITY) }) }
        val testIndex = QuranSemanticIndex(sampleVerses(), valid, center)
        assertRejected { testIndex.search(FloatArray(512)) }
        assertRejected { testIndex.search(unit(0).also { it[5] = Float.NaN }) }
    }

    private data class Row(val verse: Int, val weight: Float, val vector: FloatArray)

    private fun vectorBytes(vararg rows: Row): ByteArray = ByteBuffer
        .allocate(12 + rows.size * (8 + 4 * DIMENSIONS)).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("QSV2".toByteArray(Charsets.US_ASCII)); putInt(DIMENSIONS); putInt(rows.size)
            for (row in rows) { putInt(row.verse); putFloat(row.weight); row.vector.forEach { putFloat(it) } }
        }.array()

    private fun centroidBytes(vector: FloatArray = FloatArray(DIMENSIONS)): ByteArray = ByteBuffer.allocate(8 + 4 * DIMENSIONS)
        .order(ByteOrder.LITTLE_ENDIAN).apply {
            put("QSC1".toByteArray(Charsets.US_ASCII)); putInt(DIMENSIONS)
            vector.forEach { putFloat(it) }
        }.array()

    private fun littleEndian(bytes: ByteArray): ByteBuffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
    private fun unit(index: Int): FloatArray = FloatArray(DIMENSIONS).also { it[index] = 1f }
    private fun sampleVerses(): List<QuranSearchVerse> = listOf(
        QuranSearchVerse(1, "Fâtiha", 1, "بسم", "Birinci ayet"),
        QuranSearchVerse(1, "Fâtiha", 2, "الحمد", "İkinci ayet")
    )

    private fun assertRejected(block: () -> Unit) {
        var rejected = false
        try { block() } catch (_: IllegalArgumentException) { rejected = true }
        assertTrue("Malformed index or query must be rejected", rejected)
    }

    companion object {
        private const val DIMENSIONS = QuranSentenceEncoder.DIMENSIONS
        private lateinit var encoder: QuranSentenceEncoder
        private lateinit var index: QuranSemanticIndex
        private lateinit var verses: List<QuranSearchVerse>
        private lateinit var verseByReference: Map<String, QuranSearchVerse>
        private lateinit var goldenCases: JSONArray
        private val encodedQueries = mutableMapOf<String, FloatArray>()

        private fun asset(name: String): File = listOf(
            File("src/main/assets/quran_search/$name"),
            File("app/src/main/assets/quran_search/$name")
        ).firstOrNull(File::isFile) ?: error("Missing bundled semantic-search asset $name")

        @JvmStatic @BeforeClass fun loadBundledOfflineSearchOnce() {
            val corpus = JSONArray(asset("verses.json").readText())
            verses = List(corpus.length()) { index ->
                val row = corpus.getJSONObject(index)
                QuranSearchVerse(row.getInt("surahId"), row.getString("surahName"), row.getInt("verseNumber"),
                    row.getString("arabic"), row.getString("translation"))
            }
            verseByReference = verses.associateBy { it.reference() }
            val topicJson = JSONArray(asset("semantic-topics.json").readText())
            val referenceIndices = verses.withIndex().associate { it.value.reference() to it.index }
            val concepts = List(topicJson.length()) { topic ->
                val references = topicJson.getJSONObject(topic).getJSONArray("verses")
                QuranSearchConcept(IntArray(references.length()) { referenceIndices.getValue(references.getString(it)) })
            }
            index = QuranSemanticIndex(verses, asset("vectors.bin").readBytes(), asset("centroid.bin").readBytes(),
                concepts, asset("concept-anchors.bin").readBytes())
            val model = RandomAccessFile(asset("encoder.onnx"), "r").use {
                it.channel.map(FileChannel.MapMode.READ_ONLY, 0, it.length())
            }
            encoder = QuranSentenceEncoder(model, asset("unigram-vocab.bin").readBytes(), asset("unigram-normalizer.bin").readBytes())
            val fixture = QuranSemanticSearchTest::class.java.classLoader!!
                .getResourceAsStream("quran_search/e5_semantic_golden.json")
                ?: error("Missing independently generated semantic-search golden vectors")
            goldenCases = fixture.bufferedReader(Charsets.UTF_8).use { JSONArray(it.readText()) }
        }

        @JvmStatic @AfterClass fun closeBundledOfflineSearch() {
            if (::encoder.isInitialized) encoder.close()
            encodedQueries.clear()
        }

        private fun encoded(query: String): FloatArray = encodedQueries.getOrPut(query) { encoder.encode(query) }
        private fun QuranSearchVerse.reference(): String = "$surahId:$verseNumber"
        private fun dot(left: FloatArray, right: FloatArray): Double = left.indices.sumOf { left[it].toDouble() * right[it] }
        private fun cosine(left: FloatArray, right: FloatArray): Double = dot(left, right) / sqrt(dot(left, left) * dot(right, right))

        private val SURAH_VERSE_COUNTS = intArrayOf(
            7, 286, 200, 176, 120, 165, 206, 75, 129, 109, 123, 111, 43, 52, 99, 128, 111, 110, 98,
            135, 112, 78, 118, 64, 77, 227, 93, 88, 69, 60, 34, 30, 73, 54, 45, 83, 182, 88, 75,
            85, 54, 53, 89, 59, 37, 35, 38, 29, 18, 45, 60, 49, 62, 55, 78, 96, 29, 22, 24, 13,
            14, 11, 11, 18, 12, 12, 30, 52, 52, 44, 28, 28, 20, 56, 40, 31, 50, 40, 46, 42, 29,
            19, 36, 25, 22, 17, 19, 26, 30, 20, 15, 21, 11, 8, 8, 19, 5, 8, 8, 11, 11, 8, 3,
            9, 5, 4, 7, 3, 6, 3, 5, 4, 5, 6
        )
    }
}
