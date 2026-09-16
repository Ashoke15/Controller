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
import java.io.OutputStream
import java.util.UUID
import java.util.concurrent.Executors

class BluetoothSppManager(
    private val context: Context,
    private val onConnected: () -> Unit,
    private val onDisconnected: () -> Unit,
    private val onError: (String) -> Unit
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

                isConnected = true

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
    // Disconnect
    // ---------------------------------------------------------

    fun disconnect() {

        executor.execute {

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
            socket?.close()
        } catch (_: IOException) {
        }

        outputStream = null
        socket = null
    }

    // ---------------------------------------------------------
    // Shutdown executor
    // ---------------------------------------------------------

    fun shutdown() {

        try {
            closeQuietly()
        } finally {
            executor.shutdownNow()
        }
    }
}