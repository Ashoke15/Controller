package com.diyproject.controller.joystick.compose

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

class GyroSteeringController(
    context: Context,
    private val onSteeringChanged: (steeringX: Float) -> Unit
) : SensorEventListener {

    private val sensorManager =
        context.applicationContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationSensor: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    private val rotationMatrix = FloatArray(9)
    private val orientationRad = FloatArray(3)

    private var calibrationRollDeg: Float? = null
    private var filteredSteering = 0f

    /** True if this device actually has a rotation-vector sensor. */
    val isAvailable: Boolean get() = rotationSensor != null

    fun start() {
        calibrationRollDeg = null
        filteredSteering = 0f
        rotationSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
        filteredSteering = 0f
        onSteeringChanged(0f)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ROTATION_VECTOR) return

        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
        SensorManager.getOrientation(rotationMatrix, orientationRad)
        val rollDeg = Math.toDegrees(orientationRad[2].toDouble()).toFloat()

        // First reading after start() defines "center" (zero steering).
        val zero = calibrationRollDeg ?: rollDeg.also { calibrationRollDeg = it }
        val relativeDeg = (rollDeg - zero).coerceIn(-MAX_TILT_DEG, MAX_TILT_DEG)
        val target = relativeDeg / MAX_TILT_DEG

        filteredSteering += (target - filteredSteering) * SMOOTHING
        onSteeringChanged(filteredSteering)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) { /* no-op */ }

    private companion object {
        /** 0..1, higher = snappier but jitterier. */
        const val SMOOTHING = 0.2f
        /** Tilt (degrees from calibrated center) at which steering saturates to +-1. */
        const val MAX_TILT_DEG = 35f
    }
}