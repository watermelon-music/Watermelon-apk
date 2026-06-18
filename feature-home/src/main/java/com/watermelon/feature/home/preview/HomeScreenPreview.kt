package com.watermelon.feature.home.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.watermelon.core.designsystem.theme.WatermelonTheme
import com.watermelon.domain.model.Song
import com.watermelon.feature.home.HomeUiState
import com.watermelon.feature.home.HomeScreenContent

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    WatermelonTheme {
        HomeScreenContent(
            uiState = HomeUiState(
                trendingMusic = listOf(
                    Song(
                        id = "1", 
                        title = "Shape of You", 
                        artistName = "Ed Sheeran", 
                        coverUrl = "",
                        artistId = "a1",
                        albumId = "al1",
                        albumName = "Divide",
                        durationMs = 233000L,
                        audioUrl = "",
                        genre = "Pop",
                        releaseDate = "2017"
                    )
                ),
                isLoading = false
            ),
            onSearchClick = {},
            onSettingsClick = {},
            onSongClick = { _, _ -> },
            onPlayerClick = {},
            onAddToPlaylist = {},
            snackbarHostState = androidx.compose.material3.SnackbarHostState()
        )
    }
}
