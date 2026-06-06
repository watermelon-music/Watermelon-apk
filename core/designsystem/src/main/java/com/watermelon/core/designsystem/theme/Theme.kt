package com.watermelon.core.designsystem.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = WatermelonRed,
    onPrimary = Color.White,
    primaryContainer = WatermelonRedDark,
    onPrimaryContainer = Color.White,
    secondary = DarkSurfaceVariant,
    onSecondary = DarkOnBackground,
    secondaryContainer = DarkSurfaceElevated,
    onSecondaryContainer = DarkOnSurface,
    tertiary = WatermelonRedMuted,
    onTertiary = DarkOnBackground,
    tertiaryContainer = Color(0xFF2A0A0A),
    onTertiaryContainer = WatermelonRedLight,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnBackground,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceTint = WatermelonRed,
    inverseSurface = LightSurface,
    inverseOnSurface = LightOnSurface,
    error = Color(0xFFFF5252),
    onError = Color.White,
    errorContainer = Color(0xFF2A0A0A),
    onErrorContainer = Color(0xFFFFB3B3),
    outline = Color(0xFF2A2A2A),
    outlineVariant = DarkSurfaceVariant,
    scrim = Color.Black.copy(alpha = 0.8f)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFDC2626),
    onPrimary = Color.White,
    primaryContainer = WatermelonRedLight,
    onPrimaryContainer = Color(0xFF7F1D1D),
    secondary = LightSurfaceVariant,
    onSecondary = LightOnBackground,
    secondaryContainer = LightSurfaceElevated,
    onSecondaryContainer = LightOnSurface,
    tertiary = Color(0xFFFCA5A5),
    onTertiary = LightOnBackground,
    tertiaryContainer = WatermelonRedLight,
    onTertiaryContainer = Color(0xFF7F1D1D),
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnBackground,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    surfaceTint = WatermelonRed,
    inverseSurface = DarkSurface,
    inverseOnSurface = DarkOnSurface,
    error = Color(0xFFDC2626),
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D),
    outline = Color(0xFFE5E5E5),
    outlineVariant = LightSurfaceVariant,
    scrim = Color.Black.copy(alpha = 0.5f)
)

@Composable
fun WatermelonTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = WatermelonTypography,
        content = content
    )
}
