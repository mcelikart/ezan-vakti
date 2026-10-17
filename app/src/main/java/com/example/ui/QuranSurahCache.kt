package com.example.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * In-memory cache of downloaded surahs, keyed by surah id plus the set of
 * active editions.
 *
 * Why this exists: opening the Quran reader used to issue six or seven serial
 * HTTP requests (Arabic, transliteration, then one per active meal) and then
 * regex-parse 286 verses on the main thread. The screen looked frozen for
 * several seconds. Fetching is now parallel and parsing is off the main
 * dispatcher, and this cache removes the network cost entirely on a second
 * visit.
 *
 * Scope is deliberately a single screen session: a process-lifetime cache would
 * pin the full text of every surah the user ever opened in memory.
 */
class QuranSurahCache(private val maxEntries: Int = 6) {
    private data class Key(val surahId: Int, val editions: List<String>)

    private val entries = object : LinkedHashMap<Key, List<SurahVerse>>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Key, List<SurahVerse>>?) =
            size > maxEntries
    }

    @Synchronized
    fun get(surahId: Int, editions: List<String>): List<SurahVerse>? = entries[Key(surahId, editions)]

    @Synchronized
    fun put(surahId: Int, editions: List<String>, verses: List<SurahVerse>) {
        entries[Key(surahId, editions)] = verses
    }

    @Synchronized
    fun clear() = entries.clear()
}

@Composable
internal fun rememberQuranSurahCache(): QuranSurahCache = remember { QuranSurahCache() }
