package com.watermelon.core.designsystem.animation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.fadeInAnimation(delayMillis: Int = 0): Modifier = composed {
    val tween = tween<Float>(durationMillis = 400, delayMillis = delayMillis, easing = EaseOutCubic)
    animateContentSize()
    this.alpha(
        animateFloatAsState(
            targetValue = 1f,
            animationSpec = tween,
            label = "fade"
        ).value
    )
}

fun Modifier.shimmerEffect(durationMillis: Int = 1200): Modifier = composed {
    val shimmerColors = listOf(
        Color.White.copy(alpha = 0.1f),
        Color.White.copy(alpha = 0.3f),
        Color.White.copy(alpha = 0.1f)
    )

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnim.value, y = translateAnim.value)
    )

    this.background(brush).clip(RoundedCornerShape(8.dp))
}

@Composable
fun ShimmerCard(modifier: Modifier = Modifier, height: Dp = 120.dp) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .shimmerEffect()
    )
}

@Composable
fun ShimmerCircle(modifier: Modifier = Modifier, size: Dp = 48.dp) {
    Box(
        modifier = modifier
            .size(size)
            .shimmerEffect()
    )
}
