package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.data.Timings
import com.example.ui.theme.*
import com.github.takahirom.roborazzi.captureRoboImage
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
@Config(qualifiers = "w320dp-h1000dp-xhdpi", sdk = [36])
class ThemeSceneScreenshotTest {
    @get:Rule val compose = createComposeRule()

    private fun captureCollection(suffix: String, fontScale: Float = 1f) {
        var theme by mutableStateOf(AppTheme.KUBBELI_SAFIR)
        File("build/theme-scenes").mkdirs()
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                MyApplicationTheme(darkTheme = false, appTheme = theme) {
                    Column(Modifier.fillMaxSize().drawCustomBackground(theme = theme, isDark = false)
                        .verticalScroll(rememberScrollState())) {
                        if (theme != AppTheme.CLASSIC_EMERALD) {
                            Box(Modifier.padding(20.dp)) {
                                HeritageHomeHeader("Alcı, Sincan, Ankara", java.util.Locale.forLanguageTag("tr-TR")) {}
                            }
                        }
                        ThemePrayerScene(
                            Timings("05:30", "06:45", "12:46", "16:12", "18:52", "18:52", "20:15", "05:20", "00:10"),
                            "02:28:01", "Güneş" to "Öğle", .4f
                        )
                    }
                }
            }
        }
        AppTheme.entries.forEach { next ->
            compose.runOnIdle { theme = next }
            compose.waitForIdle()
            compose.onNodeWithTag("composition-${next.spec.composition}").assertExists()
            listOf("05:30", "06:45", "12:46", "16:12", "18:52", "20:15").forEach { time ->
                assertTrue("${next.name}: missing $time", compose.onAllNodesWithText(time).fetchSemanticsNodes().isNotEmpty())
            }
            val layouts = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
            compose.onAllNodes(hasText("02:28:01")).fetchSemanticsNodes().forEach { node ->
                val key = androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult
                if (node.config.contains(key)) node.config[key].action?.invoke(layouts)
            }
            assertTrue("${next.name}: missing countdown layout", layouts.isNotEmpty())
            compose.onRoot().captureRoboImage("build/theme-scenes/${next.name}-$suffix.png")
            assertTrue("${next.name}: clipped countdown ${layouts.map { "${it.size} width=${it.didOverflowWidth} height=${it.didOverflowHeight}" }}", layouts.none { it.hasVisualOverflow })
        }
    }

    @Test fun compactCollection() = captureCollection("320")
    @Test fun compactCollectionWithLargeText() = captureCollection("320-large-text", fontScale = 1.5f)
    @Test @Config(qualifiers = "w420dp-h1000dp-xhdpi")
    fun wideCollection() = captureCollection("420")
}
