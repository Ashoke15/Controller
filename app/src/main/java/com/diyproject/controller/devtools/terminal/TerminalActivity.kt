package com.diyproject.controller.devtools.terminal

import android.Manifest
import android.content.Intent
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
import com.diyproject.controller.devtools.common.LineEnding
import com.diyproject.controller.devtools.common.RawCommand
import com.diyproject.controller.devtools.common.TranscriptDirection
import com.diyproject.controller.devtools.common.TranscriptEntry
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

/**
 * Real serial monitor: owns its own [BluetoothSppManager] connection
 * (same class the D-pad controller and Code Control use), decodes every
 * incoming line via its Rx loop, and reflects both directions of actual
 * traffic — nothing shown here is simulated or replayed from a fixture.
 */
class TerminalActivity : ComponentActivity() {

    private lateinit var bt: BluetoothSppManager

    private var isConnected by mutableStateOf(false)
    private var connectedDeviceLabel by mutableStateOf<String?>(null)
    private var lineEnding by mutableStateOf(LineEnding.NL)
    private var txByteCount by mutableStateOf(0L)
    private var rxByteCount by mutableStateOf(0L)
    private val transcript = mutableStateListOf<TranscriptEntry>()

    private val nextId = AtomicLong(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

        bt = BluetoothSppManager(
            context = this,
            onConnected = {
                isConnected = true
                appendSystem("connected" + (connectedDeviceLabel?.let { " to $it" } ?: ""))
            },
            onDisconnected = {
                isConnected = false
                appendSystem("disconnected")
                connectedDeviceLabel = null
            },
            onError = { msg ->
                runOnUiThread { Toast.makeText(this, msg, Toast.LENGTH_SHORT).show() }
                appendSystem("error: $msg")
            },
            onDataReceived = { line ->
                appendEntry(TranscriptDirection.RX, line)
                // Approximate: the stripped '\n' terminator isn't in `line`
                // anymore, so +1 accounts for it. A dropped/absent '\r'
                // before it (CRLF senders) isn't recovered here — this is a
                // display counter, not a byte-exact accounting ledger.
                rxByteCount += line.toByteArray(Charsets.UTF_8).size + 1
            }
        )

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier, color = Color.Transparent) {
                    TerminalScreen(
                        isConnected = isConnected,
                        connectedDeviceLabel = connectedDeviceLabel,
                        transcript = transcript,
                        txByteCount = txByteCount,
                        rxByteCount = rxByteCount,
                        lineEnding = lineEnding,
                        onLineEndingChange = { lineEnding = it },
                        onSend = { raw -> sendRaw(raw) },
                        onClearTranscript = {
                            transcript.clear()
                            txByteCount = 0L
                            rxByteCount = 0L
                        },
                        onExportTranscript = { exportTranscript() },
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
        txByteCount += RawCommand.byteLength(withEnding)
    }

    private fun appendEntry(direction: TranscriptDirection, raw: String) {
        transcript.add(TranscriptEntry(nextId.getAndIncrement(), direction, raw, System.currentTimeMillis()))
        if (transcript.size > MAX_TRANSCRIPT_ENTRIES) {
            transcript.removeRange(0, transcript.size - MAX_TRANSCRIPT_ENTRIES)
        }
    }

    private fun appendSystem(message: String) = appendEntry(TranscriptDirection.SYSTEM, message)

    private fun exportTranscript() {
        if (transcript.isEmpty()) {
            Toast.makeText(this, "Nothing to export yet", Toast.LENGTH_SHORT).show()
            return
        }
        val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
        val body = transcript.joinToString("\n") { entry ->
            val tag = when (entry.direction) {
                TranscriptDirection.TX -> "TX"
                TranscriptDirection.RX -> "RX"
                TranscriptDirection.SYSTEM -> "--"
            }
            "${stamp.format(entry.timestampMs)}  $tag  ${entry.raw.replace("\r\n", "\\r\\n").replace("\n", "\\n").replace("\r", "\\r")}"
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Terminal log")
            putExtra(Intent.EXTRA_TEXT, body)
        }
        startActivity(Intent.createChooser(intent, "Export terminal log"))
    }

    private fun checkBluetoothPermissionsAndShowPicker() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_CONNECT), 103)
                return
            }
        }
        showDevicePicker()
    }

    private fun showDevicePicker() {
        try {
            val devices = bt.getPairedDevices()
            when {
                devices.isEmpty() -> Toast.makeText(this, "No paired devices. Pair your HC-05 first.", Toast.LENGTH_LONG).show()
                devices.size == 1 -> {
                    val d = devices.first()
                    connectedDeviceLabel = d.name ?: d.address
                    bt.connect(d)
                }
                else -> {
                    val names = devices.map { v -> v.name ?: v.address }.toTypedArray()
                    AlertDialog.Builder(this)
                        .setTitle("Select device")
                        .setItems(names) { _, which ->
                            val d = devices[which]
                            connectedDeviceLabel = d.name ?: d.address
                            bt.connect(d)
                        }
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
        const val MAX_TRANSCRIPT_ENTRIES = 1000
    }
}