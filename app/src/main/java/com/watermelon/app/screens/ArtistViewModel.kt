package com.watermelon.app.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.watermelon.domain.model.Artist
import com.watermelon.domain.model.Song
import com.watermelon.domain.repository.ArtistRepository
import com.watermelon.feature.player.PlayerViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.URLDecoder
import javax.inject.Inject

@HiltViewModel
class ArtistViewModel @Inject constructor(
    private val artistRepository: ArtistRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val channelUrl: String = savedStateHandle.get<String>("channelUrl")?.let {
        URLDecoder.decode(it, "UTF-8")
    } ?: ""

    private val _artist = MutableStateFlow<Artist?>(null)
    val artist: StateFlow<Artist?> = _artist.asStateFlow()

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadArtist()
    }

    private fun loadArtist() {
        viewModelScope.launch {
            _isLoading.value = true
            artistRepository.getArtistDetails(channelUrl).onSuccess { _artist.value = it }
            artistRepository.getArtistSongs(channelUrl).onSuccess { _songs.value = it }
            _isLoading.value = false
        }
    }

    fun playAll(playerViewModel: PlayerViewModel) {
        val list = _songs.value
        if (list.isNotEmpty()) playerViewModel.playQueue(list, 0)
    }
}