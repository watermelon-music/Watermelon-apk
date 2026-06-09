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
import kotlinx.coroutines.flow.StateFlow
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

    val playlists: StateFlow<List<Playlist>> = playlistRepository.getUserPlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
        plan != SubscriptionPlan.FREE || list.size < 3
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    suspend fun createPlaylist(name: String, description: String?) {
        playlistRepository.createPlaylist(name, description, coverUrl = null)
    }
}
