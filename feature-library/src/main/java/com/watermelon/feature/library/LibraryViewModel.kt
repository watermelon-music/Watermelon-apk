package com.watermelon.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.watermelon.domain.model.Playlist
import com.watermelon.domain.model.Song
import com.watermelon.domain.model.SubscriptionPlan
import com.watermelon.domain.repository.AuthRepository
import com.watermelon.domain.repository.PlaylistRepository
import com.watermelon.domain.repository.UserActionsRepository
import com.watermelon.domain.repository.DownloadRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository,
    private val downloadRepository: DownloadRepository,
    userActionsRepository: UserActionsRepository,
    authRepository: AuthRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val playlists: StateFlow<List<Playlist>> = playlistRepository.getUserPlaylists() as StateFlow<List<Playlist>>

    init {
        viewModelScope.launch {
            kotlinx.coroutines.delay(800)
            _isLoading.value = false
        }
    }

    val favorites: StateFlow<List<Song>> = userActionsRepository.getFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentlyPlayed: StateFlow<List<Song>> = userActionsRepository.getRecentlyPlayed()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val downloadedSongs: StateFlow<List<Song>> = downloadRepository.getDownloads()
        .map { list ->
            list.map { entity ->
                Song(
                    id = entity.songId,
                    title = entity.title,
                    artistId = entity.artist,
                    artistName = entity.artist,
                    albumId = null,
                    albumName = null,
                    durationMs = 0L,
                    coverUrl = entity.coverUrl,
                    audioUrl = entity.localFilePath,
                    genre = null,
                    releaseDate = null
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteDownload(songId: String) {
        viewModelScope.launch {
            downloadRepository.deleteDownload(songId)
        }
    }

    val userPlan: StateFlow<SubscriptionPlan> = authRepository.getCurrentUser()
        .map { it?.plan ?: SubscriptionPlan.FREE }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SubscriptionPlan.FREE)

    val canCreatePlaylist: StateFlow<Boolean> = kotlinx.coroutines.flow.combine(
        playlists,
        userPlan
    ) { list, plan ->
        val max = if (plan == SubscriptionPlan.FREE) 2 else 5
        list.size < max
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    suspend fun createPlaylist(name: String, description: String?): Result<Playlist> {
        if (!canCreatePlaylist.value) {
            return Result.failure(IllegalStateException("Playlist limit reached. Upgrade to Premium."))
        }
        return playlistRepository.createPlaylist(name, description, coverUrl = null).also {
            it.onSuccess { _createMessage.value = "Playlist created" }
            it.onFailure { e -> _createMessage.value = e.message ?: "Failed to create playlist" }
        }
    }

    private val _createMessage = MutableStateFlow<String?>(null)
    val createMessage: StateFlow<String?> = _createMessage.asStateFlow()

    fun clearCreateMessage() {
        _createMessage.value = null
    }

    fun deletePlaylist(playlistId: String) {
        viewModelScope.launch {
            playlistRepository.deletePlaylist(playlistId)
        }
    }

    fun sharePlaylist(playlistId: String, onResult: (String) -> Unit) {
        viewModelScope.launch {
            playlistRepository.sharePlaylist(playlistId)
                .onSuccess { onResult(it) }
        }
    }

    fun editPlaylist(playlistId: String, name: String, description: String?) {
        viewModelScope.launch {
            playlistRepository.editPlaylist(playlistId, name, description)
        }
    }

    fun removeSongFromPlaylist(playlistId: String, songId: String) {
        viewModelScope.launch {
            playlistRepository.removeSongFromPlaylist(playlistId, songId)
        }
    }

    fun addSongToPlaylist(playlistId: String, song: Song) {
        viewModelScope.launch {
            playlistRepository.addSongToPlaylist(playlistId, song)
        }
    }
}
