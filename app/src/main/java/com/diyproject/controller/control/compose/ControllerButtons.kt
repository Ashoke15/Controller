package com.diyproject.controller.control.compose

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * Reusable glass-morphism control button.
 * Supports press-and-hold (onPress/onRelease) for continuous movement.
 */
@Composable
fun GlassControlButton(
    modifier: Modifier = Modifier,
    contentDescription: String,
    onPress: () -> Unit,
    onRelease: () -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "scale"
    )
    val borderAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.5f else 0.25f,
        label = "borderAlpha"
    )
    val fillAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.08f else 0.04f,
        label = "fillAlpha"
    )

    Box(
        modifier = modifier
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clip(ControllerShapes.glassButton)
            .background(Color.White.copy(alpha = fillAlpha))
            .border(1.dp, Color.Cyan.copy(alpha = borderAlpha), ControllerShapes.glassButton)
            .pointerInput(Unit) {
                detectPressGesture(
                    onPress = onPress,
                    onRelease = onRelease
                )
            }
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
        content = content
    )
}

private suspend fun PointerInputScope.detectPressGesture(
    onPress: () -> Unit,
    onRelease: () -> Unit
) {
    awaitEachGesture {
        awaitFirstDown()
        onPress()
        waitForUpOrCancellation()
        onRelease()
    }
}

@Composable
fun DirectionArrow(icon: androidx.compose.ui.graphics.vector.ImageVector, size: androidx.compose.ui.unit.Dp) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = ControllerColors.textPrimary,
        modifier = Modifier.size(size)
    )
}

@Composable
fun GlassIconToggleButton(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isOn: Boolean,
    contentDescription: String,
    onToggle: () -> Unit
) {
    val alpha by animateFloatAsState(if (isOn) 1f else 0.5f, label = "toggleAlpha")
    Box(
        modifier = modifier
            .size(ControllerDimens.iconButtonSize)
            .clip(ControllerShapes.glassButton)
            .background(Color.White.copy(alpha = 0.04f))
            .border(1.dp, ControllerColors.glassBorder, ControllerShapes.glassButton)
            .pointerInput(Unit) {
                detectTapGestures { onToggle() }
            }
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = ControllerColors.textPrimary.copy(alpha = alpha),
            modifier = Modifier.size(20.dp)
        )
    }
}