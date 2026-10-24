package com.example.alarm

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/** Full-screen access improves lock-screen presentation; it is not required to ring. */
internal data class SunriseAlarmReadiness(
    val exact: Boolean,
    val notifications: Boolean,
    val fullScreen: Boolean,
    val dndBlocksAlarms: Boolean
) {
    val canArm: Boolean get() = exact && notifications
}

object SunriseAlarmPermissions {
    fun canScheduleExact(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            runCatching { context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms() }.getOrDefault(false)

    fun canNotify(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return false
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return true
        val manager = context.getSystemService(NotificationManager::class.java)
        // A channel that does not exist yet will be created when the first alarm rings.
        val channel = manager.getNotificationChannel(SunriseAlarmRingingService.CHANNEL_ID) ?: return true
        if (channel.importance == NotificationManager.IMPORTANCE_NONE) return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && channel.group != null &&
            manager.getNotificationChannelGroup(channel.group)?.isBlocked == true
        ) return false
        return true
    }

    fun canFullScreen(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE ||
            runCatching { context.getSystemService(NotificationManager::class.java).canUseFullScreenIntent() }.getOrDefault(false)

    /** Reads DND; never changes it or asks for broad Notification Policy access. */
    fun dndSuppressesAlarms(context: Context): Boolean {
        val manager = context.getSystemService(NotificationManager::class.java)
        return when (manager.currentInterruptionFilter) {
            NotificationManager.INTERRUPTION_FILTER_NONE -> true
            NotificationManager.INTERRUPTION_FILTER_PRIORITY -> {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) false
                else runCatching {
                    val policy = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        manager.consolidatedNotificationPolicy
                    } else manager.notificationPolicy
                    policy.priorityCategories and NotificationManager.Policy.PRIORITY_CATEGORY_ALARMS == 0
                }.getOrDefault(true)
            }
            else -> false
        }
    }

    internal fun read(context: Context) = SunriseAlarmReadiness(
        exact = canScheduleExact(context),
        notifications = canNotify(context),
        fullScreen = canFullScreen(context),
        dndBlocksAlarms = dndSuppressesAlarms(context)
    )

    internal fun exactSettingsIntent(context: Context): Intent =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, packageUri(context))
        } else appSettingsIntent(context)

    internal fun fullScreenSettingsIntent(context: Context): Intent =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, packageUri(context))
        } else appSettingsIntent(context)

    internal fun notificationSettingsIntent(context: Context): Intent {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return appSettingsIntent(context)
        val channel = context.getSystemService(NotificationManager::class.java)
            .getNotificationChannel(SunriseAlarmRingingService.CHANNEL_ID)
        val appEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
        return if (appEnabled && channel?.importance == NotificationManager.IMPORTANCE_NONE) {
            Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                .putExtra(Settings.EXTRA_CHANNEL_ID, SunriseAlarmRingingService.CHANNEL_ID)
        } else Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    }

    internal fun appSettingsIntent(context: Context) = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri(context))

    private fun packageUri(context: Context) = Uri.parse("package:${context.packageName}")

    internal fun openSettings(context: Context, requested: Intent) {
        for (intent in listOf(requested, appSettingsIntent(context))) {
            try {
                if (context.findActivity() == null) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return
            } catch (_: ActivityNotFoundException) {
                // Some manufacturers omit a particular settings activity.
            } catch (_: SecurityException) {
                // App details is available even when a manufacturer protects a special panel.
            }
        }
        Toast.makeText(context, "Telefon ayarları açılamadı.", Toast.LENGTH_LONG).show()
    }
}

@Composable
fun SunriseAlarmPermissionPanel(onPermissionsChanged: () -> Unit = {}) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val changed by rememberUpdatedState(onPermissionsChanged)
    var readiness by remember(context) { mutableStateOf(SunriseAlarmPermissions.read(context)) }
    val preferences = remember(context) { context.getSharedPreferences("sunrise_alarm_permission_ui", Context.MODE_PRIVATE) }

    fun refresh() {
        readiness = SunriseAlarmPermissions.read(context)
        changed()
    }

    val notificationsPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { refresh() }
    DisposableEffect(lifecycle, context) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) refresh() }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    SunriseAlarmPermissionContent(
        readiness = readiness,
        onExact = { SunriseAlarmPermissions.openSettings(context, SunriseAlarmPermissions.exactSettingsIntent(context)) },
        onNotifications = {
            val runtimeMissing = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            val activity = context.findActivity()
            val canAskAgain = !preferences.getBoolean("notifications_requested", false) ||
                (activity != null && ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.POST_NOTIFICATIONS))
            if (runtimeMissing && canAskAgain) {
                preferences.edit().putBoolean("notifications_requested", true).apply()
                notificationsPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                SunriseAlarmPermissions.openSettings(context, SunriseAlarmPermissions.notificationSettingsIntent(context))
            }
        },
        onFullScreen = { SunriseAlarmPermissions.openSettings(context, SunriseAlarmPermissions.fullScreenSettingsIntent(context)) },
        onDnd = { SunriseAlarmPermissions.openSettings(context, Intent(Settings.ACTION_SOUND_SETTINGS)) }
    )
}

@Composable
internal fun SunriseAlarmPermissionContent(
    readiness: SunriseAlarmReadiness,
    onExact: () -> Unit,
    onNotifications: () -> Unit,
    onFullScreen: () -> Unit,
    onDnd: () -> Unit
) {
    Column(Modifier.fillMaxWidth().testTag("sunrise-alarm-permissions"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Sessiz modda da çalar. Alarm sesi sıfırsa çalarken geçici olarak açılır.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (!readiness.exact) PermissionStep(
            "Alarmın tam saatinde çalması için izin gerekli.", "Alarm iznini aç", "sunrise-permission-exact", onExact
        )
        if (!readiness.notifications) PermissionStep(
            "Alarmı kurmak, kapatmak ve ertelemek için bildirimleri açın.", "Bildirimleri aç", "sunrise-permission-notifications", onNotifications
        )
        if (!readiness.fullScreen) PermissionStep(
            "Alarmın kilit ekranını açması için tam ekran izni verebilirsiniz.", "Kilit ekranında göster", "sunrise-permission-fullscreen", onFullScreen
        )
        if (readiness.dndBlocksAlarms) PermissionStep(
            "Rahatsız Etmeyin’de alarmlara izin verin.", "Ses ayarlarını aç", "sunrise-permission-dnd", onDnd
        )
    }
}

@Composable
private fun PermissionStep(message: String, action: String, tag: String, onClick: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().testTag(tag)
    ) {
        Column(Modifier.padding(start = 12.dp, end = 8.dp, top = 12.dp, bottom = 4.dp)) {
            Text(message, style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = onClick, modifier = Modifier.align(Alignment.End)) { Text(action) }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> if (baseContext !== this) baseContext.findActivity() else null
    else -> null
}
