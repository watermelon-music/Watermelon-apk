package com.watermelon.feature.player

import android.content.Context
import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.watermelon.domain.model.Song
import com.watermelon.domain.autoplay.AutoplayEngine
import com.watermelon.domain.autoplay.TransitionTracker
import com.watermelon.domain.repository.DownloadRepository
import com.watermelon.domain.repository.LyricsRepository
import com.watermelon.domain.repository.RadioStationRepository
import com.watermelon.domain.repository.StreamingRepository
import com.watermelon.domain.repository.UrlExtractorRepository
import com.watermelon.domain.repository.UserActionsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject

enum class RepeatMode { NONE, ONE, ALL }

data class PlayerUiState(
    val isPlaying: Boolean = false,
    val currentTitle: String = "",
    val currentArtist: String = "",
    val artworkUrl: String = "",
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val isBuffering: Boolean = false,
    val errorMessage: String? = null,
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false,
    val isShuffleOn: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.NONE,
    val currentSongId: String = "",
    val isFavorite: Boolean = false,
    val lyrics: String? = null,
    val isLyricsLoading: Boolean = false,
    val isDownloading: Boolean = false,
    val downloadProgress: Float = 0f,
    val downloadErrorMessage: String? = null,
    val isRadioStream: Boolean = false
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient,
    private val streamingRepository: StreamingRepository,
    private val urlExtractor: UrlExtractorRepository,
    private val userActionsRepository: UserActionsRepository,
    private val radioStationRepository: RadioStationRepository,
    private val lyricsRepository: LyricsRepository,
    private val catalogRepository: com.watermelon.domain.repository.MusicCatalogRepository,
    private val playlistRepository: com.watermelon.domain.repository.PlaylistRepository,
    private val downloadRepository: com.watermelon.domain.repository.DownloadRepository,
    private val autoplayEngine: AutoplayEngine,
    private val transitionTracker: TransitionTracker
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queue: StateFlow<List<Song>> = _queue.asStateFlow()

    private val _sleepTimerRemainingSeconds = MutableStateFlow<Int?>(null)
    val sleepTimerRemainingSeconds: StateFlow<Int?> = _sleepTimerRemainingSeconds.asStateFlow()

    private val _showAddToPlaylistSheet = MutableStateFlow(false)
    val showAddToPlaylistSheet: StateFlow<Boolean> = _showAddToPlaylistSheet.asStateFlow()

    private val _playlists = MutableStateFlow<List<com.watermelon.domain.model.Playlist>>(emptyList())
    val playlists: StateFlow<List<com.watermelon.domain.model.Playlist>> = _playlists.asStateFlow()

    private val _addToPlaylistMessage = MutableStateFlow<String?>(null)
    val addToPlaylistMessage: StateFlow<String?> = _addToPlaylistMessage.asStateFlow()

    private val internalQueue = mutableListOf<Song>()
    private var originalQueue = listOf<Song>()
    private var currentIndex = -1
    private var isShuffleOn = false
    private var repeatMode = RepeatMode.NONE
    private var consecutiveErrors = 0
    private var currentExtractionJob: Job? = null
    private val MAX_CONSECUTIVE_ERRORS = 3

    private var positionUpdateJob: Job? = null
    private var currentRadioStation: com.watermelon.domain.model.RadioStation? = null

    private fun startPositionUpdates() {
        if (positionUpdateJob?.isActive == true) return
        positionUpdateJob = viewModelScope.launch {
            while (true) {
                updatePosition()
                // Higher frequency while actively playing to keep the slider smooth.
                delay(if (streamingRepository.isPlaying()) 250L else 750L)
            }
        }
    }

    private fun stopPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = null
    }

    private val listener = object : StreamingRepository.Callback {
        override fun onPlaybackStateChanged(isBuffering: Boolean) {
            _uiState.update { it.copy(isBuffering = isBuffering) }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) {
                consecutiveErrors = 0
            }
            // Keep updating even when paused so the slider stays in sync after a seek.
            startPositionUpdates()
            _uiState.update { it.copy(isPlaying = isPlaying) }
        }

        override fun onPositionDiscontinuity() {
            updatePosition()
        }

        override fun onDurationChanged(durationMs: Long) {
            _uiState.update { it.copy(durationMs = durationMs) }
        }

        override fun onPlaybackError(error: String) {
            consecutiveErrors++
            _uiState.update {
                            it.copy(isBuffering = false, isPlaying = false, errorMessage = "Playback error. Retrying...")
            }
            if (consecutiveErrors > MAX_CONSECUTIVE_ERRORS) {
                Timber.e("Max consecutive errors reached. Stopping auto-retry.")
                return
            }
            viewModelScope.launch {
                try {
                    delay(3000)
                    _uiState.update { it.copy(errorMessage = null) }
                    if (consecutiveErrors <= 2) {
                        // Invalidate cached URL before retry — Google Video URLs expire quickly
                        val currentSong = internalQueue.getOrNull(currentIndex)
                        if (currentSong != null) {
                            val sourceUrl = currentSong.audioUrl?.takeIf { it.isNotBlank() }
                                ?: "https://www.youtube.com/watch?v=${currentSong.id}"
                            urlExtractor.invalidateCache(sourceUrl)
                        }
                        retryCurrent()
                    } else if (hasNextInternal()) {
                        consecutiveErrors = 0
                        playNextInternal()
                    }
                } catch (_: Exception) {
                    // Prevent crash from unexpected error during auto-skip
                }
            }
        }

        override fun onPlaybackCompleted() {
            when (repeatMode) {
                RepeatMode.ONE -> {
                    streamingRepository.seekTo(0)
                    streamingRepository.resume()
                }
                RepeatMode.ALL -> {
                    if (hasNextInternal()) {
                        playNextInternal()
                    } else if (internalQueue.isNotEmpty()) {
                        currentIndex = 0
                        playCurrent()
                    }
                }
                RepeatMode.NONE -> {
                    if (hasNextInternal()) {
                        playNextInternal()
                    } else if (internalQueue.size > 1) { // Implicitly repeat playlists by default if they are > 1
                        currentIndex = 0
                        playCurrent()
                    } else {
                        val lastSong = internalQueue.getOrNull(currentIndex)
                        if (lastSong != null) {
                            _uiState.update { it.copy(isBuffering = true) }
                            viewModelScope.launch {
                                smartRefillQueue(lastSong)
                            }
                        } else {
                            _uiState.update { it.copy(isPlaying = false, positionMs = 0) }
                        }
                    }
                }
            }
        }
    }

    init {
        streamingRepository.addListener(listener)
        loadPlaylists()
        updatePosition()
        // Always run the position updater while the ViewModel is alive; the loop
        // throttles itself when nothing is playing.
        startPositionUpdates()
    }

    private fun loadPlaylists() {
        playlistRepository.getUserPlaylists()
            .catch { /* silently ignore */ }
            .onEach { _playlists.value = it }
            .launchIn(viewModelScope)
    }

    fun onAddToPlaylistClick() {
        _showAddToPlaylistSheet.value = true
    }

    fun onDismissAddToPlaylist() {
        _showAddToPlaylistSheet.value = false
    }

    fun onPlaylistSelected(playlistId: String) {
        val song = internalQueue.getOrNull(currentIndex) ?: return
        viewModelScope.launch {
            val result = playlistRepository.addSongToPlaylist(playlistId, song)
            _addToPlaylistMessage.value = if (result.isSuccess) {
                "Added to playlist"
            } else {
                result.exceptionOrNull()?.message ?: "Failed to add song"
            }
            _showAddToPlaylistSheet.value = false
        }
    }

    fun clearAddToPlaylistMessage() {
        _addToPlaylistMessage.value = null
    }

    fun playSong(song: Song) {
        playQueue(listOf(song), 0)
    }

    fun playQueue(songs: List<Song>, startIndex: Int = 0) {
        if (songs.isEmpty()) return
        originalQueue = songs.toList()
        internalQueue.clear()
        internalQueue.addAll(songs)
        currentIndex = startIndex.coerceIn(0, internalQueue.size - 1)
        if (isShuffleOn) {
            shuffleQueue()
        }
        playCurrent()
    }

    fun loadAndPlay(
        sourceUrl: String,
        title: String,
        artist: String,
        artwork: String,
        songId: String = "",
        isRadioStream: Boolean = false,
        durationMs: Long = 0L,
        radioStation: com.watermelon.domain.model.RadioStation? = null
    ) {
        currentRadioStation = if (isRadioStream) radioStation else null
        if (isRadioStream && radioStation != null) {
            viewModelScope.launch {
                val favs = runCatching { radioStationRepository.getFavoriteStations().first() }.getOrDefault(emptyList())
                val uuid = radioStation.stationuuid ?: "${radioStation.name}_${radioStation.url}"
                val isFav = favs.any { (it.stationuuid ?: "${it.name}_${it.url}") == uuid }
                _uiState.update { it.copy(isFavorite = isFav) }
            }
        }
        currentExtractionJob?.cancel()
        consecutiveErrors = 0
        currentExtractionJob = viewModelScope.launch {
            _uiState.update { 
                it.copy(
                    isBuffering = true, 
                    errorMessage = null, 
                    isRadioStream = isRadioStream,
                    positionMs = 0L,
                    durationMs = if (durationMs > 0) durationMs else 0L
                ) 
            }
            if (sourceUrl.startsWith("content://") ||
                sourceUrl.startsWith("file:") ||
                sourceUrl.startsWith("/")) {
                // Normalize local-file inputs to a canonical URI that ExoPlayer's
                // DefaultDataSource can read for seeking. content:// URIs are passed
                // through as-is so the system content resolver handles them.
                val playUrl = when {
                    sourceUrl.startsWith("content://") -> sourceUrl
                    sourceUrl.startsWith("/") -> android.net.Uri.fromFile(File(sourceUrl)).toString()
                    sourceUrl.startsWith("file:///") -> sourceUrl
                    sourceUrl.startsWith("file://") -> sourceUrl
                    sourceUrl.startsWith("file:/") -> "file:///" + sourceUrl.substringAfter("file:/")
                    else -> sourceUrl
                }
                Timber.d("Local playback requested. playUrl: $playUrl")
                streamingRepository.play(playUrl, title, artist, artwork)
                _uiState.update {
                    it.copy(
                        currentTitle = title,
                        currentArtist = artist,
                        artworkUrl = artwork,
                        currentSongId = songId,
                        isBuffering = false,
                        positionMs = 0L,
                        // Keep any known duration from metadata until the player reports the actual one.
                        durationMs = if (durationMs > 0) durationMs else 0L
                    )
                }
                return@launch
            }
            runCatching {
                val extractResult = urlExtractor.extractAudioUrl(sourceUrl)
                extractResult
                    .onSuccess { directUrl ->
                        val cleanUrl = directUrl.replace(Regex("[?&]range=[^&]*"), "")
                            .replace(Regex("[?&]$"), "")
                        streamingRepository.play(cleanUrl, title, artist, artwork)
                        _uiState.update {
                            it.copy(
                                currentTitle = title,
                                currentArtist = artist,
                                artworkUrl = artwork,
                                currentSongId = songId,
                                isBuffering = false,
                                positionMs = 0L,
                                durationMs = if (durationMs > 0) durationMs else 0L
                            )
                        }
                    }
                    .onFailure { e ->
                        _uiState.update {
                            it.copy(isBuffering = false, errorMessage = "Something went wrong. Please try again.")
                        }
                    }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(isBuffering = false, errorMessage = e.localizedMessage ?: "Playback error")
                }
            }
        }
    }

    private fun playCurrent() {
        val song = internalQueue.getOrNull(currentIndex) ?: return
        viewModelScope.launch {
            runCatching { userActionsRepository.recordRecentlyPlayed(song) }
            runCatching { transitionTracker.recordPlayStart(song) }
            val favorites = runCatching { userActionsRepository.getFavorites().first() }.getOrDefault(emptyList())
            _uiState.update { 
                it.copy(
                    isFavorite = favorites.any { f -> f.id == song.id },
                    positionMs = 0L,
                    durationMs = if (song.durationMs > 0) song.durationMs else 0L
                ) 
            }

            val localPath = runCatching { downloadRepository.getDownloadPath(song.id) }.getOrNull()
            val audioUrl = when {
                localPath == null -> {
                    song.audioUrl?.takeIf { it.isNotBlank() }
                        ?: "https://www.youtube.com/watch?v=${song.id}"
                }
                localPath.startsWith("content://") -> {
                    localPath
                }
                else -> {
                    val localFile = File(localPath)
                    if (localFile.exists()) android.net.Uri.fromFile(localFile).toString()
                    else song.audioUrl?.takeIf { it.isNotBlank() }
                        ?: "https://www.youtube.com/watch?v=${song.id}"
                }
            }
            loadAndPlay(
                sourceUrl = audioUrl,
                title = song.title,
                artist = song.artistName,
                artwork = song.coverUrl ?: "",
                songId = song.id,
                isRadioStream = false,
                durationMs = song.durationMs,
                radioStation = null
            )
            updateQueueState()
            fetchLyrics(song)
        }
    }

    private fun fetchLyrics(song: Song) {
        viewModelScope.launch {
            _uiState.update { it.copy(lyrics = null, isLyricsLoading = true) }
            val result = lyricsRepository.getLyrics(song.artistName, song.title)
            result
                .onSuccess { lyrics ->
                    _uiState.update { it.copy(lyrics = lyrics, isLyricsLoading = false) }
                }
                .onFailure {
                    _uiState.update { it.copy(lyrics = null, isLyricsLoading = false) }
                }
        }
    }

    fun retryCurrent() {
        if (currentIndex in internalQueue.indices) {
            playCurrent()
        }
    }

    fun togglePlayPause() {
        if (streamingRepository.isPlaying()) {
            streamingRepository.pause()
        } else {
            streamingRepository.resume()
        }
    }

    fun playNext() {
        if (hasNextInternal()) {
            playNextInternal()
        } else {
            val currentSong = internalQueue.getOrNull(currentIndex)
            if (currentSong != null) {
                viewModelScope.launch {
                    smartRefillQueue(currentSong)
                }
            }
        }
    }

    fun playPrevious() {
        if (currentIndex > 0) {
            currentIndex--
            playCurrent()
        } else if (internalQueue.isNotEmpty()) {
            currentIndex = internalQueue.size - 1
            playCurrent()
        }
    }

    fun seekTo(positionMs: Long) {
        val duration = _uiState.value.durationMs
        val clamped = if (duration > 0) positionMs.coerceIn(0L, duration) else positionMs.coerceAtLeast(0L)
        // Optimistically reflect the seek target so the slider doesn't snap back
        // before the player's discontinuity callback arrives.
        _uiState.update { it.copy(positionMs = clamped) }
        streamingRepository.seekTo(clamped)
        // Re-sync from the player once it acknowledges the seek (covers paused state too).
        viewModelScope.launch {
            delay(50)
            updatePosition()
        }
    }

    fun updatePosition() {
        val pos = streamingRepository.currentPosition().coerceAtLeast(0L)
        val dur = streamingRepository.duration()
        _uiState.update {
            it.copy(
                positionMs = pos,
                durationMs = if (dur > 0) dur else it.durationMs
            )
        }
    }

    fun setVolume(volume: Float) {
        streamingRepository.setVolume(volume)
    }

    fun toggleShuffle() {
        isShuffleOn = !isShuffleOn
        if (isShuffleOn) {
            shuffleQueue()
        } else {
            val currentSong = internalQueue.getOrNull(currentIndex)
            internalQueue.clear()
            internalQueue.addAll(originalQueue)
            currentIndex = internalQueue.indexOfFirst { it.id == currentSong?.id }.coerceAtLeast(0)
        }
        updateQueueState()
    }

    fun toggleRepeat() {
        repeatMode = when (repeatMode) {
            RepeatMode.NONE -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.NONE
        }
        updateQueueState()
    }

    fun toggleFavorite() {
        if (_uiState.value.isRadioStream) {
            val station = currentRadioStation ?: return
            viewModelScope.launch {
                val uuid = station.stationuuid ?: "${station.name}_${station.url}"
                runCatching {
                    if (_uiState.value.isFavorite) {
                        radioStationRepository.removeFavorite(uuid)
                    } else {
                        radioStationRepository.addFavorite(station)
                    }
                }
                val favs = runCatching { radioStationRepository.getFavoriteStations().first() }.getOrDefault(emptyList())
                _uiState.update { it.copy(isFavorite = favs.any { (it.stationuuid ?: "${it.name}_${it.url}") == uuid }) }
            }
            return
        }
        val song = internalQueue.getOrNull(currentIndex) ?: return
        viewModelScope.launch {
            runCatching {
                if (_uiState.value.isFavorite) {
                    userActionsRepository.removeFromFavorites(song.id)
                } else {
                    userActionsRepository.addToFavorites(song)
                }
            }
            val favorites = runCatching { userActionsRepository.getFavorites().first() }.getOrDefault(emptyList())
            _uiState.update { it.copy(isFavorite = favorites.any { f -> f.id == song.id }) }
        }
    }

    fun jumpToQueueIndex(index: Int) {
        if (index in internalQueue.indices) {
            currentIndex = index
            playCurrent()
        }
    }

    private fun hasNextInternal(): Boolean {
        return currentIndex < internalQueue.size - 1 || (repeatMode == RepeatMode.ALL && internalQueue.isNotEmpty())
    }

    private fun playNextInternal() {
        val previousSong = internalQueue.getOrNull(currentIndex)
        if (currentIndex < internalQueue.size - 1) {
            currentIndex++
            playCurrent()
        } else if (repeatMode == RepeatMode.ALL && internalQueue.isNotEmpty()) {
            currentIndex = 0
            playCurrent()
        }
        val nextSong = internalQueue.getOrNull(currentIndex)
        if (previousSong != null && nextSong != null && previousSong.id != nextSong.id) {
            viewModelScope.launch {
                runCatching { transitionTracker.recordTransition(previousSong.id, nextSong.id) }
            }
        }
        // Proactive refill: keep at least 10 future songs queued
        val remaining = internalQueue.size - currentIndex - 1
        if (remaining < 10) {
            val currentSong = internalQueue.getOrNull(currentIndex)
            if (currentSong != null) {
                viewModelScope.launch {
                    runCatching {
                        val excludeIds = internalQueue.map { it.id }.toSet()
                        val batch = mutableListOf<Song>()
                        repeat(10 - remaining) {
                            autoplayEngine.findNextSong(currentSong, excludeIds + batch.map { it.id })?.let { batch.add(it) }
                        }
                        if (batch.isNotEmpty()) {
                            internalQueue.addAll(batch)
                            originalQueue = internalQueue.toList()
                            updateQueueState()
                        }
                    }
                }
            }
        }
    }

    private fun shuffleQueue() {
        val currentSong = internalQueue.getOrNull(currentIndex)
        internalQueue.shuffle()
        if (currentSong != null) {
            val idx = internalQueue.indexOfFirst { it.id == currentSong.id }
            if (idx > 0) {
                internalQueue.removeAt(idx)
                internalQueue.add(0, currentSong)
            }
        }
        currentIndex = 0
    }

    private fun updateQueueState() {
        _queue.value = internalQueue.toList()
        _uiState.update {
            it.copy(
                hasNext = hasNextInternal(),
                hasPrevious = currentIndex > 0,
                isShuffleOn = isShuffleOn,
                repeatMode = repeatMode,
                currentSongId = internalQueue.getOrNull(currentIndex)?.id ?: ""
            )
        }
    }

    fun checkFavoriteStatus() {
        if (_uiState.value.isRadioStream) {
            val station = currentRadioStation ?: return
            viewModelScope.launch {
                val favs = runCatching { radioStationRepository.getFavoriteStations().first() }.getOrDefault(emptyList())
                val uuid = station.stationuuid ?: "${station.name}_${station.url}"
                _uiState.update { it.copy(isFavorite = favs.any { (it.stationuuid ?: "${it.name}_${it.url}") == uuid }) }
            }
            return
        }
        val song = internalQueue.getOrNull(currentIndex) ?: return
        viewModelScope.launch {
            val favorites = runCatching { userActionsRepository.getFavorites().first() }.getOrDefault(emptyList())
            _uiState.update { it.copy(isFavorite = favorites.any { f -> f.id == song.id }) }
        }
    }

    private fun smartRefillQueue(lastSong: Song) {
        // AutoplayRepositoryImpl maintains an internal batch cache.
        // Calling findNextSong refills the cache automatically when it drops below 10 songs.
        viewModelScope.launch {
            val excludeIds = internalQueue.map { it.id }.toSet()
            val nextSong = runCatching {
                autoplayEngine.findNextSong(lastSong, excludeIds)
            }.getOrNull()
            if (nextSong != null) {
                internalQueue.add(nextSong)
                originalQueue = internalQueue.toList()
                currentIndex++
                playCurrent()
            } else {
                _uiState.update { it.copy(isPlaying = false, positionMs = 0, isBuffering = false) }
            }
        }
    }

    fun startDownload() {
        val song = internalQueue.getOrNull(currentIndex) ?: return
        val sourceUrl = song.audioUrl?.takeIf { it.isNotBlank() }
            ?: "https://www.youtube.com/watch?v=${song.id}"
        viewModelScope.launch {
            _uiState.update { it.copy(isDownloading = true, downloadProgress = 0f, downloadErrorMessage = null) }
            runCatching {
                val directUrl = urlExtractor.extractAudioUrl(sourceUrl).getOrThrow()
                withContext(Dispatchers.IO) {
                    downloadToPrivateStorage(song, directUrl)
                }
            }.onSuccess {
                _uiState.update { it.copy(isDownloading = false, downloadProgress = 1f, downloadErrorMessage = null) }
            }.onFailure { e ->
                Timber.e(e, "Download failed")
                _uiState.update { it.copy(isDownloading = false, downloadProgress = 0f, downloadErrorMessage = e.localizedMessage ?: "Download failed") }
            }
        }
    }

    private suspend fun downloadToPrivateStorage(song: Song, url: String) {
        val tempDir = context.cacheDir
        val tempFile = File(tempDir, "${song.id}.mp3")
        if (tempFile.exists()) tempFile.delete()

        val request = okhttp3.Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; SM-S918B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Mobile Safari/537.36")
            .build()
        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Timber.e("Download failed with HTTP ${response.code}: ${response.message}")
                throw IOException("Server returned ${response.code}")
            }
            val body = response.body ?: throw IOException("Empty response")
            val totalBytes = body.contentLength()
            Timber.d("Starting download. Total size: $totalBytes bytes")

            body.byteStream().use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(8192)
                    var downloadedBytes = 0L
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        downloadedBytes += read
                        if (totalBytes > 0) {
                            val progress = downloadedBytes.toFloat() / totalBytes.toFloat()
                            _uiState.update { it.copy(downloadProgress = progress) }
                        }
                    }
                }
            }
        }

        val fileSize = tempFile.length()

        val musicDir = File(context.filesDir, "downloads")
        if (!musicDir.exists()) musicDir.mkdirs()
        val destFile = File(musicDir, "${song.id}.mp3")
        if (destFile.exists()) destFile.delete()

        if (!tempFile.renameTo(destFile)) {
            tempFile.inputStream().use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            tempFile.delete()
        }

        // Record in database so DownloadsScreen can show it
        downloadRepository.recordDownload(song, destFile.absolutePath, fileSize)
    }

    private var sleepTimerJob: Job? = null

    fun startSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        sleepTimerJob = viewModelScope.launch {
            var remaining = minutes * 60
            _sleepTimerRemainingSeconds.value = remaining
            while (remaining > 0 && _sleepTimerRemainingSeconds.value != null) {
                delay(1000L)
                remaining--
                _sleepTimerRemainingSeconds.value = remaining
            }
            if (_sleepTimerRemainingSeconds.value != null) {
                streamingRepository.pause()
                _uiState.update { it.copy(isPlaying = false) }
                _sleepTimerRemainingSeconds.value = null
            }
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        _sleepTimerRemainingSeconds.value = null
    }

    /**
     * Stop playback and wipe player state. Intended for sign-out so the mini-player
     * doesn't keep showing the previous user's last track.
     */
    fun clearPlayer() {
        currentExtractionJob?.cancel()
        sleepTimerJob?.cancel()
        runCatching { streamingRepository.stop() }
        internalQueue.clear()
        originalQueue = emptyList()
        currentIndex = -1
        consecutiveErrors = 0
        _queue.value = emptyList()
        _sleepTimerRemainingSeconds.value = null
        _uiState.value = PlayerUiState()
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun clearDownloadError() {
        _uiState.update { it.copy(downloadErrorMessage = null) }
    }

    fun isAutoplayEnabled(): Boolean = autoplayEngine.isAutoplayEnabled()

    override fun onCleared() {
        super.onCleared()
        stopPositionUpdates()
        streamingRepository.removeListener(listener)
    }
}
