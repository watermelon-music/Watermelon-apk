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
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.*
import androidx.compose.material3.IconButtonDefaults
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
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.watermelon.core.designsystem.theme.WatermelonRed
import com.watermelon.core.designsystem.theme.WatermelonSpacing
import com.watermelon.domain.model.PlaylistSong
import com.watermelon.domain.model.Song

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    playlistId: String,
    onBackClick: () -> Unit,
    onSongClick: (Song, List<Song>) -> Unit,
    onShuffleClick: (List<Song>) -> Unit = {},
    onPlayAllClick: (List<Song>) -> Unit = {},
    viewModel: PlaylistDetailViewModel = hiltViewModel()
) {
    val playlist by viewModel.playlist.collectAsStateWithLifecycle()
    LaunchedEffect(playlistId) { viewModel.loadPlaylist(playlistId) }
    val context = LocalContext.current
    var songToDelete by remember { mutableStateOf<PlaylistSong?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                            tint = WatermelonRed
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = playlist?.name ?: "Playlist",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val link = playlistId
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Playlist ID", link))
                        Toast.makeText(context, "Playlist ID copied to clipboard!", Toast.LENGTH_SHORT).show()
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
                    val covers = playlist?.songs?.mapNotNull { it.coverUrl }?.take(4) ?: emptyList()
                    when {
                        covers.isEmpty() -> {
                            AsyncImage(
                                model = "https://picsum.photos/seed/$playlistId/400/400",
                                contentDescription = "Playlist cover",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        covers.size == 1 -> {
                            AsyncImage(model = covers[0], contentDescription = null, modifier = Modifier.fillMaxSize())
                        }
                        covers.size == 2 -> {
                            Row(modifier = Modifier.fillMaxSize()) {
                                AsyncImage(model = covers[0], contentDescription = null, modifier = Modifier.weight(1f).fillMaxHeight())
                                AsyncImage(model = covers[1], contentDescription = null, modifier = Modifier.weight(1f).fillMaxHeight())
                            }
                        }
                        covers.size == 3 -> {
                            Row(modifier = Modifier.fillMaxSize()) {
                                AsyncImage(model = covers[0], contentDescription = null, modifier = Modifier.weight(1f).fillMaxHeight())
                                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                    AsyncImage(model = covers[1], contentDescription = null, modifier = Modifier.weight(1f).fillMaxWidth())
                                    AsyncImage(model = covers[2], contentDescription = null, modifier = Modifier.weight(1f).fillMaxWidth())
                                }
                            }
                        }
                        else -> {
                            Column(modifier = Modifier.fillMaxSize()) {
                                Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                    AsyncImage(model = covers[0], contentDescription = null, modifier = Modifier.weight(1f).fillMaxHeight())
                                    AsyncImage(model = covers[1], contentDescription = null, modifier = Modifier.weight(1f).fillMaxHeight())
                                }
                                Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                    AsyncImage(model = covers[2], contentDescription = null, modifier = Modifier.weight(1f).fillMaxHeight())
                                    AsyncImage(model = covers[3], contentDescription = null, modifier = Modifier.weight(1f).fillMaxHeight())
                                }
                            }
                        }
                    }
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
                        playlist?.id?.let { pid ->
                            Spacer(modifier = Modifier.height(8.dp))
                            val blocks = listOf('▁', '▂', '▃', '▄', '▅', '▆', '▇', '█')
                            val code = remember(pid) {
                                val random = kotlin.random.Random(pid.hashCode())
                                buildString {
                                    for (i in 0 until 16) {
                                        append(blocks[random.nextInt(blocks.size)])
                                    }
                                }
                            }
                            Text(
                                text = code,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimary,
                                letterSpacing = 2.sp
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(WatermelonSpacing.md)
            ) {
                FilledIconButton(
                    onClick = {
                        val songs = playlist?.songs?.map { it.toSong() } ?: emptyList()
                        if (songs.isNotEmpty()) {
                            onPlayAllClick(songs)
                        }
                    },
                    modifier = Modifier.size(48.dp),
                    enabled = !playlist?.songs.isNullOrEmpty(),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = WatermelonRed
                    )
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = "Play All", modifier = Modifier.size(24.dp))
                }
                OutlinedIconButton(
                    onClick = {
                        val songs = playlist?.songs?.map { it.toSong() } ?: emptyList()
                        if (songs.isNotEmpty()) {
                            onShuffleClick(songs)
                        }
                    },
                    modifier = Modifier.size(48.dp),
                    enabled = !playlist?.songs.isNullOrEmpty()
                ) {
                    Icon(Icons.Filled.Shuffle, contentDescription = "Shuffle", modifier = Modifier.size(24.dp))
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
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No songs yet",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Add songs from search or home",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(WatermelonSpacing.md)
                ) {
                    items(songs, key = { it.songId }) { song ->
                        PlaylistSongItem(
                            song = song,
                            onClick = { 
                                val allSongs = songs.map { it.toSong() }
                                onSongClick(song.toSong(), allSongs) 
                            },
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
                    ?: com.watermelon.core.designsystem.R.drawable.app_logo,
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
