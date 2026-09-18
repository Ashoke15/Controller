package com.diyproject.controller.control

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import java.util.concurrent.Executors

class BluetoothSppManager(
    private val context: Context,
    private val onConnected: () -> Unit,
    private val onDisconnected: () -> Unit,
    private val onError: (String) -> Unit,
    private val onDataReceived: (String) -> Unit = {}
) {

    companion object {
        private const val TAG = "BluetoothSppManager"

        private val SPP_UUID: UUID =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(BluetoothManager::class.java)

    private val adapter = bluetoothManager?.adapter

    private val executor = Executors.newSingleThreadExecutor()

    private var socket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null

    private var readThread: Thread? = null

    @Volatile
    private var readerRunning: Boolean = false

    @Volatile
    var isConnected: Boolean = false
        private set

    // ---------------------------------------------------------
    // Permission helpers
    // ---------------------------------------------------------

    private fun hasConnectPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun hasScanPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun reportPermissionError() {
        onError(
            "Bluetooth permission is required. " +
                    "Please allow Bluetooth access in Settings."
        )
    }

    // ---------------------------------------------------------
    // Paired devices
    // ---------------------------------------------------------

    fun getPairedDevices(): List<BluetoothDevice> {

        if (!hasConnectPermission()) {
            reportPermissionError()
            return emptyList()
        }

        return try {
            adapter?.bondedDevices?.toList() ?: emptyList()
        } catch (e: SecurityException) {
            Log.e(TAG, "Unable to read paired devices", e)
            onError("Bluetooth permission was denied.")
            emptyList()
        }
    }

    // ---------------------------------------------------------
    // Connect
    // ---------------------------------------------------------

    fun connect(device: BluetoothDevice) {

        if (!hasConnectPermission()) {
            reportPermissionError()
            return
        }

        if (adapter == null) {
            onError("Bluetooth is not available on this device.")
            return
        }

        executor.execute {

            try {

                if (hasScanPermission()) {
                    try {
                        adapter.cancelDiscovery()
                    } catch (e: SecurityException) {
                        Log.w(
                            TAG,
                            "Unable to cancel Bluetooth discovery",
                            e
                        )
                    }
                }

                if (!hasConnectPermission()) {
                    reportPermissionError()
                    return@execute
                }

                val sock =
                    device.createRfcommSocketToServiceRecord(SPP_UUID)

                sock.connect()

                socket = sock
                outputStream = sock.outputStream
                inputStream = sock.inputStream

                isConnected = true

                startReadLoop(sock.inputStream)

                onConnected()

                Log.d(TAG, "Bluetooth connected successfully")

            } catch (e: IOException) {

                Log.e(TAG, "Connection failed", e)

                isConnected = false

                closeQuietly()

                onError(
                    "Connection failed: ${
                        e.message ?: "Unknown Bluetooth error"
                    }"
                )

            } catch (e: SecurityException) {

                Log.e(TAG, "Bluetooth permission error", e)

                isConnected = false

                closeQuietly()

                onError(
                    "Bluetooth permission was denied."
                )
            }
        }
    }

    // ---------------------------------------------------------
    // Send command
    // ---------------------------------------------------------

    fun send(command: String) {

        if (!isConnected) {
            return
        }

        if (!hasConnectPermission()) {
            reportPermissionError()
            return
        }

        executor.execute {

            try {

                outputStream?.write(command.toByteArray())
                outputStream?.flush()

            } catch (e: IOException) {

                Log.e(TAG, "Send failed", e)

                isConnected = false

                onDisconnected()

                closeQuietly()

            } catch (e: SecurityException) {

                Log.e(TAG, "Bluetooth permission error", e)

                isConnected = false

                onDisconnected()

                closeQuietly()
            }
        }
    }

    // ---------------------------------------------------------
    // Receive loop (Rx)
    // ---------------------------------------------------------
    //
    // Runs on its own dedicated thread, not `executor` — a blocking
    // InputStream.read() sitting in that single-thread pool would starve
    // every connect()/send() queued behind it. Bytes are buffered until a
    // '\n' (an Arduino Serial.println() terminator); a '\r' immediately
    // before it is trimmed. A device that never sends a trailing newline
    // simply never flushes its last partial line — same tradeoff any real
    // serial terminal makes. Byte-by-byte ASCII casting is deliberate:
    // simple serial firmware protocols like this one don't send multi-byte
    // UTF-8, so there's no decoder state to get wrong mid-stream.

    private fun startReadLoop(input: InputStream) {
        readerRunning = true
        readThread = Thread {
            val buffer = ByteArray(1024)
            val line = StringBuilder()

            while (readerRunning) {
                val bytesRead = try {
                    input.read(buffer)
                } catch (e: IOException) {
                    if (readerRunning) {
                        Log.e(TAG, "Read failed", e)
                        handleReadFailure()
                    }
                    return@Thread
                }

                if (bytesRead == -1) {
                    if (readerRunning) {
                        Log.w(TAG, "Input stream closed by remote device")
                        handleReadFailure()
                    }
                    return@Thread
                }

                for (i in 0 until bytesRead) {
                    val c = buffer[i].toInt().toChar()
                    if (c == '\n') {
                        val text = line.toString().trimEnd('\r')
                        line.clear()
                        if (text.isNotEmpty()) {
                            onDataReceived(text)
                        }
                    } else {
                        line.append(c)
                    }
                }
            }
        }.apply {
            name = "BluetoothSppManager-Rx"
            isDaemon = true
            start()
        }
    }

    private fun handleReadFailure() {
        readerRunning = false
        if (isConnected) {
            isConnected = false
            onDisconnected()
        }
        closeQuietly()
    }

    private fun stopReadLoop() {
        readerRunning = false
        // Not interrupting/joining here on purpose: closeQuietly() closing
        // the socket makes the blocking read() throw immediately, which is
        // what actually ends the loop. readerRunning already being false
        // is what stops that IOException from being treated as a failure.
        readThread = null
    }

    // ---------------------------------------------------------
    // Disconnect
    // ---------------------------------------------------------

    fun disconnect() {

        executor.execute {

            stopReadLoop()

            closeQuietly()

            isConnected = false

            onDisconnected()
        }
    }

    // ---------------------------------------------------------
    // Close resources
    // ---------------------------------------------------------

    private fun closeQuietly() {

        try {
            outputStream?.close()
        } catch (_: IOException) {
        }

        try {
            inputStream?.close()
        } catch (_: IOException) {
        }

        try {
            socket?.close()
        } catch (_: IOException) {
        }

        outputStream = null
        inputStream = null
        socket = null
    }

    // ---------------------------------------------------------
    // Shutdown executor
    // ---------------------------------------------------------

    fun shutdown() {

        try {
            stopReadLoop()
            closeQuietly()
        } finally {
            executor.shutdownNow()
        }
    }
}