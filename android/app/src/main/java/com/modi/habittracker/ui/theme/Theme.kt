package com.modi.habittracker.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Shared design tokens with the web client. Dark only, by design. */
object AppColors {
    val Background = Color(0xFF0A0C10)
    val BackgroundSoft = Color(0xFF0D1017)
    val Surface = Color(0xFF141821)
    val SurfaceVariant = Color(0xFF1B2130)
    val SurfaceHigh = Color(0xFF242B3C)
    val Line = Color(0x12FFFFFF)
    val LineStrong = Color(0x24FFFFFF)
    val Text = Color(0xFFECF1F8)
    val Muted = Color(0xFF8B96A9)
    val MutedDim = Color(0xFF6C778A)
    val Accent = Color(0xFF6EE7A8)
    val AccentStrong = Color(0xFF8FF0BE)
    val AccentInk = Color(0xFF06130C)
    val Streak = Color(0xFFFFB86B)
    val Danger = Color(0xFFFF7A7A)
}

private val DarkColors = darkColorScheme(
    primary = AppColors.Accent,
    onPrimary = AppColors.AccentInk,
    primaryContainer = Color(0xFF1C3B2C),
    onPrimaryContainer = AppColors.AccentStrong,
    secondary = AppColors.Streak,
    onSecondary = Color(0xFF2A1704),
    secondaryContainer = Color(0xFF3A2A18),
    onSecondaryContainer = Color(0xFFFFD9A8),
    tertiary = Color(0xFF8FB4FF),
    background = AppColors.Background,
    onBackground = AppColors.Text,
    surface = AppColors.Surface,
    onSurface = AppColors.Text,
    surfaceVariant = AppColors.SurfaceVariant,
    onSurfaceVariant = AppColors.Muted,
    surfaceContainer = AppColors.Surface,
    surfaceContainerHigh = AppColors.SurfaceVariant,
    surfaceContainerHighest = AppColors.SurfaceHigh,
    surfaceContainerLow = AppColors.BackgroundSoft,
    outline = AppColors.LineStrong,
    outlineVariant = AppColors.Line,
    error = AppColors.Danger,
    onError = Color(0xFF2A0505),
    errorContainer = Color(0xFF3A1414),
    onErrorContainer = Color(0xFFFFB4B3),
)

private val AppTypography = Typography(
    displaySmall = TextStyle(
        fontSize = 34.sp,
        lineHeight = 38.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-1.1).sp,
    ),
    headlineSmall = TextStyle(
        fontSize = 24.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.6).sp,
    ),
    titleLarge = TextStyle(
        fontSize = 19.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.45).sp,
    ),
    titleMedium = TextStyle(
        fontSize = 16.5.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.2).sp,
    ),
    titleSmall = TextStyle(
        fontSize = 14.5.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontSize = 12.5.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(
        fontSize = 14.5.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    labelMedium = TextStyle(
        fontSize = 12.5.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    labelSmall = TextStyle(
        fontSize = 11.5.sp,
        lineHeight = 15.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.2.sp,
    ),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

@Composable
fun HabitTrackerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
