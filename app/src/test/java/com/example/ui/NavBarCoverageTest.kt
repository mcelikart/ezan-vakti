package com.example.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.navBarBottomPadding
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Scroll content passes behind the floating bar and ends at its upper edge. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NavBarCoverageTest {
    @get:Rule val compose = createComposeRule()

    @Test fun viewportExtendsBehindIslandAndFinalRowScrollsToItsUpperEdge() {
        compose.setContent {
            MyApplicationTheme {
                FloatingNavigationScaffold(
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    imeInsets = WindowInsets(0, 0, 0, 0),
                    bottomBar = { Box(Modifier.fillMaxWidth().height(112.dp)) }
                ) {
                    LazyColumn(
                        Modifier.fillMaxSize().testTag("coverage-scroll"),
                        contentPadding = PaddingValues(bottom = navBarBottomPadding())
                    ) {
                        items((0..20).toList()) { index ->
                            Box(Modifier.fillMaxWidth().height(48.dp).testTag("coverage-row-$index"))
                        }
                    }
                }
            }
        }
        compose.waitForIdle()
        val viewport = compose.onNodeWithTag("app-content-viewport").getUnclippedBoundsInRoot()
        val bar = compose.onNodeWithTag("app-bottom-navigation").getUnclippedBoundsInRoot()
        assertTrue("The scrolling viewport must extend behind navigation", viewport.bottom >= bar.bottom - 0.5.dp)
        assertEquals(112f, (bar.bottom - bar.top).value, 0.5f)
        compose.onNodeWithTag("coverage-scroll").performScrollToIndex(20)
        val last = compose.onNodeWithTag("coverage-row-20").getUnclippedBoundsInRoot()
        assertEquals("Trailing scroll padding must end at the island's upper edge", bar.top.value, last.bottom.value, 1f)
    }
}
