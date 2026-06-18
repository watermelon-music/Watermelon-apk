package com.watermelon.feature.player.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.watermelon.core.designsystem.theme.WatermelonTheme
import com.watermelon.feature.player.MiniPlayerContent
import com.watermelon.feature.player.PlayerUiState

@Preview(showBackground = true)
@Composable
fun MiniPlayerPreview() {
    WatermelonTheme {
        MiniPlayerContent(
            state = PlayerUiState(
                currentTitle = "Shape of You",
                currentArtist = "Ed Sheeran",
                isPlaying = true,
                durationMs = 233000L,
                positionMs = 100000L,
                hasNext = true,
                hasPrevious = true
            ),
            onClick = {},
            onPrevious = {},
            onTogglePlayPause = {},
            onNext = {}
        )
    }
}
