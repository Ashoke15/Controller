package com.diyproject.controller.joystick.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp


object ControllerTheme {

    // Deep console navy, with cyan for telemetry/gimbal and amber for
    // throttle/warning — a small, deliberate palette rather than
    // per-component ad-hoc colors.
    val backgroundDeep = Color(0xFF060812L)
    val backgroundTop = Color(0xFF0B1220L)

    val panelFill = Color(0x14FFFFFFL)       // ~8% white
    val panelFillStrong = Color(0x24FFFFFFL) // ~14% white
    val panelBorder = Color(0x2AFFFFFFL)     // ~16% white

    val cyan = Color(0xFF4CE1E8L)
    val cyanDim = Color(0xFF2E7B80L)
    val amber = Color(0xFFFFB454L)
    val red = Color(0xFFFF5C5CL)
    val green = Color(0xFF5CFFA0L)

    val textPrimary = Color(0xFFF2F5FAL)
    val textMuted = Color(0x8FF2F5FAL)
    val textFaint = Color(0x55F2F5FAL)

    fun backgroundBrush(): Brush = Brush.verticalGradient(
        colors = listOf(backgroundTop, backgroundDeep)
    )

    fun glowBrush(): Brush = Brush.radialGradient(
        colors = listOf(cyan.copy(alpha = 0.08f), Color.Transparent),
        radius = 1600f
    )
}

/** Frosted "glass" console panel — clip, tint, and a hairline border. */
fun Modifier.glassPanel(corner: Dp = 20.dp): Modifier = this
    .clip(RoundedCornerShape(corner))
    .background(ControllerTheme.panelFill)
    .border(1.dp, ControllerTheme.panelBorder, RoundedCornerShape(corner))