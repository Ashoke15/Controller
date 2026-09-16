package com.diyproject.controller.control.compose

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ControllerTopBar(
    isConnected: Boolean,
    onConnectClick: () -> Unit,
    headlightOn: Boolean,
    onHeadlightToggle: () -> Unit,
    backlightOn: Boolean,
    onBacklightToggle: () -> Unit,
    hazardOn: Boolean,
    onHazardToggle: () -> Unit,
    onHornPress: () -> Unit,
    onHornRelease: () -> Unit,
    speed: Int,
    onSpeedChange: (Float) -> Unit,
    onSpeedCommit: () -> Unit,
    onSettingsClick: () -> Unit,
    onStopAllClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // BT connection pill
        ConnectionPill(isConnected = isConnected, onClick = onConnectClick)

        // Headlight toggle
        GlassIconToggleButton(
            icon = Icons.Filled.Lightbulb,
            isOn = headlightOn,
            contentDescription = "Headlight",
            activeTint = Color(0xFFFFE082),
            onToggle = onHeadlightToggle
        )

        // Backlight toggle
        GlassIconToggleButton(
            icon = Icons.Filled.LightMode,
            isOn = backlightOn,
            contentDescription = "Backlight",
            activeTint = Color(0xFFFFB74D),
            onToggle = onBacklightToggle
        )

        // Horn - momentary
        GlassMomentaryButton(
            icon = Icons.Filled.Campaign,
            contentDescription = "Horn",
            activeTint = Color(0xFFFF7043),
            onPress = onHornPress,
            onRelease = onHornRelease
        )

        // Hazard / triangle indicator
        GlassIconToggleButton(
            icon = Icons.Filled.Warning,
            isOn = hazardOn,
            contentDescription = "Hazard",
            activeTint = Color(0xFFFF3D00),
            onToggle = onHazardToggle
        )

        Spacer(modifier = Modifier.weight(1f))

        // STOP button - center-ish
        StopButton(onClick = onStopAllClick)

        // Command list / settings icon
        GlassIconToggleButton(
            icon = Icons.AutoMirrored.Filled.FormatListBulleted,
            isOn = true,
            contentDescription = "Command list",
            onToggle = onSettingsClick
        )

        Spacer(modifier = Modifier.weight(1f))

        // Speed label
        Text(
            text = "SPD",
            color = ControllerColors.textSecondary,
            fontSize = 10.sp
        )

        // Thick vertical speed slider on far right
        VerticalSpeedSlider(
            speed = speed,
            onSpeedChange = onSpeedChange,
            onSpeedCommit = onSpeedCommit
        )

        Text(
            text = "$speed%",
            color = ControllerColors.glowCyan,
            fontSize = 11.sp,
            modifier = Modifier.width(30.dp)
        )
    }
}

// ---------- Connection pill ----------

@Composable
private fun ConnectionPill(isConnected: Boolean, onClick: () -> Unit) {
    val bg = if (isConnected) ControllerColors.accentGreen.copy(alpha = 0.12f)
    else ControllerColors.accentRed.copy(alpha = 0.12f)
    val border = if (isConnected) ControllerColors.accentGreen else ControllerColors.accentRed
    val label = if (isConnected) "Connected" else "Connect"

    Row(
        modifier = Modifier
            .clip(ControllerShapes.pill)
            .background(bg)
            .border(1.dp, border, ControllerShapes.pill)
            .clickableSimplePublic(onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(border, shape = CircleShape)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(text = label, color = border, fontSize = 11.sp)
    }
}

// ---------- STOP button ----------

@Composable
private fun StopButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(ControllerColors.accentRed)
            .clickableSimplePublic(onClick)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "STOP",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )
    }
}

// ---------- Momentary press button (horn) ----------

@Composable
private fun GlassMomentaryButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    activeTint: Color,
    onPress: () -> Unit,
    onRelease: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(if (isPressed) 1f else 0.5f, label = "hornAlpha")

    Box(
        modifier = Modifier
            .size(ControllerDimens.iconButtonSize)
            .clip(ControllerShapes.glassButton)
            .background(if (isPressed) activeTint.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.04f))
            .border(1.dp, ControllerColors.glassBorder, ControllerShapes.glassButton)
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown()
                    isPressed = true
                    onPress()
                    waitForUpOrCancellation()
                    isPressed = false
                    onRelease()
                }
            }
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isPressed) activeTint else ControllerColors.textPrimary.copy(alpha = alpha),
            modifier = Modifier.size(20.dp)
        )

        if (isPressed) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-4).dp)
                    .background(activeTint, shape = CircleShape)
                    .border(1.dp, Color.White, CircleShape)
            )
        }
    }
}

// ---------- Vertical thick speed slider ----------

@Composable
private fun VerticalSpeedSlider(
    speed: Int,
    onSpeedChange: (Float) -> Unit,
    onSpeedCommit: () -> Unit
) {
    Slider(
        value = speed.toFloat(),
        onValueChange = onSpeedChange,
        onValueChangeFinished = onSpeedCommit,
        valueRange = 0f..100f,
        steps = 9,
        modifier = Modifier
            .width(200.dp)
            .height(60.dp),
        colors = SliderDefaults.colors(
            thumbColor = ControllerColors.glowCyan,
            activeTrackColor = ControllerColors.glowCyan,
            inactiveTrackColor = ControllerColors.glowCyan.copy(alpha = 0.2f)
        )
    )
}

// ---------- Shared click helper ----------

fun Modifier.clickableSimplePublic(onClick: () -> Unit): Modifier =
    this.then(
        Modifier.pointerInput(Unit) {
            detectTapGestures(onTap = { onClick() })
        }
    )