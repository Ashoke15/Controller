package com.diyproject.controller.home.compose

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Design tokens for the RC Controller robotics UI.
 * Single source of truth for color, spacing and radii — nothing in the
 * home screen should hardcode a raw color or dp value outside this file.
 */
object AppColors {
    val Background = Color(0xFF0A0E14)
    val Surface = Color(0xFF11161D)
    val SurfaceElevated = Color(0xFF171E28)

    val CardBorder = Color(0x1FFFFFFF)

    val AccentCyan = Color(0xFF00E5FF)
    val AccentCyanDim = Color(0x2600E5FF)

    val Connected = Color(0xFF00E676)
    val Warning = Color(0xFFFFB300)
    val Critical = Color(0xFFFF3B57)

    val TextPrimary = Color(0xFFF3F6FA)
    val TextSecondary = Color(0xFF8B949E)
    val TextTertiary = Color(0xFF5B6472)
}

object AppDimens {
    val Space4 = 4.dp
    val Space8 = 8.dp
    val Space12 = 12.dp
    val Space16 = 16.dp
    val Space20 = 20.dp
    val Space24 = 24.dp
    val Space32 = 32.dp

    val RadiusSmall = 12.dp
    val RadiusMedium = 16.dp
    val RadiusLarge = 20.dp
    val RadiusPill = 50.dp

    val BorderWidth = 1.dp
    val BorderWidthSelected = 1.5.dp

    val IconBadgeSize = 40.dp
    val IconSizeSmall = 16.dp
    val IconSizeMedium = 20.dp
    val IconSizeLarge = 26.dp

    val TouchTargetMin = 48.dp

    val FeatureCardWidth = 208.dp
    val TemplateCardWidth = 168.dp
    val TemplateThumbHeight = 72.dp

    // Subtle depth for cards. Kept small and dark-theme-appropriate so
    // surfaces read as "lifted" without the glary look a default Material
    // shadow gets on a near-black background.
    val ElevationRest = 3.dp
    val ElevationRaised = 8.dp
}

private val AppTypography = Typography(
    titleLarge = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.2.sp),
    titleMedium = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp),
    titleSmall = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
    bodyMedium = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Normal, lineHeight = 18.sp),
    bodySmall = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal, lineHeight = 16.sp),
    labelLarge = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.4.sp),
    labelSmall = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.6.sp),
)

/** Wrap the home screen (and its dialogs) in this once, at the setContent{} root. */
@Composable
fun RcControllerTheme(content: @Composable () -> Unit) {
    val colorScheme = darkColorScheme(
        background = AppColors.Background,
        onBackground = AppColors.TextPrimary,
        surface = AppColors.Surface,
        onSurface = AppColors.TextPrimary,
        surfaceVariant = AppColors.SurfaceElevated,
        primary = AppColors.AccentCyan,
        onPrimary = Color(0xFF00141A),
        error = AppColors.Critical,
        onError = Color.White,
    )
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content,
    )
}