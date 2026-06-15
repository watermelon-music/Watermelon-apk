package com.watermelon.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.watermelon.domain.model.Playlist
import com.watermelon.domain.model.Song
import com.watermelon.domain.model.SubscriptionPlan
import com.watermelon.domain.repository.AuthRepository
import com.watermelon.domain.repository.PlaylistRepository
import com.watermelon.domain.repository.UserActionsRepository
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

    val userPlan: StateFlow<SubscriptionPlan> = authRepository.getCurrentUser()
        .map { it?.plan ?: SubscriptionPlan.FREE }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SubscriptionPlan.FREE)

    val canCreatePlaylist: StateFlow<Boolean> = kotlinx.coroutines.flow.combine(
        playlists,
        userPlan
    ) { list, plan ->
        val max = if (plan == SubscriptionPlan.FREE) 3 else 10
        list.size < max
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    suspend fun createPlaylist(name: String, description: String?) {
        playlistRepository.createPlaylist(name, description, coverUrl = null)
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
