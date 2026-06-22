package com.watermelon.feature.player

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.watermelon.core.designsystem.layout.adaptiveAlbumArtFraction
import com.watermelon.core.designsystem.layout.adaptiveHorizontalPadding
import com.watermelon.core.designsystem.layout.adaptiveMaxContentWidth
import com.watermelon.core.designsystem.layout.isLandscape
import com.watermelon.core.designsystem.theme.WatermelonRed
import com.watermelon.core.designsystem.theme.WatermelonRedDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    onBackClick: () -> Unit,
    onQueueClick: () -> Unit = {},
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current

    val isPlaying = state.isPlaying
    var localProgress by remember { mutableStateOf<Float?>(null) }
    val scope = rememberCoroutineScope()



    // Artwork pulse animation when playing
    val pulseAnim by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 1f,
        targetValue = if (isPlaying) 1.03f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "artworkPulse"
    )

    val sleepTimer by viewModel.sleepTimerRemainingSeconds.collectAsStateWithLifecycle()
    val sleepTimerText = sleepTimer?.let {
        val m = it / 60
        val s = it % 60
        "%02d:%02d".format(m, s)
    }
    var showTimerDialog by remember { mutableStateOf(false) }

    val showPlaylistSheet by viewModel.showAddToPlaylistSheet.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val playlistMessage by viewModel.addToPlaylistMessage.collectAsStateWithLifecycle()
    val playlistSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(playlistMessage) {
        playlistMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearAddToPlaylistMessage()
        }
    }

    val context = LocalContext.current
    DisposableEffect(isPlaying) {
        val window = (context as? Activity)?.window
        if (isPlaying) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Observe download errors
    val downloadError = state.downloadErrorMessage
    LaunchedEffect(downloadError) {
        if (!downloadError.isNullOrBlank()) {
            snackbarHostState.showSnackbar(downloadError)
            viewModel.clearDownloadError()
        }
    }

    val storagePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.startDownload()
        } else {
            Toast.makeText(context, "Storage permission optional — download uses app-private folder", Toast.LENGTH_LONG).show()
            viewModel.startDownload()
        }
    }

    fun requestDownload() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        ) {
            storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            viewModel.startDownload()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Now Playing") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleFavorite() }) {
                        Icon(
                            imageVector = if (state.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = if (state.isFavorite) "Unfavorite" else "Favorite",
                            tint = if (state.isFavorite) WatermelonRed else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (!state.isRadioStream) {
                        if (state.isDownloading) {
                            Box(
                                modifier = Modifier.size(48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    progress = { state.downloadProgress.coerceIn(0f, 1f) },
                                    modifier = Modifier.size(28.dp),
                                    strokeWidth = 2.dp,
                                    color = WatermelonRed
                                )
                            }
                        } else {
                            IconButton(onClick = { requestDownload() }) {
                                Icon(
                                    imageVector = Icons.Filled.Download,
                                    contentDescription = "Download",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    IconButton(onClick = { showTimerDialog = true }) {
                        if (sleepTimerText != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = sleepTimerText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = WatermelonRed
                                )
                                Icon(
                                    imageVector = Icons.Filled.Timer,
                                    contentDescription = "Sleep Timer",
                                    tint = WatermelonRed,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Timer,
                                contentDescription = "Sleep Timer",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (!state.isRadioStream) {
                        IconButton(onClick = { viewModel.onAddToPlaylistClick() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                                contentDescription = "Add to Playlist",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onQueueClick) {
                        Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = "Queue")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        val landscape = isLandscape()
        val maxWidth = adaptiveMaxContentWidth()
        val artFraction = adaptiveAlbumArtFraction()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .then(
                    if (maxWidth == androidx.compose.ui.unit.Dp.Unspecified)
                        Modifier.fillMaxWidth()
                    else
                        Modifier.widthIn(max = maxWidth).fillMaxWidth()
                )
                .padding(horizontal = adaptiveHorizontalPadding()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(if (landscape) 8.dp else 16.dp))

            // Animated artwork with shadow/glow
            Box(
                modifier = Modifier
                    .fillMaxWidth(artFraction)
                    .aspectRatio(1f),
                contentAlignment = Alignment.Center
            ) {
                // Glow behind artwork when playing
                if (isPlaying) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .scale(1.08f)
                            .clip(RoundedCornerShape(28.dp))
                            .alpha(0.15f)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(WatermelonRed, Color.Transparent),
                                    radius = 400f
                                )
                            )
                    )
                }

                Card(
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(pulseAnim),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    AsyncImage(
                        model = state.artworkUrl.takeIf { it.isNotBlank() }
                            ?: com.watermelon.core.designsystem.R.drawable.app_logo,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.height(if (landscape) 16.dp else 32.dp))

            // Title & Artist
            Text(
                text = state.currentTitle.takeIf { it.isNotBlank() } ?: "Unknown Title",
                style = MaterialTheme.typography.headlineMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(6.dp))
            if (viewModel.isAutoplayEnabled()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Autoplay",
                    style = MaterialTheme.typography.labelSmall,
                    color = WatermelonRed.copy(alpha = 0.8f)
                )
            }
            Text(
                text = state.currentArtist.takeIf { it.isNotBlank() } ?: "Unknown Artist",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Seekbar with duration labels — tap or drag to seek
            val currentProgress = remember(state.positionMs, state.durationMs) {
                if (state.durationMs > 0) state.positionMs.toFloat() / state.durationMs.toFloat() else 0f
            }
            val displayProgress = localProgress ?: currentProgress

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                var seekJob by remember { mutableStateOf<Job?>(null) }
                Slider(
                    value = displayProgress.coerceIn(0f, 1f),
                    onValueChange = {
                        val progress = it.coerceIn(0f, 1f)
                        localProgress = progress
                        // Debounce seek during drag; seek immediately on tap
                        seekJob?.cancel()
                        seekJob = scope.launch {
                            delay(50)
                            if (state.durationMs > 0) {
                                viewModel.seekTo((progress * state.durationMs).toLong())
                            }
                        }
                    },
                    onValueChangeFinished = {
                        seekJob?.cancel()
                        localProgress = null
                    },
                    enabled = state.durationMs > 0 && !state.isRadioStream,
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = WatermelonRed,
                        activeTrackColor = WatermelonRed,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    formatDuration(if (localProgress != null) (localProgress!! * state.durationMs).toLong() else state.positionMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    formatDuration(state.durationMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Playback controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        bottom = WindowInsets.navigationBars.asPaddingValues()
                            .calculateBottomPadding() + 8.dp
                    ),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.toggleShuffle() }) {
                    Icon(
                        imageVector = Icons.Filled.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (state.isShuffleOn) WatermelonRed else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(24.dp)
                    )
                }

                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.playPrevious()
                    },
                    enabled = state.hasPrevious,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.Filled.SkipPrevious,
                        contentDescription = "Previous",
                        modifier = Modifier.size(36.dp),
                        tint = if (state.hasPrevious) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                    )
                }

                // Main play button with red background
                FilledIconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.togglePlayPause()
                    },
                    modifier = Modifier.size(72.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = WatermelonRed,
                        contentColor = Color.White
                    ),
                    shape = CircleShape
                ) {
                    if (state.isBuffering) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            color = Color.White,
                            strokeWidth = 3.dp
                        )
                    } else {
                        Icon(
                            imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (state.isPlaying) "Pause" else "Play",
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.playNext()
                    },
                    enabled = state.hasNext,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.Filled.SkipNext,
                        contentDescription = "Next",
                        modifier = Modifier.size(36.dp),
                        tint = if (state.hasNext) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                    )
                }

                IconButton(onClick = { viewModel.toggleRepeat() }) {
                    val (icon, desc) = when (state.repeatMode) {
                        RepeatMode.ONE -> Icons.Filled.RepeatOne to "Repeat One"
                        else -> Icons.Filled.Repeat to "Repeat"
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = desc,
                        tint = if (state.repeatMode != RepeatMode.NONE) WatermelonRed else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Error card
            if (state.errorMessage != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = state.errorMessage ?: "",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = { viewModel.retryCurrent() }) {
                            Text("Retry")
                        }
                    }
                }
            }

            // Lyrics - only shown if available
            val lyrics = state.lyrics
            if (state.isLyricsLoading) {
                Spacer(modifier = Modifier.height(20.dp))
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    color = WatermelonRed,
                    strokeWidth = 2.dp
                )
            } else if (!lyrics.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(20.dp))
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp, max = 260.dp),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                    )
                ) {
                    val lyricsScroll = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .verticalScroll(lyricsScroll)
                    ) {
                        Text(
                            text = "Lyrics",
                            style = MaterialTheme.typography.titleMedium,
                            color = WatermelonRed
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = lyrics,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            if (state.durationMs > 0) {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = {
                        val shareText = buildString {
                            append("Listening to \"${state.currentTitle}\" by ${state.currentArtist} on Watermelon ")
                            append("🍉")
                        }
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        }
                        context.startActivity(Intent.createChooser(sendIntent, null))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Share,
                        contentDescription = "Share",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share this song")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
        }
    }

    if (showPlaylistSheet) {
        ModalBottomSheet(
            onDismissRequest = viewModel::onDismissAddToPlaylist,
            sheetState = playlistSheetState
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

    if (showTimerDialog) {
        AlertDialog(
            onDismissRequest = { showTimerDialog = false },
            title = { Text("Sleep Timer") },
            text = {
                Column {
                    listOf(15, 30, 45, 60).forEach { mins ->
                        TextButton(
                            onClick = {
                                viewModel.startSleepTimer(mins)
                                showTimerDialog = false
                            }
                        ) {
                            Text("$mins minutes")
                        }
                    }
                    if (sleepTimer != null) {
                        TextButton(
                            onClick = {
                                viewModel.cancelSleepTimer()
                                showTimerDialog = false
                            }
                        ) {
                            Text("Cancel Timer", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showTimerDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
