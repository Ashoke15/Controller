package com.diyproject.controller.home.compose

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.layout.fillMaxSize

/**
 * Shared "tappable surface" behavior: a light press-down scale plus a
 * clickable with no default ripple (every card here draws its own state).
 * Previously each card/button hand-rolled its own interaction source +
 * animateFloatAsState + clickable block; centralizing it here means one
 * spring curve to tune and no risk of the four copies drifting apart.
 */
@Composable
private fun Modifier.pressable(
    onClick: () -> Unit,
    enabled: Boolean = true,
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "pressScale",
    )
    return this
        .scale(scale)
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = onClick,
        )
}

/** Subtle dark-theme-appropriate depth: a soft shadow plus the card's own border. */
private fun Modifier.elevatedCard(
    shape: Shape,
    elevation: Dp,
    tint: Color = Color.Black,
) = this.shadow(
    elevation = elevation,
    shape = shape,
    ambientColor = tint.copy(alpha = 0.35f),
    spotColor = tint.copy(alpha = 0.45f),
)

/* ------------------------------------------------------------------ */
/*  Header                                                              */
/* ------------------------------------------------------------------ */

@Composable
fun HomeHeader(
    title: String,
    tagline: String,
    brandIcon: Painter,
    connected: Boolean,
    connectionLabel: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppDimens.Space16, vertical = AppDimens.Space12),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(AppDimens.IconBadgeSize)
                    .clip(CircleShape)
                    .background(AppColors.AccentCyanDim)
                    .border(AppDimens.BorderWidth, AppColors.AccentCyan.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = brandIcon,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp) // Keeps the logo comfortably inside the circular border
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = AppDimens.Space12),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = AppColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = tagline,
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextSecondary,
                    maxLines = 1,
                )
            }

            ConnectionStatusChip(connected = connected, label = connectionLabel)
        }

        // Thin separator so the header reads as its own band instead of
        // bleeding straight into the first section.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(AppColors.CardBorder),
        )
    }
}

/* ------------------------------------------------------------------ */
/*  Connection status                                                   */
/* ------------------------------------------------------------------ */

@Composable
fun ConnectionStatusChip(
    connected: Boolean,
    label: String,
    modifier: Modifier = Modifier,
) {
    // Color now animates instead of snapping, so when `connected` flips in
    // real time (see MainActivity's Bluetooth receiver) the chip visibly
    // crossfades red<->green rather than jump-cutting — the status change
    // reads as "live" instead of "the UI glitched."
    val statusColor by animateColorAsState(
        targetValue = if (connected) AppColors.Connected else AppColors.Critical,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "statusColor",
    )

    val infiniteTransition = rememberInfiniteTransition(label = "statusPulse")
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        // Slow, calm breathing while connected; a quicker blink while
        // disconnected so the chip still visibly "does something" instead
        // of sitting frozen — the previous version had no range to animate
        // in the disconnected case, so the dot just sat static at 1f.
        targetValue = if (connected) 0.35f else 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (connected) 1400 else 700,
                easing = FastOutSlowInEasing,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "dotAlpha",
    )

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(AppDimens.RadiusPill))
            .background(statusColor.copy(alpha = 0.12f))
            .border(
                width = AppDimens.BorderWidth,
                color = statusColor.copy(alpha = 0.5f),
                shape = RoundedCornerShape(AppDimens.RadiusPill),
            )
            .padding(horizontal = AppDimens.Space12, vertical = AppDimens.Space8)
            .semantics { contentDescription = label },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .alpha(dotAlpha)
                .clip(CircleShape)
                .background(statusColor),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = statusColor,
            modifier = Modifier.padding(start = 6.dp),
        )
    }
}

/* ------------------------------------------------------------------ */
/*  Section header                                                      */
/* ------------------------------------------------------------------ */

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    count: Int? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AppDimens.Space16),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(14.dp)
                .background(AppColors.AccentCyan, RoundedCornerShape(2.dp)),
        )
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.titleMedium,
            color = AppColors.TextPrimary,
            modifier = Modifier
                .weight(1f)
                .padding(start = AppDimens.Space8),
        )
        if (count != null) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = AppColors.TextTertiary,
            )
        }
    }
}

/* ------------------------------------------------------------------ */
/*  Feature card (Control / Developer sections)                         */
/* ------------------------------------------------------------------ */

@Composable
fun FeatureCard(
    icon: Painter,
    title: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: String? = null,
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(AppDimens.RadiusLarge)

    Column(
        modifier = modifier
            .widthIn(min = AppDimens.FeatureCardWidth)
            .elevatedCard(shape = shape, elevation = AppDimens.ElevationRest)
            .clip(shape)
            .background(AppColors.SurfaceElevated.copy(alpha = if (enabled) 1f else 0.5f))
            .border(AppDimens.BorderWidth, AppColors.CardBorder, shape)
            .pressable(onClick = onClick, enabled = enabled)
            .semantics { contentDescription = "$title. $description" }
            .padding(AppDimens.Space16),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(
                modifier = Modifier
                    .size(AppDimens.IconBadgeSize)
                    .clip(RoundedCornerShape(AppDimens.RadiusSmall))
                    .background(AppColors.AccentCyanDim),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = icon,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().padding(1.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            if (badge != null) {
                Text(
                    text = badge,
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.TextTertiary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                )
            } else {
                Text(
                    text = "\u2192",
                    style = MaterialTheme.typography.titleSmall,
                    color = AppColors.TextTertiary,
                )
            }
        }

        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = AppColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = AppDimens.Space12),
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.TextSecondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

/* ------------------------------------------------------------------ */
/*  Vehicle template card                                               */
/* ------------------------------------------------------------------ */

@Composable
fun VehicleTemplateCard(
    icon: Painter,
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(AppDimens.RadiusLarge)
    val borderColor by animateColorAsState(
        targetValue = if (selected) AppColors.AccentCyan else AppColors.CardBorder,
        animationSpec = tween(200),
        label = "templateBorder",
    )

    Column(
        modifier = modifier
            .width(AppDimens.TemplateCardWidth)
            .elevatedCard(
                shape = shape,
                elevation = if (selected) AppDimens.ElevationRaised else AppDimens.ElevationRest,
                tint = if (selected) AppColors.AccentCyan else Color.Black,
            )
            .clip(shape)
            .background(AppColors.SurfaceElevated)
            .border(
                width = if (selected) AppDimens.BorderWidthSelected else AppDimens.BorderWidth,
                color = borderColor,
                shape = shape,
            )
            .pressable(onClick = onClick)
            .semantics {
                contentDescription = if (selected) "$title, selected. $description" else "$title. $description"
            },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(AppDimens.TemplateThumbHeight)
                .background(AppColors.AccentCyanDim),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = icon,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(1.dp)
            )
            val checkmarkScale by animateFloatAsState(
                targetValue = if (selected) 1f else 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium,
                ),
                label = "checkmarkScale",
            )
            if (checkmarkScale > 0.01f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .scale(checkmarkScale)
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(AppColors.AccentCyan),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "\u2713",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF00141A),
                    )
                }
            }
        }

        Column(modifier = Modifier.padding(AppDimens.Space12)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = AppColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

/* ------------------------------------------------------------------ */
/*  Utility action button (Share / About)                               */
/* ------------------------------------------------------------------ */

@Composable
fun UtilityActionButton(
    icon: Painter,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(AppDimens.RadiusMedium)

    Row(
        modifier = modifier
            .clip(shape)
            .background(AppColors.Surface)
            .border(AppDimens.BorderWidth, AppColors.CardBorder, shape)
            .pressable(onClick = onClick)
            .heightIn(min = AppDimens.TouchTargetMin)
            .padding(vertical = AppDimens.Space12)
            .semantics { contentDescription = label },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = icon,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = AppColors.TextPrimary,
            modifier = Modifier.padding(start = AppDimens.Space8),
        )
    }
}

/* ------------------------------------------------------------------ */
/*  Footer                                                               */
/* ------------------------------------------------------------------ */

@Composable
fun AppFooterText(
    versionLabel: String,
    madeBy: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppDimens.Space16)
                .height(1.dp)
                .background(AppColors.CardBorder),
        )
        Spacer(modifier = Modifier.height(AppDimens.Space12))
        Text(
            text = versionLabel,
            style = MaterialTheme.typography.labelSmall,
            color = AppColors.TextSecondary,
        )
        Text(
            text = madeBy,
            style = MaterialTheme.typography.labelSmall,
            color = AppColors.TextTertiary,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

/* ------------------------------------------------------------------ */
/*  About dialog                                                        */
/* ------------------------------------------------------------------ */

@Composable
fun AboutDialog(
    appName: String,
    versionLabel: String,
    message: String,
    madeBy: String,
    closeLabel: String,
    brandIcon: Painter,
    onDismiss: () -> Unit,
) {
    val cardShape = RoundedCornerShape(AppDimens.RadiusLarge)
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .elevatedCard(shape = cardShape, elevation = AppDimens.ElevationRaised)
                .clip(cardShape)
                .background(AppColors.Surface)
                .border(AppDimens.BorderWidth, AppColors.CardBorder, cardShape)
                .padding(AppDimens.Space24),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(AppColors.AccentCyanDim)
                    .border(AppDimens.BorderWidth, AppColors.AccentCyan.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = brandIcon,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp) // Slightly larger padding for the 56dp box
                )
            }

            Text(
                text = appName,
                style = MaterialTheme.typography.titleLarge,
                color = AppColors.TextPrimary,
                modifier = Modifier.padding(top = AppDimens.Space12),
            )
            Text(
                text = versionLabel,
                style = MaterialTheme.typography.labelMedium,
                color = AppColors.AccentCyan,
                modifier = Modifier.padding(top = 2.dp),
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextSecondary,
                modifier = Modifier.padding(top = AppDimens.Space16),
            )
            Text(
                text = madeBy,
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.TextTertiary,
                modifier = Modifier
                    .padding(top = AppDimens.Space16)
                    .alpha(0.8f),
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = AppDimens.Space20)
                    .clip(RoundedCornerShape(AppDimens.RadiusMedium))
                    .background(AppColors.AccentCyanDim)
                    .pressable(onClick = onDismiss)
                    .heightIn(min = AppDimens.TouchTargetMin)
                    .padding(vertical = AppDimens.Space12),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = closeLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = AppColors.AccentCyan,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}