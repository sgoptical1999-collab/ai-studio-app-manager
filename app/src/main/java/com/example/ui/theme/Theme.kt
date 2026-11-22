package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val AppDarkColorScheme = darkColorScheme(
    primary = ElectricBlue,
    onPrimary = TextPrimary,
    primaryContainer = ElectricBlueContainer,
    onPrimaryContainer = ElectricBlueBright,
    secondary = RoyalPurple,
    onSecondary = TextPrimary,
    secondaryContainer = RoyalPurpleContainer,
    onSecondaryContainer = RoyalPurpleBright,
    tertiary = SkyCyan,
    onTertiary = NavyVoid,
    tertiaryContainer = StatusInfoContainer,
    onTertiaryContainer = SkyCyan,
    background = NavyBackground,
    onBackground = TextPrimary,
    surface = NavySurface,
    onSurface = TextPrimary,
    surfaceVariant = NavySurfaceElevated,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = NavyVoid,
    surfaceContainerLow = NavyBackground,
    surfaceContainer = NavySurface,
    surfaceContainerHigh = NavySurfaceElevated,
    surfaceContainerHighest = NavySurfaceHighest,
    outline = NavyBorder,
    outlineVariant = NavyBorderSubtle,
    error = StatusError,
    onError = TextPrimary,
    errorContainer = StatusErrorContainer,
    onErrorContainer = StatusError
)

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun AiStudioAppManagerTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AppDarkColorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
