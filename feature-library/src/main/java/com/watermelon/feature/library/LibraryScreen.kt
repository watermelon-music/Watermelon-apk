package com.watermelon.feature.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.grid.itemsIndexed as gridItemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import android.content.Intent
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import android.os.Environment
import com.watermelon.core.designsystem.animation.ShimmerCard
import com.watermelon.core.designsystem.layout.adaptiveHorizontalPadding
import com.watermelon.core.designsystem.layout.adaptiveListMinSize
import com.watermelon.core.designsystem.layout.adaptiveMaxContentWidth
import com.watermelon.core.designsystem.theme.WatermelonSpacing
import com.watermelon.domain.model.Playlist
import com.watermelon.domain.model.Song
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onBackClick: () -> Unit,
    onPlaylistClick: (Playlist) -> Unit,
    onSongClick: (Song, List<Song>) -> Unit,
    onCreatePlaylist: () -> Unit = {},
    onNavigateToPremium: () -> Unit = {},
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val recentlyPlayed by viewModel.recentlyPlayed.collectAsStateWithLifecycle()
    val downloadedSongs by viewModel.downloadedSongs.collectAsStateWithLifecycle()
    val canCreate by viewModel.canCreatePlaylist.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val createMessage by viewModel.createMessage.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by remember { mutableIntStateOf(0) }
    var showPaywall by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(createMessage) {
        createMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearCreateMessage()
        }
    }
    val tabs = listOf("Playlists", "Favorites", "Feed", "Downloads")
    val tabIcons = listOf(
        Icons.AutoMirrored.Filled.QueueMusic,
        Icons.Filled.Favorite,
        Icons.Filled.History,
        Icons.Filled.Download
    )

    var showDeleteDialog by rememberSaveable { mutableStateOf<Playlist?>(null) }
    var showEditDialog by rememberSaveable { mutableStateOf<Playlist?>(null) }
    var showQrDialog by rememberSaveable { mutableStateOf<Playlist?>(null) }
    var shareMessage by rememberSaveable { mutableStateOf<String?>(null) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
    ) { paddingValues ->
        LibraryScreenContent(
            paddingValues = paddingValues,
            selectedTab = selectedTab,
            isLoading = isLoading,
            playlists = playlists,
            favorites = favorites,
            recentlyPlayed = recentlyPlayed,
            downloadedSongs = downloadedSongs,
            onPlaylistClick = onPlaylistClick,
            onSongClick = onSongClick,
            onDeleteDownload = { viewModel.deleteDownload(it.id) },
            onPlayPlaylist = { playlist -> 
                // Just map it and trigger the click on the first song to queue the whole playlist
                val songs = playlist.songs.map { 
                    Song(
                        id = it.songId,
                        title = it.title,
                        artistId = "",
                        artistName = it.artist,
                        albumId = null,
                        albumName = null,
                        durationMs = 0L,
                        coverUrl = it.coverUrl,
                        audioUrl = it.audioUrl,
                        genre = "",
                        releaseDate = ""
                    ) 
                }
                if (songs.isNotEmpty()) {
                    onSongClick(songs.first(), songs)
                }
            },
            onShufflePlaylist = { onPlaylistClick(it) },
            onSharePlaylist = { playlist ->
                viewModel.sharePlaylist(playlist.id) { code ->
                    shareMessage = "Share code: $code"
                    val shareText = playlist.id
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, shareText)
                    }
                    val shareIntent = Intent.createChooser(sendIntent, "Share Playlist")
                    context.startActivity(shareIntent)
                }
            },
            onEditPlaylist = { showEditDialog = it },
            onShowQr = { showQrDialog = it },
            onDeletePlaylist = { showDeleteDialog = it },
            onTabSelected = { selectedTab = it },
            tabs = tabs,
            tabIcons = tabIcons
        )
    }

    if (showDeleteDialog != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Delete Playlist") },
            text = { Text("Delete \"${showDeleteDialog?.name}\"? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog?.let { viewModel.deletePlaylist(it.id) }
                    showDeleteDialog = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showPaywall) {
        AlertDialog(
            onDismissRequest = { showPaywall = false },
            title = { Text("Playlist Limit Reached") },
            text = {
                Text("Free users can create up to 2 playlists. Upgrade to Premium to create up to 5 playlists.")
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

    if (showEditDialog != null) {
        var editName by rememberSaveable { mutableStateOf(showEditDialog?.name ?: "") }
        var editDesc by rememberSaveable { mutableStateOf(showEditDialog?.description ?: "") }
        AlertDialog(
            onDismissRequest = { showEditDialog = null },
            title = { Text("Edit Playlist") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = editName, onValueChange = { editName = it }, label = { Text("Name") })
                    OutlinedTextField(value = editDesc, onValueChange = { editDesc = it }, label = { Text("Description") })
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showEditDialog?.let { viewModel.editPlaylist(it.id, editName, editDesc.takeIf { it.isNotBlank() }) }
                    showEditDialog = null
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    shareMessage?.let { msg ->
        LaunchedEffect(msg) {
            kotlinx.coroutines.delay(3000)
            shareMessage = null
        }
        Snackbar(
            modifier = Modifier.padding(16.dp),
            action = {
                TextButton(onClick = { shareMessage = null }) {
                    Text("Dismiss")
                }
            }
        ) { Text(msg) }
    }

    showQrDialog?.let { playlist ->
        val deepLink = "https://watermelon.app/playlist/${playlist.id}"
        AlertDialog(
            onDismissRequest = { showQrDialog = null },
            title = { Text("${playlist.name} — QR Code") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val bitmap = remember(deepLink) { generateQrBitmap(deepLink, 512) }
                    bitmap?.let {
                        androidx.compose.foundation.Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = "QR Code",
                            modifier = Modifier.size(200.dp)
                        )
                    } ?: Text("Failed to generate QR")
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(deepLink, style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "image/png"
                        val path = android.provider.MediaStore.Images.Media.insertImage(
                            context.contentResolver, generateQrBitmap(deepLink, 512), "qr_${playlist.id}", null
                        )
                        putExtra(Intent.EXTRA_STREAM, android.net.Uri.parse(path))
                        putExtra(Intent.EXTRA_TEXT, "Scan this QR to open ${playlist.name} on Watermelon\n$deepLink")
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Share QR"))
                }) {
                    Text("Share")
                }
            },
            dismissButton = {
                TextButton(onClick = { showQrDialog = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun LibraryScreenContent(
    paddingValues: PaddingValues,
    selectedTab: Int,
    isLoading: Boolean,
    playlists: List<Playlist>,
    favorites: List<Song>,
    recentlyPlayed: List<Song>,
    downloadedSongs: List<Song>,
    onPlaylistClick: (Playlist) -> Unit,
    onSongClick: (Song, List<Song>) -> Unit,
    onPlayPlaylist: (Playlist) -> Unit,
    onShufflePlaylist: (Playlist) -> Unit,
    onSharePlaylist: (Playlist) -> Unit,
    onEditPlaylist: (Playlist) -> Unit,
    onShowQr: (Playlist) -> Unit,
    onDeletePlaylist: (Playlist) -> Unit,
    onDeleteDownload: (Song) -> Unit,
    onTabSelected: (Int) -> Unit,
    tabs: List<String>,
    tabIcons: List<androidx.compose.ui.graphics.vector.ImageVector>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { onTabSelected(index) },
                    icon = {
                        Icon(
                            imageVector = tabIcons[index],
                            contentDescription = title,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                )
            }
        }

        when (selectedTab) {
            0 -> if (isLoading && playlists.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    repeat(5) { ShimmerCard(height = 72.dp) }
                }
            } else PlaylistList(
                playlists = playlists,
                onPlaylistClick = onPlaylistClick,
                onPlayPlaylist = onPlayPlaylist,
                onShufflePlaylist = onShufflePlaylist,
                onSharePlaylist = onSharePlaylist,
                onEditPlaylist = onEditPlaylist,
                onShowQr = onShowQr,
                onDeletePlaylist = onDeletePlaylist,
                modifier = Modifier.fillMaxSize()
            )
            1 -> SongList(
                songs = favorites,
                onSongClick = { onSongClick(it, favorites) },
                emptyText = "No favorites yet",
                modifier = Modifier.fillMaxSize()
            )
            2 -> FeedContent(
                recentlyPlayed = recentlyPlayed,
                onSongClick = { onSongClick(it, recentlyPlayed) },
                modifier = Modifier.fillMaxSize()
            )
            3 -> DownloadsPlaceholder(
                downloadedSongs = downloadedSongs,
                onPlaySong = { song -> onSongClick(song, downloadedSongs) },
                onDeleteDownload = onDeleteDownload
            )
        }
    }
}

@Composable
private fun PlaylistList(
    playlists: List<Playlist>,
    onPlaylistClick: (Playlist) -> Unit,
    onPlayPlaylist: (Playlist) -> Unit,
    onShufflePlaylist: (Playlist) -> Unit,
    onSharePlaylist: (Playlist) -> Unit,
    onEditPlaylist: (Playlist) -> Unit,
    onShowQr: (Playlist) -> Unit,
    onDeletePlaylist: (Playlist) -> Unit,
    modifier: Modifier = Modifier
) {
    if (playlists.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "No playlists yet",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Create a playlist to get started",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = adaptiveListMinSize()),
            modifier = modifier.padding(horizontal = adaptiveHorizontalPadding(), vertical = WatermelonSpacing.md),
            verticalArrangement = Arrangement.spacedBy(WatermelonSpacing.md),
            horizontalArrangement = Arrangement.spacedBy(WatermelonSpacing.md)
        ) {
            gridItemsIndexed(playlists, key = { _, pl -> pl.id }) { index, playlist ->
                var menuExpanded by remember { mutableStateOf(false) }

                // Alternating watermelon gradient backgrounds
                // Even index: dark (black + red), Odd index: light (white + red)
                val isDark = index % 2 == 0
                val cardGradient = if (isDark) {
                    Brush.horizontalGradient(listOf(Color(0xFF1A0000), Color(0xFF3D0000), Color(0xFF8B0000)))
                } else {
                    Brush.horizontalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFFFE4E4), Color(0xFFFFCCCC)))
                }
                val onCardColor = if (isDark) Color.White else Color(0xFF1A0000)
                val subtitleColor = if (isDark) Color.White.copy(alpha = 0.65f) else Color(0xFF8B0000).copy(alpha = 0.75f)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPlaylistClick(playlist) },
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(cardGradient)
                    ) {
                        Row(
                            modifier = Modifier.padding(WatermelonSpacing.md),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PlaylistCoverGrid(
                                songs = playlist.songs,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.width(WatermelonSpacing.md))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = playlist.name,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                    ),
                                    color = onCardColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (!playlist.description.isNullOrBlank()) {
                                    Text(
                                        text = playlist.description ?: "",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = subtitleColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = "${playlist.songs.size} songs",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = subtitleColor
                                )
                            }
                            Box {
                                IconButton(onClick = { menuExpanded = true }) {
                                    Icon(
                                        imageVector = Icons.Filled.MoreVert,
                                        contentDescription = "More",
                                        tint = onCardColor
                                    )
                                }
                                DropdownMenu(
                                    expanded = menuExpanded,
                                    onDismissRequest = { menuExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Play") },
                                        onClick = { menuExpanded = false; onPlayPlaylist(playlist) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Shuffle") },
                                        onClick = { menuExpanded = false; onShufflePlaylist(playlist) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Share") },
                                        onClick = { menuExpanded = false; onSharePlaylist(playlist) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Edit") },
                                        onClick = { menuExpanded = false; onEditPlaylist(playlist) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Show QR") },
                                        onClick = { menuExpanded = false; onShowQr(playlist) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                                        onClick = { menuExpanded = false; onDeletePlaylist(playlist) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistCoverGrid(
    songs: List<com.watermelon.domain.model.PlaylistSong>,
    modifier: Modifier = Modifier
) {
    val covers = songs.mapNotNull { it.coverUrl }.take(4)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        when (covers.size) {
            0 -> {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                    contentDescription = null,
                    modifier = Modifier.align(Alignment.Center).size(32.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
            1 -> {
                AsyncImage(model = covers[0], contentDescription = null, modifier = Modifier.fillMaxSize())
            }
            2 -> {
                Row(modifier = Modifier.fillMaxSize()) {
                    AsyncImage(model = covers[0], contentDescription = null, modifier = Modifier.weight(1f).fillMaxHeight())
                    AsyncImage(model = covers[1], contentDescription = null, modifier = Modifier.weight(1f).fillMaxHeight())
                }
            }
            3 -> {
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
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = adaptiveListMinSize()),
            modifier = modifier.padding(horizontal = adaptiveHorizontalPadding(), vertical = WatermelonSpacing.md),
            verticalArrangement = Arrangement.spacedBy(WatermelonSpacing.md),
            horizontalArrangement = Arrangement.spacedBy(WatermelonSpacing.md)
        ) {
            gridItems(songs, key = { it.id }) { song ->
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
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = adaptiveListMinSize()),
        modifier = modifier.padding(horizontal = adaptiveHorizontalPadding(), vertical = WatermelonSpacing.md),
        verticalArrangement = Arrangement.spacedBy(WatermelonSpacing.md),
        horizontalArrangement = Arrangement.spacedBy(WatermelonSpacing.md)
    ) {
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
            Text(
                text = "Recently Played",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = WatermelonSpacing.sm)
            )
        }
        if (recentlyPlayed.isEmpty()) {
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
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
            gridItems(recentlyPlayed, key = { it.id }) { song ->
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

@Composable
private fun DownloadsPlaceholder(
    downloadedSongs: List<Song>,
    onPlaySong: (Song) -> Unit,
    onDeleteDownload: (Song) -> Unit
) {
    var songToDelete by remember { mutableStateOf<Song?>(null) }

    if (downloadedSongs.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Downloads",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Downloaded songs will appear here",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = adaptiveListMinSize()),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = adaptiveHorizontalPadding(), vertical = WatermelonSpacing.md),
            verticalArrangement = Arrangement.spacedBy(WatermelonSpacing.md),
            horizontalArrangement = Arrangement.spacedBy(WatermelonSpacing.md)
        ) {
            gridItems(
                items = downloadedSongs,
                key = { it.id }
            ) { song ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPlaySong(song) },
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(WatermelonSpacing.md),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!song.coverUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = song.coverUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(WatermelonSpacing.md))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = song.title,
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (!song.artistName.isNullOrBlank() && song.artistName != "Unknown Artist") {
                                Text(
                                    text = song.artistName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        IconButton(
                            onClick = { songToDelete = song },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (songToDelete != null) {
        AlertDialog(
            onDismissRequest = { songToDelete = null },
            title = { Text("Delete Download") },
            text = { Text("Remove \"${songToDelete?.title}\" from downloads? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        songToDelete?.let { s ->
                            onDeleteDownload(s)
                        }
                        songToDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
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

private fun generateQrBitmap(content: String, size: Int = 512): android.graphics.Bitmap? {
    return try {
        val writer = com.google.zxing.qrcode.QRCodeWriter()
        val bitMatrix = writer.encode(content, com.google.zxing.BarcodeFormat.QR_CODE, size, size)
        val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.RGB_565)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }
        bitmap
    } catch (e: Exception) {
        null
    }
}