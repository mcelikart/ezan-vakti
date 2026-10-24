package com.example.ui

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.PrayerTimeCache
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PrayerServiceTest {
    private lateinit var context: Context
    @Before fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("alarm_prefs", 0).edit().clear().commit()
        context.getSharedPreferences("prayer_daily_times", 0).edit().clear().commit()
        context.getSharedPreferences("countdown_service_prefs", 0).edit().clear().commit()
        context.getSharedPreferences("prayer_alarm_scheduler", 0).edit().clear().commit()
    }

    @Test fun countdownImmediatelyEntersForegroundAsIndependentPersistentNotification() {
        val controller = Robolectric.buildService(CountdownService::class.java).create()
        try {
            val service = shadowOf(controller.get())
            val notification = service.lastForegroundNotification
            assertEquals(1001, service.lastForegroundNotificationId)
            assertTrue(notification.flags and Notification.FLAG_ONGOING_EVENT != 0)
            assertEquals(0, notification.flags and Notification.FLAG_GROUP_SUMMARY)
            assertEquals(CountdownService.COUNTDOWN_GROUP, notification.group)
            assertNotEquals(PrayerNotificationReceiver.PRAYER_GROUP, notification.group)
            assertEquals(CountdownService.COUNTDOWN_GROUP, notification.publicVersion.group)
            assertEquals(Notification.VISIBILITY_PUBLIC, notification.visibility)
            assertNotNull(notification.publicVersion)
            assertEquals(0L, notification.timeoutAfter)
            val channel = controller.get().getSystemService(NotificationManager::class.java)
                .getNotificationChannel(CountdownService.CHANNEL_ID)
            assertEquals(NotificationManager.IMPORTANCE_DEFAULT, channel.importance)
            assertNull(channel.sound)
            assertEquals(Notification.VISIBILITY_PUBLIC, channel.lockscreenVisibility)
        } finally { controller.destroy() }
    }

    @Test fun countdownAlwaysFormatsHoursMinutesAndLiveSeconds() {
        assertEquals("02:03:05", CountdownService.formatRemaining(7_385_000L))
        assertEquals("00:00:01", CountdownService.formatRemaining(1L))
        assertEquals("00:00:00", CountdownService.formatRemaining(-1L))
    }

    @Test fun repeatedSchedulingReplacesAlarmsAndDisabledAlarmsAreCancelled() {
        val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
        context.getSharedPreferences("prayer_daily_times", 0).edit()
            .putString(PrayerTimeCache.date(tomorrow.timeInMillis), "Öğle,12:30").commit()
        val alarms = shadowOf(context.getSystemService(AlarmManager::class.java))
        PrayerAlarmScheduler.scheduleAllPrayerAlarms(context)
        PrayerAlarmScheduler.scheduleAllPrayerAlarms(context)
        assertEquals(2, alarms.scheduledAlarms.size)
        val scheduled = alarms.scheduledAlarms.map { it.triggerAtTime }.sorted()
        assertEquals(600_000L, scheduled[1] - scheduled[0])
        context.getSharedPreferences("alarm_prefs", 0).edit()
            .putBoolean("key_exact_enabled_Öğle", false).putBoolean("key_10min_enabled_Öğle", false).commit()
        PrayerAlarmScheduler.scheduleAllPrayerAlarms(context)
        assertEquals(0, alarms.scheduledAlarms.size)
    }

    @Test fun missingTomorrowDoesNotReuseTodaysTimes() {
        val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
        context.getSharedPreferences("countdown_service_prefs", 0).edit()
            .putString("ALL_PRAYERS", "Öğle,12:30").putString("ALL_PRAYERS_DATE", PrayerTimeCache.date()).commit()
        assertEquals("", PrayerTimeCache.read(context, tomorrow))
    }
}
