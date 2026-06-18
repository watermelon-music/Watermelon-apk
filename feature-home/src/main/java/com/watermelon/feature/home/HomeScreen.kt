@file:OptIn(ExperimentalMaterial3Api::class)

package com.watermelon.feature.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.watermelon.core.designsystem.animation.ShimmerCard
import com.watermelon.core.designsystem.theme.WatermelonRed
import com.watermelon.feature.player.PlayerViewModel
import com.watermelon.core.designsystem.theme.WatermelonSpacing
import com.watermelon.domain.model.Song

@Composable
fun HomeScreen(
    onSearchClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onSongClick: (Song, List<Song>) -> Unit = { _, _ -> },
    onPlayerClick: () -> Unit = {},
    playerViewModel: PlayerViewModel,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val showSheet by viewModel.showAddToPlaylistSheet.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val snackbarHostState = remember { SnackbarHostState() }
    val toastMessage by viewModel.addToPlaylistMessage.collectAsStateWithLifecycle()

    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearAddToPlaylistMessage()
        }
    }

    HomeScreenContent(
        uiState = uiState,
        onSearchClick = onSearchClick,
        onSettingsClick = onSettingsClick,
        onSongClick = onSongClick,
        onPlayerClick = onPlayerClick,
        onAddToPlaylist = viewModel::onAddToPlaylistClick,
        snackbarHostState = snackbarHostState
    )

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = viewModel::onDismissAddToPlaylist,
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = "Add to Playlist",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (playlists.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No playlists yet. Create one in Library.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    playlists.forEach { playlist ->
                        ListItem(
                            headlineContent = {
                                Text(
                                    playlist.name,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            leadingContent = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                                    contentDescription = null,
                                    tint = WatermelonRed
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.onPlaylistSelected(playlist.id)
                                }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenContent(
    uiState: HomeUiState,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onSongClick: (Song, List<Song>) -> Unit,
    onPlayerClick: () -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    snackbarHostState: SnackbarHostState
) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Watermelon",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            color = WatermelonRed,
                            letterSpacing = 2.sp
                        )
                    )
                },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            HomeShimmerContent(paddingValues = paddingValues)
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(vertical = WatermelonSpacing.md)
            ) {
                item { SearchBarShortcut(onClick = onSearchClick) }

                if (uiState.trendingMusic.isNotEmpty()) {
                    item {
                        SectionHeader(title = "Trending")
                        BigSongRow(
                            songs = uiState.trendingMusic,
                            onSongClick = onSongClick,
                            onAddToPlaylist = onAddToPlaylist
                        )
                    }
                }

                if (uiState.bollywood.isNotEmpty()) {
                    item {
                        SectionHeader(title = "Bollywood")
                        BigSongRow(
                            songs = uiState.bollywood,
                            onSongClick = onSongClick,
                            onAddToPlaylist = onAddToPlaylist
                        )
                    }
                }

                if (uiState.hollywood.isNotEmpty()) {
                    item {
                        SectionHeader(title = "Hollywood")
                        BigSongRow(
                            songs = uiState.hollywood,
                            onSongClick = onSongClick,
                            onAddToPlaylist = onAddToPlaylist
                        )
                    }
                }

                if (uiState.pop.isNotEmpty()) {
                    item {
                        SectionHeader(title = "Pop")
                        BigSongRow(
                            songs = uiState.pop,
                            onSongClick = onSongClick,
                            onAddToPlaylist = onAddToPlaylist
                        )
                    }
                }

                if (uiState.rock.isNotEmpty()) {
                    item {
                        SectionHeader(title = "Rock")
                        BigSongRow(
                            songs = uiState.rock,
                            onSongClick = onSongClick,
                            onAddToPlaylist = onAddToPlaylist
                        )
                    }
                }

                if (uiState.jazz.isNotEmpty()) {
                    item {
                        SectionHeader(title = "Jazz")
                        BigSongRow(
                            songs = uiState.jazz,
                            onSongClick = onSongClick,
                            onAddToPlaylist = onAddToPlaylist
                        )
                    }
                }

                if (uiState.classical.isNotEmpty()) {
                    item {
                        SectionHeader(title = "Classical")
                        BigSongRow(
                            songs = uiState.classical,
                            onSongClick = onSongClick,
                            onAddToPlaylist = onAddToPlaylist
                        )
                    }
                }

                if (uiState.hiphop.isNotEmpty()) {
                    item {
                        SectionHeader(title = "Hip Hop")
                        BigSongRow(
                            songs = uiState.hiphop,
                            onSongClick = onSongClick,
                            onAddToPlaylist = onAddToPlaylist
                        )
                    }
                }

                if (uiState.electronic.isNotEmpty()) {
                    item {
                        SectionHeader(title = "Electronic")
                        BigSongRow(
                            songs = uiState.electronic,
                            onSongClick = onSongClick,
                            onAddToPlaylist = onAddToPlaylist
                        )
                    }
                }

                if (uiState.recentlyPlayed.isNotEmpty()) {
                    item {
                        SectionHeader(title = "Recently Played")
                        SongRow(
                            songs = uiState.recentlyPlayed,
                            onSongClick = onSongClick,
                            onAddToPlaylist = onAddToPlaylist
                        )
                    }
                }

                if (uiState.favorites.isNotEmpty()) {
                    item {
                        SectionHeader(title = "Favorites")
                        SongRow(
                            songs = uiState.favorites,
                            onSongClick = onSongClick,
                            onAddToPlaylist = onAddToPlaylist
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeShimmerContent(paddingValues: PaddingValues) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = WatermelonSpacing.md),
        verticalArrangement = Arrangement.spacedBy(WatermelonSpacing.lg)
    ) {
        ShimmerCard(height = 56.dp)
        repeat(4) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(WatermelonSpacing.md)) {
                items(4) {
                    ShimmerCard(modifier = Modifier.width(170.dp), height = 200.dp)
                }
            }
        }
    }
}

@Composable
private fun SearchBarShortcut(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = WatermelonSpacing.md)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(WatermelonSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(WatermelonSpacing.md))
            Text(
                text = "Search songs, artists...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}




@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(
            start = WatermelonSpacing.md,
            top = WatermelonSpacing.lg,
            bottom = WatermelonSpacing.sm
        ),
        color = MaterialTheme.colorScheme.onBackground
    )
}

@Composable
private fun BigSongRow(
    songs: List<Song>,
    onSongClick: (Song, List<Song>) -> Unit,
    onAddToPlaylist: (Song) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = WatermelonSpacing.md),
        horizontalArrangement = Arrangement.spacedBy(WatermelonSpacing.md)
    ) {
        items(songs, key = { it.id }) { song ->
            BigSongItem(
                song = song,
                onClick = { onSongClick(song, songs) },
                onAddToPlaylist = { onAddToPlaylist(song) }
            )
        }
    }
}

@Composable
private fun BigSongItem(song: Song, onClick: () -> Unit, onAddToPlaylist: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { it / 4 }
    ) {
        Column(
            modifier = Modifier.width(170.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Card(
                modifier = Modifier
                    .size(170.dp)
                    .clickable(onClick = onClick),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(6.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AsyncImage(
                        model = song.coverUrl?.takeIf { it.isNotBlank() }
                            ?: com.watermelon.core.designsystem.R.drawable.app_logo,
                        contentDescription = song.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                                    startY = 80f
                                )
                            )
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp),
                        contentAlignment = Alignment.TopEnd
                    ) {
                        IconButton(
                            onClick = onAddToPlaylist,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                                contentDescription = "Add to Playlist",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp),
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        FilledIconButton(
                            onClick = onClick,
                            modifier = Modifier.size(40.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = WatermelonRed,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = "Play",
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(WatermelonSpacing.sm))
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = song.artistName,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SongRow(
    songs: List<Song>,
    onSongClick: (Song, List<Song>) -> Unit,
    onAddToPlaylist: (Song) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = WatermelonSpacing.md),
        horizontalArrangement = Arrangement.spacedBy(WatermelonSpacing.md)
    ) {
        items(songs, key = { it.id }) { song ->
            SongItem(
                song = song,
                onClick = { onSongClick(song, songs) },
                onAddToPlaylist = { onAddToPlaylist(song) }
            )
        }
    }
}

@Composable
private fun SongItem(song: Song, onClick: () -> Unit, onAddToPlaylist: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { it / 4 }
    ) {
        Column(
            modifier = Modifier.width(130.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Card(
                modifier = Modifier
                    .size(130.dp)
                    .clickable(onClick = onClick),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AsyncImage(
                        model = song.coverUrl?.takeIf { it.isNotBlank() }
                            ?: com.watermelon.core.designsystem.R.drawable.app_logo,
                        contentDescription = song.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(6.dp),
                        contentAlignment = Alignment.TopEnd
                    ) {
                        IconButton(
                            onClick = onAddToPlaylist,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                                contentDescription = "Add to Playlist",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(WatermelonSpacing.sm))
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = song.artistName,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
