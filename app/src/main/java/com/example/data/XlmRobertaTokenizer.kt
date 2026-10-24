package com.example.data

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.regex.Pattern

/** Exact cased XLM-R Unigram tokenization for the bundled multilingual E5 encoder. */
internal class XlmRobertaTokenizer(vocabularyBytes: ByteArray, normalizerBytes: ByteArray) {
    private val pieceIds: HashMap<String, Int>
    private val scores: DoubleArray
    private val maxPieceLength: Int
    private val unknownScore: Double
    private val normalizer = PrecompiledNormalizer(normalizerBytes)
    private val addedTokens = listOf("<s>" to 0, "<pad>" to 1, "</s>" to 2, "<unk>" to 3, "<mask>" to 250001)

    init {
        DataInputStream(ByteArrayInputStream(vocabularyBytes)).use { input ->
            require(input.readInt() == 0x584C5531) { "Invalid XLM-R vocabulary header" }
            val size = input.readInt()
            require(size == 250002) { "Unexpected E5 vocabulary size" }
            scores = DoubleArray(size)
            pieceIds = HashMap((size / 0.75f).toInt() + 1)
            var longest = 0
            for (id in 0 until size) {
                val length = input.readInt()
                require(length in 1..1024) { "Invalid vocabulary piece length" }
                val bytes = ByteArray(length)
                input.readFully(bytes)
                val piece = String(bytes, Charsets.UTF_8)
                val score = input.readDouble()
                require(score.isFinite()) { "Invalid Unigram score" }
                require(pieceIds.put(piece, id) == null) { "Duplicate Unigram piece" }
                scores[id] = score
                longest = maxOf(longest, piece.codePointCount(0, piece.length))
            }
            require(input.read() == -1) { "Trailing vocabulary bytes" }
            for ((piece, id) in addedTokens) require(pieceIds[piece] == id) { "Wrong E5 special token ID" }
            maxPieceLength = longest
            unknownScore = scores.minOrNull()!! - 10.0
        }
    }

    /** Includes <s>/</s>, reserves their slots during truncation, and never pads. */
    fun encode(text: String, maxLength: Int = 128): LongArray {
        require(maxLength >= 2) { "maxLength must leave room for <s> and </s>" }
        val ids = ArrayList<Long>(minOf(maxLength, 128))
        ids.add(0L)
        val limit = maxLength - 1
        var cursor = 0
        while (cursor < text.length && ids.size < limit) {
            // Added special tokens are extracted before the normalizer, including
            // tokens touching words. Each remaining text span gets its own prefix.
            var nextPosition = text.length
            var nextToken: Pair<String, Int>? = null
            for (candidate in addedTokens) {
                val found = text.indexOf(candidate.first, cursor)
                if (found >= 0 && found < nextPosition) {
                    nextPosition = found
                    nextToken = candidate
                }
            }
            appendNormalizedSpan(text.substring(cursor, nextPosition), ids, limit)
            val special = nextToken ?: break
            if (ids.size >= limit) break
            ids.add(special.second.toLong())
            cursor = nextPosition + special.first.length
        }
        ids.add(2L)
        return ids.toLongArray()
    }

    private fun appendNormalizedSpan(text: String, ids: MutableList<Long>, limit: Int) {
        val normalized = normalizer.normalize(text)
        if (normalized.isEmpty() || ids.size >= limit) return
        val replaced = normalized.replace(' ', '▁')
        val metaspace = if (replaced.startsWith('▁')) replaced else "▁$replaced"
        var start = 0
        for (offset in 1 until metaspace.length) {
            if (metaspace[offset] == '▁') {
                appendUnigram(metaspace.substring(start, offset), ids, limit)
                if (ids.size >= limit) return
                start = offset
            }
        }
        appendUnigram(metaspace.substring(start), ids, limit)
    }

    private fun appendUnigram(text: String, ids: MutableList<Long>, limit: Int) {
        if (text.isEmpty() || ids.size >= limit) return
        val count = text.codePointCount(0, text.length)
        val boundaries = IntArray(count + 1)
        for (index in 0 until count) {
            boundaries[index + 1] = boundaries[index] + Character.charCount(Character.codePointAt(text, boundaries[index]))
        }
        val bestScore = DoubleArray(count + 1) { Double.NEGATIVE_INFINITY }
        val previous = IntArray(count + 1) { -1 }
        val bestId = IntArray(count + 1) { -1 }
        bestScore[0] = 0.0
        for (start in 0 until count) {
            if (bestScore[start] == Double.NEGATIVE_INFINITY) continue
            var hasSingleCodePoint = false
            for (end in start + 1..minOf(count, start + maxPieceLength)) {
                val piece = text.substring(boundaries[start], boundaries[end])
                val id = pieceIds[piece] ?: continue
                if (end == start + 1) hasSingleCodePoint = true
                val score = bestScore[start] + scores[id]
                if (score > bestScore[end]) {
                    bestScore[end] = score
                    previous[end] = start
                    bestId[end] = id
                }
            }
            if (!hasSingleCodePoint) {
                val score = bestScore[start] + unknownScore
                if (score > bestScore[start + 1]) {
                    bestScore[start + 1] = score
                    previous[start + 1] = start
                    bestId[start + 1] = 3
                }
            }
        }
        val reversed = ArrayList<Int>()
        var offset = count
        while (offset > 0) {
            check(previous[offset] >= 0) { "Unigram path is incomplete" }
            reversed.add(bestId[offset])
            offset = previous[offset]
        }
        var previousWasUnknown = false
        for (index in reversed.lastIndex downTo 0) {
            val id = reversed[index]
            if (id != 3 || !previousWasUnknown) {
                if (ids.size >= limit) return
                ids.add(id.toLong())
            }
            previousWasUnknown = id == 3
        }
    }

    /** Uses the shipped SentencePiece table and HF's short-grapheme transformation. */
    private class PrecompiledNormalizer(private val bytes: ByteArray) {
        private val units: IntArray
        private val replacementStart: Int
        private val graphemes = Pattern.compile("\\X")

        init {
            require(bytes.size >= 8) { "Missing SentencePiece character map" }
            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            val trieBytes = buffer.int
            require(trieBytes >= 4 && trieBytes % 4 == 0 && trieBytes < bytes.size - 4) { "Invalid SentencePiece map" }
            units = IntArray(trieBytes / 4) { buffer.int }
            replacementStart = 4 + trieBytes
        }

        fun normalize(text: String): String {
            if (text.isEmpty()) return text
            val out = ByteArrayOutputStream(text.length + 16)
            val matcher = graphemes.matcher(text)
            while (matcher.find()) {
                val grapheme = matcher.group()
                val encoded = grapheme.toByteArray(Charsets.UTF_8)
                // HF tokenizers 0.21.4 tries a whole grapheme only below six
                // UTF-8 bytes; its first matching prefix replaces that cluster.
                // Longer clusters are transformed one code point at a time.
                if (encoded.size < 6 && appendReplacement(encoded, out)) continue
                var cursor = 0
                while (cursor < grapheme.length) {
                    val end = cursor + Character.charCount(Character.codePointAt(grapheme, cursor))
                    val character = grapheme.substring(cursor, end).toByteArray(Charsets.UTF_8)
                    if (!appendReplacement(character, out)) out.write(character)
                    cursor = end
                }
            }
            val mapped = String(out.toByteArray(), Charsets.UTF_8)
            // The second official normalizer is exactly regex " {2,}" -> " ".
            return buildString(mapped.length) {
                var previousSpace = false
                for (character in mapped) {
                    if (character != ' ' || !previousSpace) append(character)
                    previousSpace = character == ' '
                }
            }
        }

        private fun appendReplacement(input: ByteArray, out: ByteArrayOutputStream): Boolean {
            var node = unitOffset(units[0])
            for (byte in input) {
                val label = byte.toInt() and 0xFF
                if (label == 0) break
                node = node xor label
                if (node !in units.indices) break
                val unit = units[node]
                if ((unit and 0x800000FF.toInt()) != label) break
                node = node xor unitOffset(unit)
                if ((unit and 0x100) != 0) {
                    require(node in units.indices) { "Invalid normalization trie" }
                    val from = replacementStart + (units[node] and Int.MAX_VALUE)
                    require(from in replacementStart until bytes.size) { "Invalid normalization replacement" }
                    var until = from
                    while (until < bytes.size && bytes[until] != 0.toByte()) until++
                    require(until < bytes.size) { "Unterminated normalization replacement" }
                    out.write(bytes, from, until - from)
                    return true
                }
            }
            return false
        }

        private fun unitOffset(unit: Int): Int = (unit ushr 10) shl ((unit and 0x200) ushr 6)
    }
}
