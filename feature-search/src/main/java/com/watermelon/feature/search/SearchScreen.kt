package com.watermelon.feature.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.watermelon.core.designsystem.animation.ShimmerCard
import com.watermelon.core.designsystem.layout.adaptiveHorizontalPadding
import com.watermelon.core.designsystem.layout.adaptiveListMinSize
import com.watermelon.core.designsystem.layout.adaptiveMaxContentWidth
import com.watermelon.core.designsystem.theme.WatermelonRed
import com.watermelon.core.designsystem.theme.WatermelonSpacing
import com.watermelon.domain.model.Song
import com.watermelon.domain.model.CommunityPlaylist

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onBackClick: () -> Unit,
    onSongClick: (Song, Int, List<Song>) -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val showSheet by viewModel.showAddToPlaylistSheet.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val toastMessage by viewModel.addToPlaylistMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearAddToPlaylistMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Search") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        SearchScreenContent(
            query = query,
            results = results,
            isLoading = isLoading,
            padding = padding,
            onQueryChange = viewModel::onQueryChange,
            onSongClick = onSongClick,
            onAddToPlaylist = viewModel::onAddToPlaylistClick,
            onSavePlaylist = viewModel::onSavePlaylist,
            playlistResults = viewModel.playlistResults.collectAsStateWithLifecycle().value,
            selectedCategory = viewModel.selectedCategory.collectAsStateWithLifecycle().value,
            onCategorySelected = viewModel::onCategorySelected
        )
    }

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

@Composable
fun SearchScreenContent(
    query: String,
    results: List<Song>,
    isLoading: Boolean,
    padding: PaddingValues,
    onQueryChange: (String) -> Unit,
    onSongClick: (Song, Int, List<Song>) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    onSavePlaylist: (CommunityPlaylist) -> Unit = {},
    playlistResults: List<CommunityPlaylist> = emptyList(),
    selectedCategory: SearchCategory = SearchCategory.ALL,
    onCategorySelected: (SearchCategory) -> Unit = {}
) {
    val maxWidth = adaptiveMaxContentWidth()
    val widthModifier = if (maxWidth == androidx.compose.ui.unit.Dp.Unspecified)
        Modifier.fillMaxWidth()
    else
        Modifier.widthIn(max = maxWidth).fillMaxWidth()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentAlignment = Alignment.TopCenter
    ) {
    Column(
        modifier = widthModifier
            .fillMaxHeight()
            .padding(horizontal = adaptiveHorizontalPadding())
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search songs, artists...") },
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = WatermelonRed
                )
            },
            shape = RoundedCornerShape(20.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = WatermelonRed,
                focusedLabelColor = WatermelonRed,
                focusedLeadingIconColor = WatermelonRed,
                unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )

        Spacer(modifier = Modifier.height(WatermelonSpacing.md))

        // Category Filter Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(WatermelonSpacing.sm)
        ) {
            FilterChip(
                selected = selectedCategory == SearchCategory.ALL,
                onClick = { onCategorySelected(SearchCategory.ALL) },
                label = { Text("All") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = WatermelonRed,
                    selectedLabelColor = Color.White
                )
            )
            FilterChip(
                selected = selectedCategory == SearchCategory.SONGS,
                onClick = { onCategorySelected(SearchCategory.SONGS) },
                label = { Text("Songs") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = WatermelonRed,
                    selectedLabelColor = Color.White
                )
            )
            FilterChip(
                selected = selectedCategory == SearchCategory.PLAYLISTS,
                onClick = { onCategorySelected(SearchCategory.PLAYLISTS) },
                label = { Text("Playlists") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = WatermelonRed,
                    selectedLabelColor = Color.White
                )
            )
        }

        Spacer(modifier = Modifier.height(WatermelonSpacing.md))

        if (isLoading) {
            Column(verticalArrangement = Arrangement.spacedBy(WatermelonSpacing.md)) {
                repeat(6) {
                    ShimmerCard(height = 72.dp)
                }
            }
        } else if (query.isBlank()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Spacer(modifier = Modifier.height(WatermelonSpacing.md))
                    Text(
                        text = "Type to search",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else if (selectedCategory == SearchCategory.PLAYLISTS) {
            if (playlistResults.isNotEmpty()) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = adaptiveListMinSize()),
                    verticalArrangement = Arrangement.spacedBy(WatermelonSpacing.md),
                    horizontalArrangement = Arrangement.spacedBy(WatermelonSpacing.md)
                ) {
                    items(playlistResults, key = { it.id }) { playlist ->
                        PlaylistResultItem(
                            playlist = playlist,
                            onClick = { /* TODO: Navigate to playlist */ },
                            onSave = { onSavePlaylist(playlist) }
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Spacer(modifier = Modifier.height(WatermelonSpacing.md))
                        Text(
                            text = "No playlists found",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            if (results.isEmpty() && playlistResults.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Spacer(modifier = Modifier.height(WatermelonSpacing.md))
                        Text(
                            text = "No results found",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Column {
                    if (playlistResults.isNotEmpty()) {
                        Text(
                            text = "Playlists",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(
                                start = adaptiveHorizontalPadding(),
                                top = WatermelonSpacing.md,
                                bottom = WatermelonSpacing.md
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = adaptiveHorizontalPadding()),
                            horizontalArrangement = Arrangement.spacedBy(WatermelonSpacing.md)
                        ) {
                            items(playlistResults, key = { it.id }) { playlist ->
                                PlaylistResultItem(
                                    playlist = playlist,
                                    onClick = { /* TODO */ },
                                    onSave = { onSavePlaylist(playlist) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(WatermelonSpacing.lg))
                    }
                    if (results.isNotEmpty()) {
                        Text(
                            text = "Songs",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(
                                start = adaptiveHorizontalPadding(),
                                bottom = WatermelonSpacing.md
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = adaptiveListMinSize()),
                            verticalArrangement = Arrangement.spacedBy(WatermelonSpacing.md),
                            horizontalArrangement = Arrangement.spacedBy(WatermelonSpacing.md)
                        ) {
                            items(results, key = { it.id }) { song ->
                                val index = results.indexOf(song)
                                SearchResultItem(
                                    song = song,
                                    onClick = { onSongClick(song, index, results) },
                                    onAddToPlaylist = { onAddToPlaylist(song) }
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
private fun SearchResultItem(
    song: Song,
    onClick: () -> Unit,
    onAddToPlaylist: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(song.id) { visible = true }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 5 }
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(2.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(WatermelonSpacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = song.coverUrl?.takeIf { it.isNotBlank() }
                        ?: com.watermelon.core.designsystem.R.drawable.app_logo,
                    contentDescription = song.title,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
                Spacer(modifier = Modifier.width(WatermelonSpacing.md))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = song.artistName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilledIconButton(
                        onClick = onClick,
                        modifier = Modifier.size(40.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = WatermelonRed,
                            contentColor = androidx.compose.ui.graphics.Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    IconButton(
                        onClick = onAddToPlaylist,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistResultItem(
    playlist: CommunityPlaylist,
    onClick: () -> Unit,
    onSave: () -> Unit = {}
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(playlist.id) { visible = true }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 5 }
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(2.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(WatermelonSpacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = playlist.coverUrl?.takeIf { it.isNotBlank() }
                        ?: com.watermelon.core.designsystem.R.drawable.app_logo,
                    contentDescription = playlist.name,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
                Spacer(modifier = Modifier.width(WatermelonSpacing.md))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = playlist.name,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${playlist.likeCount} likes • ${playlist.songCount} songs",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (playlist.creatorDisplayName.isNotBlank()) {
                        Text(
                            text = "by ${playlist.creatorDisplayName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilledIconButton(
                        onClick = onClick,
                        modifier = Modifier.size(40.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = WatermelonRed,
                            contentColor = androidx.compose.ui.graphics.Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Open",
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    FilledIconButton(
                        onClick = onSave,
                        modifier = Modifier.size(40.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = WatermelonRed,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                            contentDescription = "Add to Library",
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}
