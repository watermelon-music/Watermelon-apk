package com.watermelon.feature.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.watermelon.domain.repository.StreamingRepository
import com.watermelon.domain.repository.UrlExtractorRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlayerUiState(
    val isPlaying: Boolean = false,
    val currentTitle: String = "",
    val currentArtist: String = "",
    val artworkUrl: String = "",
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val isBuffering: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val streamingRepository: StreamingRepository,
    private val urlExtractor: UrlExtractorRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

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
    }

    init {
        streamingRepository.addListener(listener)
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

    fun togglePlayPause() {
        if (streamingRepository.isPlaying()) {
            streamingRepository.pause()
        } else {
            streamingRepository.resume()
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

    override fun onCleared() {
        super.onCleared()
        streamingRepository.removeListener(listener)
    }
}
