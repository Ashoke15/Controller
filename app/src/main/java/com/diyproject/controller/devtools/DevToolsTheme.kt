package com.diyproject.controller.devtools

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Visual identity for developer-tool surfaces — Code Control now,
 * Terminal/Macros meant to share it later. Deliberately squarer and
 * plainer than joystick.compose's ControllerTheme (sharp corners,
 * monospace-first, hairline dividers) so this reads as instrumentation
 * for a developer, not a consumer gauge cluster. Self-contained for the
 * same reason ControllerTheme is: nothing here depends on `control.compose`.
 */
object DevToolsTheme {

    val backgroundDeep = Color(0xFF07090DL)
    val backgroundTop = Color(0xFF0C1014L)

    val panelFill = Color(0x12FFFFFFL)
    val panelFillStrong = Color(0x20FFFFFFL)
    val panelBorder = Color(0x22FFFFFFL)

    val txColor = Color(0xFFFFB454L)     // outgoing — amber
    val rxColor = Color(0xFF57E389L)     // incoming — terminal green
    val systemColor = Color(0xFF8A93A6L) // connection/lifecycle notices
    val errorColor = Color(0xFFFF5C5CL)
    val accent = Color(0xFF4CE1E8L)

    val textPrimary = Color(0xFFEAF0F7L)
    val textMuted = Color(0x8FEAF0F7L)
    val textFaint = Color(0x50EAF0F7L)

    val mono = FontFamily.Monospace

    fun backgroundBrush(): Brush = Brush.verticalGradient(listOf(backgroundTop, backgroundDeep))
}

/** Sharp-cornered console panel — less rounded than ControllerTheme's
 *  glassPanel, so it reads as "instrument" rather than "gauge". */
fun Modifier.consolePanel(corner: Dp = 10.dp): Modifier = this
    .clip(RoundedCornerShape(corner))
    .background(DevToolsTheme.panelFill)
    .border(1.dp, DevToolsTheme.panelBorder, RoundedCornerShape(corner))