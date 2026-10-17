package com.example.ui

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class CompassTracker(context: Context) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager

    fun hasCompassSensors(): Boolean {
        val sm = sensorManager ?: return false
        val rotation = sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val accel = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnetic = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        return rotation != null || (accel != null && magnetic != null)
    }

    val azimuthFlow: Flow<Float> = callbackFlow {
        val sm = sensorManager
        if (sm == null) {
            trySend(0f)
            close()
            return@callbackFlow
        }

        val rotationSensor = sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val accelerometer = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnetometer = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        val listener = object : SensorEventListener {
            private val gravity = FloatArray(3)
            private val geomagnetic = FloatArray(3)
            private var hasGravity = false
            private var hasGeomagnetic = false

            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null) return

                if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                    val rotationMatrix = FloatArray(9)
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    
                    val orientationAngles = FloatArray(3)
                    SensorManager.getOrientation(rotationMatrix, orientationAngles)
                    
                    var azimuth = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
                    if (azimuth < 0) azimuth += 360f
                    
                    trySend(azimuth)
                } else {
                    // Fallback using Accelerometer & Magnetometer
                    if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                        System.arraycopy(event.values, 0, gravity, 0, event.values.size)
                        hasGravity = true
                    } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                        System.arraycopy(event.values, 0, geomagnetic, 0, event.values.size)
                        hasGeomagnetic = true
                    }

                    if (hasGravity && hasGeomagnetic) {
                        val r = FloatArray(9)
                        val i = FloatArray(9)
                        if (SensorManager.getRotationMatrix(r, i, gravity, geomagnetic)) {
                            val orientation = FloatArray(3)
                            SensorManager.getOrientation(r, orientation)
                            var azimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()
                            if (azimuth < 0) azimuth += 360f
                            trySend(azimuth)
                        }
                    }
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        if (rotationSensor != null) {
            sm.registerListener(listener, rotationSensor, SensorManager.SENSOR_DELAY_UI)
        } else {
            // Register fallback sensors
            if (accelerometer != null) {
                sm.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)
            }
            if (magnetometer != null) {
                sm.registerListener(listener, magnetometer, SensorManager.SENSOR_DELAY_UI)
            }
        }

        awaitClose {
            try {
                sm.unregisterListener(listener)
            } catch (_: Exception) {}
        }
    }

    companion object {
        fun calculateQiblaAngle(lat: Double, lng: Double): Float {
            // Mecca coordinates
            val meccaLat = Math.toRadians(21.422487)
            val meccaLng = Math.toRadians(39.826206)
            
            val userLat = Math.toRadians(lat)
            val userLng = Math.toRadians(lng)
            
            val dLng = meccaLng - userLng
            
            val y = sin(dLng) * cos(meccaLat)
            val x = cos(userLat) * sin(meccaLat) - sin(userLat) * cos(meccaLat) * cos(dLng)
            
            var angle = Math.toDegrees(atan2(y, x)).toFloat()
            if (angle < 0) angle += 360f
            return angle
        }
    }
}
