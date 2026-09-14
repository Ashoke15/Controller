package com.diyproject.controller.control.compose

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object ControllerColors {
    val background = Color(0xFF0A0E14)
    val glowCyan = Color(0xFF00E5FF)
    val glassFill = Color.White.copy(alpha = 0.04f)
    val glassBorder = Color.Cyan.copy(alpha = 0.25f)
    val textPrimary = Color.White
    val textSecondary = Color(0xFF8B949E)
    val accentRed = Color(0xFFFF3B57)
    val accentGreen = Color(0xFF00FF88)
}

object ControllerShapes {
    val glassButton = RoundedCornerShape(24.dp)
    val pill = RoundedCornerShape(50.dp)
    val panel = RoundedCornerShape(28.dp)
}

object ControllerDimens {
    val topBarHeight = 56.dp
    val iconButtonSize = 44.dp
    val stopButtonHeight = 48.dp
    val panelPadding = 16.dp
    val controlGap = 16.dp
}