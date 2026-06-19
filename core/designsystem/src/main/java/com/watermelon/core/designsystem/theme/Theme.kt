package com.watermelon.core.designsystem.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

fun Context.findActivity(): Activity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

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

private val ObsidianGoldColorScheme = darkColorScheme(
    primary = Color(0xFFD4AF37),
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF3D2E00),
    onPrimaryContainer = Color(0xFFFFF8E1),
    secondary = Color(0xFF2A2A2A),
    onSecondary = Color(0xFFE0E0E0),
    secondaryContainer = Color(0xFF333333),
    onSecondaryContainer = Color(0xFFE0E0E0),
    tertiary = Color(0xFFBFA76F),
    onTertiary = Color(0xFF1A1A1A),
    tertiaryContainer = Color(0xFF423300),
    onTertiaryContainer = Color(0xFFFFF0B2),
    background = Color(0xFF0D0D0D),
    onBackground = Color(0xFFF5F5F5),
    surface = Color(0xFF1A1A1A),
    onSurface = Color(0xFFF5F5F5),
    surfaceVariant = Color(0xFF242424),
    onSurfaceVariant = Color(0xFFB0B0B0),
    surfaceTint = Color(0xFFD4AF37),
    inverseSurface = LightSurface,
    inverseOnSurface = LightOnSurface,
    error = Color(0xFFFF5252),
    onError = Color.White,
    errorContainer = Color(0xFF2A0A0A),
    onErrorContainer = Color(0xFFFFB3B3),
    outline = Color(0xFF3A3A3A),
    outlineVariant = Color(0xFF242424),
    scrim = Color.Black.copy(alpha = 0.8f)
)

private val EmeraldDynastyColorScheme = darkColorScheme(
    primary = Color(0xFF50C878),
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF004D00),
    onPrimaryContainer = Color(0xFFE0F8E0),
    secondary = Color(0xFF1A2E1A),
    onSecondary = Color(0xFFD1E8D1),
    secondaryContainer = Color(0xFF253F25),
    onSecondaryContainer = Color(0xFFD1E8D1),
    tertiary = Color(0xFF80D080),
    onTertiary = Color(0xFF0A1A0A),
    tertiaryContainer = Color(0xFF004D00),
    onTertiaryContainer = Color(0xFFB2E8B2),
    background = Color(0xFF0A1A0A),
    onBackground = Color(0xFFE8F5E8),
    surface = Color(0xFF142814),
    onSurface = Color(0xFFE8F5E8),
    surfaceVariant = Color(0xFF1E3820),
    onSurfaceVariant = Color(0xFFA8C8A8),
    surfaceTint = Color(0xFF50C878),
    inverseSurface = LightSurface,
    inverseOnSurface = LightOnSurface,
    error = Color(0xFFFF5252),
    onError = Color.White,
    errorContainer = Color(0xFF2A0A0A),
    onErrorContainer = Color(0xFFFFB3B3),
    outline = Color(0xFF2A4A2A),
    outlineVariant = Color(0xFF1E3820),
    scrim = Color.Black.copy(alpha = 0.8f)
)

private val SapphireEliteColorScheme = darkColorScheme(
    primary = Color(0xFF3B82F6),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF002766),
    onPrimaryContainer = Color(0xFFE0ECFF),
    secondary = Color(0xFF152238),
    onSecondary = Color(0xFFD1D8E4),
    secondaryContainer = Color(0xFF1E2E4A),
    onSecondaryContainer = Color(0xFFD1D8E4),
    tertiary = Color(0xFF60A5FA),
    onTertiary = Color(0xFF0A1525),
    tertiaryContainer = Color(0xFF003B7A),
    onTertiaryContainer = Color(0xFFB2D4FF),
    background = Color(0xFF0A0E1A),
    onBackground = Color(0xFFE8ECF5),
    surface = Color(0xFF12182E),
    onSurface = Color(0xFFE8ECF5),
    surfaceVariant = Color(0xFF1E2840),
    onSurfaceVariant = Color(0xFFA8B8D4),
    surfaceTint = Color(0xFF3B82F6),
    inverseSurface = LightSurface,
    inverseOnSurface = LightOnSurface,
    error = Color(0xFFFF5252),
    onError = Color.White,
    errorContainer = Color(0xFF2A0A0A),
    onErrorContainer = Color(0xFFFFB3B3),
    outline = Color(0xFF2A3A5A),
    outlineVariant = Color(0xFF1E2840),
    scrim = Color.Black.copy(alpha = 0.8f)
)

private val AmethystDreamsColorScheme = darkColorScheme(
    primary = Color(0xFFA78BFA),
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF3A1C4A),
    onPrimaryContainer = Color(0xFFF3E8FF),
    secondary = Color(0xFF2A1A3A),
    onSecondary = Color(0xFFD8D1E0),
    secondaryContainer = Color(0xFF3A254A),
    onSecondaryContainer = Color(0xFFD8D1E0),
    tertiary = Color(0xFFC4B5FD),
    onTertiary = Color(0xFF1A1025),
    tertiaryContainer = Color(0xFF4A2C5A),
    onTertiaryContainer = Color(0xFFE8D8FF),
    background = Color(0xFF140A1E),
    onBackground = Color(0xFFF0E8F5),
    surface = Color(0xFF201430),
    onSurface = Color(0xFFF0E8F5),
    surfaceVariant = Color(0xFF2E1E40),
    onSurfaceVariant = Color(0xFFB8A8C8),
    surfaceTint = Color(0xFFA78BFA),
    inverseSurface = LightSurface,
    inverseOnSurface = LightOnSurface,
    error = Color(0xFFFF5252),
    onError = Color.White,
    errorContainer = Color(0xFF2A0A0A),
    onErrorContainer = Color(0xFFFFB3B3),
    outline = Color(0xFF3A2A4A),
    outlineVariant = Color(0xFF2E1E40),
    scrim = Color.Black.copy(alpha = 0.8f)
)

private val CrimsonRoyaleColorScheme = darkColorScheme(
    primary = Color(0xFFFF3333),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF4A0000),
    onPrimaryContainer = Color(0xFFFFE0E0),
    secondary = Color(0xFF2A0A0A),
    onSecondary = Color(0xFFE8D1D1),
    secondaryContainer = Color(0xFF3A1414),
    onSecondaryContainer = Color(0xFFE8D1D1),
    tertiary = Color(0xFFFF6666),
    onTertiary = Color(0xFF1A0A0A),
    tertiaryContainer = Color(0xFF5A0000),
    onTertiaryContainer = Color(0xFFFFB3B3),
    background = Color(0xFF0D0000),
    onBackground = Color(0xFFF5E8E8),
    surface = Color(0xFF1A0A0A),
    onSurface = Color(0xFFF5E8E8),
    surfaceVariant = Color(0xFF2A1414),
    onSurfaceVariant = Color(0xFFC8A8A8),
    surfaceTint = Color(0xFFFF3333),
    inverseSurface = LightSurface,
    inverseOnSurface = LightOnSurface,
    error = Color(0xFFFF0000),
    onError = Color.White,
    errorContainer = Color(0xFF2A0A0A),
    onErrorContainer = Color(0xFFFFB3B3),
    outline = Color(0xFF4A2A2A),
    outlineVariant = Color(0xFF2A1414),
    scrim = Color.Black.copy(alpha = 0.8f)
)

sealed class AppTheme(val key: String, val label: String) {
    data object Light : AppTheme("light", "Light Watermelon")
    data object Dark : AppTheme("dark", "Dark Watermelon")

    companion object {
        val all: List<AppTheme> = listOf(Light, Dark)
        fun fromKey(key: String): AppTheme = all.find { it.key == key } ?: Dark
    }
}

@Composable
fun WatermelonTheme(
    themeMode: String = "system",
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val context = LocalContext.current
    val theme = AppTheme.fromKey(themeMode)

    val colorScheme = when {
        theme == AppTheme.Dark -> DarkColorScheme
        theme == AppTheme.Light -> LightColorScheme
        else -> if (isSystemDark) DarkColorScheme else LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context.findActivity()
            val window = activity?.window ?: return@SideEffect
            window.statusBarColor = colorScheme.background.toArgb()
            // Transparent so the bottom NavigationBar's container color paints
            // continuously into the system gesture/3-button area instead of
            // showing a different `background` strip below it.
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
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
