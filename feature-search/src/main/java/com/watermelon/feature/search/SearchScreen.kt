package com.watermelon.feature.search

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.watermelon.core.designsystem.layout.adaptiveHorizontalPadding
import com.watermelon.core.designsystem.theme.WatermelonRed
import com.watermelon.core.designsystem.theme.WatermelonSpacing
import com.watermelon.domain.model.Artist
import com.watermelon.domain.model.CommunityPlaylist
import com.watermelon.domain.model.Song

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onBackClick: () -> Unit = {},
    onSongClick: (Song, Int, List<Song>) -> Unit = { _, _, _ -> },
    onArtistClick: (Artist) -> Unit = {}
) {
    val viewModel: SearchViewModel = hiltViewModel()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()
    val artists by viewModel.artistResults.collectAsStateWithLifecycle()
    val playlistResults by viewModel.playlistResults.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val playPlaylistEvent by viewModel.playPlaylistEvent.collectAsStateWithLifecycle()

    LaunchedEffect(playPlaylistEvent) {
        playPlaylistEvent?.let { songs ->
            if (results.isNotEmpty()) {
                onSongClick(songs.first(), 0, songs)
            }
            viewModel.clearPlayPlaylistEvent()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Search", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search songs, artists, playlists...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            // Category chips
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val labels = mapOf(
                    SearchCategory.ALL to "All",
                    SearchCategory.SONGS to "Songs",
                    SearchCategory.PLAYLISTS to "Playlists",
                    SearchCategory.ARTISTS to "Artists"
                )
                SearchCategory.entries.forEach { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { viewModel.onCategorySelected(category) },
                        label = { Text(labels[category] ?: "") }
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            when (selectedCategory) {
                SearchCategory.ALL -> AllResultsView(
                    songs = results,
                    artists = artists,
                    playlists = playlistResults,
                    onSongClick = onSongClick,
                    onArtistClick = onArtistClick,
                    onPlaylistClick = { viewModel.onPlaylistClick(it) },
                    onSavePlaylist = { viewModel.onSavePlaylist(it) },
                    query = query
                )
                SearchCategory.SONGS -> SongsGridView(
                    songs = results,
                    onSongClick = { index -> onSongClick(songs[index], index, songs) }
                )
                SearchCategory.PLAYLISTS -> PlaylistsGridView(
                    playlists = playlistResults,
                    onPlaylistClick = { viewModel.onPlaylistClick(it) },
                    onSave = { viewModel.onSavePlaylist(it) }
                )
                SearchCategory.ARTISTS -> ArtistsGridView(
                    artists = artists,
                    onArtistClick = onArtistClick
                )
            }
        }
    }
}

@Composable
private fun AllResultsView(
    songs: List<Song>,
    artists: List<Artist>,
    playlists: List<CommunityPlaylist>,
    onSongClick: (Song, Int, List<Song>) -> Unit,
    onArtistClick: (Artist) -> Unit,
    onPlaylistClick: (CommunityPlaylist) -> Unit,
    onSavePlaylist: (CommunityPlaylist) -> Unit,
    query: String
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        if (artists.isNotEmpty()) {
            item {
                Text("Artists", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(artists.take(10)) { artist ->
                        CompactArtistCard(artist = artist, onClick = { onArtistClick(artist) })
                    }
                }
            }
        }
        if (playlistResults.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Playlists", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(playlists.take(10)) { playlist ->
                        PlaylistResultItem(
                            playlist = playlist,
                            onClick = { onPlaylistClick(playlist) },
                            onSave = { onSavePlaylist(playlist) }
                        )
                    }
                }
            }
        }
        if (results.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Songs", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
            }
            items(results, key = { it.id }) { song ->
                SongListItem(
                    song = song,
                    onClick = { onSongClick(song, results.indexOf(song), songs) }
                )
            }
        }
        if (query.isNotBlank() && songs.isEmpty() && artists.isEmpty() && playlists.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No results found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun SongsGridView(songs: List<Song>, onSongClick: (Int) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(results, key = { it.id }) { song ->
            val index = results.indexOf(song)
            SongGridItem(song = song, onClick = { onSongClick(index) })
        }
    }
}

@Composable
private fun PlaylistsGridView(
    playlists: List<CommunityPlaylist>,
    onPlaylistClick: (CommunityPlaylist) -> Unit,
    onSave: (CommunityPlaylist) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(playlistResults, key = { it.id }) { playlist ->
            PlaylistResultItem(
                playlist = playlist,
                onClick = { onPlaylistClick(playlist) },
                onSave = { onSave(playlist) }
            )
        }
    }
}

@Composable
private fun ArtistsGridView(artists: List<Artist>, onArtistClick: (Artist) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(artists, key = { it.id }) { artist ->
            CompactArtistCard(artist = artist, onClick = { onArtistClick(artist) })
        }
    }
}

@Composable
private fun CompactArtistCard(
    artist: Artist,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(100.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = artist.imageUrl?.takeIf { it.isNotBlank() }
                ?: com.watermelon.core.designsystem.R.drawable.app_logo,
            contentDescription = artist.name,
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            artist.name,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SongListItem(
    song: Song,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = song.coverUrl,
            contentDescription = song.title,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                song.title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                song.artistName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
        IconButton(onClick = onClick) {
            Icon(Icons.Default.PlayArrow, contentDescription = "Play")
        }
    }
}

@Composable
private fun SongGridItem(song: Song, onClick: () -> Unit) {
    Column(
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Card(
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(2.dp),
            modifier = Modifier.aspectRatio(1f)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = song.coverUrl,
                    contentDescription = song.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f))
                            )
                        )
                )
                IconButton(
                    onClick = onClick,
                    modifier = Modifier.align(Alignment.BottomEnd)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.White)
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            song.title,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            song.artistName,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

@Composable
private fun PlaylistResultItem(
    playlist: CommunityPlaylist,
    onClick: () -> Unit,
    onSave: () -> Unit = {}
) {
    val cardWidth = maxOf(148.dp, (LocalConfiguration.current.screenWidthDp.dp - 48.dp) / 2.3f)
    Column(
        modifier = Modifier.width(cardWidth)
    ) {
        Card(
            modifier = Modifier
                .size(cardWidth)
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = playlist.coverUrl?.takeIf { it.isNotBlank() }
                        ?: com.watermelon.core.designsystem.R.drawable.app_logo,
                    contentDescription = playlist.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))
                            )
                        )
                )
                IconButton(
                    onClick = onSave,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(28.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.PlaylistAdd,
                        contentDescription = "Save",
                        tint = Color.White
                    )
                }
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp)
                ) {
                    Text(
                        text = playlist.name,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${playlist.songCount} songs",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            playlist.name,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            "by ${playlist.creatorDisplayName}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}
