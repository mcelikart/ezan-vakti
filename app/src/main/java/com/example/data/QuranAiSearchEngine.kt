package com.example.data

import android.content.Context

data class QuranAiSearchResult(
    val surahId: Int,
    val surahName: String,
    val verseNumber: String,
    val verseNumberInt: Int,
    val arabic: String,
    val translation: String,
    val relevanceSummary: String,
    val aiTafsirInsight: String,
    val keyConcepts: List<String>,
    val relevancePercentage: Int = 95,
    val themeCategory: String = "Kur'an Rehberliği"
)

data class QuranTopicIndex(
    val id: String,
    val title: String,
    val icon: String,
    val category: String,
    val description: String,
    val verses: List<QuranIndexVerseItem>
)

data class QuranIndexVerseItem(
    val surahId: Int,
    val surahName: String,
    val verseNumber: Int,
    val arabic: String,
    val translation: String,
    val explanation: String,
    val tags: List<String>
)


/** Compatibility entry point for the fully local semantic verse search. */
object QuranAiSearchEngine {
    fun release() = OfflineQuranSearch.release()

    @Suppress("UNUSED_PARAMETER")
    suspend fun searchQuranWithAi(
        query: String,
        context: Context,
        filterCategory: String = "Tümü",
        onProgress: (Float, String) -> Unit = { _, _ -> }
    ): List<QuranAiSearchResult> = OfflineQuranSearch.search(context, query)
}
