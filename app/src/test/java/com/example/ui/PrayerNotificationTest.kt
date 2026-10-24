package com.example.ui

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
        listOf("prayer_deliveries_v2", "prayer_daily_times", "countdown_service_prefs")
            .forEach { context.getSharedPreferences(it, 0).edit().clear().commit() }
        context.getSharedPreferences("alarm_prefs", 0).edit().clear()
            .putInt("key_exact_sound_Öğle", 0).putInt("key_10min_sound_Öğle", 0)
            .putBoolean("key_vibration_enabled_Öğle", false).commit()
        now = System.currentTimeMillis() / 60_000L * 60_000L
    }

    private fun cache(time: Long) {
        val clock = SimpleDateFormat("HH:mm", Locale.US).format(Date(time))
        context.getSharedPreferences("prayer_daily_times", 0).edit()
            .putString(PrayerTimeCache.date(time), "Öğle,$clock").commit()
    }

    private fun deliver(warning: Boolean = false, time: Long = now, updateCache: Boolean = true) {
        if (updateCache) cache(time)
        PrayerNotificationReceiver().onReceive(context,
            Intent().setAction(if (warning) PrayerAlarmScheduler.ACTION_WARNING_PRAYER_ALARM else PrayerAlarmScheduler.ACTION_EXACT_PRAYER_ALARM)
                .putExtra(PrayerAlarmScheduler.EXTRA_PRAYER_NAME, "Öğle")
                .putExtra(PrayerAlarmScheduler.EXTRA_PRAYER_TIME_MILLIS, time))
    }

    @Test fun dismissedAlertDoesNotReappearOnRepeatedBroadcast() {
        deliver()
        manager.cancel(PrayerNotificationReceiver.NOTIFICATION_ID_EXACT)
        deliver()
        assertEquals(0, shadowOf(manager).size())
    }

    @Test fun warningIsReplacedByPrayerAndBothHavePrayerBasedExpiry() {
        val prayerTime = now + 600_000L
        deliver(true, prayerTime)
        val warning = shadowOf(manager).getNotification(PrayerNotificationReceiver.NOTIFICATION_ID_WARNING)
        assertNotNull(warning)
        assertEquals(prayerTime + 300_000L, warning.extras.getLong(PrayerAlertExpiry.EXTRA_EXPIRES_AT))
        assertTrue(warning.timeoutAfter in 840_000L..900_000L)

        deliver()
        val exact = shadowOf(manager).getNotification(PrayerNotificationReceiver.NOTIFICATION_ID_EXACT)
        assertNotNull(exact)
        assertNull(shadowOf(manager).getNotification(PrayerNotificationReceiver.NOTIFICATION_ID_WARNING))
        assertEquals(PrayerNotificationReceiver.PRAYER_GROUP, exact.group)
        assertEquals(0, exact.flags and Notification.FLAG_ONGOING_EVENT)
        assertTrue(exact.flags and Notification.FLAG_AUTO_CANCEL != 0)
        assertEquals(now + 300_000L, exact.extras.getLong(PrayerAlertExpiry.EXTRA_EXPIRES_AT))
        assertTrue(exact.timeoutAfter in 240_000L..300_000L)
        assertEquals(0, exact.defaults and Notification.DEFAULT_SOUND)
        val channel = manager.getNotificationChannel(exact.channelId)
        assertEquals(NotificationManager.IMPORTANCE_DEFAULT, channel.importance)
        assertNull(channel.sound)
    }

    @Test fun delayedPrayerDeliveryDoesNotExtendExpiryPastFiveMinutesAfterPrayer() {
        deliver(time = now - 120_000L)
        val exact = shadowOf(manager).getNotification(PrayerNotificationReceiver.NOTIFICATION_ID_EXACT)
        assertNotNull(exact)
        assertEquals(now + 180_000L, exact.extras.getLong(PrayerAlertExpiry.EXTRA_EXPIRES_AT))
        assertTrue(exact.timeoutAfter in 120_000L..180_000L)
    }

    @Test fun disabledEarlyLateAndSupersededAlarmsStaySilent() {
        context.getSharedPreferences("alarm_prefs", 0).edit().putBoolean("key_exact_enabled_Öğle", false).commit()
        deliver()
        context.getSharedPreferences("alarm_prefs", 0).edit().putBoolean("key_exact_enabled_Öğle", true).commit()
        deliver(time = now + 60_000L)
        deliver(time = now - 300_000L)
        deliver(true, now - 600_000L)
        cache(now + 60_000L)
        deliver(updateCache = false)
        assertEquals(0, shadowOf(manager).size())
    }

    @Test fun obsoleteAutoDismissCannotCancelCountdownOrAlerts() {
        deliver()
        PrayerNotificationReceiver().onReceive(context, Intent("com.example.ACTION_AUTO_DISMISS")
            .putExtra("EXTRA_DISMISS_ID", PrayerNotificationReceiver.NOTIFICATION_ID_EXACT))
        assertNotNull(shadowOf(manager).getNotification(PrayerNotificationReceiver.NOTIFICATION_ID_EXACT))
    }

    @Test fun prayerAlertNeverRestartsAnExistingCountdownService() {
        manager.notify(1001, androidx.core.app.NotificationCompat.Builder(context, CountdownService.CHANNEL_ID)
            .setSmallIcon(com.example.R.drawable.ic_notification).setContentTitle("Sayaç").build())
        deliver()
        assertNull(shadowOf(context as android.app.Application).nextStartedService)
    }

    @Test fun countdownIsSilentIndependentAndRanksAboveTemporaryPrayerAlert() {
        val controller = Robolectric.buildService(CountdownService::class.java).create()
        try {
            val countdown = shadowOf(controller.get()).lastForegroundNotification
            manager.notify(1001, countdown)
            deliver()
            val active = shadowOf(manager).getNotification(1001)
            val alert = shadowOf(manager).getNotification(2001)
            assertSame(countdown, active)
            assertEquals(0, active.flags and Notification.FLAG_GROUP_SUMMARY)
            assertNotEquals(active.group, alert.group)
            assertTrue(active.priority > alert.priority)
            assertTrue(active.sortKey < alert.sortKey)
            assertEquals(manager.getNotificationChannel(active.channelId).importance,
                manager.getNotificationChannel(alert.channelId).importance)
            assertNull(manager.getNotificationChannel(active.channelId).sound)
            assertFalse(manager.getNotificationChannel(active.channelId).shouldVibrate())
            assertEquals(0L, active.timeoutAfter)
            assertTrue(active.flags and Notification.FLAG_ONGOING_EVENT != 0)
            assertTrue(active.flags and Notification.FLAG_NO_CLEAR != 0)
            manager.cancel(2001)
            assertNotNull(shadowOf(manager).getNotification(1001))
            assertEquals(1, shadowOf(manager).size())
        } finally { controller.destroy() }
    }

    @Test fun countdownRemainsSilentAfterServiceRecreation() {
        repeat(2) {
            val controller = Robolectric.buildService(CountdownService::class.java).create()
            try {
                val countdown = shadowOf(controller.get()).lastForegroundNotification
                val channel = manager.getNotificationChannel(countdown.channelId)
                assertNull(channel.sound)
                assertFalse(channel.shouldVibrate())
                assertEquals(NotificationManager.IMPORTANCE_DEFAULT, channel.importance)
                assertEquals(0, countdown.defaults and (Notification.DEFAULT_SOUND or Notification.DEFAULT_VIBRATE))
                assertEquals(0, countdown.publicVersion.defaults and (Notification.DEFAULT_SOUND or Notification.DEFAULT_VIBRATE))
            } finally { controller.destroy() }
        }
    }
}
