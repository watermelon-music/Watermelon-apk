package com.watermelon.feature.playlist

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.watermelon.core.designsystem.theme.WatermelonSpacing
import com.watermelon.domain.model.PlaylistSong
import com.watermelon.domain.model.Song

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    playlistId: String,
    onBackClick: () -> Unit,
    onSongClick: (Song) -> Unit,
    onShuffleClick: () -> Unit = {},
    onPlayAllClick: () -> Unit = {},
    viewModel: PlaylistDetailViewModel = hiltViewModel()
) {
    val playlist by viewModel.playlist.collectAsStateWithLifecycle()
    LaunchedEffect(playlistId) { viewModel.loadPlaylist(playlistId) }
    val context = LocalContext.current
    var songToDelete by remember { mutableStateOf<PlaylistSong?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(playlist?.name ?: "Playlist") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val ids = playlist?.songs?.joinToString(",") { it.songId } ?: ""
                        val link = "watermelon://playlist?songs=$ids"
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Playlist", link))
                        Toast.makeText(context, "Playlist link copied!", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Filled.Share, contentDescription = "Share")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(WatermelonSpacing.md)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = WatermelonSpacing.md),
                shape = RoundedCornerShape(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val cover = playlist?.coverUrl
                        ?: playlist?.songs?.firstOrNull()?.coverUrl
                        ?: "https://picsum.photos/seed/$playlistId/400/400"
                    AsyncImage(
                        model = cover,
                        contentDescription = "Playlist cover",
                        modifier = Modifier.fillMaxSize()
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Text(
                            text = playlist?.name ?: "Playlist",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Text(
                            text = "${playlist?.songs?.size ?: 0} songs",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(WatermelonSpacing.md)
            ) {
                Button(
                    onClick = {
                        val songs = playlist?.songs?.map { it.toSong() } ?: emptyList()
                        if (songs.isNotEmpty()) {
                            onPlayAllClick()
                            // Trigger playback via a callback or global player
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !playlist?.songs.isNullOrEmpty()
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Play All")
                }
                OutlinedButton(
                    onClick = onShuffleClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !playlist?.songs.isNullOrEmpty()
                ) {
                    Icon(Icons.Filled.Shuffle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Shuffle")
                }
            }

            Spacer(modifier = Modifier.height(WatermelonSpacing.md))

            Text(
                text = "Songs",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = WatermelonSpacing.md)
            )

            val songs = playlist?.songs ?: emptyList()
            if (songs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No songs yet. Add songs from search or home.")
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(WatermelonSpacing.md)
                ) {
                    items(songs, key = { it.songId }) { song ->
                        PlaylistSongItem(
                            song = song,
                            onClick = { onSongClick(song.toSong()) },
                            onDelete = { songToDelete = song }
                        )
                    }
                }
            }
        }
    }

    if (songToDelete != null) {
        AlertDialog(
            onDismissRequest = { songToDelete = null },
            title = { Text("Remove Song") },
            text = { Text("Remove \"${songToDelete?.title}\" from this playlist?") },
            confirmButton = {
                TextButton(onClick = {
                    songToDelete?.let {
                        viewModel.removeSong(playlistId, it.songId)
                    }
                    songToDelete = null
                }) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { songToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun PlaylistSong.toSong(): Song {
    return Song(
        id = songId,
        title = title,
        artistId = "",
        artistName = artist,
        albumId = null,
        albumName = null,
        durationMs = 0L,
        coverUrl = coverUrl,
        audioUrl = audioUrl,
        genre = "",
        releaseDate = ""
    )
}

@Composable
private fun PlaylistSongItem(
    song: PlaylistSong,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = song.coverUrl
                    ?: "https://via.placeholder.com/48?text=♪",
                contentDescription = song.title,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
            Spacer(modifier = Modifier.width(WatermelonSpacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title.takeIf { it.isNotBlank() } ?: "Unknown",
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist.takeIf { it.isNotBlank() } ?: "Unknown Artist",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Remove",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
