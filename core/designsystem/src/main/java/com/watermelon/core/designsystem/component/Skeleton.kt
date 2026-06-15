package com.watermelon.core.designsystem.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val ShimmerColor = Color(0xFF2A2A2A)
private val ShimmerHighlight = Color(0xFF3A3A3A)

@Composable
fun SkeletonBox(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(8.dp)
) {
    val shimmerBrush = shimmerBrush()
    Box(
        modifier = modifier
            .clip(shape)
            .background(shimmerBrush)
    )
}

@Composable
fun SkeletonCard(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        SkeletonBox(modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            SkeletonBox(modifier = Modifier.fillMaxWidth(0.7f).height(16.dp))
            Spacer(modifier = Modifier.height(8.dp))
            SkeletonBox(modifier = Modifier.fillMaxWidth(0.4f).height(14.dp))
        }
    }
}

@Composable
private fun shimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )
    return Brush.linearGradient(
        colors = listOf(ShimmerColor, ShimmerHighlight, ShimmerColor),
        start = Offset(translateAnim.value - 200f, 0f),
        end = Offset(translateAnim.value + 200f, 0f)
    )
}
