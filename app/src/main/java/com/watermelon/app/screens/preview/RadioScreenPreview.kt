package com.watermelon.app.screens.preview

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.watermelon.app.screens.RadioScreenContent
import com.watermelon.app.screens.RadioUiState
import com.watermelon.core.designsystem.theme.WatermelonTheme
import com.watermelon.app.screens.RadioTab

@Preview(showBackground = true)
@Composable
fun RadioScreenPreview() {
    WatermelonTheme {
        RadioScreenContent(
            uiState = RadioUiState(
                selectedTab = RadioTab.BROWSE,
                isLoading = false,
                countries = emptyList(),
                languages = emptyList(),
                favoriteStations = emptyList(),
                recentStations = emptyList(),
                searchResults = emptyList(),
                searchQuery = "",
                isSearching = false,
                selectedCountry = null,
                countryStations = emptyList(),
                selectedLanguage = null,
                languageStations = emptyList(),
                error = null
            ),
            padding = PaddingValues(0.dp),
            onPlayStation = {},
            onToggleFavorite = {},
            isFavorite = { false },
            onSelectTab = {},
            onSelectCountry = {},
            onClearCountry = {},
            onSelectLanguage = {},
            onClearLanguage = {},
            onSearchQueryChange = {},
            onErrorDismiss = {}
        )
    }
}
