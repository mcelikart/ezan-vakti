package com.example.ui

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PrayerNotificationTest {
    private lateinit var context: Context
    private lateinit var manager: NotificationManager
    private var now = 0L
    @Before fun setup() {
        context = ApplicationProvider.getApplicationContext()
        manager = context.getSystemService(NotificationManager::class.java)
        shadowOf(manager).setNotificationsEnabled(true)
        manager.cancelAll()
        context.getSharedPreferences("prayer_deliveries_v2", 0).edit().clear().commit()
        context.getSharedPreferences("alarm_prefs", 0).edit().clear()
            .putInt("key_exact_sound_Öğle", 0).putInt("key_10min_sound_Öğle", 0).commit()
        now = System.currentTimeMillis()
    }
    private fun deliver(warning: Boolean = false, time: Long = now) {
        PrayerNotificationReceiver().onReceive(context,
            Intent().setAction(if (warning) PrayerAlarmScheduler.ACTION_WARNING_PRAYER_ALARM else PrayerAlarmScheduler.ACTION_EXACT_PRAYER_ALARM)
                .putExtra(PrayerAlarmScheduler.EXTRA_PRAYER_NAME, "Öğle")
                .putExtra(PrayerAlarmScheduler.EXTRA_PRAYER_TIME_MILLIS, time))
    }
    @Test fun dismissedAlertDoesNotReappearOnRepeatedBroadcast() {
        deliver()
        manager.cancel(PrayerNotificationReceiver.NOTIFICATION_ID_EXACT)
        deliver(time = now - 1_000)
        assertEquals(0, shadowOf(manager).size())
    }
    @Test fun warningAndPrayerAreSeparateDismissibleChildrenWithoutTimeout() {
        deliver(true, now + 600_000)
        deliver()
        val warning = shadowOf(manager).getNotification(PrayerNotificationReceiver.NOTIFICATION_ID_WARNING)
        val exact = shadowOf(manager).getNotification(PrayerNotificationReceiver.NOTIFICATION_ID_EXACT)
        assertNotNull(warning)
        assertNotNull(exact)
        assertEquals(PrayerNotificationReceiver.PRAYER_GROUP, exact.group)
        assertEquals(0, exact.flags and Notification.FLAG_ONGOING_EVENT)
        assertTrue(exact.flags and Notification.FLAG_AUTO_CANCEL != 0)
        assertEquals(0L, exact.timeoutAfter)
        assertEquals(0, exact.defaults and Notification.DEFAULT_SOUND)
    }
    @Test fun disabledAndLateAlarmsStaySilent() {
        context.getSharedPreferences("alarm_prefs", 0).edit().putBoolean("key_exact_enabled_Öğle", false).commit()
        deliver()
        deliver(true, now - 600_000)
        assertEquals(0, shadowOf(manager).size())
    }
    @Test fun obsoleteAutoDismissCannotCancelCountdownOrAlerts() {
        deliver()
        PrayerNotificationReceiver().onReceive(context, Intent("com.example.ACTION_AUTO_DISMISS")
            .putExtra("EXTRA_DISMISS_ID", PrayerNotificationReceiver.NOTIFICATION_ID_EXACT))
        assertNotNull(shadowOf(manager).getNotification(PrayerNotificationReceiver.NOTIFICATION_ID_EXACT))
    }

    @Test fun countdownStaysIndependentWhileBothAlertsAreVisibleAndDismissed() {
        val controller = org.robolectric.Robolectric.buildService(CountdownService::class.java).create()
        try {
            val countdown = shadowOf(controller.get()).lastForegroundNotification
            manager.notify(1001, countdown)
            deliver(true, now + 600_000)
            deliver()
            assertEquals(3, shadowOf(manager).size())
            val active = shadowOf(manager).getNotification(1001)
            assertEquals(0, active.flags and Notification.FLAG_GROUP_SUMMARY)
            assertNotEquals(active.group, shadowOf(manager).getNotification(2001).group)
            assertNotEquals(active.group, shadowOf(manager).getNotification(3001).group)
            manager.cancel(2001)
            manager.cancel(3001)
            assertNotNull(shadowOf(manager).getNotification(1001))
            assertEquals(1, shadowOf(manager).size())
        } finally { controller.destroy() }
    }
}
