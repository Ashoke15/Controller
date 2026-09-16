package com.diyproject.controller.control.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.runtime.getValue
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

// ---------- Direction state ----------

enum class ActiveDirection { NONE, FORWARD, BACK, LEFT, RIGHT,
    FORWARD_LEFT, FORWARD_RIGHT, BACK_LEFT, BACK_RIGHT }

// ---------- Data classes (unchanged) ----------

data class ControllerState(
    val isConnected: Boolean,
    val headlightOn: Boolean,
    val backlightOn: Boolean,
    val hazardOn: Boolean,
    val speed: Int
)

data class ControllerActions(
    val onConnectClick: () -> Unit,
    val onForwardPress: () -> Unit,
    val onForwardRelease: () -> Unit,
    val onBackPress: () -> Unit,
    val onBackRelease: () -> Unit,
    val onLeftPress: () -> Unit,
    val onLeftRelease: () -> Unit,
    val onRightPress: () -> Unit,
    val onRightRelease: () -> Unit,
    val onHeadlightToggle: () -> Unit,
    val onBacklightToggle: () -> Unit,
    val onHornPress: () -> Unit,
    val onHornRelease: () -> Unit,
    val onHazardToggle: () -> Unit,
    val onSpeedChange: (Float) -> Unit,
    val onSpeedCommit: () -> Unit,
    val onSettingsClick: () -> Unit,
    val onStopAllClick: () -> Unit,
    // diagonal commands
    val onForwardLeftPress: () -> Unit,
    val onForwardRightPress: () -> Unit,
    val onBackLeftPress: () -> Unit,
    val onBackRightPress: () -> Unit,
    val onDiagonalRelease: () -> Unit
)

// ---------- Root screen ----------

@Composable
fun ControllerScreen(
    state: ControllerState,
    actions: ControllerActions
) {
    var activeDirection by remember { mutableStateOf(ActiveDirection.NONE) }

    Surface(color = ControllerColors.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            ControllerTopBar(
                isConnected = state.isConnected,
                onConnectClick = actions.onConnectClick,
                headlightOn = state.headlightOn,
                onHeadlightToggle = actions.onHeadlightToggle,
                backlightOn = state.backlightOn,
                onBacklightToggle = actions.onBacklightToggle,
                hazardOn = state.hazardOn,
                onHazardToggle = actions.onHazardToggle,
                onHornPress = actions.onHornPress,
                onHornRelease = actions.onHornRelease,
                speed = state.speed,
                onSpeedChange = actions.onSpeedChange,
                onSpeedCommit = actions.onSpeedCommit,
                onSettingsClick = actions.onSettingsClick,
                onStopAllClick = actions.onStopAllClick
            )

            MultiTouchControlPanel(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 32.dp),
                activeDirection = activeDirection,
                onDirectionChanged = { activeDirection = it },
                actions = actions
            )
        }
    }
}

// ---------- Multi-touch panel ----------
// Tracks which logical buttons are held down via pointer IDs,
// then resolves the correct command (including diagonals).

@Composable
private fun MultiTouchControlPanel(
    modifier: Modifier = Modifier,
    activeDirection: ActiveDirection,
    onDirectionChanged: (ActiveDirection) -> Unit,
    actions: ControllerActions
) {
    // Track which directions are currently held by pointer ID
    val heldDirections = remember { mutableStateMapOf<PointerId, String>() }

    // Resend loop: keeps re-affirming the held command every 200ms so a single
    // dropped serial packet, or a firmware inactivity watchdog, can't stop the
    // car while the finger is still down.
    val scope = rememberCoroutineScope()
    var heartbeatJob by remember { mutableStateOf<Job?>(null) }

    fun commandForDirection(dir: ActiveDirection): (() -> Unit)? = when (dir) {
        ActiveDirection.FORWARD_LEFT  -> actions.onForwardLeftPress
        ActiveDirection.FORWARD_RIGHT -> actions.onForwardRightPress
        ActiveDirection.BACK_LEFT     -> actions.onBackLeftPress
        ActiveDirection.BACK_RIGHT    -> actions.onBackRightPress
        ActiveDirection.FORWARD       -> actions.onForwardPress
        ActiveDirection.BACK          -> actions.onBackPress
        ActiveDirection.LEFT          -> actions.onLeftPress
        ActiveDirection.RIGHT         -> actions.onRightPress
        ActiveDirection.NONE          -> null
    }

    // Resolve combined direction and (re)start/stop the heartbeat
    fun resolve() {
        val held = heldDirections.values.toSet()
        val dir = when {
            held.contains("UP") && held.contains("LEFT")   -> ActiveDirection.FORWARD_LEFT
            held.contains("UP") && held.contains("RIGHT")  -> ActiveDirection.FORWARD_RIGHT
            held.contains("DOWN") && held.contains("LEFT") -> ActiveDirection.BACK_LEFT
            held.contains("DOWN") && held.contains("RIGHT")-> ActiveDirection.BACK_RIGHT
            held.contains("UP")    -> ActiveDirection.FORWARD
            held.contains("DOWN")  -> ActiveDirection.BACK
            held.contains("LEFT")  -> ActiveDirection.LEFT
            held.contains("RIGHT") -> ActiveDirection.RIGHT
            else -> ActiveDirection.NONE
        }

        heartbeatJob?.cancel()
        onDirectionChanged(dir)

        val send = commandForDirection(dir)
        if (send == null) {
            actions.onDiagonalRelease()
            return
        }

        send() // fire immediately, matching the old on-press behavior
        heartbeatJob = scope.launch {
            while (isActive) {
                delay(200)
                send()
            }
        }
    }

    fun buttonPointerInput(label: String) = Modifier.pointerInput(label) {
        awaitEachGesture {
            val down = awaitPointerEvent().changes.firstOrNull() ?: return@awaitEachGesture
            down.consume()
            heldDirections[down.id] = label
            resolve()
            // Wait until this pointer lifts
            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                change.consume()
                if (!change.pressed) break
            }
            heldDirections.remove(down.id)
            resolve()
        }
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ---- LEFT SIDE: UP / DOWN ----
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            DirectionButton(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .then(buttonPointerInput("UP")),
                icon = Icons.Filled.KeyboardArrowUp,
                isActive = activeDirection == ActiveDirection.FORWARD
                        || activeDirection == ActiveDirection.FORWARD_LEFT
                        || activeDirection == ActiveDirection.FORWARD_RIGHT,
                contentDescription = "Forward"
            )

            DirectionButton(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .then(buttonPointerInput("DOWN")),
                icon = Icons.Filled.KeyboardArrowDown,
                isActive = activeDirection == ActiveDirection.BACK
                        || activeDirection == ActiveDirection.BACK_LEFT
                        || activeDirection == ActiveDirection.BACK_RIGHT,
                contentDescription = "Back"
            )
        }

        // ---- CENTER: direction indicator ----
        Box(
            modifier = Modifier
                .width(72.dp)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            DirectionFlashIndicator(activeDirection = activeDirection)
        }

        // ---- RIGHT SIDE: LEFT / RIGHT ----
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DirectionButton(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .then(buttonPointerInput("LEFT")),
                icon = Icons.Filled.KeyboardArrowLeft,
                isActive = activeDirection == ActiveDirection.LEFT
                        || activeDirection == ActiveDirection.FORWARD_LEFT
                        || activeDirection == ActiveDirection.BACK_LEFT,
                contentDescription = "Left"
            )

            DirectionButton(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .then(buttonPointerInput("RIGHT")),
                icon = Icons.Filled.KeyboardArrowRight,
                isActive = activeDirection == ActiveDirection.RIGHT
                        || activeDirection == ActiveDirection.FORWARD_RIGHT
                        || activeDirection == ActiveDirection.BACK_RIGHT,
                contentDescription = "Right"
            )
        }
    }
}

// ---------- Single direction button ----------

@Composable
private fun DirectionButton(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    isActive: Boolean,
    contentDescription: String
) {
    // Declare BEFORE Icon — not inside its parameters
    val blinkAlpha by animateFloatAsState(
        targetValue = if (isActive) 1f else 0.7f,
        animationSpec = if (isActive) infiniteRepeatable(
            animation = tween(300),
            repeatMode = RepeatMode.Reverse
        ) else tween(150),
        label = "blink"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(
                if (isActive) Color.White.copy(alpha = 0.10f)
                else Color.White.copy(alpha = 0.04f)
            )
            .border(
                width = if (isActive) 1.5.dp else 1.dp,
                color = if (isActive) Color.Cyan.copy(alpha = 0.55f)
                else Color.Cyan.copy(alpha = 0.20f),
                shape = RoundedCornerShape(24.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isActive) ControllerColors.glowCyan.copy(alpha = blinkAlpha)
            else ControllerColors.textPrimary.copy(alpha = 0.7f),
            modifier = Modifier.size(52.dp)
        )
    }
}
// ---------- Center direction flash indicator ----------

@Composable
private fun DirectionFlashIndicator(activeDirection: ActiveDirection) {
    val icon: ImageVector? = when (activeDirection) {
        ActiveDirection.FORWARD       -> Icons.Filled.KeyboardArrowUp
        ActiveDirection.BACK          -> Icons.Filled.KeyboardArrowDown
        ActiveDirection.LEFT          -> Icons.AutoMirrored.Filled.KeyboardArrowLeft
        ActiveDirection.RIGHT         -> Icons.AutoMirrored.Filled.KeyboardArrowRight
        ActiveDirection.FORWARD_LEFT  -> Icons.Filled.NorthWest
        ActiveDirection.FORWARD_RIGHT -> Icons.Filled.NorthEast
        ActiveDirection.BACK_LEFT     -> Icons.Filled.SouthWest
        ActiveDirection.BACK_RIGHT    -> Icons.Filled.SouthEast
        ActiveDirection.NONE          -> null
    }

    AnimatedVisibility(
        visible = icon != null,
        enter = fadeIn(animationSpec = tween(10)),
        exit = fadeOut(animationSpec = tween(10))
    ) {
        icon?.let {
            Icon(
                imageVector = it,
                contentDescription = null,
                tint = ControllerColors.glowCyan,
                modifier = Modifier.size(50.dp)
            )
        }
    }
}