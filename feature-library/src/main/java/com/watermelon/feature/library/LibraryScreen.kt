package com.watermelon.feature.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.watermelon.core.designsystem.theme.WatermelonSpacing
import com.watermelon.domain.model.Playlist
import com.watermelon.domain.model.Song

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onBackClick: () -> Unit,
    onPlaylistClick: (Playlist) -> Unit,
    onSongClick: (Song) -> Unit,
    onCreatePlaylist: () -> Unit = {},
    onNavigateToPremium: () -> Unit = {},
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val recentlyPlayed by viewModel.recentlyPlayed.collectAsStateWithLifecycle()
    val canCreate by viewModel.canCreatePlaylist.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
    var showPaywall by remember { mutableStateOf(false) }
    val tabs = listOf("Playlists", "Favorites", "Feed")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Your Library") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = {
                        if (canCreate) onCreatePlaylist() else showPaywall = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Create Playlist",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            when (selectedTab) {
                0 -> PlaylistList(
                    playlists = playlists,
                    onPlaylistClick = onPlaylistClick,
                    modifier = Modifier.fillMaxSize()
                )
                1 -> SongList(
                    songs = favorites,
                    onSongClick = onSongClick,
                    emptyText = "No favorites yet",
                    modifier = Modifier.fillMaxSize()
                )
                2 -> FeedContent(
                    recentlyPlayed = recentlyPlayed,
                    onSongClick = onSongClick,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    if (showPaywall) {
        AlertDialog(
            onDismissRequest = { showPaywall = false },
            title = { Text("Playlist Limit Reached") },
            text = {
                Text("Free users can create up to 3 playlists. Upgrade to Premium for unlimited playlists.")
            },
            confirmButton = {
                TextButton(onClick = {
                    showPaywall = false
                    onNavigateToPremium()
                }) {
                    Text("Go Premium")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPaywall = false }) {
                    Text("Maybe Later")
                }
            }
        )
    }
}

@Composable
private fun PlaylistList(
    playlists: List<Playlist>,
    onPlaylistClick: (Playlist) -> Unit,
    modifier: Modifier = Modifier
) {
    if (playlists.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(
                text = "No playlists yet",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            modifier = modifier.padding(WatermelonSpacing.md),
            verticalArrangement = Arrangement.spacedBy(WatermelonSpacing.md)
        ) {
            items(playlists, key = { it.id }) { playlist ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPlaylistClick(playlist) },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(WatermelonSpacing.md),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = playlist.coverUrl,
                            contentDescription = playlist.name,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.width(WatermelonSpacing.md))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = playlist.name,
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = playlist.description ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SongList(
    songs: List<Song>,
    onSongClick: (Song) -> Unit,
    emptyText: String,
    modifier: Modifier = Modifier
) {
    if (songs.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(
                text = emptyText,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            modifier = modifier.padding(WatermelonSpacing.md),
            verticalArrangement = Arrangement.spacedBy(WatermelonSpacing.md)
        ) {
            items(songs, key = { it.id }) { song ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSongClick(song) },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(WatermelonSpacing.md),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = song.coverUrl,
                            contentDescription = song.title,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.width(WatermelonSpacing.md))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = song.title,
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = song.artistName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeedContent(
    recentlyPlayed: List<Song>,
    onSongClick: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.padding(WatermelonSpacing.md),
        verticalArrangement = Arrangement.spacedBy(WatermelonSpacing.md)
    ) {
        item {
            Text(
                text = "Recently Played",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = WatermelonSpacing.sm)
            )
        }
        if (recentlyPlayed.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Play some music to see your activity",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(recentlyPlayed, key = { it.id }) { song ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSongClick(song) },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(WatermelonSpacing.md),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = song.coverUrl,
                            contentDescription = song.title,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.width(WatermelonSpacing.md))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = song.title,
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = song.artistName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
