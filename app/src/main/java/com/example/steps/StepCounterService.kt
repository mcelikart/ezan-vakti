package com.example.steps

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.ui.BackgroundServices

class StepCounterService : Service(), SensorEventListener {
    private lateinit var sensors: SensorManager
    private lateinit var worker: HandlerThread
    private lateinit var handler: Handler
    private lateinit var store: StepStore
    private var listening = false
    private var lastUpdate = 0L
    private var boot = 0
    private var primarySensor: Sensor? = null
    private val refresh = object : Runnable {
        override fun run() {
            if (listening) sensors.flush(this@StepCounterService)
            publish()
            handler.postDelayed(this, 30_000L)
        }
    }

    override fun onCreate() {
        super.onCreate()
        sensors = getSystemService(SensorManager::class.java)
        store = StepStore.get(this)
        worker = HandlerThread("StepCounter").apply { start() }
        handler = Handler(worker.looper)
        boot = Settings.Global.getInt(contentResolver, Settings.Global.BOOT_COUNT, 0)
        BackgroundServices.armWatchdog(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            // Legacy channels from when the pedometer had its own notification.
            manager.deleteNotificationChannel("daily_steps_v1")
            manager.deleteNotificationChannel("daily_steps_v2")
            manager.deleteNotificationChannel(CHANNEL)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!enabled(this) || !hasPermission(this) || sensor(this) == null) {
            stopSelf()
            return START_NOT_STICKY
        }
        BackgroundServices.armWatchdog(this)
        // Deliberately NOT a foreground service.
        //
        // A foreground service is forced to post a notification, and an empty
        // one still shows as a blank row in the shade — which is exactly the
        // leftover the pedometer was supposed to stop producing. The visible
        // step total lives inside the prayer countdown notification, so this
        // service has nothing to show.
        //
        // This is safe because the step sensor is cumulative: if Android reclaims
        // the service, the next app resume restarts it and StepCounterMath diffs
        // the new total against the stored checkpoint, so no steps are lost or
        // counted twice.
        if (!listening) {
            try {
                primarySensor = sensor(this)
                listening = sensors.registerListener(this, primarySensor, SensorManager.SENSOR_DELAY_NORMAL, handler)
                if (!listening && primarySensor?.type == Sensor.TYPE_STEP_COUNTER) {
                    primarySensor = preferredSensor(sensors, Sensor.TYPE_STEP_DETECTOR)
                    if (primarySensor != null) listening = sensors.registerListener(this, primarySensor, SensorManager.SENSOR_DELAY_NORMAL, handler)
                }
                if (listening && primarySensor?.type == Sensor.TYPE_STEP_COUNTER) {
                    preferredSensor(sensors, Sensor.TYPE_STEP_DETECTOR)?.let {
                        sensors.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL, handler)
                    }
                }
            } catch (error: SecurityException) {
                Log.w("StepCounter", "Activity permission revoked", error)
            }
            if (!listening) {
                settings(this).edit().putBoolean("enabled", false).apply()
                stopSelf()
                return START_NOT_STICKY
            }
            settings(this).edit().putString("sensor_mode",
                if (primarySensor?.type == Sensor.TYPE_STEP_COUNTER) "Donanım sayacı" else "Adım algılayıcı").apply()
            handler.post(refresh)
        } else handler.post { publish() }
        return START_STICKY
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.values.isEmpty() || !event.values[0].isFinite() || event.values[0] < 0) return
        if (event.timestamp < 0 || event.timestamp > SystemClock.elapsedRealtimeNanos() + 2_000_000_000L) return
        if (event.sensor.type != Sensor.TYPE_STEP_COUNTER && event.sensor.type != Sensor.TYPE_STEP_DETECTOR) return
        if (event.sensor.type == Sensor.TYPE_STEP_DETECTOR && event.values[0] != 1f) return
        if (event.sensor.type == Sensor.TYPE_STEP_COUNTER && event.values[0] % 1f != 0f) return
        val eventTime = System.currentTimeMillis() -
            (SystemClock.elapsedRealtimeNanos() - event.timestamp).coerceAtLeast(0L) / 1_000_000L
        val sample = CounterSample(event.values[0].toLong(), boot, event.timestamp)
        if (primarySensor?.type == Sensor.TYPE_STEP_COUNTER && event.sensor.type == Sensor.TYPE_STEP_DETECTOR) {
            store.observeDetector(sample, StepStore.dayKey(eventTime))
            return
        }
        if (event.sensor.type != primarySensor?.type) return
        store.record(sample, StepStore.dayKey(eventTime), event.sensor.type == Sensor.TYPE_STEP_DETECTOR)
        if (SystemClock.elapsedRealtime() - lastUpdate >= 5_000L) publish()
    }

    private fun publish() {
        // The countdown notification owns the step row, so there is nothing to
        // publish here. Kept as a hook because the sensor callback and the
        // periodic refresh both route through it.
        lastUpdate = SystemClock.elapsedRealtime()
    }


    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onTaskRemoved(rootIntent: Intent?) {
        BackgroundServices.scheduleRecovery(this)
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        sensors.unregisterListener(this)
        handler.removeCallbacksAndMessages(null)
        worker.quitSafely()
        if (enabled(this) && hasPermission(this)) BackgroundServices.scheduleRecovery(this)
        super.onDestroy()
    }

    companion object {
        const val NOTIFICATION_ID = 4001
        const val ACTION_OPEN_STEPS = "com.example.OPEN_STEPS"
        internal const val CHANNEL = "daily_steps_main_v3"
        fun settings(context: Context) = context.getSharedPreferences("step_settings", Context.MODE_PRIVATE)
        fun enabled(context: Context) = settings(context).getBoolean("enabled", false)
        fun hasPermission(context: Context) = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
        fun sensor(context: Context): Sensor? = context.getSystemService(SensorManager::class.java).let {
            preferredSensor(it, Sensor.TYPE_STEP_COUNTER) ?: preferredSensor(it, Sensor.TYPE_STEP_DETECTOR)
        }
        private fun preferredSensor(manager: SensorManager, type: Int): Sensor? =
            manager.getDefaultSensor(type, true) ?: manager.getDefaultSensor(type)
        fun startIfEnabled(context: Context) {
            if (!enabled(context) || !hasPermission(context) || sensor(context) == null) return
            try {
                // Plain startService, not startForegroundService: this service
                // owns no notification, and a foreground start from the
                // background throws on Android 8+.
                context.startService(Intent(context, StepCounterService::class.java))
            } catch (error: IllegalStateException) {
                // Started from the background (legacy broadcast path). The next
                // app resume restarts it, and the cumulative sensor counter means
                // nothing is lost in the meantime.
                Log.w("StepCounter", "Deferred to next foreground resume", error)
            } catch (error: SecurityException) {
                Log.w("StepCounter", "Permission unavailable", error)
            }
        }
    }
}

/**
 * Kept only so an old `setDeleteIntent` PendingIntent from a previous install
 * cannot crash the app. The pedometer no longer posts a dismissible
 * notification, so this does nothing beyond restarting the service.
 */
class StepNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = StepCounterService.startIfEnabled(context)
}
