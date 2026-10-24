package com.example.alarm

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
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
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w320dp-h800dp-xhdpi")
class SunriseAlarmPermissionPanelTest {
    @get:Rule val compose = createComposeRule()

    @Test fun readyStateOnlyShowsTheBriefAlarmVolumeInformation() {
        compose.setContent {
            MyApplicationTheme {
                SunriseAlarmPermissionContent(
                    readiness = SunriseAlarmReadiness(true, true, true, false),
                    onExact = {}, onNotifications = {}, onFullScreen = {}, onDnd = {}
                )
            }
        }
        compose.onNodeWithText("Sessiz modda da çalar. Alarm sesi sıfırsa çalarken geçici olarak açılır.").assertIsDisplayed()
        listOf("exact", "notifications", "fullscreen", "dnd").forEach {
            compose.onNodeWithTag("sunrise-permission-$it").assertDoesNotExist()
        }
    }

    @Test fun missingPermissionsAndDndHaveSeparateReachableActionsWithLargeText() {
        val actions = mutableListOf<String>()
        compose.setContent {
            MyApplicationTheme {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 1.5f)) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().testTag("permission-scroll"),
                        contentPadding = PaddingValues(20.dp)
                    ) {
                        item {
                            SunriseAlarmPermissionContent(
                                readiness = SunriseAlarmReadiness(false, false, false, true),
                                onExact = { actions += "exact" }, onNotifications = { actions += "notifications" },
                                onFullScreen = { actions += "fullscreen" }, onDnd = { actions += "dnd" }
                            )
                        }
                    }
                }
            }
        }
        listOf("Alarm iznini aç", "Bildirimleri aç", "Kilit ekranında göster", "Ses ayarlarını aç").forEach {
            compose.onNodeWithText(it).performScrollTo().assertIsDisplayed().performClick()
        }
        compose.runOnIdle { assertEquals(listOf("exact", "notifications", "fullscreen", "dnd"), actions) }
        compose.onNodeWithText("Rahatsız Etmeyin’de alarmlara izin verin.").assertIsDisplayed()
        File("build/sunrise-alarm-scenes").mkdirs()
        compose.onRoot().captureRoboImage("build/sunrise-alarm-scenes/permissions-large-text.png")
    }
}
