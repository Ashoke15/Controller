package com.diyproject.controller.joystick.compose

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.diyproject.controller.control.compose.ControllerColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Callbacks for a stick. Values are normalized to the range -1f..1f on both
 * axes, with (0,0) at rest and (0,-1) meaning "full forward" / (1,0) meaning
 * "full right" — [JoystickPad] reports both axes, [AxisStick] reports only
 * its locked axis (the other component is always 0).
 *
 * [onVectorChange] fires on every drag update — use it for immediate,
 * event-driven sending. [onVectorRepeat] is optional and, if you supply it,
 * fires every [repeatIntervalMs] while the knob is held away from center —
 * a protocol keep-alive, mirroring the D-pad's hold-to-repeat behavior.
 * [onReleased] fires once, after the knob has already animated back to
 * (0,0), when the finger lifts or the gesture is cancelled.
 */
data class JoystickActions(
    val onDragStart: () -> Unit = {},
    val onVectorChange: (x: Float, y: Float) -> Unit = { _, _ -> },
    val onVectorRepeat: (x: Float, y: Float) -> Unit = { _, _ -> },
    val onReleased: () -> Unit = {},
    val repeatIntervalMs: Long = 120L
)

/**
 * A circular glass base with a draggable knob, free on both axes. Sizes
 * itself responsively to the space it's given via [BoxWithConstraints].
 *
 * Kept around as a general-purpose free-roam pad (e.g. for other screens);
 * the main driving screen uses [AxisStick] instead — see [JoystickScreen].
 */
@Composable
fun JoystickPad(
    actions: JoystickActions,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        val baseDiameter = min(this.maxWidth, this.maxHeight)
        val knobDiameter = baseDiameter * 0.42f
        val density = LocalDensity.current
        val maxOffsetPx = with(density) { ((baseDiameter - knobDiameter) / 2f).toPx() }
        val haptics = LocalHapticFeedback.current

        val knobOffset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
        val scope = rememberCoroutineScope()
        var repeatJob: Job? by remember { mutableStateOf<Job?>(null) }

        fun vectorFor(offset: Offset): Pair<Float, Float> {
            if (maxOffsetPx <= 0f) return 0f to 0f
            return (offset.x / maxOffsetPx) to (offset.y / maxOffsetPx)
        }

        // Base pad: frosted circle with a thin glowing ring, matching the
        // rest of the controller's glass language.
        Box(
            modifier = Modifier
                .size(baseDiameter)
                .clip(CircleShape)
                .background(ControllerColors.glassFill)
                .border(1.dp, ControllerColors.glassBorder, CircleShape)
        )

        // Draggable knob.
        Box(
            modifier = Modifier
                .size(knobDiameter)
                .offset {
                    IntOffset(
                        knobOffset.value.x.roundToInt(),
                        knobOffset.value.y.roundToInt()
                    )
                }
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            ControllerColors.glowCyan.copy(alpha = 0.35f),
                            Color.White.copy(alpha = 0.10f)
                        )
                    )
                )
                .border(1.5.dp, ControllerColors.glowCyan.copy(alpha = 0.6f), CircleShape)
                .semantics { contentDescription = "Joystick knob" }
                .then(
                    if (enabled) {
                        Modifier.pointerInput(actions, maxOffsetPx) {
                            detectDragGestures(
                                onDragStart = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    actions.onDragStart()
                                    repeatJob?.cancel()
                                    repeatJob = startRepeating(scope, actions.repeatIntervalMs) {
                                        val (x, y) = vectorFor(knobOffset.value)
                                        actions.onVectorRepeat(x, y)
                                    }
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val clamped = clampToRadius(knobOffset.value + dragAmount, maxOffsetPx)
                                    scope.launch { knobOffset.snapTo(clamped) }
                                    val (x, y) = vectorFor(clamped)
                                    actions.onVectorChange(x, y)
                                },
                                onDragEnd = {
                                    repeatJob?.cancel()
                                    repeatJob = null
                                    scope.launch {
                                        knobOffset.animateTo(
                                            Offset.Zero,
                                            spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessMedium
                                            )
                                        )
                                        actions.onVectorChange(0f, 0f)
                                        actions.onReleased()
                                    }
                                },
                                onDragCancel = {
                                    repeatJob?.cancel()
                                    repeatJob = null
                                    scope.launch {
                                        knobOffset.animateTo(Offset.Zero)
                                        actions.onVectorChange(0f, 0f)
                                        actions.onReleased()
                                    }
                                }
                            )
                        }
                    } else Modifier
                )
        )

        LaunchedEffect(enabled) {
            if (!enabled) {
                repeatJob?.cancel()
                repeatJob = null
                knobOffset.snapTo(Offset.Zero)
            }
        }
    }
}

/** Which axis an [AxisStick] is locked to. */
enum class StickAxis { VERTICAL, HORIZONTAL }

/**
 * A single-axis gimbal — moves along one axis only and springs back to
 * center on release, like the throttle or steering stick on a real RC
 * transmitter (Mode 2 layout: throttle = vertical, steering = horizontal).
 * Reports through the same [JoystickActions] as [JoystickPad]; the locked
 * axis's component of (x, y) is always 0.
 */
@Composable
fun AxisStick(
    axis: StickAxis,
    actions: JoystickActions,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        val trackLength = if (axis == StickAxis.VERTICAL) this.maxHeight else this.maxWidth
        val trackThickness = if (axis == StickAxis.VERTICAL) this.maxWidth else this.maxHeight
        val knobDiameter = trackThickness * 0.9f
        val density = LocalDensity.current
        val maxOffsetPx = with(density) { ((trackLength - knobDiameter) / 2f).toPx() }
        val haptics = LocalHapticFeedback.current

        val knobOffset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
        val scope = rememberCoroutineScope()
        var repeatJob: Job? by remember { mutableStateOf<Job?>(null) }

        fun vectorFor(offset: Offset): Pair<Float, Float> {
            if (maxOffsetPx <= 0f) return 0f to 0f
            return (offset.x / maxOffsetPx) to (offset.y / maxOffsetPx)
        }

        fun clampToAxis(target: Offset): Offset = when (axis) {
            StickAxis.VERTICAL -> Offset(0f, target.y.coerceIn(-maxOffsetPx, maxOffsetPx))
            StickAxis.HORIZONTAL -> Offset(target.x.coerceIn(-maxOffsetPx, maxOffsetPx), 0f)
        }

        // Track: pill-shaped, oriented along the locked axis.
        Box(
            modifier = Modifier
                .size(width = trackThickness, height = trackLength)
                .clip(RoundedCornerShape(percent = 50))
                .background(ControllerColors.glassFill)
                .border(1.dp, ControllerColors.glassBorder, RoundedCornerShape(percent = 50))
        )

        // Knob, constrained to the track's axis.
        Box(
            modifier = Modifier
                .size(knobDiameter)
                .offset {
                    IntOffset(
                        knobOffset.value.x.roundToInt(),
                        knobOffset.value.y.roundToInt()
                    )
                }
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            ControllerColors.glowCyan.copy(alpha = 0.35f),
                            Color.White.copy(alpha = 0.10f)
                        )
                    )
                )
                .border(1.5.dp, ControllerColors.glowCyan.copy(alpha = 0.6f), CircleShape)
                .semantics {
                    contentDescription =
                        if (axis == StickAxis.VERTICAL) "Throttle stick" else "Steering stick"
                }
                .then(
                    if (enabled) {
                        Modifier.pointerInput(actions, maxOffsetPx, axis) {
                            detectDragGestures(
                                onDragStart = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    actions.onDragStart()
                                    repeatJob?.cancel()
                                    repeatJob = startRepeating(scope, actions.repeatIntervalMs) {
                                        val (x, y) = vectorFor(knobOffset.value)
                                        actions.onVectorRepeat(x, y)
                                    }
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val clamped = clampToAxis(knobOffset.value + dragAmount)
                                    scope.launch { knobOffset.snapTo(clamped) }
                                    val (x, y) = vectorFor(clamped)
                                    actions.onVectorChange(x, y)
                                },
                                onDragEnd = {
                                    repeatJob?.cancel()
                                    repeatJob = null
                                    scope.launch {
                                        knobOffset.animateTo(
                                            Offset.Zero,
                                            spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessMedium
                                            )
                                        )
                                        actions.onVectorChange(0f, 0f)
                                        actions.onReleased()
                                    }
                                },
                                onDragCancel = {
                                    repeatJob?.cancel()
                                    repeatJob = null
                                    scope.launch {
                                        knobOffset.animateTo(Offset.Zero)
                                        actions.onVectorChange(0f, 0f)
                                        actions.onReleased()
                                    }
                                }
                            )
                        }
                    } else Modifier
                )
        )

        LaunchedEffect(enabled) {
            if (!enabled) {
                repeatJob?.cancel()
                repeatJob = null
                knobOffset.snapTo(Offset.Zero)
            }
        }
    }
}

private fun clampToRadius(offset: Offset, maxRadiusPx: Float): Offset {
    if (maxRadiusPx <= 0f) return Offset.Zero
    val distance = sqrt(offset.x.pow(2) + offset.y.pow(2))
    if (distance <= maxRadiusPx) return offset
    val scaleFactor = maxRadiusPx / distance
    return Offset(offset.x * scaleFactor, offset.y * scaleFactor)
}

private fun startRepeating(
    scope: CoroutineScope,
    intervalMs: Long,
    action: () -> Unit
): Job = scope.launch {
    while (isActive) {
        delay(intervalMs)
        action()
    }
}