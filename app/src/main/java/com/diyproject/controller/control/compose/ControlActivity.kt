package com.diyproject.controller.control.compose

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.diyproject.controller.control.BluetoothSppManager
import com.diyproject.controller.control.CarCommand

class ControlActivity : ComponentActivity() {

    private lateinit var bt: BluetoothSppManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        var isConnected by mutableStateOf(false)
        var headlightOn by mutableStateOf(false)
        var backlightOn by mutableStateOf(false)
        var hazardOn by mutableStateOf(false)
        var speed by mutableIntStateOf(50)
        var showCommandList by mutableStateOf(false)

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
            // Command list dialog — sits OUTSIDE ControllerScreen, not inside its parameters
            if (showCommandList) {
                CommandListDialog(onDismiss = { showCommandList = false })
            }

            ControllerScreen(
                state = ControllerState(
                    isConnected = isConnected,
                    headlightOn = headlightOn,
                    backlightOn = backlightOn,
                    hazardOn = hazardOn,
                    speed = speed
                ),
                actions = ControllerActions(
                    onConnectClick = { checkBluetoothPermissionsAndShowPicker() },
                    onForwardPress = { bt.send(CarCommand.FORWARD) },
                    onForwardRelease = { bt.send(CarCommand.STOP) },
                    onBackPress = { bt.send(CarCommand.BACK) },
                    onBackRelease = { bt.send(CarCommand.STOP) },
                    onLeftPress = { bt.send(CarCommand.LEFT) },
                    onLeftRelease = { bt.send(CarCommand.STOP) },
                    onRightPress = { bt.send(CarCommand.RIGHT) },
                    onRightRelease = { bt.send(CarCommand.STOP) },
                    onForwardLeftPress = { bt.send(CarCommand.FORWARD_LEFT) },
                    onForwardRightPress = { bt.send(CarCommand.FORWARD_RIGHT) },
                    onBackLeftPress = { bt.send(CarCommand.BACK_LEFT) },
                    onBackRightPress = { bt.send(CarCommand.BACK_RIGHT) },
                    onDiagonalRelease = { bt.send(CarCommand.STOP) },
                    onHeadlightToggle = {
                        headlightOn = !headlightOn
                        bt.send(if (headlightOn) CarCommand.FRONT_LIGHTS_ON else CarCommand.FRONT_LIGHTS_OFF)
                    },
                    onBacklightToggle = {
                        backlightOn = !backlightOn
                        bt.send(if (backlightOn) CarCommand.BACK_LIGHTS_ON else CarCommand.BACK_LIGHTS_OFF)
                    },
                    onHornPress = { bt.send(CarCommand.HORN_ON) },
                    onHornRelease = { bt.send(CarCommand.HORN_OFF) },
                    onHazardToggle = {
                        hazardOn = !hazardOn
                        bt.send(if (hazardOn) CarCommand.EXTRA_ON else CarCommand.EXTRA_OFF)
                    },
                    onSpeedChange = { newValue -> speed = newValue.toInt() },
                    onSpeedCommit = { bt.send(CarCommand.speedToChar(speed)) },
                    onSettingsClick = { showCommandList = !showCommandList },
                    onStopAllClick = { bt.send(CarCommand.STOP_ALL) }
                )
            )
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
            val names = devices.map { it.name ?: it.address }.toTypedArray()
            AlertDialog.Builder(this)
                .setTitle("Select car module")
                .setItems(names) { _, which -> bt.connect(devices[which]) }
                .show()
        } catch (e: SecurityException) {
            Toast.makeText(this, "Bluetooth permission missing", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.insetsController?.let {
                    it.hide(
                        android.view.WindowInsets.Type.statusBars() or
                                android.view.WindowInsets.Type.navigationBars()
                    )
                    it.systemBarsBehavior =
                        android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            } else {
                @Suppress("DEPRECATION")
                window.decorView.systemUiVisibility = (
                        android.view.View.SYSTEM_UI_FLAG_FULLSCREEN or
                                android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                                android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        bt.disconnect()
    }
}

@Composable
fun CommandListDialog(onDismiss: () -> Unit) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF161B22))
                .border(1.dp, Color.Cyan.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                .padding(20.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Command Reference",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            val commands = listOf(
                "Forward"        to "F",
                "Back"           to "B",
                "Left"           to "L",
                "Right"          to "R",
                "Forward Left"   to "G",
                "Forward Right"  to "I",
                "Back Left"      to "H",
                "Back Right"     to "J",
                "Stop"           to "S",
                "Front Lights ON"  to "W",
                "Front Lights OFF" to "w",
                "Back Lights ON"   to "U",
                "Back Lights OFF"  to "u",
                "Horn ON"        to "V",
                "Horn OFF"       to "v",
                "Hazard ON"      to "X",
                "Hazard OFF"     to "x",
                "Speed 0%"       to "0",
                "Speed 50%"      to "5",
                "Speed 100%"     to "q",
                "Stop All"       to "D"
            )

            commands.forEach { (label, cmd) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = label,
                        color = Color(0xFF8B949E),
                        fontSize = 13.sp
                    )
                    Text(
                        text = "\"$cmd\"",
                        color = Color(0xFF00E5FF),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
                Divider(color = Color.White.copy(alpha = 0.05f))
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Close", color = Color(0xFF00E5FF))
                }
            }
        }
    }
}