package com.example.alarm

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import com.github.takahirom.roborazzi.captureRoboImage
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w320dp-h480dp-xhdpi", sdk = [36])
class SunriseAlarmRingingContentTest {
    @get:Rule val compose = createComposeRule()
    private val now = 1_800_000_000_000L
    private fun record(id: String) = SunriseAlarmRingRecord(
        SunriseAlarmDelivery(id, "token-$id", now, now + 30 * 60_000L, "2027-01-15"),
        "Sabah namazına hazırlık", now, now + 10 * 60_000L
    )

    @Test fun bigTextNarrowScreenCanReachWholeDismissButtonAndClockStaysWithinWidth() {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.7f)) {
                MaterialTheme { SunriseAlarmRingingContent(listOf(record("a")), now, {}, {}) }
            }
        }
        val clock = compose.onNodeWithTag("ringing_clock").getUnclippedBoundsInRoot()
        val root = compose.onRoot().getUnclippedBoundsInRoot()
        assertTrue(clock.left >= root.left && clock.right <= root.right)
        val dismiss = compose.onNodeWithTag("ringing_dismiss").performScrollTo().assertIsDisplayed()
        val bounds = dismiss.getUnclippedBoundsInRoot()
        assertTrue(bounds.top >= root.top && bounds.bottom <= root.bottom)
        File("build/alarm-scenes").mkdirs()
        compose.onRoot().captureRoboImage("build/alarm-scenes/ringing-compact-large-text.png")
    }

    @Test fun dismissAndSnoozeTargetOnlyTheDisplayedTokenWhileOverlappingAlarmRemainsQueued() {
        var snoozed: String? = null
        var dismissed: String? = null
        compose.setContent {
            MaterialTheme { SunriseAlarmRingingContent(listOf(record("a"), record("b")), now, { snoozed = it }, { dismissed = it }) }
        }
        compose.onNodeWithText("2 alarm çalıyor").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("ringing_snooze").performScrollTo().performClick()
        compose.onNodeWithTag("ringing_dismiss").performScrollTo().performClick()
        assertEquals("token-a", snoozed)
        assertEquals("token-a", dismissed)
        File("build/alarm-scenes").mkdirs()
        compose.onRoot().captureRoboImage("build/alarm-scenes/ringing.png")
    }
}
