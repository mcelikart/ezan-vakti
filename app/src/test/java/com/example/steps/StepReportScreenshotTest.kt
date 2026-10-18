package com.example.steps

import android.Manifest
import android.app.Application
import android.hardware.Sensor
import android.hardware.SensorManager
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.example.ui.ThemeGalleryScreen
import com.example.ui.theme.AppTheme
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowSensor
import java.io.File

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h780dp-xhdpi", sdk = [36])
class StepReportScreenshotTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var store: StepStore

    @Before fun seed() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        shadowOf(app).grantPermissions(Manifest.permission.ACTIVITY_RECOGNITION, Manifest.permission.POST_NOTIFICATIONS)
        shadowOf(app.getSystemService(SensorManager::class.java)).addSensor(ShadowSensor.newInstance(Sensor.TYPE_STEP_COUNTER))
        StepCounterService.settings(app).edit().putBoolean("enabled", true).commit()
        store = StepStore.get(app)
        store.writableDatabase.delete("daily_steps", null, null)
        store.writableDatabase.delete("body_profiles", null, null)
        store.resetBaseline()
        for (offset in 6 downTo 0) {
            val day = WalkingDates.shift(StepStore.dayKey(), ReportPeriod.DAY, -offset)
            store.writableDatabase.execSQL("INSERT INTO daily_steps VALUES (?, ?)",
                arrayOf<Any>(day, if (offset == 0) 6200 else 2500 + offset * 300))
        }
        store.saveProfile(WalkingProfile(175.0, 75.0, 70.0))
        File("build/step-reports").mkdirs()
    }

    private fun show(dark: Boolean, theme: AppTheme = AppTheme.DEFAULT) {
        compose.setContent { MyApplicationTheme(darkTheme = dark, appTheme = theme) { StepReportScreen {} } }
        compose.waitUntil(10_000) { compose.onAllNodes(hasText("6.200")).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun selectReport(period: ReportPeriod) {
        compose.onNodeWithText(period.title).performClick().assertIsSelected()
        val range = WalkingDates.range(StepStore.dayKey(), period)
        val expected = java.text.NumberFormat.getIntegerInstance(java.util.Locale.forLanguageTag("tr-TR"))
            .format(store.range(range.start, range.end).sumOf { it.steps })
        compose.waitUntil(10_000) { compose.onAllNodes(hasText(expected)).fetchSemanticsNodes().isNotEmpty() }
        if (period == ReportPeriod.MONTH) {
            val month = java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.forLanguageTag("tr-TR"))
                .format(WalkingDates.calendar(StepStore.dayKey()).time)
            compose.onNodeWithText(month).assertExists()
        }
        compose.mainClock.advanceTimeByFrame()
        compose.waitForIdle()
    }

    @Test fun dailyWeeklyMonthlyAndProfile() {
        show(false)
        compose.onNodeWithText("163").assertExists()
        compose.onRoot().captureRoboImage("build/step-reports/daily-light.png")
        selectReport(ReportPeriod.WEEK)
        compose.waitUntil(10_000) { compose.onAllNodes(hasText("Aktif gün")).fetchSemanticsNodes().isNotEmpty() }
        compose.onRoot().captureRoboImage("build/step-reports/weekly-light.png")
        selectReport(ReportPeriod.MONTH)
        compose.onRoot().captureRoboImage("build/step-reports/monthly-light.png")
        selectReport(ReportPeriod.DAY)
        compose.waitUntil(10_000) { compose.onAllNodes(hasText("6.200")).fetchSemanticsNodes().isNotEmpty() }
    }

    @Test fun profileValidationAndDecimalComma() {
        show(false)
        compose.onNodeWithContentDescription("Yürüyüş profili").performClick()
        compose.onNodeWithText("Boy (cm)").assertExists()
        compose.onRoot().captureRoboImage("build/step-reports/profile-light.png")
        compose.onNodeWithText("Kilo (kg)").performTextReplacement("0")
        compose.onNodeWithContentDescription("Kaydet").assertIsNotEnabled()
        compose.onNodeWithText("Kilo (kg)").performTextReplacement("80,5")
        compose.onNodeWithContentDescription("Kaydet").assertIsEnabled().performClick()
        compose.waitUntil(10_000) { store.profile()?.weightKg == 80.5 }
        assertEquals(80.5, store.profile()!!.weightKg, 0.001)
    }

    @Test @Config(qualifiers = "w320dp-h640dp-xhdpi")
    fun compactDarkScreen() {
        show(true, AppTheme.KUBBELI_SAFIR)
        compose.onRoot().captureRoboImage("build/step-reports/daily-dark-compact.png")
        compose.onNodeWithContentDescription("Yürüyüş profili").performClick()
        compose.onAllNodes(isRoot()).onLast().captureRoboImage("build/step-reports/profile-dark-compact.png")
    }

    @Test @Config(qualifiers = "w600dp-h900dp-xhdpi")
    fun wideScreen() {
        show(false, AppTheme.KUBBELI_SEDEF)
        selectReport(ReportPeriod.WEEK)
        compose.onRoot().captureRoboImage("build/step-reports/weekly-wide.png")
    }

    @Test @Config(qualifiers = "w320dp-h640dp-xhdpi")
    fun themeGalleryRemainsScrollableOnSmallPhone() {
        var selected: AppTheme? = null
        compose.setContent { MyApplicationTheme(darkTheme = false) {
            ThemeGalleryScreen(AppTheme.DEFAULT, { selected = it }, {})
        } }
        compose.onNodeWithText("Tema Galerisi").assertExists()
        compose.onRoot().captureRoboImage("build/step-reports/themes-compact.png")
        compose.onNodeWithTag("theme-list").performScrollToIndex(2)
        compose.onNode(hasText("Kubbeli Safir") and hasClickAction()).performClick()
        assertEquals(AppTheme.KUBBELI_SAFIR, selected)
        compose.onNodeWithTag("theme-list").performScrollToIndex(1)
        compose.onNode(hasText("Kubbeli Zümrüt") and hasClickAction()).performClick()
        assertEquals(AppTheme.CLASSIC_EMERALD, selected)
        compose.onRoot().captureRoboImage("build/step-reports/themes-emerald-compact.png")
        compose.onNodeWithTag("theme-list").performScrollToIndex(AppTheme.entries.size)
        compose.onNode(hasText("Kubbeli Gece Mavisi") and hasClickAction()).assertIsDisplayed().performClick()
        assertEquals(AppTheme.KUBBELI_GECE_MAVISI, selected)
    }
}
