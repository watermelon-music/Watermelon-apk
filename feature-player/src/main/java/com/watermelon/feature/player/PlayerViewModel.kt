package com.watermelon.feature.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.watermelon.domain.model.Song
import com.watermelon.domain.repository.StreamingRepository
import com.watermelon.domain.repository.UrlExtractorRepository
import com.watermelon.domain.repository.UserActionsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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
    val isFavorite: Boolean = false
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val streamingRepository: StreamingRepository,
    private val urlExtractor: UrlExtractorRepository,
    private val userActionsRepository: UserActionsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queue: StateFlow<List<Song>> = _queue.asStateFlow()

    private val internalQueue = mutableListOf<Song>()
    private var originalQueue = listOf<Song>()
    private var currentIndex = -1
    private var isShuffleOn = false
    private var repeatMode = RepeatMode.NONE

    private val listener = object : StreamingRepository.Callback {
        override fun onPlaybackStateChanged(isBuffering: Boolean) {
            _uiState.update { it.copy(isBuffering = isBuffering) }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _uiState.update { it.copy(isPlaying = isPlaying) }
        }

        override fun onPositionDiscontinuity() {
            updatePosition()
        }

        override fun onDurationChanged(durationMs: Long) {
            _uiState.update { it.copy(durationMs = durationMs) }
        }

        override fun onPlaybackError(error: String) {
            _uiState.update {
                it.copy(isBuffering = false, isPlaying = false, errorMessage = error)
            }
        }

        override fun onPlaybackCompleted() {
            when (repeatMode) {
                RepeatMode.ONE -> {
                    streamingRepository.seekTo(0)
                    streamingRepository.resume()
                }
                else -> {
                    if (hasNextInternal()) {
                        playNextInternal()
                    } else if (repeatMode == RepeatMode.ALL && internalQueue.isNotEmpty()) {
                        currentIndex = 0
                        playCurrent()
                    } else {
                        _uiState.update { it.copy(isPlaying = false, positionMs = 0) }
                    }
                }
            }
        }
    }

    init {
        streamingRepository.addListener(listener)
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

    fun loadAndPlay(sourceUrl: String, title: String, artist: String, artwork: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBuffering = true, errorMessage = null) }
            val extractResult = urlExtractor.extractAudioUrl(sourceUrl)
            extractResult
                .onSuccess { directUrl ->
                    streamingRepository.play(directUrl)
                    _uiState.update {
                        it.copy(
                            currentTitle = title,
                            currentArtist = artist,
                            artworkUrl = artwork,
                            isBuffering = false
                        )
                    }
                }
                .onFailure { e ->
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
            val favorites = runCatching { userActionsRepository.getFavorites().first() }.getOrDefault(emptyList())
            _uiState.update { it.copy(isFavorite = favorites.any { f -> f.id == song.id }) }
        }
        loadAndPlay(song.audioUrl ?: "", song.title, song.artistName, song.coverUrl ?: "")
        _uiState.update { it.copy(currentSongId = song.id) }
        updateQueueState()
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
        }
    }

    fun playPrevious() {
        if (currentIndex > 0) {
            currentIndex--
            playCurrent()
        }
    }

    fun seekTo(positionMs: Long) {
        streamingRepository.seekTo(positionMs)
        updatePosition()
    }

    fun updatePosition() {
        _uiState.update {
            it.copy(
                positionMs = streamingRepository.currentPosition(),
                durationMs = if (streamingRepository.duration() > 0) streamingRepository.duration() else it.durationMs
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
        if (currentIndex < internalQueue.size - 1) {
            currentIndex++
            playCurrent()
        } else if (repeatMode == RepeatMode.ALL && internalQueue.isNotEmpty()) {
            currentIndex = 0
            playCurrent()
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
        val song = internalQueue.getOrNull(currentIndex) ?: return
        viewModelScope.launch {
            val favorites = runCatching { userActionsRepository.getFavorites().first() }.getOrDefault(emptyList())
            _uiState.update { it.copy(isFavorite = favorites.any { f -> f.id == song.id }) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        streamingRepository.removeListener(listener)
    }
}
