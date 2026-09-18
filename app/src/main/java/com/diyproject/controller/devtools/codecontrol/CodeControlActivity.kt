package com.diyproject.controller.devtools.codecontrol

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
import com.diyproject.controller.control.BluetoothSppManager
import java.util.concurrent.atomic.AtomicLong

/**
 * Hosts the raw-command console. Owns its own [BluetoothSppManager]
 * instance — same shared class the D-pad controller uses — so Code
 * Control works standalone regardless of what other screen the app was
 * last on.
 *
 * SEND writes exactly what the developer typed, plus whichever line
 * ending they picked, straight to the socket's OutputStream via
 * BluetoothSppManager.send(). Every byte the module writes back is
 * decoded live by the manager's Rx loop and appended as an RX line —
 * nothing in this transcript is simulated.
 */
class CodeControlActivity : ComponentActivity() {

    private lateinit var bt: BluetoothSppManager
    private lateinit var historyStore: CommandHistoryStore

    private var isConnected by mutableStateOf(false)
    private var lineEnding by mutableStateOf(LineEnding.NL)
    private var history by mutableStateOf<List<String>>(emptyList())
    private val transcript = mutableStateListOf<TranscriptEntry>()

    private val nextId = AtomicLong(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

        historyStore = CommandHistoryStore(this)
        history = historyStore.load()

        bt = BluetoothSppManager(
            context = this,
            onConnected = {
                isConnected = true
                appendSystem("connected")
            },
            onDisconnected = {
                isConnected = false
                appendSystem("disconnected")
            },
            onError = { msg ->
                runOnUiThread { Toast.makeText(this, msg, Toast.LENGTH_SHORT).show() }
                appendSystem("error: $msg")
            },
            onDataReceived = { line -> appendEntry(TranscriptDirection.RX, line) }
        )

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier, color = Color.Transparent) {
                    CodeControlScreen(
                        isConnected = isConnected,
                        transcript = transcript,
                        history = history,
                        lineEnding = lineEnding,
                        onLineEndingChange = { lineEnding = it },
                        onSend = { raw -> sendRaw(raw) },
                        onClearTranscript = { transcript.clear() },
                        onConnectClick = { checkBluetoothPermissionsAndShowPicker() }
                    )
                }
            }
        }
    }

    private fun sendRaw(raw: String) {
        val withEnding = RawCommand.withLineEnding(raw, lineEnding)
        bt.send(withEnding)
        appendEntry(TranscriptDirection.TX, withEnding)
        history = historyStore.record(raw)
    }

    private fun appendEntry(direction: TranscriptDirection, raw: String) {
        transcript.add(TranscriptEntry(nextId.getAndIncrement(), direction, raw, System.currentTimeMillis()))
        if (transcript.size > MAX_TRANSCRIPT_ENTRIES) {
            transcript.removeRange(0, transcript.size - MAX_TRANSCRIPT_ENTRIES)
        }
    }

    private fun appendSystem(message: String) {
        appendEntry(TranscriptDirection.SYSTEM, message)
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
                    102
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
                        .setTitle("Select device")
                        .setItems(names) { _, which -> bt.connect(devices[which]) }
                        .show()
                }
            }
        } catch (_: SecurityException) {
            Toast.makeText(this, "Bluetooth permission missing", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        bt.disconnect()
    }

    private companion object {
        const val MAX_TRANSCRIPT_ENTRIES = 500
    }
}