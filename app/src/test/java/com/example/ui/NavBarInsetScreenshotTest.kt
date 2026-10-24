package com.example.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.example.data.QuranAiSearchResult
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.captureRoboImage
import java.io.File
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Runs the measured navigation shell used by every MainApp route. A node may be
 * "displayed" while an overlay still covers it, so compare the final control's
 * whole rectangle with the bar's actual top edge. Real fihrist, nested Scaffold
 * and fixed-footer screens run without a network or a PrayerViewModel.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xhdpi", sdk = [36])
class NavBarInsetScreenshotTest {
    @get:Rule val compose = createComposeRule()

    private var openedVerse: Pair<Int, Int>? = null
    private val verses by lazy {
        val assets = ApplicationProvider.getApplicationContext<Context>().assets
        val corpus = assets.open("quran_search/verses.json").bufferedReader().use { JSONArray(it.readText()) }
        val notes = assets.open("quran_search/footnotes.json").bufferedReader().use { JSONObject(it.readText()) }
        val rows = (0 until corpus.length()).map(corpus::getJSONObject)
        // Authentic text even in a layout fixture; the final item also has a
        // real publisher note, so expanded content exercises the longest case.
        (rows.take(29) + rows.single { it.getInt("surahId") == 11 && it.getInt("verseNumber") == 1 }).map { verse ->
            val surah = verse.getInt("surahId")
            val ayah = verse.getInt("verseNumber")
            QuranAiSearchResult(
                surahId = surah, surahName = verse.getString("surahName"),
                verseNumber = ayah.toString(), verseNumberInt = ayah,
                arabic = verse.getString("arabic"), translation = verse.getString("translation"),
                relevanceSummary = "", keyConcepts = emptyList(),
                aiTafsirInsight = notes.optString("$surah:$ayah", "")
            )
        }
    }

    private fun openShell(
        systemBottom: Dp = 24.dp,
        barHeight: Dp = 88.dp,
        fontScale: Float = 1f,
        content: @Composable () -> Unit
    ) {
        compose.setContent {
            MyApplicationTheme(darkTheme = false) {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                    FloatingNavigationScaffold(
                        contentWindowInsets = WindowInsets(bottom = systemBottom),
                        imeInsets = WindowInsets(0, 0, 0, 0),
                        bottomBar = { NavigationIsland(barHeight + systemBottom) },
                        content = content
                    )
                }
            }
        }
    }

    private fun openFihrist(fontScale: Float = 1f, systemBottom: Dp = 24.dp) {
        openShell(fontScale = fontScale, systemBottom = systemBottom) {
            QuranIndexSearchContent(onBack = {}, onOpenVerse = { surah, ayah -> openedVerse = surah to ayah }, search = { verses })
        }
        compose.onNodeWithTag("quran-index-query").performTextInput("sabır")
        compose.onNodeWithTag("quran-index-submit").performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("30 ayet").fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun scrollFihristToLastAction(): SemanticsNodeInteraction {
        compose.onNodeWithTag("quran-index-results").performScrollToIndex(30)
        compose.onNodeWithTag("quran-index-results").performTouchInput { swipeUp() }
        compose.waitForIdle()
        return compose.onAllNodesWithText("Kur'an'da aç").onLast().performScrollTo()
    }

    private fun assertAboveBar(node: SemanticsNodeInteraction) {
        node.assertIsDisplayed()
        // boundsInRoot is clipped; that alone could hide the covered part of
        // a control and make a broken layout appear to pass this assertion.
        val control = node.getUnclippedBoundsInRoot()
        val bar = compose.onNodeWithTag("app-bottom-navigation").getUnclippedBoundsInRoot()
        val viewport = compose.onNodeWithTag("app-content-viewport").getUnclippedBoundsInRoot()
        assertTrue("control $control must be wholly above navigation $bar", control.bottom <= bar.top + 0.5.dp)
        assertTrue("whole control must be inside the content viewport", control.top >= viewport.top && control.bottom <= viewport.bottom + 0.5.dp)
        assertTrue("control must have a non-zero full height", control.bottom > control.top)
    }

    @Test fun finalFihristResultAndActionsClearTheActualNavigationBar() {
        openFihrist()
        val open = scrollFihristToLastAction()
        assertAboveBar(open)
        assertAboveBar(compose.onAllNodesWithContentDescription("Ayet işlemleri").onLast())
        open.performClick()
        assertEquals(11 to 1, openedVerse)
        File("build/navbar").mkdirs()
        compose.onRoot().captureRoboImage("build/navbar/fihrist-final-result.png")
    }

    @Test
    @Config(qualifiers = "w320dp-h800dp-xhdpi", sdk = [36])
    fun expandedLastFihristRowClearsThreeButtonNavigationWithLargeText() {
        openFihrist(fontScale = 1.5f, systemBottom = 48.dp)
        scrollFihristToLastAction()
        compose.onAllNodesWithText("Arapça").onLast().performScrollTo().performClick()
        compose.onAllNodesWithText("Notlar").onLast().performScrollTo().performClick()
        val action = scrollFihristToLastAction()
        compose.onNodeWithText("${verses.last().surahName}, 1. ayet").assertExists()
        assertAboveBar(action)
        assertAboveBar(compose.onAllNodesWithContentDescription("Ayet işlemleri").onLast())
        File("build/navbar").mkdirs()
        compose.onRoot().captureRoboImage("build/navbar/fihrist-large-text-three-buttons.png")
    }

    @Test
    @Config(qualifiers = "w320dp-h800dp-xhdpi", sdk = [36])
    fun nestedAboutScaffoldFinalButtonRemainsReachableAtLargeText() {
        openShell(fontScale = 1.5f, systemBottom = 48.dp) { AboutSubScreen(onBack = {}) }
        val sources = compose.onNodeWithText("Kaynaklar ve lisanslar").performScrollTo()
        assertAboveBar(sources)
        sources.performClick()
        compose.onNodeWithText("Kapat").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w320dp-h800dp-xhdpi", sdk = [36])
    fun prayerLessonFixedNextButtonClearsNavigationAtLargeText() {
        ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences("prayer_learning_v1", Context.MODE_PRIVATE)
            .edit().clear().commit()
        openShell(fontScale = 1.5f, systemBottom = 48.dp) { PrayerLearningScreen(onBack = {}) }
        compose.onNodeWithTag("prayer-learning-catalog").performScrollToNode(hasTestTag("learn-FAJR"))
        compose.onNodeWithTag("learn-FAJR").performClick()
        assertAboveBar(compose.onNodeWithTag("lesson-next"))
        compose.onNodeWithTag("lesson-next").performClick()
        compose.onNodeWithTag("lesson-step-title").assertTextEquals("İftitah tekbiri")
    }

    @Test fun resizingAndHidingNavigationUpdateTheContentViewportWithoutAStaleGap() {
        val barHeight = mutableStateOf(64.dp)
        val showBar = mutableStateOf(true)
        compose.setContent {
            MyApplicationTheme {
                FloatingNavigationScaffold(
                    showBottomBar = showBar.value,
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    imeInsets = WindowInsets(0, 0, 0, 0),
                    bottomBar = { NavigationIsland(barHeight.value) }
                ) { EndingList() }
            }
        }
        fun checkLast() {
            compose.onNodeWithTag("ending-list").performScrollToIndex(39)
            compose.onNodeWithTag("ending-list").performTouchInput { swipeUp() }
            assertAboveBar(compose.onNodeWithTag("ending-row-39"))
        }
        checkLast()
        val smallViewport = compose.onNodeWithTag("ending-list").fetchSemanticsNode().boundsInRoot
        compose.runOnIdle { barHeight.value = 144.dp }
        checkLast()
        val tallViewport = compose.onNodeWithTag("ending-list").fetchSemanticsNode().boundsInRoot
        assertTrue("larger measured bar must shrink content", tallViewport.height < smallViewport.height)
        compose.runOnIdle { showBar.value = false }
        compose.onNodeWithTag("app-bottom-navigation").assertDoesNotExist()
        val full = compose.onRoot().fetchSemanticsNode().boundsInRoot
        val restored = compose.onNodeWithTag("ending-list").fetchSemanticsNode().boundsInRoot
        assertEquals("reader route must reclaim the hidden bar's entire space", full.height, restored.height, 1f)
    }

    @Test fun keyboardReservesItsHeightAndTemporarilyHidesNavigation() {
        val keyboard = mutableStateOf(0.dp)
        compose.setContent {
            MyApplicationTheme {
                FloatingNavigationScaffold(
                    contentWindowInsets = WindowInsets(bottom = 24.dp),
                    imeInsets = WindowInsets(bottom = keyboard.value),
                    bottomBar = { NavigationIsland(112.dp) }
                ) { EndingList() }
            }
        }
        compose.onNodeWithTag("app-bottom-navigation").assertIsDisplayed()
        compose.runOnIdle { keyboard.value = 300.dp }
        compose.onNodeWithTag("app-bottom-navigation").assertDoesNotExist()
        compose.onNodeWithTag("ending-list").performScrollToIndex(39)
        compose.onNodeWithTag("ending-list").performTouchInput { swipeUp() }
        val root = compose.onRoot().getUnclippedBoundsInRoot()
        val list = compose.onNodeWithTag("ending-list").getUnclippedBoundsInRoot()
        val last = compose.onNodeWithTag("ending-row-39").getUnclippedBoundsInRoot()
        assertEquals("system navigation and IME must not be added twice", 300f,
            (root.bottom - list.bottom).value, 1f)
        assertTrue("last row must remain above keyboard", last.bottom <= list.bottom + 0.5.dp)
        compose.runOnIdle { keyboard.value = 0.dp }
        compose.onNodeWithTag("app-bottom-navigation").assertIsDisplayed()
        val restored = compose.onNodeWithTag("ending-list").getUnclippedBoundsInRoot()
        assertEquals("dismissed keyboard must restore measured navigation reserve", 112f,
            (root.bottom - restored.bottom).value, 1f)
    }
}

@Composable
private fun NavigationIsland(height: Dp) {
    Column(
        Modifier.fillMaxWidth().height(height).background(MaterialTheme.colorScheme.surface),
        verticalArrangement = Arrangement.Center
    ) { Text("Ana Sayfa    Kur'an    Kıble    Zikirmatik    Takvim") }
}

@Composable
private fun EndingList() {
    LazyColumn(Modifier.fillMaxSize().testTag("ending-list")) {
        items(40) { index ->
            Box(Modifier.fillMaxWidth().height(48.dp).testTag("ending-row-$index")) {
                Text("Satır $index")
            }
        }
    }
}
