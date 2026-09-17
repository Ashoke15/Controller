package com.diyproject.controller

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.animation.AnimationUtils
import com.diyproject.controller.control.compose.ControlActivity
import android.content.Intent
import androidx.appcompat.app.AlertDialog
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File


class MainActivity : AppCompatActivity() {

    private lateinit var tvBtStatus: TextView
    private val btHelper = BluetoothStatusHelper()

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_Controller)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvBtStatus = findViewById(R.id.tvBtStatus)
        updateBluetoothStatusUI()

        setupControlSection()
        setupDeveloperSection()
        setupTemplateSection()
        setupActionButtons()
        setupFooter()
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

    private fun setupFooter() {
        val tvFooter = findViewById<TextView>(R.id.tvAppFooter)
        try {
            val pInfo = packageManager.getPackageInfo(packageName, 0)
            tvFooter.text = "RC Controller · v${pInfo.versionName}"
        } catch (e: Exception) {
            tvFooter.text = "RC Controller"
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
            "gyro" -> {
                val intent = android.content.Intent(
                    this,
                    com.diyproject.controller.gyro.compose.GyroActivity::class.java
                )
                startActivity(intent)
            }

        }
    }

    private fun setupActionButtons() {
        findViewById<View>(R.id.btnShare).setOnClickListener { shareApp() }
        findViewById<View>(R.id.btnAbout).setOnClickListener { showAboutDialog() }
    }

    private fun shareApp() {
        try {
            val sourceApk = File(applicationInfo.sourceDir)
            val shareDir = File(cacheDir, "apk_share").apply { mkdirs() }
            val shareFile = File(shareDir, "${getString(R.string.app_name).replace(" ", "_")}.apk")
            sourceApk.copyTo(shareFile, overwrite = true)

            val apkUri = FileProvider.getUriForFile(
                this,
                "$packageName.fileprovider",
                shareFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.android.package-archive"
                putExtra(Intent.EXTRA_STREAM, apkUri)
                putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name))
                putExtra(Intent.EXTRA_TEXT, getString(R.string.share_message))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, getString(R.string.btn_share)))
        } catch (e: Exception) {
            Toast.makeText(this, "Couldn't share the app file", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showAboutDialog() {
        val versionName = try {
            packageManager.getPackageInfo(packageName, 0).versionName
        } catch (e: Exception) {
            "1.0.0"
        }

        val dialogView = layoutInflater.inflate(R.layout.dialog_about, null)
        dialogView.findViewById<TextView>(R.id.tvAboutVersion).text = "Version $versionName"

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogView.findViewById<View>(R.id.tvAboutClose).setOnClickListener { dialog.dismiss() }
        dialog.show()
    }
}