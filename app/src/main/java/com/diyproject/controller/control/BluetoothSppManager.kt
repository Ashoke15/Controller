package com.diyproject.controller.control

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import java.io.IOException
import java.io.OutputStream
import java.util.UUID
import java.util.concurrent.Executors

class BluetoothSppManager(
    private val onConnected: () -> Unit,
    private val onDisconnected: () -> Unit,
    private val onError: (String) -> Unit
) {
    companion object {
        private const val TAG = "BluetoothSppManager"
        private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    private val adapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val executor = Executors.newSingleThreadExecutor()

    private var socket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null

    @Volatile
    var isConnected: Boolean = false
        private set

    fun getPairedDevices(): List<BluetoothDevice> {
        return adapter?.bondedDevices?.toList() ?: emptyList()
    }

    fun connect(device: BluetoothDevice) {
        executor.execute {
            try {
                adapter?.cancelDiscovery()
                val sock = device.createRfcommSocketToServiceRecord(SPP_UUID)
                sock.connect()
                socket = sock
                outputStream = sock.outputStream
                isConnected = true
                onConnected()
            } catch (e: IOException) {
                Log.e(TAG, "Connection failed", e)
                isConnected = false
                closeQuietly()
                onError("Connection failed: ${e.message}")
            }
        }
    }

    /** Sends a single-character (or short) command over the socket. */
    fun send(command: String) {
        if (!isConnected) return
        executor.execute {
            try {
                outputStream?.write(command.toByteArray())
                outputStream?.flush()
            } catch (e: IOException) {
                Log.e(TAG, "Send failed", e)
                isConnected = false
                onDisconnected()
                closeQuietly()
            }
        }
    }

    fun disconnect() {
        executor.execute {
            closeQuietly()
            isConnected = false
            onDisconnected()
        }
    }

    private fun closeQuietly() {
        try { outputStream?.close() } catch (_: IOException) {}
        try { socket?.close() } catch (_: IOException) {}
        outputStream = null
        socket = null
    }
}