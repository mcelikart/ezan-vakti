package com.example.ui

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

/** Cancels only the temporary alert generation that scheduled this expiry. */
internal object PrayerAlertExpiry {
    const val ACTION = "com.example.ACTION_EXPIRE_PRAYER_ALERT_V1"
    const val EXTRA_NOTIFICATION_ID = "prayer_alert_notification_id"
    const val EXTRA_EXPIRES_AT = "prayer_alert_expires_at"
    const val AFTER_PRAYER_MILLIS = 5 * 60_000L

    fun clearStaleAlerts(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val now = System.currentTimeMillis()
        manager.activeNotifications.filter {
            (it.id == PrayerNotificationReceiver.NOTIFICATION_ID_EXACT ||
                it.id == PrayerNotificationReceiver.NOTIFICATION_ID_WARNING) &&
                it.notification.extras.getLong(EXTRA_EXPIRES_AT, 0L) <= now
        }.forEach { manager.cancel(it.id) }
    }

    fun schedule(context: Context, notificationId: Int, expiresAt: Long) {
        val alarms = context.getSystemService(AlarmManager::class.java)
        val operation = pendingIntent(context, notificationId, expiresAt)
        // Native notification timeout handles API 26+; this also covers API 24/25
        // and still runs after the application process has been stopped.
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms()) {
                alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, expiresAt, operation)
            } else {
                alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, expiresAt, operation)
            }
        } catch (_: SecurityException) {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, expiresAt, operation)
        }
    }

    fun cancel(context: Context, notificationId: Int) {
        context.getSystemService(NotificationManager::class.java).cancel(notificationId)
        context.getSystemService(AlarmManager::class.java)
            .cancel(pendingIntent(context, notificationId, 0L))
    }

    fun receive(context: Context, intent: Intent) {
        val id = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        if (id != PrayerNotificationReceiver.NOTIFICATION_ID_EXACT &&
            id != PrayerNotificationReceiver.NOTIFICATION_ID_WARNING) return
        val expiresAt = intent.getLongExtra(EXTRA_EXPIRES_AT, 0L)
        if (expiresAt <= 0L || System.currentTimeMillis() < expiresAt) return
        val manager = context.getSystemService(NotificationManager::class.java)
        val active = manager.activeNotifications.firstOrNull { it.id == id } ?: return
        if (active.notification.extras.getLong(EXTRA_EXPIRES_AT) == expiresAt) {
            manager.cancel(id)
        }
    }

    fun attach(notification: Notification, expiresAt: Long) {
        notification.extras.putLong(EXTRA_EXPIRES_AT, expiresAt)
    }

    private fun pendingIntent(context: Context, id: Int, expiresAt: Long): PendingIntent =
        PendingIntent.getBroadcast(context, id,
            Intent(context, PrayerNotificationReceiver::class.java).setAction(ACTION)
                .putExtra(EXTRA_NOTIFICATION_ID, id)
                .putExtra(EXTRA_EXPIRES_AT, expiresAt),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
}
