package com.watermelon.feature.search.preview

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.watermelon.core.designsystem.theme.WatermelonTheme
import com.watermelon.feature.search.SearchScreenContent

@Preview(showBackground = true)
@Composable
fun SearchScreenPreview() {
    WatermelonTheme {
        SearchScreenContent(
            query = "",
            results = emptyList(),
            isLoading = false,
            padding = PaddingValues(0.dp),
            onQueryChange = {},
            onSongClick = { _, _, _ -> },
            onAddToPlaylist = {}
        )
    }
}
