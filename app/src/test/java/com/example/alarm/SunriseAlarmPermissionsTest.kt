package com.example.alarm

import android.Manifest
import android.app.AlarmManager
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadows.ShadowAlarmManager
import org.robolectric.shadows.ShadowNotificationManager

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], shadows = [SunrisePermissionNotificationShadow::class])
class SunriseAlarmPermissionsTest {
    private lateinit var context: Context
    private lateinit var manager: NotificationManager

    @Before fun setup() {
        context = ApplicationProvider.getApplicationContext<Application>()
        manager = context.getSystemService(NotificationManager::class.java)
        shadowOf(manager).setNotificationsEnabled(true)
        if (Build.VERSION.SDK_INT >= 33) shadowOf(context as Application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        if (Build.VERSION.SDK_INT >= 31) ShadowAlarmManager.setCanScheduleExactAlarms(true)
        SunrisePermissionNotificationShadow.fullScreenAllowed = true
        shadowOf(manager).setNotificationPolicyAccessGranted(true)
        manager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
        shadowOf(manager).setNotificationPolicyAccessGranted(false)
    }

    @Test
    @Config(sdk = [24])
    fun androidSevenNeedsNoLaterSpecialPermissionsButRespectsGlobalNotifications() {
        assertTrue(SunriseAlarmPermissions.canScheduleExact(context))
        assertTrue(SunriseAlarmPermissions.canFullScreen(context))
        assertTrue(SunriseAlarmPermissions.canNotify(context))
        shadowOf(manager).setNotificationsEnabled(false)
        assertFalse(SunriseAlarmPermissions.canNotify(context))
    }

    @Test
    @Config(sdk = [31])
    fun exactAlarmAccessTracksAndroidTwelveSpecialPermission() {
        ShadowAlarmManager.setCanScheduleExactAlarms(false)
        assertFalse(SunriseAlarmPermissions.canScheduleExact(context))
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        assertTrue(SunriseAlarmPermissions.canScheduleExact(context))
    }

    @Test
    @Config(sdk = [33])
    fun notificationRuntimeDenialPreventsReadyEvenWhenGlobalNotificationsAreEnabled() {
        shadowOf(context as Application).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        assertFalse(SunriseAlarmPermissions.canNotify(context))
        shadowOf(context as Application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        assertTrue(SunriseAlarmPermissions.canNotify(context))
    }

    @Test fun blockedRingingChannelPreventsReadinessButAnUncreatedChannelDoesNot() {
        assertTrue(SunriseAlarmPermissions.canNotify(context))
        manager.createNotificationChannel(NotificationChannel(
            SunriseAlarmRingingService.CHANNEL_ID, "Alarm", NotificationManager.IMPORTANCE_NONE
        ))
        assertFalse(SunriseAlarmPermissions.canNotify(context))
        val intent = SunriseAlarmPermissions.notificationSettingsIntent(context)
        assertEquals(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS, intent.action)
        assertEquals(SunriseAlarmRingingService.CHANNEL_ID, intent.getStringExtra(Settings.EXTRA_CHANNEL_ID))
    }

    @Test fun globalNotificationBlockOpensAppSettingsInsteadOfOnlyTheChannel() {
        shadowOf(manager).setNotificationsEnabled(false)
        assertFalse(SunriseAlarmPermissions.canNotify(context))
        val intent = SunriseAlarmPermissions.notificationSettingsIntent(context)
        assertEquals(Settings.ACTION_APP_NOTIFICATION_SETTINGS, intent.action)
        assertEquals(context.packageName, intent.getStringExtra(Settings.EXTRA_APP_PACKAGE))
    }

    @Test fun fullscreenIsOptionalForArmingButItsAndroidFourteenSettingIsReported() {
        SunrisePermissionNotificationShadow.fullScreenAllowed = false
        val readiness = SunriseAlarmPermissions.read(context)
        assertFalse(readiness.fullScreen)
        assertTrue(readiness.canArm)
        SunrisePermissionNotificationShadow.fullScreenAllowed = true
        assertTrue(SunriseAlarmPermissions.canFullScreen(context))
        assertEquals(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, SunriseAlarmPermissions.fullScreenSettingsIntent(context).action)
    }

    @Test fun exactAndFullscreenSettingsTargetThisAppOnly() {
        val exact = SunriseAlarmPermissions.exactSettingsIntent(context)
        val fullscreen = SunriseAlarmPermissions.fullScreenSettingsIntent(context)
        assertEquals(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, exact.action)
        assertEquals("package:${context.packageName}", exact.data.toString())
        assertEquals("package:${context.packageName}", fullscreen.data.toString())
    }

    @Test fun dndThatDisallowsAlarmsIsReadWithoutChangingDndOrAskingForPolicyAccess() {
        shadowOf(manager).setNotificationPolicyAccessGranted(true)
        manager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
        shadowOf(manager).setConsolidatedNotificationPolicy(NotificationManager.Policy(0, 0, 0))
        shadowOf(manager).setNotificationPolicyAccessGranted(false)
        assertTrue(SunriseAlarmPermissions.dndSuppressesAlarms(context))
        assertFalse(manager.isNotificationPolicyAccessGranted)
        assertEquals(NotificationManager.INTERRUPTION_FILTER_PRIORITY, manager.currentInterruptionFilter)
        shadowOf(manager).setConsolidatedNotificationPolicy(NotificationManager.Policy(NotificationManager.Policy.PRIORITY_CATEGORY_ALARMS, 0, 0))
        assertFalse(SunriseAlarmPermissions.dndSuppressesAlarms(context))
    }

    @Test fun totalSilenceIsBlockedWhileAlarmsOnlyModeIsAllowed() {
        shadowOf(manager).setNotificationPolicyAccessGranted(true)
        manager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)
        assertTrue(SunriseAlarmPermissions.dndSuppressesAlarms(context))
        manager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALARMS)
        assertFalse(SunriseAlarmPermissions.dndSuppressesAlarms(context))
    }
}

/** Robolectric does not yet model this AppOps-backed Android 14 API. */
@Implements(NotificationManager::class)
class SunrisePermissionNotificationShadow : ShadowNotificationManager() {
    @Implementation(minSdk = 34)
    protected fun canUseFullScreenIntent(): Boolean = fullScreenAllowed

    companion object { var fullScreenAllowed: Boolean = true }
}
