package com.example.alarm

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.example.ui.FloatingNavigationScaffold
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.captureRoboImage
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
// A 320 dp window also avoids Robolectric #8460: dialog TextFields loop at wider qualifiers.
@Config(sdk = [36], qualifiers = "w320dp-h800dp-xhdpi")
class SunriseAlarmScreenTest {
    @get:Rule val compose = createComposeRule()

    private val saved = mutableListOf<SunriseAlarm>()
    private val deleted = mutableListOf<String>()
    private var backCount = 0

    private fun openScreen(
        initial: List<SunriseAlarm> = emptyList(),
        occurrences: Map<String, SunriseAlarmOccurrence> = emptyMap(),
        fontScale: Float = 1f,
        dark: Boolean = false,
        navigation: Boolean = false,
        saveSucceeds: Boolean = true
    ) {
        val alarms = mutableStateOf(initial)
        compose.setContent {
            MyApplicationTheme(darkTheme = dark) {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                    FloatingNavigationScaffold(
                        showBottomBar = navigation,
                        contentWindowInsets = WindowInsets(0, 0, 0, 0),
                        imeInsets = WindowInsets(0, 0, 0, 0),
                        bottomBar = {
                            Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
                                Box(Modifier.fillMaxWidth().height(120.dp).testTag("sunrise-test-navigation"))
                            }
                        }
                    ) {
                        SunriseAlarmContent(
                            alarms = alarms.value,
                            occurrences = occurrences,
                            onBack = { backCount++ },
                            onSave = { alarm ->
                                if (saveSucceeds) {
                                    saved += alarm
                                    val current = alarms.value
                                    alarms.value = if (current.any { it.id == alarm.id }) {
                                        current.map { if (it.id == alarm.id) alarm else it }
                                    } else current + alarm
                                }
                                saveSucceeds
                            },
                            onDelete = { alarm ->
                                deleted += alarm.id
                                alarms.value = alarms.value.filterNot { it.id == alarm.id }
                            }
                        )
                    }
                }
            }
        }
    }

    private fun addAlarm() = compose.onNodeWithTag("sunrise-alarm-add").performClick()
    private fun saveAlarm() = compose.onNodeWithTag("sunrise-alarm-save").performClick()

    @Test fun defaultIsThirtyMinutesAndSeveralAlarmsHaveIndependentIdentities() {
        openScreen()
        compose.onNodeWithText("Henüz alarm yok").assertIsDisplayed()
        addAlarm()
        compose.onNodeWithTag("sunrise-alarm-minutes").assertTextContains("30")
        saveAlarm()
        compose.onNodeWithText("Her gün • Güneşten 30 dk önce").assertIsDisplayed()
        addAlarm()
        compose.onNodeWithTag("sunrise-alarm-minutes").performTextReplacement("45")
        compose.onNodeWithTag("sunrise-alarm-label").performTextReplacement("Erken kalkış")
        saveAlarm()
        compose.runOnIdle {
            assertEquals(listOf(30, 45), saved.map { it.minutesBeforeSunrise })
            assertNotEquals(saved[0].id, saved[1].id)
            assertTrue(saved.all { it.enabled })
            assertEquals("Erken kalkış", saved[1].label)
        }
        compose.onNodeWithTag("sunrise-alarm-list").performScrollToNode(hasText("Erken kalkış"))
        compose.onNodeWithText("Erken kalkış").assertIsDisplayed()
    }

    @Test fun invalidOffsetsCannotSaveAndZeroMeansAtSunrise() {
        openScreen()
        addAlarm()
        listOf("181", "-1", "", "otuz").forEach { invalid ->
            compose.onNodeWithTag("sunrise-alarm-minutes").performTextReplacement(invalid)
            compose.onNodeWithTag("sunrise-alarm-save").assertIsNotEnabled()
        }
        compose.onNodeWithTag("sunrise-alarm-minutes").performTextReplacement("0")
        compose.onNodeWithTag("sunrise-alarm-label").performTextReplacement("  ")
        compose.onNodeWithTag("sunrise-alarm-save").assertIsEnabled()
        saveAlarm()
        compose.runOnIdle {
            assertEquals(0, saved.single().minutesBeforeSunrise)
            assertEquals(SunriseAlarm.DEFAULT_LABEL, saved.single().label)
        }
        compose.onNodeWithText("Her gün • Güneş vaktinde").assertIsDisplayed()
    }

    @Test fun editingRetainsIdentityAndEnabledToggleDoesNotChangeOtherSettings() {
        val alarm = SunriseAlarm(id = "first", minutesBeforeSunrise = 30, label = "Sabah")
        openScreen(initial = listOf(alarm, SunriseAlarm(id = "other", label = "İkinci alarm")))
        compose.onNodeWithTag("sunrise-alarm-edit-first").performClick()
        compose.onNodeWithTag("sunrise-alarm-minutes").performTextReplacement("60")
        saveAlarm()
        compose.onNodeWithTag("sunrise-alarm-enabled-first").performClick()
        compose.onNodeWithTag("sunrise-alarm-status-first").assertTextEquals("Kapalı")
        compose.onNodeWithTag("sunrise-alarm-enabled-other").assertIsOn()
        compose.runOnIdle {
            assertEquals(listOf("first", "first"), saved.map { it.id })
            assertEquals(listOf(60, 60), saved.map { it.minutesBeforeSunrise })
            assertEquals(listOf(true, false), saved.map { it.enabled })
            assertTrue(saved.all { it.label == "Sabah" })
        }
    }

    @Test fun missingDatedSunriseNeverInventsAnAlarmTimeAndDeleteOnlyRemovesItsOwnAlarm() {
        openScreen(initial = listOf(SunriseAlarm(id = "first"), SunriseAlarm(id = "other")))
        compose.onNodeWithTag("sunrise-alarm-time-first").assertTextEquals("—:—")
        compose.onNodeWithTag("sunrise-alarm-status-first").assertTextEquals("Vakit bilgisi bekleniyor")
        compose.onNodeWithTag("sunrise-alarm-delete-first").performClick()
        compose.onNodeWithTag("sunrise-alarm-card-first").assertDoesNotExist()
        compose.onNodeWithTag("sunrise-alarm-card-other").assertIsDisplayed()
        compose.onNodeWithContentDescription("Geri").performClick()
        compose.runOnIdle {
            assertEquals(listOf("first"), deleted)
            assertEquals(1, backCount)
        }
    }

    @Test fun unsuccessfulPersistenceKeepsTheEditorOpenForRetry() {
        openScreen(saveSucceeds = false)
        addAlarm()
        saveAlarm()
        compose.onNodeWithTag("sunrise-alarm-minutes").assertIsDisplayed()
        compose.runOnIdle { assertTrue(saved.isEmpty()) }
        compose.onNodeWithText("Vazgeç").performClick()
        compose.onNodeWithTag("sunrise-alarm-minutes").assertDoesNotExist()
    }

    @Test
    @Config(sdk = [36], qualifiers = "w320dp-h800dp-xhdpi")
    fun finalAlarmActionsClearSharedNavigationAtLargeText() {
        val alarms = (1..8).map { SunriseAlarm(id = "alarm-$it", label = "Güneş alarmı $it") }
        val occurrences = alarms.associate { alarm ->
            alarm.id to SunriseAlarmOccurrence(alarm.id, 1_793_037_600_000L, 1_793_035_800_000L, "25-10-2026")
        }
        openScreen(alarms, occurrences, fontScale = 1.5f, navigation = true)
        compose.onNodeWithTag("sunrise-alarm-list").performScrollToIndex(alarms.size)
        compose.onNodeWithTag("sunrise-alarm-list").performTouchInput { swipeUp() }
        val delete = compose.onNodeWithTag("sunrise-alarm-delete-alarm-8")
        delete.assertIsDisplayed()
        val action = delete.getUnclippedBoundsInRoot()
        val navigation = compose.onNodeWithTag("sunrise-test-navigation").getUnclippedBoundsInRoot()
        val viewport = compose.onNodeWithTag("app-content-viewport").getUnclippedBoundsInRoot()
        assertTrue("Final action must be completely above the navigation", action.bottom <= navigation.top + 0.5.dp)
        assertTrue("Final action must remain inside the viewport", action.top >= viewport.top && action.bottom <= viewport.bottom + 0.5.dp)
        File("build/sunrise-alarm-scenes").mkdirs()
        compose.onRoot().captureRoboImage("build/sunrise-alarm-scenes/large-text-navigation.png")
        compose.onNodeWithTag("sunrise-alarm-edit-alarm-8").performClick()
        compose.onNodeWithTag("sunrise-alarm-minutes").assertIsDisplayed()
        compose.onNodeWithTag("sunrise-alarm-save").assertIsDisplayed()
        compose.onRoot().captureRoboImage("build/sunrise-alarm-scenes/large-text-editor.png")
    }

    @Test
    @Config(sdk = [36], qualifiers = "w360dp-h800dp-xhdpi")
    fun darkThemeShowsActualOccurrenceAndReachableControls() {
        val alarm = SunriseAlarm(id = "dark", label = "Sabah namazına hazırlık")
        openScreen(
            initial = listOf(alarm), dark = true,
            occurrences = mapOf(alarm.id to SunriseAlarmOccurrence(alarm.id, 1_793_037_600_000L, 1_793_035_800_000L, "25-10-2026"))
        )
        compose.onNodeWithTag("sunrise-alarm-time-dark").assert(!hasText("—:—"))
        compose.onNodeWithTag("sunrise-alarm-edit-dark").assertIsDisplayed()
        compose.onNodeWithTag("sunrise-alarm-delete-dark").assertIsDisplayed()
        File("build/sunrise-alarm-scenes").mkdirs()
        compose.onRoot().captureRoboImage("build/sunrise-alarm-scenes/dark.png")
    }
}
