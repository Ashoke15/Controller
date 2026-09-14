package com.diyproject.controller

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.animation.AnimationUtils
import com.diyproject.controller.control.compose.ControlActivity


class MainActivity : AppCompatActivity() {

    private lateinit var tvBtStatus: TextView
    private val btHelper = BluetoothStatusHelper()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvBtStatus = findViewById(R.id.tvBtStatus)
        updateBluetoothStatusUI()

        setupControlSection()
        setupDeveloperSection()
        setupTemplateSection()
    }

    override fun onResume() {
        super.onResume()
        updateBluetoothStatusUI()
    }

    private fun updateBluetoothStatusUI() {
        val dot = findViewById<View>(R.id.dotStatus)
        val pill = findViewById<View>(R.id.btStatusPill)

        if (btHelper.isBluetoothOn()) {
            tvBtStatus.text = "Connected Ready"
            tvBtStatus.setTextColor(resources.getColor(R.color.accent_green, theme))
            pill.setBackgroundResource(R.drawable.bg_status_pill)
            dot.setBackgroundResource(R.drawable.bg_status_pill)
            dot.startAnimation(AnimationUtils.loadAnimation(this, R.anim.pulse))
        } else {
            tvBtStatus.text = "Bluetooth Off"
            tvBtStatus.setTextColor(resources.getColor(R.color.accent_red, theme))
            pill.setBackgroundResource(R.drawable.bg_status_pill_off)
            dot.setBackgroundResource(R.drawable.bg_status_pill_off)
            dot.clearAnimation()
        }
    }

    private fun setupControlSection() {
        val items = listOf(
            FeatureItem("btn_control", "Button", "D-Pad control", android.R.drawable.ic_menu_directions),
            FeatureItem("joystick", "Joystick", "Analog control", android.R.drawable.ic_menu_mylocation),
            FeatureItem("gyro", "Gyroscope", "Tilt control", android.R.drawable.ic_menu_compass)
        )
        bindRecycler(R.id.rvControl, items)
    }

    private fun setupDeveloperSection() {
        val items = listOf(
            FeatureItem("code_control", "Code Control", "Send raw commands", android.R.drawable.ic_menu_edit),
            FeatureItem("terminal", "Terminal", "Serial monitor", android.R.drawable.ic_menu_view),
            FeatureItem("macros", "Macros", "Custom command sets", android.R.drawable.ic_menu_manage)
        )
        bindRecycler(R.id.rvDeveloper, items)
    }

    private fun setupTemplateSection() {
        val items = listOf(
            FeatureItem("rc_car", "RC Car", "Classic 4-motor car", android.R.drawable.ic_menu_gallery),
            FeatureItem("tank", "Tank Bot", "Tracked chassis", android.R.drawable.ic_menu_gallery),
            FeatureItem("robot_arm", "Robot Arm", "Servo arm template", android.R.drawable.ic_menu_gallery)
        )
        bindRecycler(R.id.rvTemplates, items)
    }

    private fun bindRecycler(recyclerId: Int, items: List<FeatureItem>) {
        val rv: RecyclerView = findViewById(recyclerId)
        rv.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rv.adapter = FeatureAdapter(items) { item ->
            handleFeatureClick(item)
        }
    }

    private fun handleFeatureClick(item: FeatureItem) {
        when (item.id) {
            "btn_control" -> {
                val intent = android.content.Intent(this, ControlActivity::class.java)
                startActivity(intent)
            }
            "joystick" -> {
                val intent = android.content.Intent(
                    this,
                    com.diyproject.controller.joystick.compose.JoystickActivity::class.java
                )
                startActivity(intent)
            }

        }
    }
}