package com.example.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
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
                        bottomBar = { Box(Modifier.fillMaxWidth().height(128.dp)) },
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
        assertTrue("entire control must be accessible: $control / $viewport", control.top >= viewport.top && control.bottom <= bar.top + 0.5.dp)
    }

    @Test fun compassFitsNarrowWidthAndCanScrollToItsFinalDirectionStatus() {
        open { CompassContent(azimuth = 50f, qiblaAngle = 150f, hasSensors = false) }
        val dial = compose.onNodeWithTag("compass-dial").getUnclippedBoundsInRoot()
        val viewport = compose.onNodeWithTag("app-content-viewport").getUnclippedBoundsInRoot()
        assertTrue("dial must fit the narrow screen", dial.left >= viewport.left && dial.right <= viewport.right)
        val status = compose.onNodeWithTag("compass-alignment-status").performScrollTo()
        assertWholeControlAboveBar(status)
        File("build/navbar").mkdirs()
        compose.onRoot().captureRoboImage("build/navbar/compass-compact-large-text.png")
    }

    @Test fun finalBackgroundPermissionActionCanScrollClearAtLargeFont() {
        open { BackgroundSettingsScreen(onBack = {}) }
        val notifications = compose.onNodeWithContentDescription("Bildirim ayarları").performScrollTo()
        assertWholeControlAboveBar(notifications)
        File("build/navbar").mkdirs()
        compose.onRoot().captureRoboImage("build/navbar/background-settings-compact.png")
    }
}
