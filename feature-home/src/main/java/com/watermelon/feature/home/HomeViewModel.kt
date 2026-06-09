package com.watermelon.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.watermelon.data.remote.watermelon.WatermelonRepository
import com.watermelon.domain.model.Song
import com.watermelon.domain.repository.MusicCatalogRepository
import com.watermelon.domain.repository.UserActionsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val musicCatalogRepository: MusicCatalogRepository,
    private val userActionsRepository: UserActionsRepository,
    private val watermelonRepository: WatermelonRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        warmUpBackend()
        loadHomeData()
    }

    private fun warmUpBackend() {
        viewModelScope.launch {
            runCatching { watermelonRepository.ping() }
        }
    }

    private fun loadHomeData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val recentlyPlayedDeferred = async { userActionsRepository.getRecentlyPlayed().first() }
            val favoritesDeferred = async { userActionsRepository.getFavorites().first() }
            val trendingDeferred = async { musicCatalogRepository.getTrendingMusic().first() }

            val bollywoodDeferred = async { musicCatalogRepository.getSongsByGenre("bollywood").first() }
            val hollywoodDeferred = async { musicCatalogRepository.getSongsByGenre("hollywood").first() }
            val popDeferred = async { musicCatalogRepository.getSongsByGenre("pop").first() }
            val rockDeferred = async { musicCatalogRepository.getSongsByGenre("rock").first() }
            val jazzDeferred = async { musicCatalogRepository.getSongsByGenre("jazz").first() }
            val classicalDeferred = async { musicCatalogRepository.getSongsByGenre("classical").first() }
            val hiphopDeferred = async { musicCatalogRepository.getSongsByGenre("hiphop").first() }
            val electronicDeferred = async { musicCatalogRepository.getSongsByGenre("electronic").first() }

            val recentlyPlayed = recentlyPlayedDeferred.await()
            val favorites = favoritesDeferred.await()
            val trending = trendingDeferred.await()

            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    recentlyPlayed = recentlyPlayed,
                    favorites = favorites,
                    trendingMusic = trending,
                    bollywood = bollywoodDeferred.await().take(7),
                    hollywood = hollywoodDeferred.await().take(7),
                    pop = popDeferred.await().take(7),
                    rock = rockDeferred.await().take(7),
                    jazz = jazzDeferred.await().take(7),
                    classical = classicalDeferred.await().take(7),
                    hiphop = hiphopDeferred.await().take(7),
                    electronic = electronicDeferred.await().take(7)
                )
            }
        }
    }
}

data class HomeUiState(
    val welcomeMessage: String = "Welcome back",
    val recentlyPlayed: List<Song> = emptyList(),
    val favorites: List<Song> = emptyList(),
    val trendingMusic: List<Song> = emptyList(),
    val bollywood: List<Song> = emptyList(),
    val hollywood: List<Song> = emptyList(),
    val pop: List<Song> = emptyList(),
    val rock: List<Song> = emptyList(),
    val jazz: List<Song> = emptyList(),
    val classical: List<Song> = emptyList(),
    val hiphop: List<Song> = emptyList(),
    val electronic: List<Song> = emptyList(),
    val isLoading: Boolean = false
)
