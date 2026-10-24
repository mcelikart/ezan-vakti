package com.example.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.nio.channels.FileChannel
import kotlin.coroutines.coroutineContext

/** All texts, model weights and index vectors ship inside the APK. */
internal class OfflineQuranSearch private constructor(context: Context) {
    private val index: QuranSemanticIndex
    private val encoder: QuranSentenceEncoder

    init {
        val assets = context.applicationContext.assets
        val footnotes = assets.open("quran_search/footnotes.json").bufferedReader().use { JSONObject(it.readText()) }
        val source = assets.open("quran_search/verses.json").bufferedReader().use { JSONArray(it.readText()) }
        val verses = (0 until source.length()).map { i ->
            val row = source.getJSONObject(i)
            val surah = row.getInt("surahId")
            val ayah = row.getInt("verseNumber")
            QuranSearchVerse(surah, row.getString("surahName"), ayah,
                row.getString("arabic"), row.getString("translation"), footnotes.optString("$surah:$ayah", ""))
        }
        require(verses.size == 6236 && verses.map { it.surahId }.distinct().size == 114)
        val referenceIndices = verses.withIndex().associate { "${it.value.surahId}:${it.value.verseNumber}" to it.index }
        val topicJson = assets.open("quran_search/semantic-topics.json").bufferedReader().use { JSONArray(it.readText()) }
        val concepts = List(topicJson.length()) { topic ->
            val references = topicJson.getJSONObject(topic).getJSONArray("verses")
            QuranSearchConcept(IntArray(references.length()) { referenceIndices.getValue(references.getString(it)) })
        }
        index = QuranSemanticIndex(verses,
            assets.open("quran_search/vectors.bin").use { it.readBytes() },
            assets.open("quran_search/centroid.bin").use { it.readBytes() },
            concepts, assets.open("quran_search/concept-anchors.bin").use { it.readBytes() })
        val model = assets.openFd("quran_search/encoder.onnx").use { descriptor ->
            descriptor.createInputStream().channel.use { channel ->
                channel.map(FileChannel.MapMode.READ_ONLY, descriptor.startOffset, descriptor.declaredLength)
            }
        }
        encoder = QuranSentenceEncoder(model,
            assets.open("quran_search/unigram-vocab.bin").use { it.readBytes() },
            assets.open("quran_search/unigram-normalizer.bin").use { it.readBytes() })
    }

    private suspend fun search(query: String): List<QuranAiSearchResult> {
        val jobContext = coroutineContext
        jobContext.ensureActive()
        return index.searchText(query, encode = { text ->
            jobContext.ensureActive()
            encoder.encode(text).also { jobContext.ensureActive() }
        }).map { (verse, _) ->
            QuranAiSearchResult(
                surahId = verse.surahId,
                surahName = verse.surahName,
                verseNumber = verse.verseNumber.toString(),
                verseNumberInt = verse.verseNumber,
                arabic = verse.arabic,
                translation = verse.translation,
                relevanceSummary = "",
                aiTafsirInsight = verse.notes,
                keyConcepts = emptyList(),
                relevancePercentage = 0,
                themeCategory = ""
            )
        }
    }

    companion object {
        @Volatile private var instance: OfflineQuranSearch? = null
        private val searchMutex = Mutex()
        private val cleanupScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

        fun release() {
            cleanupScope.launch {
                searchMutex.withLock {
                    instance?.encoder?.close()
                    instance = null
                }
            }
        }

        suspend fun search(context: Context, query: String): List<QuranAiSearchResult> = withContext(Dispatchers.Default) {
            val text = query.trim().take(2_000)
            if (text.length < 2 || text.none { it.isLetterOrDigit() }) return@withContext emptyList()
            ensureActive()
            searchMutex.withLock {
                ensureActive()
                val engine = instance ?: OfflineQuranSearch(context.applicationContext).also { instance = it }
                ensureActive()
                engine.search(text)
            }
        }
    }
}
