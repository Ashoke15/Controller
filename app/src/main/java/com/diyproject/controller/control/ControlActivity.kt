package com.diyproject.controller.control

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.diyproject.controller.R

class ControlActivity : AppCompatActivity() {

    private lateinit var tvBtStatus: TextView
    private lateinit var dotStatus: View
    private lateinit var btStatusPill: View
    private lateinit var tvSpeedValue: TextView

    private var headlightOn = false
    private var backlightOn = false
    private var hazardOn = false
    private var currentSpeed = 50

    private lateinit var bt: BluetoothSppManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_control)

        bt = BluetoothSppManager(
            onConnected = { runOnUiThread { setConnectedUi(true) } },
            onDisconnected = { runOnUiThread { setConnectedUi(false) } },
            onError = { msg -> runOnUiThread {
                setConnectedUi(false)
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }}
        )

        bindViews()
        setupDirectionalButtons()
        setupToggleButtons()
        setupSpeedSlider()
        setupStopAll()

        btStatusPill.setOnClickListener { showDevicePicker() }
    }

    private fun bindViews() {
        tvBtStatus = findViewById(R.id.tvBtStatus)
        dotStatus = findViewById(R.id.dotStatus)
        btStatusPill = findViewById(R.id.btStatusPill)
        tvSpeedValue = findViewById(R.id.tvSpeedValue)
    }

    // ---------- Bluetooth connect flow ----------

    private fun hasBtPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(
                this, Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        }
        return true
    }

    private fun requestBtPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.BLUETOOTH_CONNECT), 101
            )
        }
    }

    private fun showDevicePicker() {
        if (!hasBtPermission()) {
            requestBtPermission()
            return
        }
        val devices = bt.getPairedDevices()
        if (devices.isEmpty()) {
            Toast.makeText(this, "No paired devices. Pair your HC-05 in system Bluetooth settings first.", Toast.LENGTH_LONG).show()
            return
        }
        val names = devices.map { it.name ?: it.address }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Select car module")
            .setItems(names) { _, which ->
                connectTo(devices[which])
            }
            .show()
    }

    private fun connectTo(device: BluetoothDevice) {
        tvBtStatus.text = "Connecting..."
        bt.connect(device)
    }

    private fun setConnectedUi(connected: Boolean) {
        if (connected) {
            tvBtStatus.text = "Connected"
            tvBtStatus.setTextColor(getColor(R.color.accent_green))
            btStatusPill.setBackgroundResource(R.drawable.bg_status_pill)
            dotStatus.setBackgroundResource(R.drawable.bg_status_pill)
            dotStatus.startAnimation(AnimationUtils.loadAnimation(this, R.anim.pulse))
        } else {
            tvBtStatus.text = "Tap to connect"
            tvBtStatus.setTextColor(getColor(R.color.accent_red))
            btStatusPill.setBackgroundResource(R.drawable.bg_status_pill_off)
            dotStatus.setBackgroundResource(R.drawable.bg_status_pill_off)
            dotStatus.clearAnimation()
        }
    }

    // ---------- Directional D-pad (press = move, release = stop) ----------

    private fun setupDirectionalButtons() {
        setupHoldButton(R.id.btnUp, CarCommand.FORWARD)
        setupHoldButton(R.id.btnDown, CarCommand.BACK)
        setupHoldButton(R.id.btnLeft, CarCommand.LEFT)
        setupHoldButton(R.id.btnRight, CarCommand.RIGHT)
    }

    @Suppress("ClickableViewAccessibility")
    private fun setupHoldButton(viewId: Int, command: String) {
        val button: ImageButton = findViewById(viewId)
        button.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    bt.send(command)
                    v.alpha = 0.6f
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    bt.send(CarCommand.STOP)
                    v.alpha = 1f
                }
            }
            true
        }
    }

    // ---------- Toggle features ----------

    private fun setupToggleButtons() {
        findViewById<ImageButton>(R.id.btnHeadlight).setOnClickListener {
            headlightOn = !headlightOn
            bt.send(if (headlightOn) CarCommand.FRONT_LIGHTS_ON else CarCommand.FRONT_LIGHTS_OFF)
            it.alpha = if (headlightOn) 1f else 0.5f
        }

        findViewById<ImageButton>(R.id.btnBacklight).setOnClickListener {
            backlightOn = !backlightOn
            bt.send(if (backlightOn) CarCommand.BACK_LIGHTS_ON else CarCommand.BACK_LIGHTS_OFF)
            it.alpha = if (backlightOn) 1f else 0.5f
        }

        findViewById<ImageButton>(R.id.btnHazard).setOnClickListener {
            hazardOn = !hazardOn
            bt.send(if (hazardOn) CarCommand.EXTRA_ON else CarCommand.EXTRA_OFF)
            it.alpha = if (hazardOn) 1f else 0.5f
        }

        // Horn: momentary — on while pressed, off on release
        val hornBtn = findViewById<ImageButton>(R.id.btnHorn)
        hornBtn.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    bt.send(CarCommand.HORN_ON)
                    v.alpha = 0.6f
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    bt.send(CarCommand.HORN_OFF)
                    v.alpha = 1f
                }
            }
            true
        }

        findViewById<ImageButton>(R.id.btnSettings).setOnClickListener {
            Toast.makeText(this, "Settings coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    // ---------- Speed slider ----------

    private fun setupSpeedSlider() {
        val seek: SeekBar = findViewById(R.id.seekSpeed)
        seek.progress = currentSpeed
        tvSpeedValue.text = "$currentSpeed%"

        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                currentSpeed = progress
                tvSpeedValue.text = "$progress%"
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {
                bt.send(CarCommand.speedToChar(currentSpeed))
            }
        })
    }

    // ---------- Emergency stop ----------

    private fun setupStopAll() {
        findViewById<android.widget.Button>(R.id.btnStopAll).setOnClickListener {
            bt.send(CarCommand.STOP_ALL)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        bt.disconnect()
    }
}