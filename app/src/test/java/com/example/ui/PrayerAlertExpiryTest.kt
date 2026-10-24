package com.example.ui

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import com.example.R
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [24, 36])
class PrayerAlertExpiryTest {
    private lateinit var context: Context
    private lateinit var manager: NotificationManager

    @Before fun setup() {
        context = ApplicationProvider.getApplicationContext()
        manager = context.getSystemService(NotificationManager::class.java)
        manager.cancelAll()
    }

    private fun show(id: Int, expiresAt: Long) {
        val notification = NotificationCompat.Builder(context, "expiry_test")
            .setSmallIcon(R.drawable.ic_notification).setContentTitle("Test").build()
        PrayerAlertExpiry.attach(notification, expiresAt)
        manager.notify(id, notification)
    }

    private fun expire(id: Int, expiresAt: Long) {
        PrayerNotificationReceiver().onReceive(context, Intent(PrayerAlertExpiry.ACTION)
            .putExtra(PrayerAlertExpiry.EXTRA_NOTIFICATION_ID, id)
            .putExtra(PrayerAlertExpiry.EXTRA_EXPIRES_AT, expiresAt))
    }

    @Test fun expiryOnlyRemovesMatchingTemporaryAlertAndKeepsCountdown() {
        val expiry = System.currentTimeMillis() - 1L
        show(1001, expiry)
        show(2001, expiry)
        show(3001, expiry)
        expire(2001, expiry)
        assertNull(shadowOf(manager).getNotification(2001))
        assertNotNull(shadowOf(manager).getNotification(1001))
        assertNotNull(shadowOf(manager).getNotification(3001))
        expire(3001, expiry)
        assertNull(shadowOf(manager).getNotification(3001))
        assertNotNull(shadowOf(manager).getNotification(1001))
    }

    @Test fun staleOrEarlyExpiryCannotRemoveNewerPrayerAlertOrCountdown() {
        val oldExpiry = System.currentTimeMillis() - 1L
        val newExpiry = oldExpiry + 900_000L
        show(2001, newExpiry)
        show(1001, oldExpiry)
        expire(2001, oldExpiry)
        expire(2001, newExpiry)
        expire(1001, oldExpiry)
        assertNotNull(shadowOf(manager).getNotification(2001))
        assertNotNull(shadowOf(manager).getNotification(1001))
    }

    @Test fun serviceRecoveryClearsLegacyAndExpiredAlertsButPreservesLiveAlertsAndCountdown() {
        show(1001, 0L)
        show(2001, 0L)
        show(3001, System.currentTimeMillis() + 300_000L)
        PrayerAlertExpiry.clearStaleAlerts(context)
        assertNotNull(shadowOf(manager).getNotification(1001))
        assertNull(shadowOf(manager).getNotification(2001))
        assertNotNull(shadowOf(manager).getNotification(3001))
        show(3001, System.currentTimeMillis() - 1L)
        PrayerAlertExpiry.clearStaleAlerts(context)
        assertNull(shadowOf(manager).getNotification(3001))
        assertNotNull(shadowOf(manager).getNotification(1001))
    }

    @Test fun fallbackExpiryIsScheduledAtPrayerPlusFiveMinutesAndReplacementCancelsOldAlarm() {
        val expiry = System.currentTimeMillis() + 300_000L
        PrayerAlertExpiry.schedule(context, 3001, expiry)
        val alarms = shadowOf(context.getSystemService(AlarmManager::class.java))
        assertEquals(1, alarms.scheduledAlarms.size)
        assertEquals(expiry, alarms.scheduledAlarms.single().triggerAtTime)
        PrayerAlertExpiry.cancel(context, 3001)
        assertTrue(alarms.scheduledAlarms.isEmpty())
    }
}
