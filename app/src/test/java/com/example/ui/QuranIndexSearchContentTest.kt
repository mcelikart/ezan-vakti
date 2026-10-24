package com.example.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import com.example.data.QuranAiSearchResult
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.captureRoboImage
import kotlinx.coroutines.awaitCancellation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w360dp-h800dp-xhdpi")
class QuranIndexSearchContentTest {
    @get:Rule val compose = createComposeRule()

    private val verse = QuranAiSearchResult(
        surahId = 2,
        surahName = "Bakara",
        verseNumber = "153",
        verseNumberInt = 153,
        arabic = "يَا أَيُّهَا الَّذِينَ آمَنُوا اسْتَعِينُوا بِالصَّبْرِ وَالصَّلَاةِ إِنَّ اللَّهَ مَعَ الصَّابِرِينَ",
        translation = "Ey iman edenler! Sabır ve namazla Allah'tan yardım dileyin. Şüphesiz Allah sabredenlerle beraberdir.",
        relevanceSummary = "Görünmemesi gereken analiz özeti",
        aiTafsirInsight = "Görünmemesi gereken uzun açıklama",
        keyConcepts = listOf("Sabır"),
        relevancePercentage = 99
    )

    private fun openScreen(
        onBack: () -> Unit = {},
        onOpenVerse: (Int, Int) -> Unit = { _, _ -> },
        search: suspend (String) -> List<QuranAiSearchResult> = { listOf(verse) }
    ) {
        compose.setContent {
            MyApplicationTheme(darkTheme = false) {
                QuranIndexSearchContent(onBack = onBack, onOpenVerse = onOpenVerse, search = search)
            }
        }
    }

    private fun submit(query: String = "sabır") {
        compose.onNodeWithTag("quran-index-query").performTextInput(query)
        compose.onNodeWithTag("quran-index-submit").performClick()
    }

    private fun awaitVerse() {
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText(verse.translation).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test fun searchUsesOneFieldAndExplicitImeSubmissionThenShowsMealAndReaderAction() {
        val submitted = mutableListOf<String>()
        var opened: Pair<Int, Int>? = null
        openScreen(
            onOpenVerse = { surah, ayah -> opened = surah to ayah },
            search = { submitted += it; listOf(verse) }
        )
        compose.onAllNodes(hasSetTextAction()).assertCountEquals(1)
        compose.onNodeWithText("Kur'an Fihristi").assertIsDisplayed()
        compose.onNodeWithText("Yapay Zeka Arama Motoru").assertDoesNotExist()
        compose.onNodeWithText("Konu Fihristi").assertDoesNotExist()
        compose.onNodeWithTag("quran-index-query").performTextInput("  haksızlığa uğradım  ")
        compose.runOnIdle { assertTrue(submitted.isEmpty()) }
        compose.onNodeWithTag("quran-index-query").performImeAction()
        awaitVerse()
        compose.runOnIdle { assertEquals(listOf("haksızlığa uğradım"), submitted) }
        compose.onNodeWithText("Bakara, 153. ayet").assertIsDisplayed()
        compose.onNodeWithText(verse.relevanceSummary).assertDoesNotExist()
        compose.onNodeWithText(verse.aiTafsirInsight).assertDoesNotExist()
        compose.onNodeWithText("%99 Alaka").assertDoesNotExist()
        compose.onNodeWithText(verse.arabic).assertDoesNotExist()
        compose.onNodeWithText("Arapça").performClick()
        compose.onNodeWithTag("quran-index-arabic-2-153").assertIsDisplayed()
        compose.onNodeWithText("Arapça").performClick()
        compose.onNodeWithText(verse.arabic).assertDoesNotExist()
        compose.onNodeWithText("Notlar").performClick()
        compose.onNodeWithTag("quran-index-notes-2-153").assertIsDisplayed()
        compose.onNodeWithText("Notlar").performClick()
        compose.onNodeWithText(verse.aiTafsirInsight).assertDoesNotExist()
        compose.onNodeWithContentDescription("Ayet işlemleri").performClick()
        compose.onNodeWithText("Kopyala").assertIsDisplayed()
        compose.onNodeWithText("Paylaş").assertIsDisplayed()
        compose.onNodeWithText("Kopyala").performClick()
        compose.onNodeWithText("Kur'an'da aç").performClick()
        compose.runOnIdle { assertEquals(2 to 153, opened) }
    }

    @Test fun editingQueryCancelsInFlightSearchAndClearsOldResults() {
        var cancelled = false
        openScreen(search = { query ->
            if (query == "eski") {
                try { awaitCancellation() } finally { cancelled = true }
            }
            listOf(verse)
        })
        submit("eski")
        compose.onNodeWithTag("quran-index-loading").assertIsDisplayed()
        compose.onNodeWithTag("quran-index-query").performTextReplacement("yeni")
        compose.waitUntil(5_000) { cancelled }
        compose.onNodeWithTag("quran-index-loading").assertDoesNotExist()
        compose.onNodeWithTag("quran-index-submit").performClick()
        awaitVerse()
        compose.onNodeWithTag("quran-index-query").performTextReplacement("başka konu")
        compose.onNodeWithText(verse.translation).assertDoesNotExist()
        compose.onNodeWithContentDescription("Aramayı temizle").performClick()
        compose.onNodeWithContentDescription("Aramayı temizle").assertDoesNotExist()
    }

    @Test fun backCancelsInFlightWork() {
        var cancelled = false
        var backCount = 0
        openScreen(onBack = { backCount++ }, search = {
            try { awaitCancellation() } finally { cancelled = true }
        })
        submit()
        compose.onNodeWithTag("quran-index-loading").assertIsDisplayed()
        compose.onNodeWithContentDescription("Geri").performClick()
        compose.waitUntil(5_000) { cancelled }
        compose.runOnIdle { assertEquals(1, backCount) }
    }

    @Test fun failureCanRetryAndEmptyResultsHaveAConciseMessage() {
        var attempts = 0
        openScreen(search = {
            attempts++
            if (attempts == 1) throw IllegalStateException("test failure")
            emptyList()
        })
        submit()
        compose.onNodeWithTag("quran-index-error").assertIsDisplayed()
        compose.onNodeWithText("Tekrar dene").performClick()
        compose.onNodeWithTag("quran-index-empty").assertIsDisplayed()
        compose.onNodeWithTag("quran-index-loading").assertDoesNotExist()
        compose.runOnIdle { assertEquals(2, attempts) }
    }

    @Test fun captureMinimalResultsAndOptionalArabic() {
        File("build/quran-index-scenes").mkdirs()
        openScreen()
        submit()
        awaitVerse()
        compose.onRoot().captureRoboImage("build/quran-index-scenes/results.png")
        compose.onNodeWithText("Arapça").performClick()
        compose.onRoot().captureRoboImage("build/quran-index-scenes/arabic.png")
    }

    @Test fun darkThemeKeepsResultsOnItsMatchingSurface() {
        File("build/quran-index-scenes").mkdirs()
        compose.setContent {
            MyApplicationTheme(darkTheme = true) {
                QuranIndexSearchContent(onBack = {}, onOpenVerse = { _, _ -> }, search = { listOf(verse) })
            }
        }
        submit()
        awaitVerse()
        compose.onRoot().captureRoboImage("build/quran-index-scenes/dark.png")
    }

    @Test
    @Config(sdk = [36], qualifiers = "w320dp-h800dp-xhdpi")
    fun largeTextKeepsReaderAndArabicControlsVisibleOnNarrowScreens() {
        File("build/quran-index-scenes").mkdirs()
        compose.setContent {
            MyApplicationTheme(darkTheme = false) {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 1.5f)) {
                    QuranIndexSearchContent(onBack = {}, onOpenVerse = { _, _ -> }, search = { listOf(verse) })
                }
            }
        }
        submit()
        awaitVerse()
        compose.onNodeWithText("Kur'an'da aç").assertIsDisplayed()
        compose.onNodeWithText("Arapça").assertIsDisplayed()
        compose.onNodeWithText("Notlar").assertIsDisplayed()
        compose.onNodeWithContentDescription("Ayet işlemleri").assertIsDisplayed()
        compose.onRoot().captureRoboImage("build/quran-index-scenes/large-text.png")
    }
}
