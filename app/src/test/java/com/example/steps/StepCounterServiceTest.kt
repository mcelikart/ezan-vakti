package com.example.steps

import android.Manifest
import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.SystemClock
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSensor
import org.robolectric.shadows.ShadowSensorManager
import org.robolectric.shadows.ShadowSystemClock
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class StepCounterServiceTest {
    @Test fun counterIsAuthoritativeWithBothSensorsAndRejectsMalformedEvents() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        shadowOf(app).grantPermissions(Manifest.permission.ACTIVITY_RECOGNITION)
        val sensors = shadowOf(app.getSystemService(SensorManager::class.java))
        val counter = ShadowSensor.newInstance(Sensor.TYPE_STEP_COUNTER)
        val detector = ShadowSensor.newInstance(Sensor.TYPE_STEP_DETECTOR)
        sensors.addSensor(counter)
        sensors.addSensor(detector)
        val store = StepStore.get(app)
        store.writableDatabase.delete("daily_steps", null, null)
        store.resetBaseline()
        StepCounterService.settings(app).edit().putBoolean("enabled", true).commit()
        val controller = Robolectric.buildService(StepCounterService::class.java).create()
        val service = controller.get()
        fun send(sensor: Sensor, value: Float, stamp: Long = SystemClock.elapsedRealtimeNanos()) {
            val event = ShadowSensorManager.createSensorEvent(1)
            event.sensor = sensor
            event.values[0] = value
            event.timestamp = stamp
            service.onSensorChanged(event)
        }
        try {
            service.onStartCommand(null, 0, 1)
            assertTrue(sensors.hasListener(service, counter))
            assertTrue(sensors.hasListener(service, detector))
            send(counter, 100f)
            repeat(3) {
                ShadowSystemClock.advanceBy(Duration.ofSeconds(1))
                send(detector, 1f)
            }
            send(counter, 103f)
            send(counter, 103f)
            send(counter, Float.NaN)
            send(counter, 1000f, SystemClock.elapsedRealtimeNanos() + 20_000_000_000L)
            send(counter, 103.5f)
            send(detector, 2f)
            assertEquals(3L, store.total())
            ShadowSystemClock.advanceBy(Duration.ofSeconds(1))
            send(counter, 104f)
            assertEquals(4L, store.total())
        } finally {
            StepCounterService.settings(app).edit().putBoolean("enabled", false).commit()
            controller.destroy()
        }
        assertTrue(sensors.listeners.isEmpty())
    }

    @Test fun trackingIsStickyAndPublishesNoNotificationAtAll() {
        // The product decision: the pedometer must not appear in the shade as a
        // second, empty notification. It therefore must not be a foreground
        // service, because Android forces a foreground service to post a
        // notification and an empty one still renders as a blank row. The daily
        // total is shown inside the prayer countdown notification instead.
        val app = ApplicationProvider.getApplicationContext<Application>()
        shadowOf(app).grantPermissions(Manifest.permission.ACTIVITY_RECOGNITION)
        val sensors = shadowOf(app.getSystemService(SensorManager::class.java))
        sensors.addSensor(ShadowSensor.newInstance(Sensor.TYPE_STEP_COUNTER))
        StepCounterService.settings(app).edit().putBoolean("enabled", true).commit()
        val controller = Robolectric.buildService(StepCounterService::class.java).create()
        try {
            val service = controller.get()
            assertEquals(Service.START_STICKY, service.onStartCommand(null, 0, 1))
            service.onStartCommand(null, 0, 2)

            // No notification is posted at all.
            assertNull(shadowOf(service).lastForegroundNotification)

            // Sensor tracking itself is unaffected.
            assertEquals(1, sensors.listeners.size)
        } finally {
            StepCounterService.settings(app).edit().putBoolean("enabled", false).commit()
            controller.destroy()
        }
        assertEquals(0, sensors.listeners.size)
    }

    @Test fun missingPermissionStopsWithoutRegisteringSensor() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        shadowOf(app).denyPermissions(Manifest.permission.ACTIVITY_RECOGNITION)
        StepCounterService.settings(app).edit().putBoolean("enabled", true).commit()
        val controller = Robolectric.buildService(StepCounterService::class.java).create()
        try {
            assertEquals(Service.START_NOT_STICKY, controller.get().onStartCommand(null, 0, 1))
            assertTrue(shadowOf(controller.get()).isStoppedBySelf)
        } finally {
            StepCounterService.settings(app).edit().putBoolean("enabled", false).commit()
            controller.destroy()
        }
    }
}
