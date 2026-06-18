package com.watermelon.feature.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.watermelon.domain.model.Playlist
import com.watermelon.domain.model.Song
import com.watermelon.domain.repository.AuthRepository
import com.watermelon.domain.repository.PlaylistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistDetailViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _playlist = MutableStateFlow<Playlist?>(null)
    val playlist: StateFlow<Playlist?> = _playlist.asStateFlow()

    val currentUserId: StateFlow<String?> = authRepository.getCurrentUser()
        .map { it?.id }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private var loadJob: Job? = null

    fun loadPlaylist(id: String) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            playlistRepository.getPlaylistById(id)
                .onSuccess { remote ->
                    _playlist.value = remote
                    val currentUserIdVal = authRepository.getCurrentUserId()
                    if (remote.ownerId == currentUserIdVal) {
                        playlistRepository.getUserPlaylists()
                            .map { playlists -> playlists.find { it.id == id } }
                            .collect { local ->
                                if (local != null) {
                                    _playlist.value = local
                                }
                            }
                    }
                }
                .onFailure {
                    _playlist.value = null
                }
        }
    }

    fun removeSong(playlistId: String, songId: String) {
        viewModelScope.launch {
            playlistRepository.removeSongFromPlaylist(playlistId, songId)
        }
    }

    fun importPlaylist(playlist: Playlist) {
        viewModelScope.launch {
            playlistRepository.createPlaylist(
                name = playlist.name,
                description = playlist.description,
                coverUrl = playlist.coverUrl
            ).onSuccess { newPlaylist ->
                playlist.songs.forEach { playlistSong ->
                    val song = Song(
                        id = playlistSong.songId,
                        title = playlistSong.title,
                        artistId = "",
                        artistName = playlistSong.artist,
                        albumId = null,
                        albumName = null,
                        durationMs = 0L,
                        coverUrl = playlistSong.coverUrl ?: "",
                        audioUrl = playlistSong.audioUrl ?: "",
                        genre = "",
                        releaseDate = ""
                    )
                    playlistRepository.addSongToPlaylist(newPlaylist.id, song)
                }
                loadPlaylist(newPlaylist.id)
            }
        }
    }
}
