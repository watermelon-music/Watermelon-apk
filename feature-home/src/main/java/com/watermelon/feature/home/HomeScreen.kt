@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.watermelon.feature.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.CheckCircle
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
import coil.request.ImageRequest

import com.watermelon.core.designsystem.layout.adaptiveBigCardWidth
import com.watermelon.core.designsystem.layout.adaptiveHorizontalPadding
import com.watermelon.core.designsystem.layout.adaptiveMaxContentWidth
import com.watermelon.core.designsystem.layout.adaptiveSmallCardWidth
import com.watermelon.core.designsystem.theme.WatermelonRed
import com.watermelon.feature.player.PlayerViewModel
import com.watermelon.core.designsystem.theme.WatermelonSpacing
import com.watermelon.domain.model.Song
import com.watermelon.domain.model.CommunityPlaylist
import com.watermelon.domain.model.Artist
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    onSearchClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onSongClick: (Song, List<Song>) -> Unit = { _, _ -> },
    onPlayerClick: () -> Unit = {},
    onArtistClick: (Artist) -> Unit = {},
    onPlaylistClick: (com.watermelon.domain.model.CommunityPlaylist) -> Unit = {},
    onRadioPlay: (com.watermelon.domain.model.RadioStation) -> Unit = {},
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
        onArtistClick = onArtistClick,
        onPlaylistClick = onPlaylistClick
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
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(vertical = 16.dp, horizontal = 24.dp)
                )
                playlists.forEach { playlist ->
                    ListItem(
                        headlineContent = { Text(playlist.name) },
                        leadingContent = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, null) },
                        modifier = Modifier.clickable {
                            viewModel.onPlaylistSelected(playlist.id)
                        }
                    )
                }
                ListItem(
                    headlineContent = { Text("Create New Playlist", color = WatermelonRed) },
                    leadingContent = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, null, tint = WatermelonRed) },
                    modifier = Modifier.clickable { /* TODO */ }
                )
            }
        }
    }

}

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
    onPlaylistClick: (com.watermelon.domain.model.CommunityPlaylist) -> Unit = {},
    onRadioPlay: (com.watermelon.domain.model.RadioStation) -> Unit = {},
    snackbarHostState: SnackbarHostState,
    onArtistClick: (Artist) -> Unit = {}
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val cardWidth = maxOf(152.dp, (screenWidth - 48.dp) / 2.3f)

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
                    IconButton(onClick = onSearchClick) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                    if (user != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(user.avatarUrl ?: "")
                                .crossfade(true)
                                .build(),
                            contentDescription = "Profile",
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .size(36.dp)
                                .clip(CircleShape)
                                .clickable(onClick = onProfileClick),
                            contentScale = ContentScale.Crop
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.95f)
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ── HERO CAROUSEL ──
            if (uiState.trendingMusic.isNotEmpty()) {
                item {
                    HeroCarousel(
                        songs = uiState.trendingMusic.take(5),
                        onSongClick = { song -> onSongClick(song, uiState.trendingMusic) }
                    )
                }
            }

            // ── TOP HITS ──
            if (uiState.trendingMusic.isNotEmpty()) {
                item {
                    SectionHeader(title = "Top Hits")
                    SongHorizontalRow(songs = uiState.trendingMusic.take(8), onSongClick = onSongClick)
                }
            }

            // ── TRENDING ARTISTS ──
            item {
                SectionHeader(title = "Trending Artists")
                if (uiState.trendingArtists.isNotEmpty()) {
                    TrendingArtistsRow(
                        artists = uiState.trendingArtists,
                        onArtistClick = onArtistClick
                    )
                } else if (uiState.isLoading) {
                    Box(modifier = Modifier.height(180.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    }
                } else {
                    Box(modifier = Modifier.height(100.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("Discover artists coming soon", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // ── COMMUNITY PLAYLISTS ──
            item {
                SectionHeader(title = "Trending Playlists")
                if (uiState.communityPlaylists.isNotEmpty()) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(uiState.communityPlaylists, key = { it.id }) { playlist ->
                            PlaylistBoxCard(
                                playlist = playlist,
                                cardWidth = cardWidth,
                                onPlaylistClick = { onPlaylistClick(playlist) },
                                onSaveClick = { onSaveCommunityPlaylist(playlist) }
                            )
                        }
                    }
                } else if (uiState.isLoading) {
                    Box(modifier = Modifier.height(200.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    }
                } else {
                    Box(modifier = Modifier.height(100.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("No playlists yet", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // ── GENRE SECTIONS ──
            if (uiState.recentlyPlayed.isNotEmpty()) {
                item {
                    SectionHeader(title = "Continue Listening")
                    SongHorizontalRow(
                        songs = uiState.recentlyPlayed,
                        onSongClick = onSongClick
                    )
                }
            }

            genreSections(uiState, onSongClick)

            // ── FEATURED ARTISTS ──
            if (uiState.trendingArtists.isNotEmpty()) {
                item {
                    SectionHeader(title = "Featured Artists")
                    FeaturedArtistsRow(
                        artists = uiState.trendingArtists.take(6),
                        onArtistClick = onArtistClick
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(120.dp)) }
        }
    }
}

// ═══════════════════════════════════════════════════════════
//  HERO CAROUSEL
// ═══════════════════════════════════════════════════════════
@Composable
private fun HeroCarousel(songs: List<Song>, onSongClick: (Song) -> Unit) {
    val pagerState = rememberPagerState(pageCount = { songs.size })
    val scope = rememberCoroutineScope()

    LaunchedEffect(pagerState.currentPage) {
        delay(4000)
        scope.launch {
            val next = (pagerState.currentPage + 1) % songs.size
            pagerState.animateScrollToPage(next)
        }
    }

    Column {
        HorizontalPager(
            state = pagerState,
            beyondBoundsPageCount = 1,
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(24.dp))
        ) { page ->
            val song = songs[page]
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { onSongClick(song) }
            ) {
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
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f)),
                                startY = 60f
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(20.dp)
                ) {
                    Text(
                        text = if (page == 0) "Trending Worldwide" else "Tonight\'s Top Pick",
                        style = MaterialTheme.typography.labelSmall,
                        color = WatermelonRed,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = song.artistName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f),
                        maxLines = 1
                    )
                }
                FloatingActionButton(
                    onClick = { onSongClick(song) },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(20.dp)
                        .size(48.dp),
                    containerColor = WatermelonRed,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.White)
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        // Page indicator dots
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(songs.size) { index ->
                val color = if (index == pagerState.currentPage) WatermelonRed else Color.White.copy(alpha = 0.3f)
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(if (index == pagerState.currentPage) 8.dp else 6.dp)
                        .clip(CircleShape)
                        .background(color)
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

// ═══════════════════════════════════════════════════════════
//  ARTIST CIRCLES (BIG)
// ═══════════════════════════════════════════════════════════
@Composable
private fun TrendingArtistsRow(
    artists: List<Artist>,
    onArtistClick: (Artist) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp)
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
    val circleSize = 130.dp
    Column(
        modifier = Modifier
            .width(circleSize)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(circleSize),
            contentAlignment = Alignment.Center
        ) {
            // Glowing border
            Box(
                modifier = Modifier
                    .size(circleSize)
                    .clip(CircleShape)
                    .background(
                        Brush.sweepGradient(
                            listOf(WatermelonRed, Color(0xFF00BFFF), WatermelonRed)
                        )
                    )
            )
            AsyncImage(
                model = artist.imageUrl?.takeIf { it.isNotBlank() }
                    ?: com.watermelon.core.designsystem.R.drawable.app_logo,
                contentDescription = artist.name,
                modifier = Modifier
                    .size(circleSize - 6.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            if (artist.verified) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Verified",
                    tint = Color(0xFF00BFFF),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(24.dp)
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                        .padding(2.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = artist.name,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (artist.subscriberCount > 0) {
            val subs = when {
                artist.subscriberCount >= 1_000_000 -> "%.1fM".format(artist.subscriberCount / 1_000_000.0)
                artist.subscriberCount >= 1_000 -> "%.1fK".format(artist.subscriberCount / 1_000.0)
                else -> "${artist.subscriberCount}"
            }
            Text(
                text = "$subs followers",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════
//  PLAYLIST BOX CARD
// ═══════════════════════════════════════════════════════════
@Composable
private fun PlaylistBoxCard(
    playlist: CommunityPlaylist,
    cardWidth: androidx.compose.ui.unit.Dp,
    onPlaylistClick: () -> Unit = {},
    onSaveClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier.width(cardWidth),
        horizontalAlignment = Alignment.Start
    ) {
        Card(
            modifier = Modifier
                .size(cardWidth)
                .clickable(onClick = onPlaylistClick),
            shape = RoundedCornerShape(20.dp),
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
                        .padding(12.dp),
                    contentAlignment = Alignment.TopEnd
                ) {
                    IconButton(
                        onClick = onSaveClick,
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
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                ) {
                    Text(
                        text = playlist.name,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
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
        Spacer(modifier = Modifier.height(WatermelonSpacing.sm))
        Text(
            text = playlist.name,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
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

// ═══════════════════════════════════════════════════════════
//  MOOD ROW
// ═══════════════════════════════════════════════════════════
private data class MoodItem(val emoji: String, val label: String, val gradient: List<Color>)

@Composable
private fun LiveRadioRow(
    stations: List<com.watermelon.domain.model.RadioStation>,
    onPlay: (com.watermelon.domain.model.RadioStation) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(stations, key = { it.stationuuid ?: it.name ?: "" }) { station ->
            Card(
                modifier = Modifier
                    .width(140.dp)
                    .height(180.dp)
                    .clickable { onPlay(station) },
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AsyncImage(
                        model = station.favicon?.takeIf { it.isNotBlank() }
                            ?: com.watermelon.core.designsystem.R.drawable.app_logo,
                        contentDescription = station.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                                    startY = 60f
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = station.name ?: "",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${station.country} • LIVE",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF4CAF50)
                        )
                    }
                    IconButton(
                        onClick = { onPlay(station) },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(32.dp)
                            .background(Color.White.copy(alpha = 0.2f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NewsBroadcastRow(
    news: List<com.watermelon.feature.home.MusicNews>,
    onNewsClick: (com.watermelon.feature.home.MusicNews) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(news) { item ->
            Card(
                modifier = Modifier
                    .width(280.dp)
                    .height(120.dp)
                    .clickable { onNewsClick(item) },
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    AsyncImage(
                        model = item.imageUrl,
                        contentDescription = item.title,
                        modifier = Modifier
                            .width(120.dp)
                            .fillMaxHeight(),
                        contentScale = ContentScale.Crop
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = item.source,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = item.timeAgo,
                                style = MaterialTheme.typography.labelSmall,
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
private fun FeaturedArtistsRow(
    artists: List<Artist>,
    onArtistClick: (Artist) -> Unit
) {
    val listState = rememberLazyListState()
    LaunchedEffect(Unit) {
        if (artists.size > 1) {
            var current = 0
            while (isActive) {
                delay(2500)
                current = (current + 1) % artists.size
                listState.animateScrollToItem(current)
            }
        }
    }
    val cardWidth = (LocalConfiguration.current.screenWidthDp.dp - 48.dp).coerceIn(260.dp, 340.dp)
    LazyRow(
        state = listState,
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(artists, key = { it.id }) { artist ->
            Card(
                modifier = Modifier
                    .width(cardWidth)
                    .height(170.dp)
                    .clickable { onArtistClick(artist) },
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    AsyncImage(
                        model = artist.imageUrl?.takeIf { it.isNotBlank() }
                            ?: com.watermelon.core.designsystem.R.drawable.app_logo,
                        contentDescription = artist.name,
                        modifier = Modifier
                            .width(170.dp)
                            .fillMaxHeight(),
                        contentScale = ContentScale.Crop
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            artist.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        if (artist.subscriberCount > 0) {
                            val subs = when {
                                artist.subscriberCount >= 1_000_000 -> "%.1fM".format(artist.subscriberCount / 1_000_000.0)
                                artist.subscriberCount >= 1_000 -> "%.1fK".format(artist.subscriberCount / 1_000.0)
                                else -> "${artist.subscriberCount}"
                            }
                            Text(
                                "$subs followers",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        artist.bio?.takeIf { it.isNotBlank() }?.let { bio ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                bio,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════
//  SECTIONS
// ═══════════════════════════════════════════════════════════
fun LazyListScope.genreSections(uiState: HomeUiState, onSongClick: (Song, List<Song>) -> Unit) {
    if (uiState.bollywood.isNotEmpty()) {
        item {
            SectionHeader(title = "Bollywood")
            SongHorizontalRow(songs = uiState.bollywood, onSongClick = onSongClick)
        }
    }
    if (uiState.hollywood.isNotEmpty()) {
        item {
            SectionHeader(title = "Hollywood")
            SongHorizontalRow(songs = uiState.hollywood, onSongClick = onSongClick)
        }
    }
    if (uiState.pop.isNotEmpty()) {
        item {
            SectionHeader(title = "Pop")
            SongHorizontalRow(songs = uiState.pop, onSongClick = onSongClick)
        }
    }
    if (uiState.rock.isNotEmpty()) {
        item {
            SectionHeader(title = "Rock")
            SongHorizontalRow(songs = uiState.rock, onSongClick = onSongClick)
        }
    }
    if (uiState.jazz.isNotEmpty()) {
        item {
            SectionHeader(title = "Jazz")
            SongHorizontalRow(songs = uiState.jazz, onSongClick = onSongClick)
        }
    }
    if (uiState.classical.isNotEmpty()) {
        item {
            SectionHeader(title = "Classical")
            SongHorizontalRow(songs = uiState.classical, onSongClick = onSongClick)
        }
    }
    if (uiState.hiphop.isNotEmpty()) {
        item {
            SectionHeader(title = "Hip Hop")
            SongHorizontalRow(songs = uiState.hiphop, onSongClick = onSongClick)
        }
    }
    if (uiState.electronic.isNotEmpty()) {
        item {
            SectionHeader(title = "Electronic")
            SongHorizontalRow(songs = uiState.electronic, onSongClick = onSongClick)
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun SongHorizontalRow(
    songs: List<Song>,
    onSongClick: (Song, List<Song>) -> Unit
) {
    val cardWidth = adaptiveBigCardWidth()
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(WatermelonSpacing.md)
    ) {
        items(songs, key = { it.id }) { song ->
            Column(
                modifier = Modifier.width(cardWidth),
                horizontalAlignment = Alignment.Start
            ) {
                Card(
                    modifier = Modifier
                        .size(cardWidth)
                        .clickable { onSongClick(song, songs) },
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(2.dp)
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
                                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))
                                    )
                                )
                        )
                        Text(
                            text = song.title,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(8.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = song.artistName,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
