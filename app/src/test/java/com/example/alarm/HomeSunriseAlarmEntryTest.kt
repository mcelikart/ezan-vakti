package com.example.alarm

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.example.ui.theme.MyApplicationTheme
import com.example.alarm.HomeStepsAndAlarmRow
import com.example.data.PrayerTimeCache
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowAlarmManager

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w360dp-h800dp-xhdpi")
class HomeSunriseAlarmEntryTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var context: Context
    private lateinit var originalZone: TimeZone

    @Before fun setup() {
        originalZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        context = ApplicationProvider.getApplicationContext<Application>()
        listOf(SunriseAlarmStore.PREFERENCES, SunriseAlarmRingingState.PREFERENCES,
            "sunrise_alarm_runtime", "prayer_daily_times", "countdown_service_prefs").forEach {
            context.getSharedPreferences(it, Context.MODE_PRIVATE).edit().clear().commit()
        }
        shadowOf(context as Application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        shadowOf(context.getSystemService(NotificationManager::class.java)).setNotificationsEnabled(true)
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
    }

    @After fun cleanup() {
        TimeZone.setDefault(originalZone)
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
    }

    private fun openEntry(onClick: () -> Unit = {}) {
        compose.setContent { MyApplicationTheme { HomeSunriseAlarmEntry(onClick) } }
    }

    @Test fun homeEntryExplainsTheDefaultAndOpensAlarmModuleWhenTapped() {
        var openings = 0
        openEntry { openings++ }
        compose.onNodeWithText("Sabah Namazı Alarmı").assertIsDisplayed()
        compose.onNodeWithText("Güneşten 30 dk önce • Süreyi değiştirebilirsiniz").assertIsDisplayed()
        compose.onNodeWithTag("home-sunrise-alarm").performClick()
        compose.runOnIdle { assertEquals(1, openings) }
    }

    @Test fun homeAlarmSitsToTheRightOfTheCompactStepCounter() {
        compose.setContent {
            MyApplicationTheme {
                HomeStepsAndAlarmRow(onAlarmClick = {}) { modifier ->
                    Box(modifier.height(96.dp).background(Color.Gray).testTag("home-steps-widget"))
                }
            }
        }
        val steps = compose.onNodeWithTag("home-step-shortcut").fetchSemanticsNode().boundsInRoot
        val alarm = compose.onNodeWithTag("home-alarm-shortcut").fetchSemanticsNode().boundsInRoot
        compose.onNodeWithTag("home-sunrise-alarm").assertIsDisplayed()
        assertTrue("The alarm shortcut should be to the right of the step counter", alarm.left > steps.left)
        assertEquals("Both shortcuts should share one row", steps.width, alarm.width, 1f)
    }

    @Test fun homeImmediatelyShowsActualQueuedSnoozeInsteadOfTomorrowsRegularAlarm() {
        val alarm = SunriseAlarm(id = "wake-up")
        SunriseAlarmStore(context).save(alarm)
        // Android's virtual uptime does not change app/test System.currentTimeMillis.
        // Anchor one complete delivery to the current minute and keep every assertion relative.
        val now = System.currentTimeMillis()
        val trigger = now / 60_000L * 60_000L
        val sunrise = trigger + 30 * 60_000L
        val nextSunrise = Calendar.getInstance().apply {
            timeInMillis = sunrise
            add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis
        val clock = SimpleDateFormat("HH:mm", Locale.US)
        context.getSharedPreferences("prayer_daily_times", Context.MODE_PRIVATE).edit()
            .putString(PrayerTimeCache.date(sunrise), "Güneş,${clock.format(Date(sunrise))}")
            .putString(PrayerTimeCache.date(nextSunrise), "Güneş,${clock.format(Date(nextSunrise))}").commit()
        SunriseAlarmScheduler.refresh(context, trigger - 60_000L)
        val regular = SunriseAlarmScheduler.pending(context).single()
        assertEquals(trigger, regular.triggerAtMillis)
        assertNotNull(SunriseAlarmScheduler.receive(context, SunriseAlarmScheduler.fireIntent(context, regular), now))
        SunriseAlarmScheduler.refresh(context, now)
        val tomorrow = SunriseAlarmScheduler.pending(context).single()
        assertEquals(PrayerTimeCache.date(nextSunrise), tomorrow.sourceDate)
        val format = SimpleDateFormat("d MMM • HH:mm", Locale.forLanguageTag("tr-TR"))
        val tomorrowSubtitle = "Sonraki alarm ${format.format(Date(tomorrow.triggerAtMillis))}"
        openEntry()
        compose.onNodeWithText(tomorrowSubtitle).assertIsDisplayed()

        var snoozeSubtitle = ""
        compose.runOnIdle {
            val snoozedAt = System.currentTimeMillis()
            assertTrue(SunriseAlarmScheduler.snooze(context, regular.token, minutes = 10, now = snoozedAt))
            val pending = SunriseAlarmScheduler.pending(context)
            assertTrue(pending.contains(tomorrow))
            val snooze = pending.minBy { it.triggerAtMillis }
            assertTrue(snooze.fromSnooze)
            assertEquals(snoozedAt + 10 * 60_000L, snooze.triggerAtMillis)
            snoozeSubtitle = "Sonraki alarm ${format.format(Date(snooze.triggerAtMillis))}"
        }
        compose.waitUntil(5_000L) {
            compose.onAllNodesWithText(snoozeSubtitle).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText(snoozeSubtitle).assertIsDisplayed()
        compose.onNodeWithText(tomorrowSubtitle).assertDoesNotExist()
    }
}
