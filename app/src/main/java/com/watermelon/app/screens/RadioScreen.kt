package com.watermelon.app.screens

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
import androidx.compose.material.icons.filled.PlayArrow
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
import com.watermelon.core.designsystem.theme.WatermelonRed
import com.watermelon.data.remote.radio.RadioStationDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RadioScreen(
    onPlayStation: (RadioStationDto) -> Unit,
    viewModel: RadioViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (uiState.selectedCategory != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { viewModel.clearSelection() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                            Text(uiState.selectedCategory!!.name)
                        }
                    } else {
                        Text("Radio")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = WatermelonRed)
                }
            } else if (uiState.selectedCategory != null) {
                StationListContent(
                    category = uiState.selectedCategory!!,
                    selectedLanguage = uiState.selectedLanguage,
                    onLanguageSelect = { viewModel.selectLanguage(it) },
                    onPlayStation = onPlayStation
                )
            } else {
                CategoryGrid(
                    categories = uiState.categories,
                    onCategoryClick = { viewModel.selectCategory(it) }
                )
            }
        }
    }
}

@Composable
private fun CategoryGrid(
    categories: List<RadioCategory>,
    onCategoryClick: (RadioCategory) -> Unit
) {
    val gradients = listOf(
        Brush.linearGradient(listOf(Color(0xFFFF6B6B), Color(0xFFFF8E53))),
        Brush.linearGradient(listOf(Color(0xFF4ECDC4), Color(0xFF44A08D))),
        Brush.linearGradient(listOf(Color(0xFF667EEA), Color(0xFF764BA2))),
        Brush.linearGradient(listOf(Color(0xFFF093FB), Color(0xFFF5576C))),
        Brush.linearGradient(listOf(Color(0xFF4FACFE), Color(0xFF00F2FE))),
        Brush.linearGradient(listOf(Color(0xFF43E97B), Color(0xFF38F9D7))),
        Brush.linearGradient(listOf(Color(0xFFFA709A), Color(0xFFFEE140)))
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Browse by Category",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        items(categories) { category ->
            val gradient = gradients[categories.indexOf(category) % gradients.size]
            CategoryCard(
                category = category,
                gradient = gradient,
                onClick = { onCategoryClick(category) }
            )
        }
    }
}

@Composable
private fun CategoryCard(
    category: RadioCategory,
    gradient: Brush,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.align(Alignment.BottomStart)
            ) {
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White
                )
                Text(
                    text = "${category.stations.size} stations • ${category.languages.size} languages",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .align(Alignment.TopEnd)
                    .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
private fun StationListContent(
    category: RadioCategory,
    selectedLanguage: String?,
    onLanguageSelect: (String) -> Unit,
    onPlayStation: (RadioStationDto) -> Unit
) {
    val filteredStations = remember(category, selectedLanguage) {
        if (selectedLanguage.isNullOrBlank()) {
            category.stations
        } else {
            category.stations.filter { station ->
                station.language?.split(",")?.any {
                    it.trim().equals(selectedLanguage, ignoreCase = true)
                } == true
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Language chips
        if (category.languages.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedLanguage == null,
                        onClick = { onLanguageSelect("") },
                        label = { Text("All") }
                    )
                }
                items(category.languages) { lang ->
                    FilterChip(
                        selected = selectedLanguage == lang,
                        onClick = { onLanguageSelect(lang) },
                        label = { Text(lang.replaceFirstChar { it.uppercase() }) }
                    )
                }
            }
            Divider(modifier = Modifier.padding(horizontal = 16.dp))
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 160.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(filteredStations, key = { it.name ?: it.url ?: it.hashCode() }) { station ->
                StationCard(station = station, onPlay = { onPlayStation(station) })
            }
        }
    }
}

@Composable
private fun StationCard(station: RadioStationDto, onPlay: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPlay),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                val favicon = station.favicon
                if (!favicon.isNullOrBlank()) {
                    SubcomposeAsyncImage(
                        model = favicon,
                        contentDescription = station.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        loading = {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = WatermelonRed
                            )
                        },
                        error = {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Play",
                                    tint = WatermelonRed,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = WatermelonRed,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f)),
                                startY = 80f
                            )
                        )
                )
            }
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = station.name ?: "Unknown Station",
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${station.country ?: "Unknown"} • ${station.bitrate}kbps",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
