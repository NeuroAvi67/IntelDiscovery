package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val IntelDarkColorScheme = darkColorScheme(
    primary = ElectricBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF172B4D),
    onPrimaryContainer = ElectricBlueBright,
    secondary = TacticalCyan,
    onSecondary = Color(0xFF042029),
    secondaryContainer = Color(0xFF0D3344),
    onSecondaryContainer = TacticalCyanSoft,
    tertiary = VioletAi,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF2B1F4D),
    onTertiaryContainer = Color(0xFFD8B4FE),
    background = ObsidianBg,
    onBackground = InkPrimaryDark,
    surface = NavySurface,
    onSurface = InkPrimaryDark,
    surfaceVariant = NavySurfaceVariant,
    onSurfaceVariant = InkSecondaryDark,
    surfaceContainerLow = NavySurface,
    surfaceContainer = NavySurfaceVariant,
    surfaceContainerHigh = NavySurfaceElevated,
    outline = BorderSlate,
    outlineVariant = Color(0xFF1A2842),
    error = RosePaid,
    onError = Color.White
)

private val IntelLightColorScheme = lightColorScheme(
    primary = ElectricBlueDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = Color(0xFF1E3A8A),
    secondary = TacticalCyanDark,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCFFAFE),
    onSecondaryContainer = Color(0xFF164E63),
    tertiary = Color(0xFF6D28D9),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFEDE9FE),
    onTertiaryContainer = Color(0xFF4C1D95),
    background = DaylightBg,
    onBackground = DeepNavyInk,
    surface = DaylightSurface,
    onSurface = DeepNavyInk,
    surfaceVariant = DaylightSurfaceVariant,
    onSurfaceVariant = MutedSlateInk,
    surfaceContainerLow = DaylightSurface,
    surfaceContainer = DaylightSurfaceVariant,
    surfaceContainerHigh = Color(0xFFDCE6F7),
    outline = DaylightBorder,
    outlineVariant = Color(0xFFDCE5F2),
    error = Color(0xFFDC2626),
    onError = Color.White
)

val IntelShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun IntelAtlasTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) IntelDarkColorScheme else IntelLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = IntelTypography,
        shapes = IntelShapes,
        content = content
    )
}
