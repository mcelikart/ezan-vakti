package com.example.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalNavBarHeight
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.navBarBottomPadding
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The root reserves the measured bar exactly once for both old and new screens. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NavBarCoverageTest {
    @get:Rule val compose = createComposeRule()

    @Test fun legacyScreenPaddingAddsOnlyItsOwnSpacingInsideTheSafeViewport() {
        val extras = listOf(0.dp, 8.dp, 16.dp, 32.dp)
        val reserved = mutableListOf<Dp>()
        compose.setContent {
            MyApplicationTheme {
                // An outer caller may still provide the legacy overlay inset.
                CompositionLocalProvider(LocalNavBarHeight provides 112.dp) {
                    FloatingNavigationScaffold(
                        contentWindowInsets = WindowInsets(0, 0, 0, 0),
                        imeInsets = WindowInsets(0, 0, 0, 0),
                        bottomBar = { Box(Modifier.fillMaxWidth().height(112.dp)) }
                    ) {
                        extras.forEach { reserved.add(navBarBottomPadding(it)) }
                    }
                }
            }
        }
        compose.waitForIdle()
        assertEquals(extras, reserved)
        val viewport = compose.onNodeWithTag("app-content-viewport").getUnclippedBoundsInRoot()
        val bar = compose.onNodeWithTag("app-bottom-navigation").getUnclippedBoundsInRoot()
        assertEquals("The root must reserve the actual bar once", bar.top.value, viewport.bottom.value, 0.5f)
        assertEquals(112f, (bar.bottom - bar.top).value, 0.5f)
    }
}
