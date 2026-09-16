package com.diyproject.controller.joystick.compose

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.diyproject.controller.control.BluetoothSppManager
import com.diyproject.controller.control.CarCommand
import com.diyproject.controller.control.compose.CommandListDialog
import kotlin.math.atan2
import kotlin.math.sqrt

class JoystickActivity : ComponentActivity() {

    private lateinit var bt: BluetoothSppManager
    private var lastDirection: Direction? = null
    private var lastSentSpeed: Int = -1

    // Right-stick pan/tilt axis state (see GimbalCommand).
    private var lastPanDeg: Int = -1
    private var lastTiltDeg: Int = -1

    // Left-stick raw input, kept so gyro steering can override just the
    // x-component while still using the stick's y for throttle.
    private var leftStickX = 0f
    private var leftStickY = 0f
    private var gyroSteeringX = 0f

    private lateinit var gyroSteering: GyroSteeringController

    // Compose-observable UI state, promoted to class level (rather than
    // locals inside onCreate) so lifecycle callbacks like onResume/onPause
    // can read them too.
    private var isConnected by mutableStateOf(false)
    private var showCommandList by mutableStateOf(false)
    private var motionControlEnabled by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Hide system bars completely for true full-screen mode
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

        gyroSteering = GyroSteeringController(this) { steeringX ->
            gyroSteeringX = steeringX
            if (motionControlEnabled) updateDrive()
        }

        bt = BluetoothSppManager(
            context = this,
            onConnected = { isConnected = true },
            onDisconnected = { isConnected = false },
            onError = { msg ->
                runOnUiThread {
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                }
            }
        )

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier, color = Color.Transparent) {
                    if (showCommandList) {
                        CommandListDialog(onDismiss = { showCommandList = false })
                    }

                    JoystickScreen(
                        isConnected = isConnected,
                        motionControlEnabled = motionControlEnabled,
                        callbacks = JoystickCallbacks(
                            onConnectionToggle = { checkBluetoothPermissionsAndShowPicker() },
                            onLeftVectorChange = { x, y -> onLeftStickMoved(x, y) },
                            onLeftVectorRepeat = { x, y -> onLeftStickMoved(x, y) },
                            onLeftReleased = {
                                leftStickX = 0f
                                leftStickY = 0f
                                lastDirection = null
                                bt.send(CarCommand.speedToChar(0))
                                bt.send(CarCommand.STOP)
                            },
                            onRightVectorChange = { x, y -> handlePanTilt(x, y) },
                            onRightVectorRepeat = { x, y -> handlePanTilt(x, y) },
                            onRightReleased = { /* stick auto-centers visually; handlePanTilt(0,0) resets the gimbal to its midpoint */ },
                            onToggleMotionControl = { toggleMotionControl() },
                            onSpeedChange = { /* Handled dynamically by joystick magnitude */ },
                            onToggleHeadlight = { isOn ->
                                bt.send(if (isOn) CarCommand.FRONT_LIGHTS_ON else CarCommand.FRONT_LIGHTS_OFF)
                            },
                            onToggleCabinLight = { isOn ->
                                bt.send(if (isOn) CarCommand.BACK_LIGHTS_ON else CarCommand.BACK_LIGHTS_OFF)
                            },
                            onToggleSound = { isOn ->
                                bt.send(if (isOn) CarCommand.HORN_ON else CarCommand.HORN_OFF)
                            },
                            onToggleWarning = { isOn ->
                                bt.send(if (isOn) CarCommand.EXTRA_ON else CarCommand.EXTRA_OFF)
                            },
                            onSettingsClick = { showCommandList = !showCommandList },
                            onStopClick = {
                                lastDirection = null
                                bt.send(CarCommand.speedToChar(0))
                                bt.send(CarCommand.STOP_ALL)
                            }
                        )
                    )
                }
            }
        }
    }

    /**
     * Called on every left-stick update. Stores the raw stick vector, then
     * routes to [updateDrive], which decides whether steering (x) comes
     * from the stick itself or from [gyroSteeringX] depending on
     * [motionControlEnabled]. Throttle (y) always comes from the stick.
     */
    private fun onLeftStickMoved(x: Float, y: Float) {
        leftStickX = x
        leftStickY = y
        updateDrive()
    }

    private fun updateDrive() {
        val effectiveX = if (motionControlEnabled) gyroSteeringX else leftStickX
        handleAnalogVector(effectiveX, leftStickY)
    }

    private fun toggleMotionControl() {
        motionControlEnabled = !motionControlEnabled
        if (motionControlEnabled) {
            if (!gyroSteering.isAvailable) {
                motionControlEnabled = false
                Toast.makeText(this, "This device has no orientation sensor", Toast.LENGTH_SHORT).show()
                return
            }
            gyroSteering.start()
        } else {
            gyroSteering.stop()
            gyroSteeringX = 0f
            updateDrive()
        }
    }

    /**
     * Right stick: drives the independent pan/tilt gimbal axis rather than
     * the drivetrain. Unlike the drive stick there's no dead zone or 8-way
     * snapping — a gimbal should track the stick proportionally.
     * See [GimbalCommand] for the wire-format caveat.
     */
    private fun handlePanTilt(x: Float, y: Float) {
        val panDeg = (90 + x * 90).toInt().coerceIn(0, 180)
        val tiltDeg = (90 - y * 90).toInt().coerceIn(0, 180) // stick up (-y) -> tilt up

        if (panDeg != lastPanDeg) {
            lastPanDeg = panDeg
            bt.send(GimbalCommand.pan(panDeg))
        }
        if (tiltDeg != lastTiltDeg) {
            lastTiltDeg = tiltDeg
            bt.send(GimbalCommand.tilt(tiltDeg))
        }
    }

    /**
     * Calculates both proportional speed (from joystick distance magnitude)
     * and direction (from angle) and sends them over Bluetooth.
     */
    private fun handleAnalogVector(x: Float, y: Float) {
        val rawMagnitude = sqrt(x * x + y * y).coerceIn(0f, 1f)

        if (rawMagnitude < DEAD_ZONE) {
            if (lastDirection != null) {
                lastDirection = null
                bt.send(CarCommand.speedToChar(0))
                bt.send(CarCommand.STOP)
            }
            return
        }

        // Map magnitude outside dead zone to a 1% - 100% speed scale
        val normalizedMagnitude = ((rawMagnitude - DEAD_ZONE) / (1f - DEAD_ZONE)).coerceIn(0f, 1f)
        val speedPercent = (normalizedMagnitude * 100).toInt().coerceIn(1, 100)

        // Only send speed update if it changes to avoid data flooding
        if (speedPercent != lastSentSpeed) {
            lastSentSpeed = speedPercent
            bt.send(CarCommand.speedToChar(speedPercent))
        }

        val direction = directionFor(x, y)
        if (direction != lastDirection) {
            lastDirection = direction
            when (direction) {
                Direction.FORWARD       -> bt.send(CarCommand.FORWARD)
                Direction.FORWARD_RIGHT -> bt.send(CarCommand.FORWARD_RIGHT)
                Direction.RIGHT         -> bt.send(CarCommand.RIGHT)
                Direction.BACK_RIGHT    -> bt.send(CarCommand.BACK_RIGHT)
                Direction.BACK          -> bt.send(CarCommand.BACK)
                Direction.BACK_LEFT     -> bt.send(CarCommand.BACK_LEFT)
                Direction.LEFT          -> bt.send(CarCommand.LEFT)
                Direction.FORWARD_LEFT  -> bt.send(CarCommand.FORWARD_LEFT)
            }
        }
    }

    private fun directionFor(x: Float, y: Float): Direction {
        val degrees = Math.toDegrees(atan2(x.toDouble(), -y.toDouble()))
        val normalized = (degrees + 360.0) % 360.0
        // Idiomatic range-check form (subject-based `when`).
        return when (normalized) {
            !in 22.5..<337.5 -> Direction.FORWARD
            in 22.5..<67.5  -> Direction.FORWARD_RIGHT
            in 67.5..<112.5 -> Direction.RIGHT
            in 112.5..<157.5 -> Direction.BACK_RIGHT
            in 157.5..<202.5 -> Direction.BACK
            in 202.5..<247.5 -> Direction.BACK_LEFT
            in 247.5..<292.5 -> Direction.LEFT
            else                -> Direction.FORWARD_LEFT
        }
    }

    private fun checkBluetoothPermissionsAndShowPicker() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                    101
                )
                return
            }
        }
        showDevicePicker()
    }

    private fun showDevicePicker() {
        try {
            val devices = bt.getPairedDevices()
            when {
                devices.isEmpty() -> {
                    Toast.makeText(
                        this,
                        "No paired devices. Pair your HC-05 first.",
                        Toast.LENGTH_LONG
                    ).show()
                }
                // Only one paired device (the usual case for a dedicated
                // car module) — connect straight to it, no picker needed.
                devices.size == 1 -> {
                    bt.connect(devices.first())
                }
                else -> {
                    val names = devices.map { v -> v.name ?: v.address }.toTypedArray()
                    AlertDialog.Builder(this)
                        .setTitle("Select car module")
                        .setItems(names) { _, which -> bt.connect(devices[which]) }
                        .show()
                }
            }
        } catch (_: SecurityException) {
            Toast.makeText(this, "Bluetooth permission missing", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onPause() {
        super.onPause()
        // Always stop the sensor when backgrounded, regardless of the
        // toggle state, to avoid draining battery while the app is hidden.
        gyroSteering.stop()
    }

    override fun onResume() {
        super.onResume()
        if (motionControlEnabled) gyroSteering.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        gyroSteering.stop()
        bt.disconnect()
    }

    private enum class Direction {
        FORWARD, FORWARD_RIGHT, RIGHT, BACK_RIGHT,
        BACK, BACK_LEFT, LEFT, FORWARD_LEFT
    }

    private companion object {
        const val DEAD_ZONE = 0.15f
    }
}