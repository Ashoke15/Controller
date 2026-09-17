package com.diyproject.controller.gyro.compose

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.diyproject.controller.control.CarCommand
import com.diyproject.controller.control.compose.ControllerColors
import com.diyproject.controller.control.compose.ControllerShapes
import com.diyproject.controller.control.compose.clickableSimplePublic
import com.diyproject.controller.control.compose.controllerBackgroundBrush
import com.diyproject.controller.control.compose.glassPanel
import kotlin.math.max
import kotlin.math.min

/**
 * Production-grade full-screen landscape tilt controller.
 *
 * Three-zone composition driven entirely by [BoxWithConstraints] — no fixed
 * pixel/dp screen coordinates:
 *  - Top: a slim status strip (back / recalibrate / connection / mode).
 *  - Middle: the tilt indicator ("bubble level") on the left, a compact
 *    telemetry + STOP column on the right.
 *  - Bottom: the Digital/Analog mode toggle.
 *
 * All sensor, calibration, and Bluetooth logic lives in GyroActivity — this
 * file is UI only. [onManualStop] is the one new callback versus before: it
 * fires an immediate STOP with no animation gating it, alongside the existing
 * onPause() safety stop in GyroActivity.
 */
@Composable
fun GyroScreen(
    pitch: Float,
    roll: Float,
    mode: GyroMode,
    speedPercent: Int,
    activeCommand: String,
    isConnected: Boolean,
    deadzoneDegrees: Float,
    maxTiltDegrees: Float,
    showDevicePicker: Boolean,
    pairedDevices: List<BluetoothDevice>,
    onModeChange: (GyroMode) -> Unit,
    onConnectionClick: () -> Unit,
    onRecalibrate: () -> Unit,
    onManualStop: () -> Unit,
    onDeviceSelected: (BluetoothDevice) -> Unit,
    onDismissDevicePicker: () -> Unit,
    onExit: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(controllerBackgroundBrush())
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp, vertical = 14.dp)
        ) {
            // Responsive sizing derived from available space at composition time —
            // scales across 16:9 phones through large landscape tablets.
            val indicatorSize = (maxHeight * 0.66f).coerceIn(200.dp, 420.dp)
            val telemetryWidth = (maxWidth * 0.2f).coerceIn(148.dp, 220.dp)

            Column(modifier = Modifier.fillMaxSize()) {
                GyroTopStatusStrip(
                    isConnected = isConnected,
                    mode = mode,
                    onConnectionClick = onConnectionClick,
                    onRecalibrate = onRecalibrate,
                    onExit = onExit
                )

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        BubbleLevel(
                            pitch = pitch,
                            roll = roll,
                            deadzoneDegrees = deadzoneDegrees,
                            maxTiltDegrees = maxTiltDegrees,
                            diameter = indicatorSize
                        )
                    }

                    Spacer(modifier = Modifier.width(18.dp))

                    TelemetryColumn(
                        width = telemetryWidth,
                        isConnected = isConnected,
                        mode = mode,
                        activeCommand = activeCommand,
                        speedPercent = speedPercent,
                        onManualStop = onManualStop
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                ModeToggleSwitch(
                    mode = mode,
                    onModeChange = onModeChange,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }

        if (showDevicePicker) {
            DevicePickerDialog(
                devices = pairedDevices,
                onDeviceSelected = onDeviceSelected,
                onDismiss = onDismissDevicePicker
            )
        }
    }
}

/** Slim top strip: back, recalibrate, connection state, current mode label. */
@Composable
private fun GyroTopStatusStrip(
    isConnected: Boolean,
    mode: GyroMode,
    onConnectionClick: () -> Unit,
    onRecalibrate: () -> Unit,
    onExit: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        IconGlassButton(
            icon = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            onClick = onExit
        )
        IconGlassButton(
            icon = Icons.Filled.Refresh,
            contentDescription = "Recalibrate level",
            onClick = onRecalibrate
        )

        val dotColor = if (isConnected) ControllerColors.accentGreen else ControllerColors.accentRed
        Row(
            modifier = Modifier
                .clip(ControllerShapes.pill)
                .background(dotColor.copy(alpha = 0.10f))
                .border(1.dp, dotColor.copy(alpha = 0.55f), ControllerShapes.pill)
                .clickableSimplePublic(onConnectionClick)
                .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .background(dotColor, shape = CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isConnected) "CONNECTED" else "DISCONNECTED",
                color = dotColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "TILT CONTROL",
            color = ControllerColors.textSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = if (mode == GyroMode.DIGITAL) "DIGITAL MODE" else "ANALOG MODE",
            color = ControllerColors.glowCyan,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun IconGlassButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .glassPanel()
            .clickableSimplePublic(onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = contentDescription, tint = ControllerColors.textSecondary)
    }
}

/** Right-hand telemetry panel + emergency stop. */
@Composable
private fun TelemetryColumn(
    width: Dp,
    isConnected: Boolean,
    mode: GyroMode,
    activeCommand: String,
    speedPercent: Int,
    onManualStop: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(width)
            .fillMaxHeight(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .glassPanel()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            TelemetryRow(label = "STATUS", value = if (isConnected) "Ready" else "Offline")
            TelemetryRow(label = "MODE", value = if (mode == GyroMode.DIGITAL) "Digital" else "Analog")
            TelemetryRow(
                label = "COMMAND",
                value = commandLabel(activeCommand),
                valueColor = ControllerColors.glowCyan
            )
            if (mode == GyroMode.ANALOG) {
                TelemetryRow(label = "SPEED", value = "${speedPercent}%")
            }
        }

        EmergencyStopButton(enabled = isConnected, onClick = onManualStop)
    }
}

@Composable
private fun TelemetryRow(
    label: String,
    value: String,
    valueColor: Color = ControllerColors.textSecondary
) {
    Column {
        Text(
            text = label,
            color = ControllerColors.textSecondary.copy(alpha = 0.65f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, color = valueColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

/**
 * High-contrast emergency stop. Fires [onClick] immediately on press — no
 * animation gates the actual stop command, per production-safety requirements.
 * Visually dims (but stays clickable) when already disconnected, since a stop
 * command has nothing to reach at that point.
 */
@Composable
private fun EmergencyStopButton(enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(ControllerColors.accentRed.copy(alpha = if (enabled) 0.16f else 0.06f))
            .border(
                width = 1.5.dp,
                color = ControllerColors.accentRed.copy(alpha = if (enabled) 0.9f else 0.3f),
                shape = RoundedCornerShape(14.dp)
            )
            .clickableSimplePublic(onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "STOP",
            color = ControllerColors.accentRed.copy(alpha = if (enabled) 1f else 0.4f),
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun commandLabel(command: String): String = when (command) {
    CarCommand.FORWARD -> "FORWARD"
    CarCommand.BACK -> "BACK"
    CarCommand.LEFT -> "LEFT"
    CarCommand.RIGHT -> "RIGHT"
    CarCommand.FORWARD_LEFT -> "FWD-LEFT"
    CarCommand.FORWARD_RIGHT -> "FWD-RIGHT"
    CarCommand.BACK_LEFT -> "BACK-LEFT"
    CarCommand.BACK_RIGHT -> "BACK-RIGHT"
    else -> "STOP"
}

/**
 * Glassmorphic tilt "bubble level": a glowing dot drifts within a circular
 * glass base based on the phone's pitch (vertical axis) and roll (horizontal
 * axis). [diameter] is caller-supplied so sizing is fully responsive.
 */
@Composable
private fun BubbleLevel(
    pitch: Float,
    roll: Float,
    deadzoneDegrees: Float,
    maxTiltDegrees: Float,
    diameter: Dp
) {
    val normX = (roll / maxTiltDegrees).coerceIn(-1f, 1f)
    val normY = (-pitch / maxTiltDegrees).coerceIn(-1f, 1f)

    val animatedX by animateFloatAsState(
        targetValue = normX,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 220f),
        label = "gyroDotX"
    )
    val animatedY by animateFloatAsState(
        targetValue = normY,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 220f),
        label = "gyroDotY"
    )

    val inDeadzone = kotlin.math.abs(pitch) < deadzoneDegrees && kotlin.math.abs(roll) < deadzoneDegrees
    val dotColor = if (inDeadzone) ControllerColors.textSecondary else ControllerColors.glowCyan

    val infiniteTransition = rememberInfiniteTransition(label = "gyroPulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gyroPulseScale"
    )
    val glowScale = if (inDeadzone) 1f else pulse

    Box(
        modifier = Modifier
            .size(diameter)
            .clip(CircleShape)
            .background(ControllerColors.glassFill)
            .border(1.dp, ControllerColors.glassBorder, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = size.minDimension / 2f
            val center = Offset(size.width / 2f, size.height / 2f)

            // Faint radial backdrop glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(ControllerColors.glowCyan.copy(alpha = 0.08f), Color.Transparent),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )

            // 50%-tilt reference ring, dashed for a technical "instrument" feel
            drawCircle(
                color = ControllerColors.glassBorder.copy(alpha = 0.5f),
                radius = radius * 0.5f,
                center = center,
                style = Stroke(
                    width = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 8f))
                )
            )

            // Deadzone ring
            val deadzoneRadius = radius * (deadzoneDegrees / maxTiltDegrees)
            drawCircle(
                color = ControllerColors.textSecondary.copy(alpha = 0.35f),
                radius = deadzoneRadius,
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )

            // Outer ring
            drawCircle(
                color = ControllerColors.glassBorder,
                radius = radius - 2.dp.toPx(),
                center = center,
                style = Stroke(width = 1.5.dp.toPx())
            )

            // Crosshair, gapped around the deadzone ring
            val gap = deadzoneRadius + 6.dp.toPx()
            drawLine(
                color = ControllerColors.glassBorder,
                start = Offset(center.x, center.y - radius),
                end = Offset(center.x, center.y - gap),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = ControllerColors.glassBorder,
                start = Offset(center.x - radius, center.y),
                end = Offset(center.x - gap, center.y),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = ControllerColors.glassBorder,
                start = Offset(center.x + gap, center.y),
                end = Offset(center.x + radius, center.y),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = ControllerColors.glassBorder,
                start = Offset(center.x, center.y + gap),
                end = Offset(center.x, center.y + radius),
                strokeWidth = 1.dp.toPx()
            )

            // Tick marks at the 50%/100% radii on each axis
            listOf(0.5f, 1f).forEach { frac ->
                val tickLen = 4.dp.toPx()
                val r = radius * frac
                drawLine(ControllerColors.glassBorder, Offset(center.x + r, center.y - tickLen), Offset(center.x + r, center.y + tickLen), 1.dp.toPx())
                drawLine(ControllerColors.glassBorder, Offset(center.x - r, center.y - tickLen), Offset(center.x - r, center.y + tickLen), 1.dp.toPx())
                drawLine(ControllerColors.glassBorder, Offset(center.x - tickLen, center.y + r), Offset(center.x + tickLen, center.y + r), 1.dp.toPx())
                drawLine(ControllerColors.glassBorder, Offset(center.x - tickLen, center.y - r), Offset(center.x + tickLen, center.y - r), 1.dp.toPx())
            }

            // Glowing dot position, clamped inside the panel
            val rawOffset = Offset(animatedX * radius, animatedY * radius)
            val mag = min(1f, max(0f, kotlin.math.sqrt(rawOffset.x * rawOffset.x + rawOffset.y * rawOffset.y) / radius))
            val clampedOffset = if (mag > 0.94f) {
                Offset(rawOffset.x / mag * radius * 0.94f, rawOffset.y / mag * radius * 0.94f)
            } else rawOffset
            val dotCenter = Offset(center.x + clampedOffset.x, center.y + clampedOffset.y)

            // Direction line from center to dot once past the deadzone
            if (!inDeadzone) {
                drawLine(
                    color = dotColor.copy(alpha = 0.5f),
                    start = center,
                    end = dotCenter,
                    strokeWidth = 1.5.dp.toPx()
                )
            }

            // Soft glow behind the dot, pulsing while actively driving
            val glowRadius = 26.dp.toPx() * glowScale
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(dotColor.copy(alpha = 0.45f), Color.Transparent),
                    center = dotCenter,
                    radius = glowRadius
                ),
                radius = glowRadius,
                center = dotCenter
            )
            // Solid dot core
            drawCircle(
                color = dotColor,
                radius = 7.dp.toPx(),
                center = dotCenter
            )
        }

        // Cardinal axis labels
        Text("F", color = ControllerColors.textSecondary, fontSize = 11.sp, modifier = Modifier.align(Alignment.TopCenter).padding(top = 10.dp))
        Text("B", color = ControllerColors.textSecondary, fontSize = 11.sp, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp))
        Text("L", color = ControllerColors.textSecondary, fontSize = 11.sp, modifier = Modifier.align(Alignment.CenterStart).padding(start = 10.dp))
        Text("R", color = ControllerColors.textSecondary, fontSize = 11.sp, modifier = Modifier.align(Alignment.CenterEnd).padding(end = 10.dp))
    }
}

/**
 * Sleek two-segment glass toggle: Digital Mode | Analog Mode.
 * Built from the same glassPanel() styling as the rest of the app.
 */
@Composable
private fun ModeToggleSwitch(
    mode: GyroMode,
    onModeChange: (GyroMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .glassPanel()
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ToggleSegment(
            label = "Digital",
            selected = mode == GyroMode.DIGITAL,
            onClick = { onModeChange(GyroMode.DIGITAL) }
        )
        ToggleSegment(
            label = "Analog",
            selected = mode == GyroMode.ANALOG,
            onClick = { onModeChange(GyroMode.ANALOG) }
        )
    }
}

@Composable
private fun ToggleSegment(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (selected) ControllerColors.glowCyan.copy(alpha = 0.16f) else Color.Transparent
    val textColor = if (selected) ControllerColors.glowCyan else ControllerColors.textSecondary
    val borderColor = if (selected) ControllerColors.glowCyan else Color.Transparent

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .clickableSimplePublic(onClick)
            .padding(horizontal = 22.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

/**
 * Simple glass-styled list of paired devices, shown when the connection pill is
 * tapped while disconnected. Reading `device.name` needs BLUETOOTH_CONNECT on
 * API 31+; by the time devices reach here, BluetoothSppManager.getPairedDevices()
 * has already confirmed that permission is granted.
 */
@SuppressLint("MissingPermission")
@Composable
private fun DevicePickerDialog(
    devices: List<BluetoothDevice>,
    onDeviceSelected: (BluetoothDevice) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .glassPanel()
                .padding(16.dp)
        ) {
            Text(
                text = "Select a paired device",
                color = ControllerColors.textSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 10.dp)
            )

            if (devices.isEmpty()) {
                Text(
                    text = "No paired devices found. Pair your car's Bluetooth module in system settings first.",
                    color = ControllerColors.textSecondary,
                    fontSize = 12.sp
                )
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 260.dp)) {
                    items(devices) { device ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickableSimplePublic { onDeviceSelected(device) }
                                .padding(horizontal = 10.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .background(ControllerColors.glowCyan, shape = CircleShape)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = device.name ?: "Unknown device",
                                    color = ControllerColors.textSecondary,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = device.address,
                                    color = ControllerColors.textSecondary.copy(alpha = 0.6f),
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}