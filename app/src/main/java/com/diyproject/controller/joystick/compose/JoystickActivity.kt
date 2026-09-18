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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.diyproject.controller.control.BluetoothSppManager
import com.diyproject.controller.control.CarCommand

class JoystickActivity : ComponentActivity() {

    private lateinit var bt: BluetoothSppManager
    private lateinit var rcTransmitter: RcTransmitter
    private lateinit var gyroSteering: GyroSteeringController

    private var leftStickX = 0f
    private var leftStickY = 0f
    private var gyroSteeringX = 0f
    private var throttleLimitPercent = 100

    private var isConnected by mutableStateOf(false)
    private var showCommandList by mutableStateOf(false)
    private var motionControlEnabled by mutableStateOf(false)

    private var steeringChannel by mutableStateOf(0)
    private var throttleChannel by mutableStateOf(0)
    private var panChannel by mutableStateOf(90)
    private var tiltChannel by mutableStateOf(90)

    private val commandLog = mutableStateListOf<String>()
    private val logStartMs = System.currentTimeMillis()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

        gyroSteering = GyroSteeringController(this) { steeringX ->
            gyroSteeringX = steeringX
            if (motionControlEnabled) updateDrive()
        }

        rcTransmitter = RcTransmitter(
            scope = lifecycleScope,
            send = { bt.send(it) },
            onCommandSent = { logSent(it) }
        )

        bt = BluetoothSppManager(
            context = this,
            onConnected = {
                isConnected = true
                rcTransmitter.resendAll()
                rcTransmitter.start()
            },
            onDisconnected = {
                isConnected = false
                rcTransmitter.stop()
            },
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
                        CommandCenterDialog(
                            entries = commandLog,
                            onDismiss = { showCommandList = false }
                        )
                    }

                    JoystickScreen(
                        isConnected = isConnected,
                        motionControlEnabled = motionControlEnabled,
                        steeringChannel = steeringChannel,
                        throttleChannel = throttleChannel,
                        panChannel = panChannel,
                        tiltChannel = tiltChannel,
                        callbacks = JoystickCallbacks(
                            onConnectionToggle = { checkBluetoothPermissionsAndShowPicker() },
                            onLeftVectorChange = { x, y -> onLeftStickMoved(x, y) },
                            onLeftReleased = {
                                leftStickX = 0f
                                leftStickY = 0f
                                updateDrive()
                            },
                            onRightVectorChange = { x, y -> handlePanTilt(x, y) },
                            onRightReleased = { handlePanTilt(0f, 0f) },
                            onToggleMotionControl = { toggleMotionControl() },
                            onThrottleLimitChange = { percent ->
                                throttleLimitPercent = percent
                                updateDrive()
                            },
                            onToggleHeadlight = { isOn ->
                                val cmd = if (isOn) CarCommand.FRONT_LIGHTS_ON else CarCommand.FRONT_LIGHTS_OFF
                                bt.send(cmd); logSent(cmd)
                            },
                            onToggleCabinLight = { isOn ->
                                val cmd = if (isOn) CarCommand.BACK_LIGHTS_ON else CarCommand.BACK_LIGHTS_OFF
                                bt.send(cmd); logSent(cmd)
                            },
                            onToggleHorn = { isOn ->
                                val cmd = if (isOn) CarCommand.HORN_ON else CarCommand.HORN_OFF
                                bt.send(cmd); logSent(cmd)
                            },
                            onToggleWarning = { isOn ->
                                val cmd = if (isOn) CarCommand.EXTRA_ON else CarCommand.EXTRA_OFF
                                bt.send(cmd); logSent(cmd)
                            },
                            onSettingsClick = { showCommandList = !showCommandList },
                            onStopClick = {
                                leftStickX = 0f
                                leftStickY = 0f
                                steeringChannel = 0
                                throttleChannel = 0
                                panChannel = 90
                                tiltChannel = 90
                                rcTransmitter.sendNeutralNow()
                                bt.send(CarCommand.STOP_ALL)
                                logSent(CarCommand.STOP_ALL)
                            }
                        )
                    )
                }
            }
        }
    }

    private fun logSent(raw: String) {
        val line = raw.trim()
        if (line.isEmpty()) return
        val elapsedMs = System.currentTimeMillis() - logStartMs
        val stamp = String.format(
            "%02d:%02d.%03d",
            (elapsedMs / 60000) % 60,
            (elapsedMs / 1000) % 60,
            elapsedMs % 1000
        )
        commandLog.add(0, "$stamp  $line")
        if (commandLog.size > 60) commandLog.removeRange(60, commandLog.size)
    }

    private fun onLeftStickMoved(x: Float, y: Float) {
        leftStickX = x
        leftStickY = y
        updateDrive()
    }

    private fun updateDrive() {
        val effectiveX = if (motionControlEnabled) gyroSteeringX else leftStickX

        val steering = StickInputMath.toChannel(
            effectiveX, StickInputMath.STEERING_DEADZONE, StickInputMath.STEERING_EXPO
        )
        val rawThrottle = StickInputMath.toChannel(
            -leftStickY, StickInputMath.THROTTLE_DEADZONE, StickInputMath.THROTTLE_EXPO
        )
        val throttle = (rawThrottle * throttleLimitPercent / 100).coerceIn(-100, 100)

        steeringChannel = steering
        throttleChannel = throttle
        rcTransmitter.setSteering(steering)
        rcTransmitter.setThrottle(throttle)
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

    private fun handlePanTilt(x: Float, y: Float) {
        val pan = StickInputMath.toServoAngle(x, StickInputMath.GIMBAL_DEADZONE, StickInputMath.GIMBAL_EXPO)
        val tilt = StickInputMath.toServoAngle(-y, StickInputMath.GIMBAL_DEADZONE, StickInputMath.GIMBAL_EXPO)

        panChannel = pan
        tiltChannel = tilt
        rcTransmitter.setPan(pan)
        rcTransmitter.setTilt(tilt)
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
                    Toast.makeText(this, "No paired devices. Pair your HC-05 first.", Toast.LENGTH_LONG).show()
                }
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
        gyroSteering.stop()
        leftStickX = 0f
        leftStickY = 0f
        steeringChannel = 0
        throttleChannel = 0
        rcTransmitter.setSteering(0)
        rcTransmitter.setThrottle(0)
    }

    override fun onResume() {
        super.onResume()
        if (motionControlEnabled) gyroSteering.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        gyroSteering.stop()
        rcTransmitter.stop()
        bt.disconnect()
    }
}