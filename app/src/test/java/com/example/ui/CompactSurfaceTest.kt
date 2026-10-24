package com.example.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.captureRoboImage
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w320dp-h480dp-xhdpi", sdk = [36])
class CompactSurfaceTest {
    @get:Rule val compose = createComposeRule()

    private fun open(content: @Composable () -> Unit) {
        compose.setContent {
            MyApplicationTheme {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, 1.5f)) {
                    FloatingNavigationScaffold(
                        contentWindowInsets = WindowInsets(top = 24.dp, bottom = 48.dp),
                        imeInsets = WindowInsets(0, 0, 0, 0),
                        bottomBar = { Box(Modifier.fillMaxWidth().height(128.dp).testTag("compact-test-navigation")) },
                        content = content
                    )
                }
            }
        }
    }

    private fun assertWholeControlAboveBar(node: SemanticsNodeInteraction) {
        node.assertIsDisplayed()
        val control = node.getUnclippedBoundsInRoot()
        val viewport = compose.onNodeWithTag("app-content-viewport").getUnclippedBoundsInRoot()
        val bar = compose.onNodeWithTag("app-bottom-navigation").getUnclippedBoundsInRoot()
        assertTrue("scroll viewport should extend under the floating island", viewport.bottom >= bar.bottom - 0.5.dp)
        assertTrue("entire control must be accessible: $control / $viewport", control.top >= viewport.top && control.bottom <= bar.top + 0.5.dp)
    }

    @Test fun compassFitsNarrowWidthAndCanScrollToItsFinalDirectionStatus() {
        open { CompassContent(azimuth = 50f, qiblaAngle = 150f, hasSensors = false) }
        val dial = compose.onNodeWithTag("compass-dial").getUnclippedBoundsInRoot()
        val viewport = compose.onNodeWithTag("app-content-viewport").getUnclippedBoundsInRoot()
        assertTrue("dial must fit the narrow screen", dial.left >= viewport.left && dial.right <= viewport.right)
        compose.onNodeWithTag("compass-scroll-content").performTouchInput { repeat(3) { swipeUp() } }
        val status = compose.onNodeWithTag("compass-alignment-status")
        assertWholeControlAboveBar(status)
        File("build/navbar").mkdirs()
        compose.onRoot().captureRoboImage("build/navbar/compass-compact-large-text.png")
    }

    @Test fun finalBackgroundPermissionActionCanScrollClearAtLargeFont() {
        open { BackgroundSettingsScreen(onBack = {}) }
        compose.onNodeWithTag("background-settings-scroll").performTouchInput { repeat(3) { swipeUp() } }
        val notifications = compose.onNodeWithContentDescription("Bildirim ayarları")
        assertWholeControlAboveBar(notifications)
        File("build/navbar").mkdirs()
        compose.onRoot().captureRoboImage("build/navbar/background-settings-compact.png")
    }
}
