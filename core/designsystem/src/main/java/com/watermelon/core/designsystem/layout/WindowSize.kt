package com.watermelon.core.designsystem.layout

import android.content.res.Configuration
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.watermelon.core.designsystem.theme.WatermelonSpacing

enum class WindowSize { Compact, Medium, Expanded }

@Composable
@ReadOnlyComposable
fun rememberWindowSize(): WindowSize {
    val w = LocalConfiguration.current.screenWidthDp
    return when {
        w < 600 -> WindowSize.Compact
        w < 840 -> WindowSize.Medium
        else -> WindowSize.Expanded
    }
}

@Composable
@ReadOnlyComposable
fun isLandscape(): Boolean =
    LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

@Composable
@ReadOnlyComposable
fun screenWidthDp(): Dp = LocalConfiguration.current.screenWidthDp.dp

@Composable
@ReadOnlyComposable
fun screenHeightDp(): Dp = LocalConfiguration.current.screenHeightDp.dp

@Composable
@ReadOnlyComposable
fun adaptiveHorizontalPadding(): Dp = when (rememberWindowSize()) {
    WindowSize.Compact -> WatermelonSpacing.md
    WindowSize.Medium -> WatermelonSpacing.lg
    WindowSize.Expanded -> WatermelonSpacing.xl
}

@Composable
@ReadOnlyComposable
fun adaptiveContentPadding(): PaddingValues = PaddingValues(
    horizontal = adaptiveHorizontalPadding(),
    vertical = WatermelonSpacing.md
)

/**
 * Cap content width on wide screens so single-column lists, forms, and player
 * controls don't stretch edge-to-edge on tablets / landscape phones.
 */
@Composable
@ReadOnlyComposable
fun adaptiveMaxContentWidth(): Dp = when (rememberWindowSize()) {
    WindowSize.Compact -> Dp.Unspecified
    WindowSize.Medium -> 640.dp
    WindowSize.Expanded -> 840.dp
}

/**
 * Width of an item in a horizontal carousel. Scales modestly with screen size
 * so a single card never dominates a wide screen, and small phones don't crop.
 */
@Composable
@ReadOnlyComposable
fun adaptiveBigCardWidth(): Dp = when (rememberWindowSize()) {
    WindowSize.Compact -> 160.dp
    WindowSize.Medium -> 180.dp
    WindowSize.Expanded -> 200.dp
}

@Composable
@ReadOnlyComposable
fun adaptiveSmallCardWidth(): Dp = when (rememberWindowSize()) {
    WindowSize.Compact -> 124.dp
    WindowSize.Medium -> 140.dp
    WindowSize.Expanded -> 156.dp
}

/**
 * Adaptive minimum cell size for `LazyVerticalGrid(GridCells.Adaptive(...))`.
 */
@Composable
@ReadOnlyComposable
fun adaptiveGridMinSize(): Dp = when (rememberWindowSize()) {
    WindowSize.Compact -> 160.dp
    WindowSize.Medium -> 180.dp
    WindowSize.Expanded -> 200.dp
}

@Composable
@ReadOnlyComposable
fun adaptiveListMinSize(): Dp = when (rememberWindowSize()) {
    WindowSize.Compact -> 340.dp
    WindowSize.Medium -> 380.dp
    WindowSize.Expanded -> 420.dp
}

@Composable
@ReadOnlyComposable
fun adaptiveAlbumArtFraction(): Float = when {
    isLandscape() -> 0.42f
    rememberWindowSize() != WindowSize.Compact -> 0.55f
    else -> 0.78f
}
