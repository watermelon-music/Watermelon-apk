package com.watermelon.core.designsystem.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
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

private val AmoledColorScheme = darkColorScheme(
    primary = WatermelonRed,
    onPrimary = Color.White,
    primaryContainer = WatermelonRedDark,
    onPrimaryContainer = Color.White,
    secondary = Color(0xFF121212),
    onSecondary = DarkOnBackground,
    secondaryContainer = Color(0xFF1A1A1A),
    onSecondaryContainer = DarkOnSurface,
    tertiary = WatermelonRedMuted,
    onTertiary = DarkOnBackground,
    tertiaryContainer = Color(0xFF2A0A0A),
    onTertiaryContainer = WatermelonRedLight,
    background = Color.Black,
    onBackground = DarkOnBackground,
    surface = Color.Black,
    onSurface = DarkOnBackground,
    surfaceVariant = Color(0xFF121212),
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceTint = WatermelonRed,
    inverseSurface = LightSurface,
    inverseOnSurface = LightOnSurface,
    error = Color(0xFFFF5252),
    onError = Color.White,
    errorContainer = Color(0xFF2A0A0A),
    onErrorContainer = Color(0xFFFFB3B3),
    outline = Color(0xFF2A2A2A),
    outlineVariant = Color(0xFF121212),
    scrim = Color.Black.copy(alpha = 0.8f)
)

private val StudentColorScheme = darkColorScheme(
    primary = Color(0xFF20B2AA),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF004D40),
    onPrimaryContainer = Color.White,
    secondary = Color(0xFF2D2D44),
    onSecondary = Color(0xFFD1D1E0),
    secondaryContainer = Color(0xFF383854),
    onSecondaryContainer = Color(0xFFD1D1E0),
    tertiary = Color(0xFF80CBC4),
    onTertiary = Color(0xFF1A1A2E),
    tertiaryContainer = Color(0xFF00695C),
    onTertiaryContainer = Color(0xFFB2DFDB),
    background = Color(0xFF1A1A2E),
    onBackground = Color(0xFFD1D1E0),
    surface = Color(0xFF22223B),
    onSurface = Color(0xFFD1D1E0),
    surfaceVariant = Color(0xFF2D2D44),
    onSurfaceVariant = Color(0xFFB0B0C3),
    surfaceTint = Color(0xFF20B2AA),
    inverseSurface = LightSurface,
    inverseOnSurface = LightOnSurface,
    error = Color(0xFFFF5252),
    onError = Color.White,
    errorContainer = Color(0xFF2A0A0A),
    onErrorContainer = Color(0xFFFFB3B3),
    outline = Color(0xFF4A4A6A),
    outlineVariant = Color(0xFF2D2D44),
    scrim = Color.Black.copy(alpha = 0.8f)
)

sealed class AppTheme(val key: String, val label: String, val requiresPremium: Boolean = false, val requiresStudent: Boolean = false) {
    data object System : AppTheme("system", "System Default")
    data object Light : AppTheme("light", "Light Watermelon")
    data object Dark : AppTheme("dark", "Dark Watermelon")
    data object Amoled : AppTheme("amoled", "Pure Black", requiresPremium = true)
    data object Student : AppTheme("student", "Student Teal", requiresStudent = true)

    companion object {
        val all: List<AppTheme> = listOf(System, Light, Dark, Amoled, Student)
        fun fromKey(key: String): AppTheme = all.find { it.key == key } ?: System
    }
}

@Composable
fun WatermelonTheme(
    themeMode: String = "system",
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val colorScheme = when (AppTheme.fromKey(themeMode)) {
        AppTheme.Light -> LightColorScheme
        AppTheme.Dark -> DarkColorScheme
        AppTheme.Amoled -> AmoledColorScheme
        AppTheme.Student -> StudentColorScheme
        else -> if (isSystemDark) DarkColorScheme else LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars =
                colorScheme.background.luminance() > 0.5f
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars =
                colorScheme.background.luminance() > 0.5f
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = WatermelonTypography,
        content = content
    )
}
