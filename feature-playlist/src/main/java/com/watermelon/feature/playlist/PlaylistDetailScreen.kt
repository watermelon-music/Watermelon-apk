package com.watermelon.feature.playlist

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MusicNote
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
    val currentUserId by viewModel.currentUserId.collectAsStateWithLifecycle()
    LaunchedEffect(playlistId) { viewModel.loadPlaylist(playlistId) }
    val context = LocalContext.current
    var songToDelete by remember { mutableStateOf<PlaylistSong?>(null) }

    val songs = playlist?.songs ?: emptyList()
    val isOwned = playlist?.ownerId == currentUserId

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
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "Check out this awesome playlist on Watermelon! 🍉\n\nhttps://watermelon-api-oxx2.onrender.com/playlist/$playlistId"
                            )
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Playlist"))
                    }) {
                        Icon(Icons.Filled.Share, contentDescription = "Share")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // ── Cover Mosaic Header ──────────────────────────────
            item {
                PlaylistCoverHeader(
                    playlist = playlist,
                    playlistId = playlistId,
                    songs = songs,
                    isOwned = isOwned,
                    onImportClick = {
                        playlist?.let { viewModel.importPlaylist(it) }
                        Toast.makeText(context, "Playlist imported to library! 🍉", Toast.LENGTH_SHORT).show()
                    },
                    onPlayAllClick = onPlayAllClick,
                    onShuffleClick = onShuffleClick
                )
            }

            // ── Songs list label ──────────────────────────────────
            if (songs.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = WatermelonSpacing.md, vertical = WatermelonSpacing.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Songs",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${songs.size} tracks",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ── Empty state ──────────────────────────────────────
            if (songs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                contentDescription = null,
                                modifier = Modifier.size(72.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No songs yet",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Add songs from Search or Home",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }

            // ── Song rows with index ─────────────────────────────
            itemsIndexed(songs, key = { _, song -> song.songId }) { index, song ->
                PlaylistSongItem(
                    index = index + 1,
                    song = song,
                    onClick = {
                        val allSongs = songs.map { it.toSong() }
                        onSongClick(song.toSong(), allSongs)
                    },
                    onDelete = if (isOwned) { { songToDelete = song } } else null
                )
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
                    songToDelete?.let { viewModel.removeSong(playlistId, it.songId) }
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

@Composable
private fun PlaylistCoverHeader(
    playlist: com.watermelon.domain.model.Playlist?,
    playlistId: String,
    songs: List<PlaylistSong>,
    isOwned: Boolean,
    onImportClick: () -> Unit,
    onPlayAllClick: (List<Song>) -> Unit,
    onShuffleClick: (List<Song>) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
    ) {
        // Cover mosaic background
        val covers = songs.mapNotNull { it.coverUrl }.take(4)
        when {
            covers.isEmpty() -> {
                AsyncImage(
                    model = "https://picsum.photos/seed/$playlistId/600/600",
                    contentDescription = "Playlist cover",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            covers.size == 1 -> {
                AsyncImage(
                    model = covers[0],
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            covers.size == 2 -> {
                Row(modifier = Modifier.fillMaxSize()) {
                    AsyncImage(model = covers[0], contentDescription = null, modifier = Modifier.weight(1f).fillMaxHeight(), contentScale = ContentScale.Crop)
                    AsyncImage(model = covers[1], contentDescription = null, modifier = Modifier.weight(1f).fillMaxHeight(), contentScale = ContentScale.Crop)
                }
            }
            covers.size == 3 -> {
                Row(modifier = Modifier.fillMaxSize()) {
                    AsyncImage(model = covers[0], contentDescription = null, modifier = Modifier.weight(1f).fillMaxHeight(), contentScale = ContentScale.Crop)
                    Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        AsyncImage(model = covers[1], contentDescription = null, modifier = Modifier.weight(1f).fillMaxWidth(), contentScale = ContentScale.Crop)
                        AsyncImage(model = covers[2], contentDescription = null, modifier = Modifier.weight(1f).fillMaxWidth(), contentScale = ContentScale.Crop)
                    }
                }
            }
            else -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        AsyncImage(model = covers[0], contentDescription = null, modifier = Modifier.weight(1f).fillMaxHeight(), contentScale = ContentScale.Crop)
                        AsyncImage(model = covers[1], contentDescription = null, modifier = Modifier.weight(1f).fillMaxHeight(), contentScale = ContentScale.Crop)
                    }
                    Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        AsyncImage(model = covers[2], contentDescription = null, modifier = Modifier.weight(1f).fillMaxHeight(), contentScale = ContentScale.Crop)
                        AsyncImage(model = covers[3], contentDescription = null, modifier = Modifier.weight(1f).fillMaxHeight(), contentScale = ContentScale.Crop)
                    }
                }
            }
        }

        // Gradient overlay for readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.15f),
                            Color.Black.copy(alpha = 0.85f)
                        ),
                        startY = 80f
                    )
                )
        )

        // Playlist info overlay
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(20.dp)
        ) {
            // Waveform code
            playlist?.id?.let { pid ->
                val blocks = listOf('▁', '▂', '▃', '▄', '▅', '▆', '▇', '█')
                val code = remember(pid) {
                    val random = kotlin.random.Random(pid.hashCode())
                    buildString { for (i in 0 until 10) append(blocks[random.nextInt(blocks.size)]) }
                }
                Text(
                    text = code,
                    style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.6f)),
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            Text(
                text = playlist?.name ?: "Playlist",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${songs.size} songs",
                style = MaterialTheme.typography.bodyMedium.copy(color = Color.White.copy(alpha = 0.75f))
            )
            if (!isOwned && playlist != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onImportClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WatermelonRed,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(
                        text = "Add to Library",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }

        // Play + Shuffle buttons (bottom right)
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Shuffle
            OutlinedIconButton(
                onClick = {
                    val songsList = songs.map { it.toSong() }
                    if (songsList.isNotEmpty()) onShuffleClick(songsList)
                },
                modifier = Modifier.size(48.dp),
                enabled = songs.isNotEmpty(),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(Color.White.copy(alpha = 0.6f))
                )
            ) {
                Icon(
                    Icons.Filled.Shuffle,
                    contentDescription = "Shuffle",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
            // Play all
            FilledIconButton(
                onClick = {
                    val songsList = songs.map { it.toSong() }
                    if (songsList.isNotEmpty()) onPlayAllClick(songsList)
                },
                modifier = Modifier.size(56.dp),
                enabled = songs.isNotEmpty(),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = WatermelonRed,
                    contentColor = Color.White
                ),
                shape = CircleShape
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = "Play All", modifier = Modifier.size(28.dp))
            }
        }
    }
}

private fun PlaylistSong.toSong(): Song = Song(
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

@Composable
private fun PlaylistSongItem(
    index: Int,
    song: PlaylistSong,
    onClick: () -> Unit,
    onDelete: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = WatermelonSpacing.md, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Index number
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$index",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (index % 2 == 0)
                        MaterialTheme.colorScheme.onSurfaceVariant
                    else WatermelonRed
                )
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Cover art
        AsyncImage(
            model = song.coverUrl ?: com.watermelon.core.designsystem.R.drawable.app_logo,
            contentDescription = song.title,
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(10.dp)),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Title + artist
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title.takeIf { it.isNotBlank() } ?: "Unknown",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = song.artist.takeIf { it.isNotBlank() } ?: "Unknown Artist",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Delete button
        if (onDelete != null) {
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Remove",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }

    HorizontalDivider(
        modifier = Modifier.padding(start = (36 + 12 + 52 + 12 + 8).dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    )
}
