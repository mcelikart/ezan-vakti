package com.example.data

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt

internal data class QuranSearchVerse(
    val surahId: Int,
    val surahName: String,
    val verseNumber: Int,
    val arabic: String,
    val translation: String,
    val notes: String = ""
)

internal data class QuranSearchConcept(val verseIndices: IntArray)

/** Local neural retrieval over authentic verses and independently phrased concept views. */
internal class QuranSemanticIndex(
    val verses: List<QuranSearchVerse>,
    vectorBytes: ByteArray,
    centroidBytes: ByteArray,
    private val concepts: List<QuranSearchConcept> = emptyList(),
    conceptBytes: ByteArray? = null
) {
    private val dimensions: Int
    private val verseIndices: IntArray
    private val contextWeights: FloatArray
    private val vectors: FloatArray
    private val centroid: FloatArray
    private val anchorConcepts: IntArray
    private val anchors: FloatArray

    init {
        require(vectorBytes.size >= 12 && centroidBytes.size >= 8)
        val data = ByteBuffer.wrap(vectorBytes).order(ByteOrder.LITTLE_ENDIAN)
        require(ByteArray(4).also { data.get(it) }.contentEquals("QSV2".toByteArray()))
        dimensions = data.int
        val rows = data.int
        val center = ByteBuffer.wrap(centroidBytes).order(ByteOrder.LITTLE_ENDIAN)
        require(ByteArray(4).also { center.get(it) }.contentEquals("QSC1".toByteArray()))
        require(center.int == dimensions && center.remaining() == dimensions * 4)
        centroid = FloatArray(dimensions) { center.float.also { require(it.isFinite()) } }
        require(dimensions == QuranSentenceEncoder.DIMENSIONS && rows in verses.size..30_000)
        require(data.remaining().toLong() == rows.toLong() * (8 + dimensions * 4))
        verseIndices = IntArray(rows)
        contextWeights = FloatArray(rows)
        vectors = FloatArray(rows * dimensions)
        for (row in 0 until rows) {
            verseIndices[row] = data.int.also { require(it in verses.indices) }
            contextWeights[row] = data.float.also { require(it > 0f && it <= 1f) }
            val offset = row * dimensions
            for (i in 0 until dimensions) vectors[offset + i] = data.float.also { require(it.isFinite()) }
        }
        require(verseIndices.toSet().size == verses.size) { "Every ayah must have an index vector" }
        if (conceptBytes == null) {
            require(concepts.isEmpty())
            anchorConcepts = IntArray(0)
            anchors = FloatArray(0)
        } else {
            require(concepts.isNotEmpty() && concepts.size <= 2_000)
            require(concepts.all { it.verseIndices.isNotEmpty() && it.verseIndices.all { verse -> verse in verses.indices } })
            require(conceptBytes.size >= 16)
            val anchorData = ByteBuffer.wrap(conceptBytes).order(ByteOrder.LITTLE_ENDIAN)
            require(ByteArray(4).also { anchorData.get(it) }.contentEquals("QCA1".toByteArray()))
            require(anchorData.int == dimensions)
            val count = anchorData.int.also { require(it in concepts.size..30_000) }
            require(anchorData.int == concepts.size)
            require(anchorData.remaining().toLong() == count.toLong() * (4 + dimensions * 4))
            anchorConcepts = IntArray(count)
            anchors = FloatArray(count * dimensions)
            repeat(count) { row ->
                anchorConcepts[row] = anchorData.int.also { require(it in concepts.indices) }
                val vector = FloatArray(dimensions) { anchorData.float.also { require(it.isFinite()) } }
                require(vector.sumOf { it.toDouble() * it } in 0.99..1.01)
                val normalized = centered(vector)
                normalized.copyInto(anchors, row * dimensions)
            }
            require(anchorConcepts.toSet() == concepts.indices.toSet()) { "Every concept needs an anchor" }
        }
    }

    fun searchText(query: String, encode: (String) -> FloatArray, limit: Int = 30): List<Pair<QuranSearchVerse, Float>> {
        val focused = QuranSearchQuery.prepare(query)
        val primary = search(encode(focused), limit, focused)
        if (primary.isNotEmpty()) return primary
        val derivative = QuranSearchQuery.derivativeFallback(focused) ?: return primary
        return search(encode(derivative), limit, derivative)
    }

    fun search(queryEmbedding: FloatArray, limit: Int = 30, queryText: String = ""): List<Pair<QuranSearchVerse, Float>> {
        require(queryEmbedding.size == dimensions && queryEmbedding.all { it.isFinite() })
        val query = centered(queryEmbedding)
        val directScores = FloatArray(verses.size) { -Float.MAX_VALUE }
        val legacyScores = FloatArray(verses.size) { -Float.MAX_VALUE }
        for (row in verseIndices.indices) {
            val score = dot(query, vectors, row * dimensions) * contextWeights[row]
            val verse = verseIndices[row]
            val target = if (contextWeights[row] == TOPIC_WEIGHT) legacyScores else directScores
            if (score > target[verse]) target[verse] = score
        }
        if (anchorConcepts.isEmpty()) {
            // Supports the original passage-only format and deliberately small test indexes.
            val scores = FloatArray(verses.size) { maxOf(directScores[it], legacyScores[it]) }
            return ranked(scores, directScores, limit, MINIMUM_SIMILARITY)
        }

        val conceptScores = FloatArray(concepts.size) { -Float.MAX_VALUE }
        for (row in anchorConcepts.indices) {
            val score = dot(query, anchors, row * dimensions)
            val concept = anchorConcepts[row]
            if (score > conceptScores[concept]) conceptScores[concept] = score
        }
        val order = concepts.indices.sortedByDescending { conceptScores[it] }
        val bestAnchor = conceptScores[order.first()]
        val nextAnchor = order.getOrNull(1)?.let { conceptScores[it] } ?: -1f
        val wordCount = queryText.split(Regex("\\s+")).count { it.any(Char::isLetterOrDigit) }
        val shortQuery = queryText.isNotBlank() && wordCount <= 3
        val scores = FloatArray(verses.size) { -Float.MAX_VALUE }
        val corroboratingPassage = directScores.maxOrNull() ?: -1f
        val acceptedConcept = if (shortQuery) {
            bestAnchor >= SHORT_ANCHOR_MINIMUM && bestAnchor - nextAnchor >= SHORT_ANCHOR_MARGIN
        } else {
            bestAnchor >= STRONG_ANCHOR_MINIMUM ||
                (bestAnchor >= SENTENCE_ANCHOR_MINIMUM && corroboratingPassage >= PASSAGE_CORROBORATION)
        }
        if (acceptedConcept) {
            val anchorCutoff = if (shortQuery) bestAnchor - SHORT_ANCHOR_MARGIN else maxOf(RELATED_ANCHOR_MINIMUM, bestAnchor - 0.10f)
            for (concept in order) {
                val score = conceptScores[concept]
                if (score < anchorCutoff) break
                for (verse in concepts[concept].verseIndices) {
                    // Direct ayah similarity distinguishes related references; a single shared
                    // prototype no longer assigns every mapped verse the same final score.
                    val combined = 0.8f * score + 0.2f * directScores[verse].coerceIn(0f, 1f)
                    if (combined > scores[verse]) scores[verse] = combined
                }
                if (shortQuery) break
            }
        }
        if (!shortQuery || !acceptedConcept) {
            // An unknown subject must not turn into an isolated, superficially similar fragment.
            for (verse in verses.indices) {
                if (directScores[verse] >= DIRECT_MINIMUM && directScores[verse] > scores[verse]) scores[verse] = directScores[verse]
            }
        }
        return ranked(scores, directScores, limit, MINIMUM_SIMILARITY)
    }

    private fun ranked(scores: FloatArray, directScores: FloatArray, limit: Int, minimum: Float): List<Pair<QuranSearchVerse, Float>> {
        val best = scores.maxOrNull() ?: return emptyList()
        if (best < minimum) return emptyList()
        val cutoff = maxOf(minimum, best - 0.16f)
        return verses.indices.asSequence().filter { scores[it] >= cutoff }
            .sortedWith(compareByDescending<Int> { scores[it] }.thenByDescending { directScores[it] }.thenBy { it })
            .take(limit.coerceIn(1, 100)).map { verses[it] to scores[it] }.toList()
    }

    private fun centered(vector: FloatArray): FloatArray {
        val result = FloatArray(dimensions) { vector[it] - centroid[it] }
        val norm = sqrt(result.sumOf { it.toDouble() * it }).coerceAtLeast(1e-12)
        for (i in result.indices) result[i] = (result[i] / norm).toFloat()
        return result
    }

    private fun dot(query: FloatArray, values: FloatArray, offset: Int): Float {
        var score = 0f
        for (i in 0 until dimensions) score += query[i] * values[offset + i]
        return score
    }

    companion object {
        // Retrieval cutoffs, never confidence percentages.
        internal const val MINIMUM_SIMILARITY = 0.36f
        private const val TOPIC_WEIGHT = 0.995f
        private const val SHORT_ANCHOR_MINIMUM = 0.60f
        private const val SHORT_ANCHOR_MARGIN = 0.045f
        private const val SENTENCE_ANCHOR_MINIMUM = 0.50f
        private const val RELATED_ANCHOR_MINIMUM = 0.48f
        private const val STRONG_ANCHOR_MINIMUM = 0.58f
        private const val PASSAGE_CORROBORATION = 0.25f
        private const val DIRECT_MINIMUM = 0.43f
    }
}
