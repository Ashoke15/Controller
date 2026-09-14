package com.diyproject.controller.joystick.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.diyproject.controller.control.compose.TopBarActions
import com.diyproject.controller.control.compose.TopBarState
import com.diyproject.controller.control.compose.TopControllerBar
import com.diyproject.controller.control.compose.controllerBackgroundBrush
import com.diyproject.controller.control.compose.controllerGlowBrush
import com.diyproject.controller.control.compose.glassPanel

/**
 * Callbacks supporting dual joysticks (Left and Right).
 */
data class JoystickCallbacks(
    val onConnectionToggle: () -> Unit = {},
    val onLeftVectorChange: (x: Float, y: Float) -> Unit = { _, _ -> },
    val onLeftVectorRepeat: (x: Float, y: Float) -> Unit = { _, _ -> },
    val onLeftReleased: () -> Unit = {},
    val onRightVectorChange: (x: Float, y: Float) -> Unit = { _, _ -> },
    val onRightVectorRepeat: (x: Float, y: Float) -> Unit = { _, _ -> },
    val onRightReleased: () -> Unit = {},
    val onSpeedChange: (Int) -> Unit = {},
    val onToggleHeadlight: (Boolean) -> Unit = {},
    val onToggleCabinLight: (Boolean) -> Unit = {},
    val onToggleSound: (Boolean) -> Unit = {},
    val onToggleWarning: (Boolean) -> Unit = {},
    val onSettingsClick: () -> Unit = {},
    val onStopClick: () -> Unit = {}
)

@Composable
fun JoystickScreen(
    isConnected: Boolean,
    callbacks: JoystickCallbacks = JoystickCallbacks(),
    modifier: Modifier = Modifier
) {
    var headlightOn by remember { mutableStateOf(false) }
    var cabinLightOn by remember { mutableStateOf(false) }
    var soundOn by remember { mutableStateOf(false) }
    var warningOn by remember { mutableStateOf(false) }
    var speedPercent by remember { mutableIntStateOf(50) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(controllerBackgroundBrush())
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(controllerGlowBrush())
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
        ) {
            TopControllerBar(
                state = TopBarState(
                    isConnected = isConnected,
                    headlightOn = headlightOn,
                    cabinLightOn = cabinLightOn,
                    soundOn = soundOn,
                    warningOn = warningOn,
                    speedPercent = speedPercent
                ),
                actions = TopBarActions(
                    onConnectionClick = callbacks.onConnectionToggle,
                    onToggleHeadlight = {
                        headlightOn = !headlightOn
                        callbacks.onToggleHeadlight(headlightOn)
                    },
                    onToggleCabinLight = {
                        cabinLightOn = !cabinLightOn
                        callbacks.onToggleCabinLight(cabinLightOn)
                    },
                    onToggleSound = {
                        soundOn = !soundOn
                        callbacks.onToggleSound(soundOn)
                    },
                    onToggleWarning = {
                        warningOn = !warningOn
                        callbacks.onToggleWarning(warningOn)
                    },
                    onSpeedChange = { fraction ->
                        speedPercent = (fraction * 100).toInt().coerceIn(0, 100)
                        callbacks.onSpeedChange(speedPercent)
                    },
                    onSettingsClick = callbacks.onSettingsClick,
                    onStopClick = callbacks.onStopClick
                )
            )

            // Dual Joystick Layout for Landscape Mode
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Joystick
                JoystickPad(
                    actions = JoystickActions(
                        onVectorChange = callbacks.onLeftVectorChange,
                        onVectorRepeat = callbacks.onLeftVectorRepeat,
                        onReleased = callbacks.onLeftReleased
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.38f)
                        .aspectRatio(1f)
                        .glassPanel()
                        .padding(16.dp)
                )

                // Right Joystick
                JoystickPad(
                    actions = JoystickActions(
                        onVectorChange = callbacks.onRightVectorChange,
                        onVectorRepeat = callbacks.onRightVectorRepeat,
                        onReleased = callbacks.onRightReleased
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.62f)
                        .aspectRatio(1f)
                        .glassPanel()
                        .padding(16.dp)
                )
            }
        }
    }
}