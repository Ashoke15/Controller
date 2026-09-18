package com.diyproject.controller.joystick.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class JoystickCallbacks(
    val onConnectionToggle: () -> Unit = {},
    val onLeftVectorChange: (x: Float, y: Float) -> Unit = { _, _ -> },
    val onLeftReleased: () -> Unit = {},
    val onRightVectorChange: (x: Float, y: Float) -> Unit = { _, _ -> },
    val onRightReleased: () -> Unit = {},
    val onToggleMotionControl: () -> Unit = {},
    val onThrottleLimitChange: (Int) -> Unit = {},
    val onToggleHeadlight: (Boolean) -> Unit = {},
    val onToggleCabinLight: (Boolean) -> Unit = {},
    val onToggleHorn: (Boolean) -> Unit = {},
    val onToggleWarning: (Boolean) -> Unit = {},
    val onSettingsClick: () -> Unit = {},
    val onStopClick: () -> Unit = {}
)

@Composable
fun JoystickScreen(
    isConnected: Boolean,
    motionControlEnabled: Boolean = false,
    steeringChannel: Int = 0,
    throttleChannel: Int = 0,
    panChannel: Int = 90,
    tiltChannel: Int = 90,
    callbacks: JoystickCallbacks = JoystickCallbacks(),
    modifier: Modifier = Modifier
) {
    var headlightOn by remember { mutableStateOf(false) }
    var cabinLightOn by remember { mutableStateOf(false) }
    var hornOn by remember { mutableStateOf(false) }
    var warningOn by remember { mutableStateOf(false) }
    var throttleLimitPercent by remember { mutableIntStateOf(100) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ControllerTheme.backgroundBrush())
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ControllerTheme.glowBrush())
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ControllerTopBar(
                state = ControllerTopBarState(
                    isConnected = isConnected,
                    headlightOn = headlightOn,
                    cabinLightOn = cabinLightOn,
                    hornOn = hornOn,
                    warningOn = warningOn,
                    throttleLimitPercent = throttleLimitPercent,
                    motionControlEnabled = motionControlEnabled
                ),
                actions = ControllerTopBarActions(
                    onConnectionClick = callbacks.onConnectionToggle,
                    onToggleHeadlight = {
                        headlightOn = !headlightOn
                        callbacks.onToggleHeadlight(headlightOn)
                    },
                    onToggleCabinLight = {
                        cabinLightOn = !cabinLightOn
                        callbacks.onToggleCabinLight(cabinLightOn)
                    },
                    onToggleHorn = {
                        hornOn = !hornOn
                        callbacks.onToggleHorn(hornOn)
                    },
                    onToggleWarning = {
                        warningOn = !warningOn
                        callbacks.onToggleWarning(warningOn)
                    },
                    onToggleMotionControl = callbacks.onToggleMotionControl,
                    onThrottleLimitChange = {
                        throttleLimitPercent = it
                        callbacks.onThrottleLimitChange(it)
                    },
                    onSettingsClick = callbacks.onSettingsClick,
                    onStopClick = callbacks.onStopClick
                )
            )

            // Telemetry now lives in the gap between the two pads (STR/THR
            // against the left stick, PAN/TLT against the right) instead of
            // its own full-width strip — that strip's height goes straight
            // back into stickSize below, and the reserved width is just
            // telemetryWidth instead of the whole row.
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                val telemetryWidth = 96.dp
                val perStickWidth = (this.maxWidth - telemetryWidth) / 2f
                val stickSize = if (perStickWidth < this.maxHeight) perStickWidth else this.maxHeight

                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StickColumn(
                        title = "LEFT · STEER / THROTTLE",
                        accentColor = ControllerTheme.amber,
                        deadzoneRadiusFraction = StickInputMath.STEERING_DEADZONE,
                        actions = JoystickActions(
                            onVectorChange = callbacks.onLeftVectorChange,
                            onReleased = callbacks.onLeftReleased
                        ),
                        size = stickSize
                    )

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(horizontal = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TelemetryPair(
                            label1 = "STR", value1 = steeringChannel, range1 = -100..100,
                            label2 = "THR", value2 = throttleChannel, range2 = -100..100,
                            color = ControllerTheme.amber,
                            alignment = Alignment.Start
                        )
                        TelemetryPair(
                            label1 = "PAN", value1 = panChannel, range1 = 0..180,
                            label2 = "TLT", value2 = tiltChannel, range2 = 0..180,
                            color = ControllerTheme.cyan,
                            alignment = Alignment.End
                        )
                    }

                    StickColumn(
                        title = "RIGHT · PAN / TILT",
                        accentColor = ControllerTheme.cyan,
                        deadzoneRadiusFraction = StickInputMath.GIMBAL_DEADZONE,
                        actions = JoystickActions(
                            onVectorChange = callbacks.onRightVectorChange,
                            onReleased = callbacks.onRightReleased
                        ),
                        size = stickSize
                    )
                }
            }
        }
    }
}

@Composable
private fun StickColumn(
    title: String,
    accentColor: Color,
    deadzoneRadiusFraction: Float,
    actions: JoystickActions,
    size: Dp
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = title,
            color = ControllerTheme.textFaint,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.8.sp,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        JoystickPad(
            actions = actions,
            accentColor = accentColor,
            deadzoneRadiusFraction = deadzoneRadiusFraction,
            modifier = Modifier
                .size(size)
                .glassPanel()
                .padding(14.dp)
        )
    }
}

@Composable
private fun TelemetryPair(
    label1: String, value1: Int, range1: IntRange,
    label2: String, value2: Int, range2: IntRange,
    color: Color,
    alignment: Alignment.Horizontal
) {
    Column(
        horizontalAlignment = alignment,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ChannelReadout(label1, value1, range1, color, alignment)
        ChannelReadout(label2, value2, range2, color, alignment)
    }
}

@Composable
private fun ChannelReadout(
    label: String,
    value: Int,
    range: IntRange,
    color: Color,
    alignment: Alignment.Horizontal
) {
    val span = (range.last - range.first).toFloat()
    val fraction = ((value - range.first) / span).coerceIn(0f, 1f)

    Column(horizontalAlignment = alignment, modifier = Modifier.width(70.dp)) {
        Text(
            text = "$label $value",
            color = ControllerTheme.textMuted,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.3.sp
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .padding(top = 3.dp)
                .clip(RoundedCornerShape(50))
                .background(ControllerTheme.panelFillStrong)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceIn(0.02f, 1f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(50))
                    .background(color)
            )
        }
    }
}