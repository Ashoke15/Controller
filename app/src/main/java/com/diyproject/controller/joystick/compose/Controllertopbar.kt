package com.diyproject.controller.joystick.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class ControllerTopBarState(
    val isConnected: Boolean,
    val headlightOn: Boolean,
    val cabinLightOn: Boolean,
    val hornOn: Boolean,
    val warningOn: Boolean,
    val throttleLimitPercent: Int,
    val motionControlEnabled: Boolean = false
)

data class ControllerTopBarActions(
    val onConnectionClick: () -> Unit = {},
    val onToggleHeadlight: () -> Unit = {},
    val onToggleCabinLight: () -> Unit = {},
    val onToggleHorn: () -> Unit = {},
    val onToggleWarning: () -> Unit = {},
    val onToggleMotionControl: () -> Unit = {},
    val onThrottleLimitChange: (Int) -> Unit = {},
    val onSettingsClick: () -> Unit = {},
    val onStopClick: () -> Unit = {}
)

/**
 * Self-contained replacement for the old `TopControllerBar` from
 * `control.compose` — connection status, quick toggles, a throttle
 * limiter, settings and a hard stop, built with nothing outside this
 * package (plain text/shape chips rather than an icon library, so
 * there's no icon-pack dependency to line up either).
 *
 * Kept deliberately compact (smaller chips, tighter padding) and now
 * folds the motion-steering toggle in as its own chip (MO) instead of
 * a separate row on the screen — this is chrome around the joysticks,
 * not the main event, so it shouldn't compete with them for height.
 */
@Composable
fun ControllerTopBar(
    state: ControllerTopBarState,
    actions: ControllerTopBarActions,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .glassPanel(corner = 16.dp)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ConnectionPill(isConnected = state.isConnected, onClick = actions.onConnectionClick)

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ToggleChip("HL", state.headlightOn, actions.onToggleHeadlight, ControllerTheme.amber)
                ToggleChip("CL", state.cabinLightOn, actions.onToggleCabinLight, ControllerTheme.amber)
                ToggleChip("HN", state.hornOn, actions.onToggleHorn, ControllerTheme.cyan)
                ToggleChip("WN", state.warningOn, actions.onToggleWarning, ControllerTheme.red)
                ToggleChip("MO", state.motionControlEnabled, actions.onToggleMotionControl, ControllerTheme.cyan)
                TextChip("SET", actions.onSettingsClick)
                StopButton(actions.onStopClick)
            }
        }

        Spacer(Modifier.height(4.dp))

        ThrottleLimitRow(percent = state.throttleLimitPercent, onChange = actions.onThrottleLimitChange)
    }
}

@Composable
private fun ConnectionPill(isConnected: Boolean, onClick: () -> Unit) {
    val bg = if (isConnected) ControllerTheme.green.copy(alpha = 0.16f) else ControllerTheme.panelFillStrong
    val fg = if (isConnected) ControllerTheme.green else ControllerTheme.textMuted
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier.size(7.dp).clip(CircleShape).background(fg)
        )
        Text(
            text = if (isConnected) "CONNECTED" else "CONNECT",
            color = fg,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
private fun ToggleChip(label: String, active: Boolean, onClick: () -> Unit, activeColor: Color) {
    val bg = if (active) activeColor.copy(alpha = 0.22f) else ControllerTheme.panelFill
    val fg = if (active) activeColor else ControllerTheme.textMuted
    val border = if (active) activeColor.copy(alpha = 0.5f) else ControllerTheme.panelBorder
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(bg)
            .border(1.dp, border, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, color = fg, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun TextChip(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(30.dp)
            .clip(RoundedCornerShape(50))
            .background(ControllerTheme.panelFill)
            .border(1.dp, ControllerTheme.panelBorder, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, color = ControllerTheme.textMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StopButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(30.dp)
            .clip(RoundedCornerShape(50))
            .background(ControllerTheme.red.copy(alpha = 0.9f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text("STOP", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
    }
}

@Composable
private fun ThrottleLimitRow(percent: Int, onChange: (Int) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "THROTTLE LIMIT",
            color = ControllerTheme.textFaint,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(100.dp)
        )
        Slider(
            value = percent.toFloat(),
            onValueChange = { onChange(it.toInt().coerceIn(20, 100)) },
            valueRange = 20f..100f,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = ControllerTheme.cyan,
                activeTrackColor = ControllerTheme.cyan,
                inactiveTrackColor = ControllerTheme.panelBorder
            )
        )
        Text(
            text = "$percent%",
            color = ControllerTheme.textPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(36.dp)
        )
    }
}