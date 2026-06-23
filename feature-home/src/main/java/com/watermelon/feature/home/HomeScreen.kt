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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.watermelon.core.designsystem.animation.ShimmerCard
import com.watermelon.core.designsystem.layout.adaptiveBigCardWidth
import com.watermelon.core.designsystem.layout.adaptiveHorizontalPadding
import com.watermelon.core.designsystem.layout.adaptiveMaxContentWidth
import com.watermelon.core.designsystem.layout.adaptiveSmallCardWidth
import com.watermelon.core.designsystem.theme.WatermelonRed
import com.watermelon.feature.player.PlayerViewModel
import com.watermelon.core.designsystem.theme.WatermelonSpacing
import com.watermelon.domain.model.Song
import com.watermelon.domain.model.CommunityPlaylist
import com.watermelon.domain.model.CuratedPlaylist
import com.watermelon.domain.model.Artist
import coil.request.ImageRequest

@Composable
fun HomeScreen(
    onSearchClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onSongClick: (Song, List<Song>) -> Unit = { _, _ -> },
    onPlayerClick: () -> Unit = {},
    onArtistClick: (Artist) -> Unit = {},
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

    val user by viewModel.user.collectAsStateWithLifecycle()
    HomeScreenContent(
        uiState = uiState,
        user = user,
        onSearchClick = onSearchClick,
        onSettingsClick = onSettingsClick,
        onProfileClick = onProfileClick,
        onSongClick = onSongClick,
        onPlayerClick = onPlayerClick,
        onAddToPlaylist = viewModel::onAddToPlaylistClick,
        onSaveCommunityPlaylist = viewModel::saveCommunityPlaylist,
        snackbarHostState = snackbarHostState,
        onArtistClick = onArtistClick
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
    user: com.watermelon.domain.model.User? = null,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onProfileClick: () -> Unit = {},
    onSongClick: (Song, List<Song>) -> Unit,
    onPlayerClick: () -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    onSaveCommunityPlaylist: (com.watermelon.domain.model.CommunityPlaylist) -> Unit = {},
    snackbarHostState: SnackbarHostState,
    onArtistClick: (Artist) -> Unit = {}
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
                    IconButton(onClick = onProfileClick) {
                        if (!user?.avatarUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(user!!.avatarUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Profile",
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = (user?.displayName?.firstOrNull() ?: user?.username?.firstOrNull() ?: "P").toString(),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
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
            val maxWidth = adaptiveMaxContentWidth()
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.TopCenter
            ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxHeight()
                    .then(
                        if (maxWidth == androidx.compose.ui.unit.Dp.Unspecified)
                            Modifier.fillMaxWidth()
                        else
                            Modifier.widthIn(max = maxWidth).fillMaxWidth()
                    ),
                contentPadding = PaddingValues(vertical = WatermelonSpacing.md)
            ) {
                item { SearchBarShortcut(onClick = onSearchClick) }

                if (uiState.curatedPlaylists.isNotEmpty()) {
                    item {
                        SectionHeader(title = "Made for You")
                        CuratedPlaylistRow(
                            playlists = uiState.curatedPlaylists,
                            onPlaylistClick = { /* TODO: Navigate to curated playlist */ }
                        )
                    }
                }

                item {
                    SectionHeader(title = "Trending Artists")
                    if (uiState.trendingArtists.isNotEmpty()) {
                        TrendingArtistsRow(
                            artists = uiState.trendingArtists,
                            onArtistClick = onArtistClick
                        )
                    } else if (uiState.isLoading) {
                        Box(modifier = Modifier.height(120.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        }
                    } else {
                        Box(modifier = Modifier.height(80.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("Discover artists coming soon", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                item {
                    SectionHeader(title = "Community Playlists")
                    if (uiState.communityPlaylists.isNotEmpty()) {
                        CommunityPlaylistRow(
                            playlists = uiState.communityPlaylists,
                            onSaveClick = onSaveCommunityPlaylist
                        )
                    } else if (uiState.isLoading) {
                        Box(modifier = Modifier.height(160.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        }
                    } else {
                        Box(modifier = Modifier.height(80.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("No playlists yet", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                if (uiState.languagePlaylists.isNotEmpty()) {
                    item {
                        SectionHeader(title = "Trending by Language")
                        LanguagePlaylistRow(
                            playlists = uiState.languagePlaylists,
                            onPlaylistClick = { /* TODO: Navigate to playlist detail */ }
                        )
                    }
                }

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
}

@Composable
private fun HomeShimmerContent(paddingValues: PaddingValues) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = adaptiveHorizontalPadding()),
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
            .padding(horizontal = adaptiveHorizontalPadding())
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
private fun SectionHeader(title: String, isDarkText: Boolean = false) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        modifier = Modifier.padding(
            start = WatermelonSpacing.md,
            top = WatermelonSpacing.lg,
            bottom = WatermelonSpacing.sm
        ),
        color = if (isDarkText) Color.Black else MaterialTheme.colorScheme.onBackground
    )
}

@Composable
private fun BigSongRow(
    songs: List<Song>,
    isDarkText: Boolean = false,
    onSongClick: (Song, List<Song>) -> Unit,
    onAddToPlaylist: (Song) -> Unit
) {
    val cardWidth = adaptiveBigCardWidth()
    LazyRow(
        contentPadding = PaddingValues(horizontal = adaptiveHorizontalPadding()),
        horizontalArrangement = Arrangement.spacedBy(WatermelonSpacing.md)
    ) {
        items(songs, key = { it.id }) { song ->
            BigSongItem(
                song = song,
                cardWidth = cardWidth,
                isDarkText = isDarkText,
                onClick = { onSongClick(song, songs) },
                onAddToPlaylist = { onAddToPlaylist(song) }
            )
        }
    }
}

@Composable
private fun BigSongItem(
    song: Song,
    cardWidth: androidx.compose.ui.unit.Dp = 170.dp,
    isDarkText: Boolean = false,
    onClick: () -> Unit,
    onAddToPlaylist: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { it / 4 }
    ) {
        Column(
            modifier = Modifier.width(cardWidth),
            horizontalAlignment = Alignment.Start
        ) {
            Card(
                modifier = Modifier
                    .size(cardWidth)
                    .clickable(onClick = onClick),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(6.dp),
                colors = CardDefaults.cardColors(containerColor = if (isDarkText) Color.LightGray else MaterialTheme.colorScheme.surface)
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
                color = if (isDarkText) Color.Black else MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = song.artistName,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (isDarkText) Color.DarkGray else MaterialTheme.colorScheme.onSurfaceVariant
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
    val cardWidth = adaptiveSmallCardWidth()
    LazyRow(
        contentPadding = PaddingValues(horizontal = adaptiveHorizontalPadding()),
        horizontalArrangement = Arrangement.spacedBy(WatermelonSpacing.md)
    ) {
        items(songs, key = { it.id }) { song ->
            SongItem(
                song = song,
                cardWidth = cardWidth,
                onClick = { onSongClick(song, songs) },
                onAddToPlaylist = { onAddToPlaylist(song) }
            )
        }
    }
}

@Composable
private fun SongItem(
    song: Song,
    cardWidth: androidx.compose.ui.unit.Dp = 130.dp,
    onClick: () -> Unit,
    onAddToPlaylist: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { it / 4 }
    ) {
        Column(
            modifier = Modifier.width(cardWidth),
            horizontalAlignment = Alignment.Start
        ) {
            Card(
                modifier = Modifier
                    .size(cardWidth)
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

@Composable
private fun CuratedPlaylistRow(
    playlists: List<CuratedPlaylist>,
    onPlaylistClick: (CuratedPlaylist) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = adaptiveHorizontalPadding()),
        horizontalArrangement = Arrangement.spacedBy(WatermelonSpacing.md)
    ) {
        items(playlists, key = { it.id }) { playlist ->
            CuratedPlaylistCard(
                playlist = playlist,
                onClick = { onPlaylistClick(playlist) }
            )
        }
    }
}

@Composable
private fun CuratedPlaylistCard(
    playlist: CuratedPlaylist,
    cardWidth: androidx.compose.ui.unit.Dp = 150.dp,
    onClick: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { it / 4 }
    ) {
        Column(
            modifier = Modifier.width(cardWidth),
            horizontalAlignment = Alignment.Start
        ) {
            Card(
                modifier = Modifier
                    .size(cardWidth)
                    .clickable(onClick = onClick),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(6.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    val gradientColors = playlist.gradientColors.mapNotNull {
                        try { Color(android.graphics.Color.parseColor(it)) }
                        catch (e: Exception) { null }
                    }
                    if (playlist.coverUrl != null) {
                        AsyncImage(
                            model = playlist.coverUrl,
                            contentDescription = playlist.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else if (gradientColors.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(colors = gradientColors)
                                )
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(WatermelonRed)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f)),
                                    startY = 60f
                                )
                            )
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp),
                        contentAlignment = Alignment.BottomStart
                    ) {
                        Column {
                            Text(
                                text = playlist.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = playlist.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f),
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
private fun TrendingArtistsRow(
    artists: List<Artist>,
    onArtistClick: (Artist) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = adaptiveHorizontalPadding()),
        horizontalArrangement = Arrangement.spacedBy(WatermelonSpacing.md)
    ) {
        items(artists, key = { it.id }) { artist ->
            ArtistCircleItem(
                artist = artist,
                onClick = { onArtistClick(artist) }
            )
        }
    }
}

@Composable
private fun ArtistCircleItem(
    artist: Artist,
    onClick: () -> Unit
) {
    val itemWidth = maxOf(80.dp, (LocalConfiguration.current.screenWidthDp.dp - 48.dp) / 4.5f)
    Column(
        modifier = Modifier
            .width(itemWidth)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = artist.imageUrl?.takeIf { it.isNotBlank() }
                ?: com.watermelon.core.designsystem.R.drawable.app_logo,
            contentDescription = artist.name,
            modifier = Modifier
                .size(itemWidth * 0.85f)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(WatermelonSpacing.xs))
        Text(
            text = artist.name,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun LanguagePlaylistRow(
    playlists: List<CommunityPlaylist>,
    onPlaylistClick: (CommunityPlaylist) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = adaptiveHorizontalPadding()),
        horizontalArrangement = Arrangement.spacedBy(WatermelonSpacing.md)
    ) {
        items(playlists, key = { it.id }) { playlist ->
            LanguagePlaylistCard(
                playlist = playlist,
                onClick = { onPlaylistClick(playlist) }
            )
        }
    }
}

@Composable
private fun LanguagePlaylistCard(
    playlist: CommunityPlaylist,
    cardWidth: androidx.compose.ui.unit.Dp = 160.dp,
    onClick: () -> Unit
) {
    val languageEmoji = when {
        playlist.name.contains("Hindi", ignoreCase = true) -> "🇮🇳"
        playlist.name.contains("English", ignoreCase = true) -> "🇺🇸"
        playlist.name.contains("Telugu", ignoreCase = true) -> "🇮🇳"
        playlist.name.contains("Tamil", ignoreCase = true) -> "🇮🇳"
        playlist.name.contains("Spanish", ignoreCase = true) -> "🇪🇸"
        playlist.name.contains("Korean", ignoreCase = true) -> "🇰🇷"
        playlist.name.contains("Japanese", ignoreCase = true) -> "🇯🇵"
        playlist.name.contains("French", ignoreCase = true) -> "🇫🇷"
        playlist.name.contains("German", ignoreCase = true) -> "🇩🇪"
        playlist.name.contains("Chinese", ignoreCase = true) -> "🇨🇳"
        else -> "🌐"
    }

    Column(
        modifier = Modifier
            .width(cardWidth)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.Start
    ) {
        Card(
            modifier = Modifier.size(cardWidth, 100.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.secondaryContainer
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = languageEmoji,
                    style = MaterialTheme.typography.headlineLarge
                )
            }
        }
        Spacer(modifier = Modifier.height(WatermelonSpacing.sm))
        Text(
            text = playlist.name,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "${playlist.songCount} songs",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CommunityPlaylistRow(
    playlists: List<CommunityPlaylist>,
    onSaveClick: (com.watermelon.domain.model.CommunityPlaylist) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = adaptiveHorizontalPadding()),
        horizontalArrangement = Arrangement.spacedBy(WatermelonSpacing.md)
    ) {
        items(playlists, key = { it.id }) { playlist ->
            CommunityPlaylistCard(
                playlist = playlist,
                onSaveClick = onSaveClick
            )
        }
    }
}

@Composable
private fun CommunityPlaylistCard(
    playlist: CommunityPlaylist,
    onSaveClick: (com.watermelon.domain.model.CommunityPlaylist) -> Unit,
    cardWidth: androidx.compose.ui.unit.Dp = maxOf(
        140.dp,
        ((LocalConfiguration.current.screenWidthDp.dp - 48.dp) / 2.3f)
    )
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { it / 4 }
    ) {
        Column(
            modifier = Modifier.width(cardWidth),
            horizontalAlignment = Alignment.Start
        ) {
            Card(
                modifier = Modifier
                    .size(cardWidth)
                    .clickable { /* TODO: Navigate to playlist detail */ },
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                                    startY = 80f
                                )
                            )
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        contentAlignment = Alignment.TopEnd
                    ) {
                        Column(horizontalAlignment = Alignment.End) {
                            IconButton(
                                onClick = onLikeClick,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = "Like",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = { onSaveClick(playlist) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                                    contentDescription = "Save",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        contentAlignment = Alignment.BottomStart
                    ) {
                        Column {
                            Text(
                                text = playlist.name,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(WatermelonSpacing.sm))
            Text(
                text = playlist.name,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (playlist.creatorDisplayName.isNotBlank()) {
                Text(
                    text = "by ${playlist.creatorDisplayName}",
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
