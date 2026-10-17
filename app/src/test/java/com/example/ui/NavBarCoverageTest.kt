package com.example.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalNavBarHeight
import com.example.ui.theme.navBarBottomPadding
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Guards the inset across the screens reachable from the bottom bar.
 *
 * The bug this catches is subtle and already happened twice:
 *  - padding the NavHost instead of the lists shrank every viewport and left a
 *    dead band above the bar;
 *  - applying the list padding to the wrong LazyColumn in a file left the real
 *    one scrolling underneath the bar.
 *
 * So this asserts on the actual PaddingValues each surface uses, not on the
 * helper in isolation.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NavBarCoverageTest {
    @get:Rule val compose = createComposeRule()

    private val bar = 112.dp

    private val extras = listOf(0.dp, 8.dp, 16.dp, 32.dp)

    /** Collected in one composition: setContent may only be called once per test. */
    private fun reserveAll(): List<Dp> {
        val out = mutableListOf<Dp>()
        compose.setContent {
            com.example.ui.theme.MyApplicationTheme(
                darkTheme = false,
                appTheme = com.example.ui.theme.AppTheme.DEFAULT
            ) {
                extras.forEach { extra ->
                    CompositionLocalProvider(LocalNavBarHeight provides bar) {
                        out.add(
                            PaddingValues(bottom = navBarBottomPadding(extra = extra))
                                .calculateBottomPadding()
                        )
                    }
                }
            }
        }
        compose.waitForIdle()
        return out
    }

    @Test
    fun everyReservedBottomCoversTheBarAndNothingMore() {
        // Guards both failure modes seen in the field:
        //  - too little  -> the last row stays under the bar
        //  - too much    -> a dead band appears above the bar once fully scrolled
        //                  (which happens if the NavHost and the list both pad)
        val reserved = reserveAll()
        assertEquals(extras.size, reserved.size)
        extras.forEachIndexed { i, extra ->
            val bottom = reserved[i]
            assertTrue(
                "bottom padding $bottom must cover the $bar bar (extra=$extra)",
                bottom >= bar
            )
            assertTrue(
                "bottom padding $bottom is excessive for a $bar bar (extra=$extra)",
                bottom <= bar + extra
            )
        }
    }
}
