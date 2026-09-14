package com.diyproject.controller

import android.bluetooth.BluetoothAdapter

class BluetoothStatusHelper {

    private val adapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

    fun isBluetoothSupported(): Boolean = adapter != null

    fun isBluetoothOn(): Boolean = adapter?.isEnabled == true

    fun getAdapter(): BluetoothAdapter? = adapter
}