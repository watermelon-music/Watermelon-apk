@file:OptIn(ExperimentalMaterial3Api::class)

package com.watermelon.app.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.SubcomposeAsyncImage
import com.watermelon.core.designsystem.animation.ShimmerCard
import com.watermelon.core.designsystem.theme.WatermelonRed
import com.watermelon.core.designsystem.theme.WatermelonSpacing
import com.watermelon.domain.model.RadioCountry
import com.watermelon.domain.model.RadioLanguage
import com.watermelon.domain.model.RadioStation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RadioScreen(
    onPlayStation: (RadioStation) -> Unit,
    viewModel: RadioViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Radio,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = WatermelonRed
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Radio")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        RadioScreenContent(
            uiState = uiState,
            padding = padding,
            onPlayStation = { station ->
                viewModel.recordRecentlyPlayed(station)
                onPlayStation(station)
            },
            onToggleFavorite = viewModel::toggleFavorite,
            isFavorite = viewModel::isFavorite,
            onSelectTab = viewModel::selectTab,
            onSelectCountry = viewModel::selectCountry,
            onClearCountry = viewModel::clearCountry,
            onSelectLanguage = viewModel::selectLanguage,
            onClearLanguage = viewModel::clearLanguage,
            onSearchQueryChange = viewModel::onSearchQueryChange,
            onErrorDismiss = viewModel::clearError
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RadioScreenContent(
    uiState: RadioUiState,
    padding: PaddingValues,
    onPlayStation: (RadioStation) -> Unit,
    onToggleFavorite: (RadioStation) -> Unit,
    isFavorite: (RadioStation) -> Boolean,
    onSelectTab: (RadioTab) -> Unit,
    onSelectCountry: (RadioCountry) -> Unit,
    onClearCountry: () -> Unit,
    onSelectLanguage: (String) -> Unit,
    onClearLanguage: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onErrorDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
    ) {
        TabRow(
            selectedTabIndex = uiState.selectedTab.ordinal,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = WatermelonRed
        ) {
            val tabIcons = mapOf(
                RadioTab.BROWSE to Icons.Default.Explore,
                RadioTab.LANGUAGES to Icons.Default.Language,
                RadioTab.SEARCH to Icons.Default.Search,
                RadioTab.FAVORITES to Icons.Default.Favorite,
                RadioTab.RECENT to Icons.Default.History
            )
            RadioTab.entries.forEach { tab ->
                Tab(
                    selected = uiState.selectedTab == tab,
                    onClick = { onSelectTab(tab) },
                    icon = {
                        Icon(
                            imageVector = tabIcons[tab] ?: Icons.Default.Explore,
                            contentDescription = tab.label,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when (uiState.selectedTab) {
                RadioTab.BROWSE -> BrowseTab(
                    uiState = uiState,
                    onCountryClick = onSelectCountry,
                    onBack = onClearCountry,
                    onPlayStation = onPlayStation,
                    onToggleFavorite = onToggleFavorite,
                    isFavorite = isFavorite
                )
                RadioTab.LANGUAGES -> LanguagesTab(
                    uiState = uiState,
                    onLanguageClick = onSelectLanguage,
                    onBack = onClearLanguage,
                    onPlayStation = onPlayStation,
                    onToggleFavorite = onToggleFavorite,
                    isFavorite = isFavorite
                )
                RadioTab.SEARCH -> SearchTab(
                    uiState = uiState,
                    onQueryChange = onSearchQueryChange,
                    onPlayStation = onPlayStation,
                    onToggleFavorite = onToggleFavorite,
                    isFavorite = isFavorite
                )
                RadioTab.FAVORITES -> FavoritesTab(
                    stations = uiState.favoriteStations,
                    onPlayStation = onPlayStation,
                    onToggleFavorite = onToggleFavorite
                )
                RadioTab.RECENT -> RecentTab(
                    stations = uiState.recentStations,
                    onPlayStation = onPlayStation,
                    onToggleFavorite = onToggleFavorite,
                    isFavorite = isFavorite
                )
            }

            if (uiState.error != null) {
                ErrorBanner(
                    message = uiState.error!!,
                    onDismiss = onErrorDismiss
                )
            }
        }
    }
}


/* ---------- Browse Tab ---------- */

@Composable
private fun BrowseTab(
    uiState: RadioUiState,
    onCountryClick: (RadioCountry) -> Unit,
    onBack: () -> Unit,
    onPlayStation: (RadioStation) -> Unit,
    onToggleFavorite: (RadioStation) -> Unit,
    isFavorite: (RadioStation) -> Boolean
) {
    if (uiState.selectedCountry != null) {
        CountryDetailContent(
            country = uiState.selectedCountry!!,
            stations = uiState.countryStations,
            isLoading = uiState.isLoading,
            onBack = onBack,
            onPlayStation = onPlayStation,
            onToggleFavorite = onToggleFavorite,
            isFavorite = isFavorite
        )
    } else {
        CountryGridContent(
            countries = uiState.countries,
            isLoading = uiState.isLoading,
            onCountryClick = onCountryClick
        )
    }
}

@Composable
private fun CountryGridContent(
    countries: List<RadioCountry>,
    isLoading: Boolean,
    onCountryClick: (RadioCountry) -> Unit
) {
    if (isLoading) {
        ShimmerGrid()
    } else if (countries.isEmpty()) {
        EmptyState("No countries found")
    } else {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 140.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(WatermelonSpacing.md),
            verticalArrangement = Arrangement.spacedBy(WatermelonSpacing.md),
            horizontalArrangement = Arrangement.spacedBy(WatermelonSpacing.md)
        ) {
            items(countries, key = { it.name }) { country ->
                CountryCard(country = country, onClick = { onCountryClick(country) })
            }
        }
    }
}

@Composable
private fun CountryCard(country: RadioCountry, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.2f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Explore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(36.dp).padding(bottom = 8.dp)
                )
                Text(
                    text = country.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "${country.stationcount} stns",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CountryDetailContent(
    country: RadioCountry,
    stations: List<RadioStation>,
    isLoading: Boolean,
    onBack: () -> Unit,
    onPlayStation: (RadioStation) -> Unit,
    onToggleFavorite: (RadioStation) -> Unit,
    isFavorite: (RadioStation) -> Boolean
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(country.name) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        )
        Box(modifier = Modifier.fillMaxSize()) {
            if (isLoading) {
                ShimmerList()
            } else if (stations.isEmpty()) {
                EmptyState("No stations found for this country")
            } else {
                val grouped = remember(stations) {
                    stations.groupBy { station ->
                        station.tags?.split(",")
                            ?.firstOrNull { it.isNotBlank() }
                            ?.trim()
                            ?.replaceFirstChar { it.uppercase() }
                            ?: "General"
                    }.toSortedMap()
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(WatermelonSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(WatermelonSpacing.md)
                ) {
                    grouped.forEach { (genre, list) ->
                        item {
                            Surface(color = MaterialTheme.colorScheme.background) {
                                Text(
                                    text = genre,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = WatermelonRed,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }
                        items(list, key = { it.stationuuid ?: it.url ?: it.hashCode() }) { station ->
                            StationListItem(
                                station = station,
                                onPlay = { onPlayStation(station) },
                                onToggleFavorite = { onToggleFavorite(station) },
                                isFavorite = isFavorite(station)
                            )
                        }
                    }
                }
            }
        }
    }
}

/* ---------- Languages Tab ---------- */

@Composable
private fun LanguagesTab(
    uiState: RadioUiState,
    onLanguageClick: (String) -> Unit,
    onBack: () -> Unit,
    onPlayStation: (RadioStation) -> Unit,
    onToggleFavorite: (RadioStation) -> Unit,
    isFavorite: (RadioStation) -> Boolean
) {
    if (uiState.selectedLanguage != null) {
        LanguageDetailContent(
            language = uiState.selectedLanguage!!,
            stations = uiState.languageStations,
            isLoading = uiState.isLoading,
            onBack = onBack,
            onPlayStation = onPlayStation,
            onToggleFavorite = onToggleFavorite,
            isFavorite = isFavorite
        )
    } else {
        LanguageGridContent(
            languages = uiState.languages,
            isLoading = uiState.isLoading,
            onLanguageClick = onLanguageClick
        )
    }
}

@Composable
private fun LanguageGridContent(
    languages: List<RadioLanguage>,
    isLoading: Boolean,
    onLanguageClick: (String) -> Unit
) {
    if (isLoading) {
        ShimmerGrid()
    } else if (languages.isEmpty()) {
        EmptyState("No languages found")
    } else {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 140.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(WatermelonSpacing.md),
            verticalArrangement = Arrangement.spacedBy(WatermelonSpacing.md),
            horizontalArrangement = Arrangement.spacedBy(WatermelonSpacing.md)
        ) {
            items(languages, key = { it.name }) { language ->
                LanguageCard(language = language, onClick = { onLanguageClick(language.name) })
            }
        }
    }
}

@Composable
private fun LanguageCard(language: RadioLanguage, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.2f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.8f),
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Language,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(36.dp).padding(bottom = 8.dp)
                )
                Text(
                    text = language.name.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "${language.stationcount} stns",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun LanguageDetailContent(
    language: String,
    stations: List<RadioStation>,
    isLoading: Boolean,
    onBack: () -> Unit,
    onPlayStation: (RadioStation) -> Unit,
    onToggleFavorite: (RadioStation) -> Unit,
    isFavorite: (RadioStation) -> Boolean
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(language.replaceFirstChar { it.uppercase() }) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        )
        Box(modifier = Modifier.fillMaxSize()) {
            if (isLoading) {
                ShimmerList()
            } else if (stations.isEmpty()) {
                EmptyState("No stations found for this language")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(WatermelonSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(WatermelonSpacing.md)
                ) {
                    items(stations, key = { it.stationuuid ?: it.url ?: it.hashCode() }) { station ->
                        StationListItem(
                            station = station,
                            onPlay = { onPlayStation(station) },
                            onToggleFavorite = { onToggleFavorite(station) },
                            isFavorite = isFavorite(station)
                        )
                    }
                }
            }
        }
    }
}

/* ---------- Search Tab ---------- */

@Composable
private fun SearchTab(
    uiState: RadioUiState,
    onQueryChange: (String) -> Unit,
    onPlayStation: (RadioStation) -> Unit,
    onToggleFavorite: (RadioStation) -> Unit,
    isFavorite: (RadioStation) -> Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(WatermelonSpacing.md)
    ) {
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search stations by name...") },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = WatermelonRed)
            },
            shape = RoundedCornerShape(20.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = WatermelonRed,
                focusedLeadingIconColor = WatermelonRed
            )
        )

        Spacer(modifier = Modifier.height(WatermelonSpacing.md))

        if (uiState.isSearching) {
            ShimmerList()
        } else if (uiState.searchQuery.isBlank()) {
            EmptyState("Type to search radio stations")
        } else if (uiState.searchResults.isEmpty()) {
            EmptyState("No stations found")
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(WatermelonSpacing.md)
            ) {
                items(uiState.searchResults, key = { it.stationuuid ?: it.url ?: it.hashCode() }) { station ->
                    StationListItem(
                        station = station,
                        onPlay = { onPlayStation(station) },
                        onToggleFavorite = { onToggleFavorite(station) },
                        isFavorite = isFavorite(station)
                    )
                }
            }
        }
    }
}

/* ---------- Favorites Tab ---------- */

@Composable
private fun FavoritesTab(
    stations: List<RadioStation>,
    onPlayStation: (RadioStation) -> Unit,
    onToggleFavorite: (RadioStation) -> Unit
) {
    if (stations.isEmpty()) {
        EmptyState("No favorite stations yet")
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(WatermelonSpacing.md),
            verticalArrangement = Arrangement.spacedBy(WatermelonSpacing.md)
        ) {
            items(stations, key = { it.stationuuid ?: it.url ?: it.hashCode() }) { station ->
                StationListItem(
                    station = station,
                    onPlay = { onPlayStation(station) },
                    onToggleFavorite = { onToggleFavorite(station) },
                    isFavorite = true
                )
            }
        }
    }
}

/* ---------- Recent Tab ---------- */

@Composable
private fun RecentTab(
    stations: List<RadioStation>,
    onPlayStation: (RadioStation) -> Unit,
    onToggleFavorite: (RadioStation) -> Unit,
    isFavorite: (RadioStation) -> Boolean
) {
    if (stations.isEmpty()) {
        EmptyState("No recently played stations")
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(WatermelonSpacing.md),
            verticalArrangement = Arrangement.spacedBy(WatermelonSpacing.md)
        ) {
            items(stations, key = { it.stationuuid ?: it.url ?: it.hashCode() }) { station ->
                StationListItem(
                    station = station,
                    onPlay = { onPlayStation(station) },
                    onToggleFavorite = { onToggleFavorite(station) },
                    isFavorite = isFavorite(station)
                )
            }
        }
    }
}

/* ---------- Shared Components ---------- */

@Composable
private fun StationListItem(
    station: RadioStation,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    isFavorite: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPlay),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                val image = station.favicon
                if (!image.isNullOrBlank()) {
                    SubcomposeAsyncImage(
                        model = image,
                        contentDescription = station.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        loading = {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = WatermelonRed, strokeWidth = 2.dp)
                        },
                        error = {
                            Icon(Icons.Default.Radio, contentDescription = null, tint = WatermelonRed, modifier = Modifier.size(28.dp))
                        }
                    )
                } else {
                    Icon(Icons.Default.Radio, contentDescription = null, tint = WatermelonRed, modifier = Modifier.size(28.dp))
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = station.name ?: "Unknown Station",
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val country = station.country?.takeIf { it.isNotBlank() } ?: "Unknown"
                    val bitrate = station.bitrate?.takeIf { it > 0 }?.let { "${it}kbps" } ?: "Live"
                    Text(
                        text = "$country • $bitrate",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                    tint = if (isFavorite) WatermelonRed else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun PlayIcon() {
    Icon(
        imageVector = Icons.Default.PlayArrow,
        contentDescription = "Play",
        tint = WatermelonRed,
        modifier = Modifier.size(32.dp)
    )
}

@Composable
private fun EmptyState(text: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ErrorBanner(message: String, onDismiss: () -> Unit) {
    Snackbar(
        modifier = Modifier.padding(16.dp),
        action = {
            TextButton(onClick = onDismiss) { Text("Dismiss") }
        }
    ) {
        Text(message)
    }
}

@Composable
private fun ShimmerGrid() {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 140.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(WatermelonSpacing.md),
        verticalArrangement = Arrangement.spacedBy(WatermelonSpacing.md),
        horizontalArrangement = Arrangement.spacedBy(WatermelonSpacing.md)
    ) {
        items(8) {
            ShimmerCard(modifier = Modifier.aspectRatio(1.2f))
        }
    }
}

@Composable
private fun ShimmerList() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(WatermelonSpacing.md),
        verticalArrangement = Arrangement.spacedBy(WatermelonSpacing.md)
    ) {
        repeat(6) {
            ShimmerCard(height = 80.dp)
        }
    }
}
