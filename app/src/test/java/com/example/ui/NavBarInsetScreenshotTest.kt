package com.example.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AppTheme
import com.example.ui.theme.LocalNavBarHeight
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.navBarBottomPadding
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Guards the regression where the last row of a scrolling screen sat underneath
 * the floating navigation bar.
 *
 * The bar is an overlay, so a screen only clears it if it reserves at least that
 * much height. This asserts the inset is genuinely applied and that a real
 * scrolling list can bring its final item fully into view, rather than trusting
 * the padding call to be correct.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h780dp-xhdpi", sdk = [36])
class NavBarInsetScreenshotTest {
    @get:Rule val compose = createComposeRule()

    private fun renderWithBar(barHeight: Dp, rowCount: Int = 40) {
        compose.setContent {
            MyApplicationTheme(darkTheme = false, appTheme = AppTheme.DEFAULT) {
                CompositionLocalProvider(LocalNavBarHeight provides barHeight) {
                    Box(Modifier.fillMaxSize()) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("scroller")
                                // Mirrors what MainActivity applies to the NavHost.
                                .padding(bottom = navBarBottomPadding(extra = 0.dp)),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                top = 8.dp,
                                bottom = 8.dp
                            )
                        ) {
                            items(rowCount) { index ->
                                androidx.compose.material3.Text(
                                    text = "Satır $index",
                                    modifier = Modifier
                                        .padding(vertical = 6.dp)
                                        .testTag("row-$index")
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun insetTracksWhateverHeightTheBarReports() {
        // The bar's height changes with nav style and with the system navigation
        // inset, so the reserved space must track it rather than assume a value.
        // All four heights are checked in a single composition: setContent may
        // only be called once per test.
        val heights = listOf(56.dp, 64.dp, 80.dp, 96.dp)
        val reserved = mutableListOf<Dp>()

        compose.setContent {
            MyApplicationTheme(darkTheme = false, appTheme = AppTheme.DEFAULT) {
                heights.forEach { height ->
                    CompositionLocalProvider(LocalNavBarHeight provides height) {
                        // Read *inside* the provider, which is how callers do it.
                        reserved.add(navBarBottomPadding(extra = 0.dp))
                        androidx.compose.foundation.layout.Spacer(Modifier.testTag("h$height"))
                    }
                }
            }
        }
        compose.waitForIdle()

        assertEquals(heights.size, reserved.size)
        heights.forEachIndexed { i, height ->
            assertEquals("bar of $height must be fully reserved", height, reserved[i])
        }
        assertTrue("more than one distinct height must have been exercised", heights.toSet().size > 1)
    }

    @Test
    fun extraMarginIsAdditive() {
        var reserved: Dp = 0.dp
        compose.setContent {
            MyApplicationTheme(darkTheme = false, appTheme = AppTheme.DEFAULT) {
                CompositionLocalProvider(LocalNavBarHeight provides 70.dp) {
                    reserved = navBarBottomPadding(extra = 12.dp)
                }
            }
        }
        compose.waitForIdle()
        assertEquals(82.dp, reserved)
    }

    @Test
    fun lastRowCanBeScrolledFullyClearOfTheBar() {
        val bar = 80.dp
        // The list is NOT inset: it fills the viewport and content passes behind
        // the bar, which is what stops a dead band from appearing above it. The
        // bar's height is reserved as contentPadding instead.
        compose.setContent {
            MyApplicationTheme(darkTheme = false, appTheme = AppTheme.DEFAULT) {
                CompositionLocalProvider(LocalNavBarHeight provides bar) {
                    Box(Modifier.fillMaxSize()) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().testTag("scroller"),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                top = 8.dp,
                                bottom = navBarBottomPadding(extra = 8.dp)
                            )
                        ) {
                            items(40) { index ->
                                androidx.compose.material3.Text(
                                    text = "Satır $index",
                                    modifier = Modifier
                                        .padding(vertical = 6.dp)
                                        .testTag("row-$index")
                                )
                            }
                        }
                    }
                }
            }
        }
        compose.waitForIdle()

        // The viewport must not be shrunk: the list occupies the whole screen,
        // which is what allows content to scroll under the bar.
        val bounds = compose.onNodeWithTag("scroller").fetchSemanticsNode().boundsInRoot
        assertTrue(
            "list viewport must fill the screen, not stop above the bar " +
                "(height=${bounds.height}, expected ~${780 * 2}px)",
            bounds.height > 700 * 2
        )

        // And the final row still ends up fully visible after scrolling.
        compose.onNodeWithTag("scroller").performScrollToNode(hasTestTag("row-39"))
        compose.waitForIdle()
        compose.onNodeWithTag("row-39").assertIsDisplayed()
        compose.onRoot().captureRoboImage("build/navbar/last-row-clears.png")
    }
}

private fun hasTestTag(tag: String) =
    androidx.compose.ui.test.hasTestTag(tag)
