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
import androidx.compose.runtime.remember
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Hide system bars completely for true full-screen mode
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

        var isConnected by mutableStateOf(false)
        var showCommandList by mutableStateOf(false)

        bt = BluetoothSppManager(
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
                        callbacks = JoystickCallbacks(
                            onConnectionToggle = { checkBluetoothPermissionsAndShowPicker() },
                            onLeftVectorChange = { x, y ->
                                handleAnalogVector(x, y)
                            },
                            onLeftVectorRepeat = { x, y ->
                                handleAnalogVector(x, y)
                            },
                            onLeftReleased = {
                                lastDirection = null
                                bt.send(CarCommand.speedToChar(0))
                                bt.send(CarCommand.STOP)
                            },
                            onRightVectorChange = { _, _ -> },
                            onRightVectorRepeat = { _, _ -> },
                            onRightReleased = {},
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
        return when {
            normalized < 22.5 || normalized >= 337.5 -> Direction.FORWARD
            normalized < 67.5  -> Direction.FORWARD_RIGHT
            normalized < 112.5 -> Direction.RIGHT
            normalized < 157.5 -> Direction.BACK_RIGHT
            normalized < 202.5 -> Direction.BACK
            normalized < 247.5 -> Direction.BACK_LEFT
            normalized < 292.5 -> Direction.LEFT
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
            if (devices.isEmpty()) {
                Toast.makeText(
                    this,
                    "No paired devices. Pair your HC-05 first.",
                    Toast.LENGTH_LONG
                ).show()
                return
            }
            val names = devices.map { v -> v.name ?: v.address }.toTypedArray()
            AlertDialog.Builder(this)
                .setTitle("Select car module")
                .setItems(names) { _, which -> bt.connect(devices[which]) }
                .show()
        } catch (e: SecurityException) {
            Toast.makeText(this, "Bluetooth permission missing", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
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