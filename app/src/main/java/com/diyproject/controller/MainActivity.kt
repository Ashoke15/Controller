package com.diyproject.controller

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.diyproject.controller.control.compose.ControlActivity
import com.diyproject.controller.devtools.codecontrol.CodeControlActivity
import com.diyproject.controller.devtools.terminal.TerminalActivity
import com.diyproject.controller.gyro.compose.GyroActivity
import com.diyproject.controller.home.compose.AboutDialog
import com.diyproject.controller.home.compose.ConnectionState
import com.diyproject.controller.home.compose.FeatureSection
import com.diyproject.controller.home.compose.HomeFooter
import com.diyproject.controller.home.compose.HomeScreen
import com.diyproject.controller.home.compose.RcControllerTheme
import com.diyproject.controller.home.compose.TemplateSection
import com.diyproject.controller.home.compose.developerBadgeFor
import com.diyproject.controller.joystick.compose.JoystickActivity
import java.io.File

class MainActivity : AppCompatActivity() {

    private val btHelper = BluetoothStatusHelper()

    // Compose-observable UI state. Reading these inside setContent{} keeps the
    // screen in sync without introducing a ViewModel — same flow as the old
    // updateBluetoothStatusUI() / showAboutDialog(), just Compose-driven.
    private var connected by mutableStateOf(false)
    private var showAboutDialog by mutableStateOf(false)
    private var selectedTemplateId by mutableStateOf<String?>(null)

    /**
     * `isBluetoothOn()` is only a point-in-time check. Before, it was re-read
     * in onCreate/onResume only, so a mid-session disconnect (radio turned
     * off, RC device dropping the link) sat stale on screen until the user
     * backgrounded and reopened the app. These three broadcasts fire the
     * instant the adapter or a device's ACL connection actually changes, so
     * re-checking from here is real-time rather than "current as of the last
     * time you switched apps."
     *
     * We re-derive from btHelper.isBluetoothOn() rather than trusting the
     * intent's own extras, so this stays correct no matter how that helper
     * defines "connected" internally.
     */
    private val bluetoothStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            connected = btHelper.isBluetoothOn()
        }
    }

    private val bluetoothStateFilter = IntentFilter().apply {
        addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
        addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
    }

    private val controlItems = listOf(
        FeatureItem("btn_control", "Button", "D-Pad control", R.drawable.d_pad),
        FeatureItem("joystick", "Joystick", "Analog control", R.drawable.joystick_pig),
        FeatureItem("gyro", "Gyroscope", "Tilt control", R.drawable.gyro_pig),
    )

    private val developerItems = listOf(
        FeatureItem("code_control", "Code Control", "Send raw commands", R.drawable.code_pig),
        FeatureItem("terminal", "Terminal", "Serial monitor", R.drawable.terminal_pig),
        FeatureItem("macros", "Macros", "Custom command sets", R.drawable.macros_pig),
    )

    private val templateItems = listOf(
        FeatureItem("rc_car", "RC Car", "Classic 4-motor car", R.drawable.rc_car_pig),
        FeatureItem("tank", "Tank Bot", "Tracked chassis", R.drawable.tank_bot_pig),
        FeatureItem("robot_arm", "Robot Arm", "Servo arm template", R.drawable.robot_arm_pig),
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_Controller)
        super.onCreate(savedInstanceState)

        supportActionBar?.hide()

        connected = btHelper.isBluetoothOn()
        selectedTemplateId = templateItems.firstOrNull()?.id

        setContent {
            RcControllerTheme {
                val versionName = remember { appVersionName() }

                HomeScreen(
                    appName = stringResource(R.string.app_name),
                    tagline = stringResource(R.string.tagline),
                    brandIcon = painterResource(id = R.drawable.header_image),
                    connection = ConnectionState(
                        connected = connected,
                        label = if (connected) {
                            stringResource(R.string.bt_status_connected)
                        } else {
                            stringResource(R.string.status_disconnected)
                        },
                    ),
                    controlSection = FeatureSection(
                        title = stringResource(R.string.section_control),
                        items = controlItems,
                    ),
                    developerSection = FeatureSection(
                        title = stringResource(R.string.section_developer),
                        items = developerItems,
                        badgeFor = ::developerBadgeFor,
                    ),
                    templateSection = TemplateSection(
                        title = stringResource(R.string.section_car_templates),
                        items = templateItems,
                        selectedId = selectedTemplateId,
                    ),
                    footer = HomeFooter(
                        shareLabel = stringResource(R.string.btn_share),
                        aboutLabel = stringResource(R.string.btn_about),
                        versionLabel = "${stringResource(R.string.app_name)} \u00B7 v$versionName",
                        madeBy = stringResource(R.string.footer_made_by),
                    ),
                    onTemplateSelected = { selectedTemplateId = it },
                    onFeatureClick = ::handleFeatureClick,
                    onShareClick = ::shareApp,
                    onAboutClick = { showAboutDialog = true },
                )

                if (showAboutDialog) {
                    AboutDialog(
                        appName = stringResource(R.string.app_name),
                        versionLabel = "Version $versionName",
                        message = stringResource(R.string.about_message),
                        madeBy = stringResource(R.string.footer_made_by),
                        closeLabel = stringResource(R.string.btn_got_it),
                        brandIcon = painterResource(id = R.drawable.header_image),
                        onDismiss = { showAboutDialog = false },
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Re-sync immediately — covers anything that changed while the
        // activity was stopped — then stay subscribed for live updates
        // for the whole time the app is visible.
        connected = btHelper.isBluetoothOn()
        ContextCompat.registerReceiver(
            this,
            bluetoothStateReceiver,
            bluetoothStateFilter,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(bluetoothStateReceiver)
    }

    private fun appVersionName(): String = try {
        packageManager.getPackageInfo(packageName, 0).versionName ?: "1.0.0"
    } catch (e: Exception) {
        "1.0.0"
    }

    private fun handleFeatureClick(item: FeatureItem) {
        when (item.id) {
            "btn_control" -> startActivity(Intent(this, ControlActivity::class.java))
            "joystick" -> startActivity(Intent(this, JoystickActivity::class.java))
            "gyro" -> startActivity(Intent(this, GyroActivity::class.java))
            "code_control" -> startActivity(Intent(this, CodeControlActivity::class.java))
            "terminal" ->startActivity(Intent(this, TerminalActivity::class.java))
        }
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
                shareFile,
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
}