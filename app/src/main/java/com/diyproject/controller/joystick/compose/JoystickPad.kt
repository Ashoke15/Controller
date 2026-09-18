package com.diyproject.controller.joystick.compose

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
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
 * [onVectorChange] fires on every drag update. There's no repeat-while-held
 * timer here on purpose — [RcTransmitter] is what actually keeps streaming
 * the last known values to the car at a fixed rate, mirroring how a real
 * transmitter's frame loop works, so the UI layer doesn't need its own
 * "keep sending" workaround too. [onReleased] fires once, after the knob
 * has already animated back to (0,0), when the finger lifts or the gesture
 * is cancelled.
 */
data class JoystickActions(
    val onDragStart: () -> Unit = {},
    val onVectorChange: (x: Float, y: Float) -> Unit = { _, _ -> },
    val onReleased: () -> Unit = {}
)

/**
 * A circular glass base with a draggable knob, free on both axes — a
 * FlySky-style gimbal: crosshair guides, cardinal tick marks, and a
 * visual deadzone ring so the player can see exactly where the stick's
 * shaped output actually starts moving. Sizes itself responsively to the
 * space it's given via [BoxWithConstraints].
 *
 * Both sticks on [JoystickScreen] use this — steering+throttle on the
 * left, pan+tilt on the right — since a real transmitter's channels are
 * all proportional analog on both axes, not snapped to directions.
 *
 * While dragging, the knob position is a plain [mutableStateOf] written
 * directly from the drag callback — no coroutine launch per touch event —
 * so it tracks the finger with no extra dispatch latency. [Animatable] is
 * only used for the one-time spring-back to center on release/cancel.
 */
@Composable
fun JoystickPad(
    actions: JoystickActions,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accentColor: Color = ControllerTheme.cyan,
    deadzoneRadiusFraction: Float = 0.08f
) {
    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        val baseDiameter = min(this.maxWidth, this.maxHeight)
        val knobDiameter = baseDiameter * 0.4f
        val density = LocalDensity.current
        val maxOffsetPx = with(density) { ((baseDiameter - knobDiameter) / 2f).toPx() }
        val haptics = LocalHapticFeedback.current
        val latestActions by rememberUpdatedState(actions)

        // Live drag position — direct writes, no animation, no coroutine.
        var knobOffset by remember { mutableStateOf(Offset.Zero) }
        // Used only to animate the knob back to center on release/cancel.
        val springBack = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
        val scope = rememberCoroutineScope()

        fun vectorFor(offset: Offset): Pair<Float, Float> {
            if (maxOffsetPx <= 0f) return 0f to 0f
            return (offset.x / maxOffsetPx) to (offset.y / maxOffsetPx)
        }

        fun recenter() {
            scope.launch {
                springBack.snapTo(knobOffset)
                springBack.animateTo(
                    Offset.Zero,
                    spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ) { knobOffset = value }
                latestActions.onVectorChange(0f, 0f)   // was actions.
                latestActions.onReleased()
            }
        }

        // Base pad: frosted circle with a thin glowing ring.
        Box(
            modifier = Modifier
                .size(baseDiameter)
                .clip(CircleShape)
                .background(ControllerTheme.panelFill)
                .border(1.dp, ControllerTheme.panelBorder, CircleShape)
        )

        // Instrument overlay: crosshair, deadzone ring, cardinal ticks.
        Canvas(modifier = Modifier.size(baseDiameter)) {
            val guide = accentColor.copy(alpha = 0.25f)
            val hairline = 1.2.dp.toPx()

            drawLine(guide, Offset(size.width / 2f, 6.dp.toPx()), Offset(size.width / 2f, size.height - 6.dp.toPx()), strokeWidth = hairline)
            drawLine(guide, Offset(6.dp.toPx(), size.height / 2f), Offset(size.width - 6.dp.toPx(), size.height / 2f), strokeWidth = hairline)

            if (maxOffsetPx > 0f) {
                drawCircle(
                    color = guide,
                    radius = maxOffsetPx * deadzoneRadiusFraction,
                    center = center,
                    style = Stroke(width = hairline)
                )
            }

            val tick = 6.dp.toPx()
            val edge = 3.dp.toPx()
            drawLine(guide, Offset(size.width / 2f, edge), Offset(size.width / 2f, edge + tick), strokeWidth = hairline * 1.4f)
            drawLine(guide, Offset(size.width / 2f, size.height - edge), Offset(size.width / 2f, size.height - edge - tick), strokeWidth = hairline * 1.4f)
            drawLine(guide, Offset(edge, size.height / 2f), Offset(edge + tick, size.height / 2f), strokeWidth = hairline * 1.4f)
            drawLine(guide, Offset(size.width - edge, size.height / 2f), Offset(size.width - edge - tick, size.height / 2f), strokeWidth = hairline * 1.4f)
        }

        // Draggable knob.
        Box(
            modifier = Modifier
                .size(knobDiameter)
                .offset {
                    IntOffset(
                        knobOffset.x.roundToInt(),
                        knobOffset.y.roundToInt()
                    )
                }
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.35f),
                            Color.White.copy(alpha = 0.10f)
                        )
                    )
                )
                .border(1.5.dp, accentColor.copy(alpha = 0.65f), CircleShape)
                .semantics { contentDescription = "Joystick knob" }
                .then(
                    if (enabled) {
                        Modifier.pointerInput(maxOffsetPx) {
                            detectDragGestures(
                                onDragStart = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    latestActions.onDragStart()
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val clamped = clampToRadius(knobOffset + dragAmount, maxOffsetPx)
                                    knobOffset = clamped
                                    val (x, y) = vectorFor(clamped)
                                    latestActions.onVectorChange(x, y)
                                },
                                onDragEnd = { recenter() },
                                onDragCancel = { recenter() }
                            )
                        }
                    } else Modifier
                )
        )

        LaunchedEffect(enabled) {
            if (!enabled) {
                knobOffset = Offset.Zero
            }
        }
    }
}

/** Which axis an [AxisStick] is locked to. */
enum class StickAxis { VERTICAL, HORIZONTAL }

/**
 * A single-axis gimbal — moves along one axis only and springs back to
 * center on release, like the throttle or steering stick on a real RC
 * transmitter (Mode 2 layout: throttle = vertical, steering =
 * horizontal). Reports through the same [JoystickActions] as
 * [JoystickPad]; the locked axis's component of (x, y) is always 0.
 *
 * Kept around as a general-purpose single-axis input (e.g. a standalone
 * throttle slider elsewhere) — [JoystickScreen] currently drives both
 * the left and right sticks with the free two-axis [JoystickPad]
 * instead, since both need proportional X and Y at once.
 */
@Composable
fun AxisStick(
    axis: StickAxis,
    actions: JoystickActions,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accentColor: Color = ControllerTheme.cyan
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

        var knobOffset by remember { mutableStateOf(Offset.Zero) }
        val springBack = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
        val scope = rememberCoroutineScope()

        fun vectorFor(offset: Offset): Pair<Float, Float> {
            if (maxOffsetPx <= 0f) return 0f to 0f
            return (offset.x / maxOffsetPx) to (offset.y / maxOffsetPx)
        }

        fun clampToAxis(target: Offset): Offset = when (axis) {
            StickAxis.VERTICAL -> Offset(0f, target.y.coerceIn(-maxOffsetPx, maxOffsetPx))
            StickAxis.HORIZONTAL -> Offset(target.x.coerceIn(-maxOffsetPx, maxOffsetPx), 0f)
        }

        fun recenter() {
            scope.launch {
                springBack.snapTo(knobOffset)
                springBack.animateTo(
                    Offset.Zero,
                    spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ) { knobOffset = value }
                actions.onVectorChange(0f, 0f)
                actions.onReleased()
            }
        }

        // Track: pill-shaped, oriented along the locked axis.
        Box(
            modifier = Modifier
                .size(width = trackThickness, height = trackLength)
                .clip(RoundedCornerShape(percent = 50))
                .background(ControllerTheme.panelFill)
                .border(1.dp, ControllerTheme.panelBorder, RoundedCornerShape(percent = 50))
        )

        // Knob, constrained to the track's axis.
        Box(
            modifier = Modifier
                .size(knobDiameter)
                .offset {
                    IntOffset(
                        knobOffset.x.roundToInt(),
                        knobOffset.y.roundToInt()
                    )
                }
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.35f),
                            Color.White.copy(alpha = 0.10f)
                        )
                    )
                )
                .border(1.5.dp, accentColor.copy(alpha = 0.65f), CircleShape)
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
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val clamped = clampToAxis(knobOffset + dragAmount)
                                    knobOffset = clamped
                                    val (x, y) = vectorFor(clamped)
                                    actions.onVectorChange(x, y)
                                },
                                onDragEnd = { recenter() },
                                onDragCancel = { recenter() }
                            )
                        }
                    } else Modifier
                )
        )

        LaunchedEffect(enabled) {
            if (!enabled) {
                knobOffset = Offset.Zero
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