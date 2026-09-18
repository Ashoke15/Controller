package com.diyproject.controller.gyro.compose

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.pm.ActivityInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.view.Surface
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.diyproject.controller.control.BluetoothSppManager
import com.diyproject.controller.control.CarCommand
import kotlin.math.atan2
import kotlin.math.roundToInt
import kotlin.math.sqrt


class GyroActivity : ComponentActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var motionSensor: Sensor? = null

    private lateinit var bluetoothSppManager: BluetoothSppManager

    // ---- State exposed to Compose ----
    private val pitchState = mutableFloatStateOf(0f)
    private val rollState = mutableFloatStateOf(0f)
    private val modeState = mutableStateOf(GyroMode.DIGITAL)
    private val speedPercentState = mutableIntStateOf(0)
    private val activeCommandState = mutableStateOf(CarCommand.STOP)
    private val isConnectedState = mutableStateOf(false)
    private val pairedDevicesState = mutableStateOf<List<BluetoothDevice>>(emptyList())
    private val showDevicePickerState = mutableStateOf(false)

    // Low-pass filter factor: smaller = smoother/laggier, larger = twitchier
    private val smoothingFactor = 0.15f
    private var smoothedX = 0f
    private var smoothedY = 0f
    private var smoothedZ = 0f

    // Reused scratch buffer for remapForRotation() (avoid per-frame allocation)
    private val remappedValues = FloatArray(3)

    // Calibration: raw pitch/roll are offset by these so "flat" means "however the
    // phone was held when calibration last ran", not device-absolute level.
    private var pitchOffsetDeg = 0f
    private var rollOffsetDeg = 0f
    // True until the next sensor reading, at which point that reading becomes the new zero.
    // Starts true so opening the screen auto-zeros; onResume re-arms it too.
    private var pendingCalibration = true

    // Degrees of tilt required before we register any input at all
    private val deadzoneDegrees = 12f

    // Degrees of tilt that corresponds to 100% speed / full joystick deflection in Analog Mode
    private val maxTiltDegrees = 45f

    // Throttle outgoing BT commands so we don't flood the SPP link every sensor tick
    private var lastSentCommand: String? = null
    private var lastSentSpeed: Int = -1
    private var lastSendTimeMs = 0L
    private val minSendIntervalMs = 60L


    private val hysteresisMarginDegrees = 4f
    private var forwardActive = false
    private var backwardActive = false
    private var leftActive = false
    private var rightActive = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        enableImmersiveMode()

        bluetoothSppManager = BluetoothSppManager(
            context = this,
            onConnected = { runOnUiThread { isConnectedState.value = true } },
            onDisconnected = { runOnUiThread { isConnectedState.value = false } },
            onError = { message ->
                runOnUiThread {
                    isConnectedState.value = false
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                }
            }
        )

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        motionSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        setContent {
            GyroScreen(
                pitch = pitchState.floatValue,
                roll = rollState.floatValue,
                mode = modeState.value,
                speedPercent = speedPercentState.intValue,
                activeCommand = activeCommandState.value,
                isConnected = isConnectedState.value,
                deadzoneDegrees = deadzoneDegrees,
                maxTiltDegrees = maxTiltDegrees,
                showDevicePicker = showDevicePickerState.value,
                pairedDevices = pairedDevicesState.value,
                onModeChange = { modeState.value = it },
                onConnectionClick = { onConnectionPillClicked() },
                onRecalibrate = { recalibrate() },
                onManualStop = { manualStop() },
                onDeviceSelected = { device ->
                    showDevicePickerState.value = false
                    bluetoothSppManager.connect(device)
                },
                onDismissDevicePicker = { showDevicePickerState.value = false },
                onExit = { finish() }
            )
        }
    }

    private fun onConnectionPillClicked() {
        if (isConnectedState.value) {
            bluetoothSppManager.disconnect()
            return
        }
        pairedDevicesState.value = bluetoothSppManager.getPairedDevices()
        showDevicePickerState.value = true
    }

    private fun enableImmersiveMode() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        // The system can re-show the bars after focus changes (e.g. returning from
        // the device picker dialog); re-hide them whenever we regain focus.
        if (hasFocus) enableImmersiveMode()
    }

    override fun onResume() {
        super.onResume()
        motionSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        // Re-zero on every resume too (e.g. returning from the device picker), not just onCreate.
        pendingCalibration = true
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
        // Safety: always stop the car when leaving the tilt-control screen
        sendCarCommand(CarCommand.STOP, speed = 0)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::bluetoothSppManager.isInitialized) {
            bluetoothSppManager.shutdown()
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        remapForRotation(event.values, currentDisplayRotation(), remappedValues)

        // Exponential smoothing to tame raw sensor jitter
        smoothedX += smoothingFactor * (remappedValues[0] - smoothedX)
        smoothedY += smoothingFactor * (remappedValues[1] - smoothedY)
        smoothedZ += smoothingFactor * (remappedValues[2] - smoothedZ)

        // Standard pitch/roll from a gravity-style vector, now in screen space.
        val rollRad = atan2(smoothedX, smoothedZ)
        val pitchRad = atan2(
            smoothedY,
            sqrt(smoothedX * smoothedX + smoothedZ * smoothedZ)
        )

        val rawPitchDeg = Math.toDegrees(pitchRad.toDouble()).toFloat()
        val rawRollDeg = Math.toDegrees(rollRad.toDouble()).toFloat()

        // Calibration: whatever angle the phone is at right now becomes "flat".
        if (pendingCalibration) {
            pitchOffsetDeg = rawPitchDeg
            rollOffsetDeg = rawRollDeg
            pendingCalibration = false
        }

        val pitchDeg = rawPitchDeg - pitchOffsetDeg
        val rollDeg = rawRollDeg - rollOffsetDeg

        pitchState.floatValue = pitchDeg
        rollState.floatValue = rollDeg

        when (modeState.value) {
            GyroMode.DIGITAL -> processDigitalMode(pitchDeg, rollDeg)
            GyroMode.ANALOG -> processAnalogMode(pitchDeg, rollDeg)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) { /* no-op */ }


    private fun remapForRotation(values: FloatArray, rotation: Int, out: FloatArray) {
        val x = values[0]
        val y = values[1]
        val z = values[2]
        when (rotation) {
            Surface.ROTATION_90 -> { out[0] = y; out[1] = -x; out[2] = z }
            Surface.ROTATION_180 -> { out[0] = -x; out[1] = -y; out[2] = z }
            Surface.ROTATION_270 -> { out[0] = -y; out[1] = x; out[2] = z }
            else -> { out[0] = x; out[1] = y; out[2] = z } // ROTATION_0
        }
    }

    /** Current rotation of this activity's display (Surface.ROTATION_0/90/180/270). */
    private fun currentDisplayRotation(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            display?.rotation ?: Surface.ROTATION_0
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.rotation
        }
    }

    /** Re-arms auto-zero: the next sensor reading becomes the new "flat" reference. */
    private fun recalibrate() {
        pendingCalibration = true
    }


    private fun manualStop() {
        forwardActive = false
        backwardActive = false
        leftActive = false
        rightActive = false
        activeCommandState.value = CarCommand.STOP
        speedPercentState.intValue = 0
        sendCarCommand(CarCommand.STOP, speed = 0)
    }

    // ---------------------------------------------------------------------
    // Digital Mode: 8-way d-pad style commands, no speed variation
    // ---------------------------------------------------------------------
    private fun processDigitalMode(pitchDeg: Float, rollDeg: Float) {
        // Convention matches the bubble-level dot: positive pitch = forward (dot moves
        // toward "F"), negative pitch = backward — these must stay in sync with
        // normY = -pitch/maxTilt in GyroScreen's BubbleLevel.
        val forward = axisExceeds(pitchDeg, forwardActive).also { forwardActive = it }
        val backward = axisExceeds(-pitchDeg, backwardActive).also { backwardActive = it }
        val left = axisExceeds(-rollDeg, leftActive).also { leftActive = it }
        val right = axisExceeds(rollDeg, rightActive).also { rightActive = it }

        val command = when {
            forward && left -> CarCommand.FORWARD_LEFT
            forward && right -> CarCommand.FORWARD_RIGHT
            backward && left -> CarCommand.BACK_LEFT
            backward && right -> CarCommand.BACK_RIGHT
            forward -> CarCommand.FORWARD
            backward -> CarCommand.BACK
            left -> CarCommand.LEFT
            right -> CarCommand.RIGHT
            else -> CarCommand.STOP
        }

        activeCommandState.value = command
        speedPercentState.intValue = if (command == CarCommand.STOP) 0 else 100
        sendCarCommand(command, speed = null) // digital mode: no speed byte, full-power discrete moves
    }

    private fun axisExceeds(value: Float, wasActive: Boolean): Boolean {
        val exitThreshold = (deadzoneDegrees - hysteresisMarginDegrees).coerceAtLeast(0f)
        val threshold = if (wasActive) exitThreshold else deadzoneDegrees
        return value > threshold
    }

    private fun processAnalogMode(pitchDeg: Float, rollDeg: Float) {
        val x = applyDeadzone(rollDeg).coerceIn(-maxTiltDegrees, maxTiltDegrees) / maxTiltDegrees
        val y = applyDeadzone(pitchDeg).coerceIn(-maxTiltDegrees, maxTiltDegrees) / maxTiltDegrees

        val magnitude = sqrt(x * x + y * y).coerceIn(0f, 1f)
        val speedPercent = (magnitude * 100f).roundToInt()

        val command = if (magnitude < 0.02f) {
            CarCommand.STOP
        } else {
            // atan2(y, x): 0° = right(+x), 90° = forward(+y), etc.
            val angleDeg = (Math.toDegrees(atan2(y, x).toDouble()).toFloat() + 360f) % 360f
            angleToJoystickCommand(angleDeg)
        }

        activeCommandState.value = command
        speedPercentState.intValue = speedPercent
        sendCarCommand(command, speed = speedPercent)
    }

    /** Maps a 0..360° joystick angle to one of the 8 directional commands, 45° per sector. */
    private fun angleToJoystickCommand(angleDeg: Float): String = when {
        angleDeg !in 22.5f..337.5f -> CarCommand.RIGHT
        angleDeg < 67.5f -> CarCommand.FORWARD_RIGHT
        angleDeg < 112.5f -> CarCommand.FORWARD
        angleDeg < 157.5f -> CarCommand.FORWARD_LEFT
        angleDeg < 202.5f -> CarCommand.LEFT
        angleDeg < 247.5f -> CarCommand.BACK_LEFT
        angleDeg < 292.5f -> CarCommand.BACK
        else -> CarCommand.BACK_RIGHT
    }

    private fun applyDeadzone(value: Float): Float =
        if (kotlin.math.abs(value) < deadzoneDegrees) 0f
        else if (value > 0) value - deadzoneDegrees else value + deadzoneDegrees

    private fun sendCarCommand(command: String, speed: Int?) {
        val now = System.currentTimeMillis()
        val changed = command != lastSentCommand || speed != lastSentSpeed
        if (!changed && now - lastSendTimeMs < minSendIntervalMs) return
        if (!changed && command == CarCommand.STOP) return // no need to spam repeated STOPs

        bluetoothSppManager.send(command)
        if (speed != null) {
            bluetoothSppManager.send(CarCommand.speedToChar(speed))
        }

        lastSentCommand = command
        lastSentSpeed = speed ?: -1
        lastSendTimeMs = now
    }

}

enum class GyroMode { DIGITAL, ANALOG }