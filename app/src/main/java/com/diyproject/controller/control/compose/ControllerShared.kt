package com.diyproject.controller.control.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun controllerBackgroundBrush(): Brush = Brush.verticalGradient(
    colors = listOf(ControllerColors.background, Color(0xFF05070B))
)

@Composable
fun controllerGlowBrush(): Brush = Brush.radialGradient(
    colors = listOf(
        ControllerColors.glowCyan.copy(alpha = 0.10f),
        Color.Transparent
    )
)

fun Modifier.glassPanel(): Modifier = this
    .clip(ControllerShapes.panel)
    .background(ControllerColors.glassFill)
    .border(1.dp, ControllerColors.glassBorder, ControllerShapes.panel)

data class TopBarState(
    val isConnected: Boolean,
    val headlightOn: Boolean,
    val cabinLightOn: Boolean,
    val soundOn: Boolean,
    val warningOn: Boolean,
    val speedPercent: Int
)

data class TopBarActions(
    val onConnectionClick: () -> Unit,
    val onToggleHeadlight: () -> Unit,
    val onToggleCabinLight: () -> Unit,
    val onToggleSound: () -> Unit,
    val onToggleWarning: () -> Unit,
    val onSpeedChange: (Float) -> Unit,
    val onSettingsClick: () -> Unit,
    val onStopClick: () -> Unit
)

@Composable
fun TopControllerBar(
    state: TopBarState,
    actions: TopBarActions
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SharedConnectionPill(isConnected = state.isConnected, onClick = actions.onConnectionClick)

        GlassIconToggleButton(
            icon = Icons.Filled.Lightbulb,
            isOn = state.headlightOn,
            contentDescription = "Headlight",
            onToggle = actions.onToggleHeadlight
        )

        GlassIconToggleButton(
            icon = Icons.Filled.LightMode,
            isOn = state.cabinLightOn,
            contentDescription = "Cabin light",
            onToggle = actions.onToggleCabinLight
        )

        GlassIconToggleButton(
            icon = Icons.Filled.VolumeUp,
            isOn = state.soundOn,
            contentDescription = "Sound",
            onToggle = actions.onToggleSound
        )

        GlassIconToggleButton(
            icon = Icons.Filled.Warning,
            isOn = state.warningOn,
            contentDescription = "Warning lights",
            onToggle = actions.onToggleWarning
        )

        Spacer(modifier = Modifier.weight(1f))

        SharedStopButton(onClick = actions.onStopClick)

        GlassIconToggleButton(
            icon = Icons.AutoMirrored.Filled.FormatListBulleted,
            isOn = true,
            contentDescription = "Command list",
            onToggle = actions.onSettingsClick
        )

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "SPD",
            color = ControllerColors.textSecondary,
            fontSize = 10.sp
        )

        Slider(
            value = (state.speedPercent / 100f).coerceIn(0f, 1f),
            onValueChange = actions.onSpeedChange,
            valueRange = 0f..1f,
            modifier = Modifier
                .width(200.dp)
                .height(60.dp),
            colors = SliderDefaults.colors(
                thumbColor = ControllerColors.glowCyan,
                activeTrackColor = ControllerColors.glowCyan,
                inactiveTrackColor = ControllerColors.glowCyan.copy(alpha = 0.2f)
            )
        )

        Text(
            text = "${state.speedPercent}%",
            color = ControllerColors.glowCyan,
            fontSize = 11.sp,
            modifier = Modifier.width(30.dp)
        )
    }
}

@Composable
private fun SharedConnectionPill(isConnected: Boolean, onClick: () -> Unit) {
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

@Composable
private fun SharedStopButton(onClick: () -> Unit) {
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