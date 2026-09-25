package com.aegis.safety.core.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.aegis.safety.ai.vision.FallDetector
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.buffer

/** Streams accelerometer + gyroscope samples already packaged for FallDetector. */
@Singleton
class SensorProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accel = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyro = sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    val isAvailable: Boolean get() = accel != null

    fun imu(samplingUs: Int = SensorManager.SENSOR_DELAY_GAME): Flow<FallDetector.ImuSample> =
        callbackFlow {
            var ax = 0f; var ay = 0f; var az = 0f
            var gx = 0f; var gy = 0f; var gz = 0f
            var lastAccel = 0L
            var lastGyro = 0L

            val listener = object : SensorEventListener {
                override fun onSensorChanged(e: SensorEvent) {
                    when (e.sensor.type) {
                        Sensor.TYPE_ACCELEROMETER -> {
                            ax = e.values[0]; ay = e.values[1]; az = e.values[2]
                            lastAccel = e.timestamp
                        }
                        Sensor.TYPE_GYROSCOPE -> {
                            gx = e.values[0]; gy = e.values[1]; gz = e.values[2]
                            lastGyro = e.timestamp
                        }
                    }
                    if (lastAccel > 0 && lastGyro > 0) {
                        trySend(
                            FallDetector.ImuSample(ax, ay, az, gx, gy, gz,
                                (lastAccel / 1_000_000L))
                        )
                    }
                }
                override fun onAccuracyChanged(s: Sensor?, a: Int) = Unit
            }

            if (accel != null) sm.registerListener(listener, accel, samplingUs)
            if (gyro != null) sm.registerListener(listener, gyro, samplingUs)

            awaitClose { sm.unregisterListener(listener) }
        }
}