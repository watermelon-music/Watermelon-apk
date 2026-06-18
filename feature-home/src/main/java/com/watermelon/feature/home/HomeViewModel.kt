package com.watermelon.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.watermelon.data.remote.watermelon.WatermelonRepository
import com.watermelon.domain.model.Playlist
import com.watermelon.domain.model.Song
import com.watermelon.domain.repository.MusicCatalogRepository
import com.watermelon.domain.repository.PlaylistRepository
import com.watermelon.domain.repository.UserActionsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.TimeZone
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val musicCatalogRepository: MusicCatalogRepository,
    private val userActionsRepository: UserActionsRepository,
    private val playlistRepository: PlaylistRepository,
    private val watermelonRepository: WatermelonRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()

    private val _showAddToPlaylistSheet = MutableStateFlow(false)
    val showAddToPlaylistSheet: StateFlow<Boolean> = _showAddToPlaylistSheet.asStateFlow()

    private val _selectedSong = MutableStateFlow<Song?>(null)
    val selectedSong: StateFlow<Song?> = _selectedSong.asStateFlow()

    private val _addToPlaylistMessage = MutableStateFlow<String?>(null)
    val addToPlaylistMessage: StateFlow<String?> = _addToPlaylistMessage.asStateFlow()

    init {
        warmUpBackend()
        observeRecentlyPlayed()
        observeFavorites()
        loadHomeData()
        loadPlaylists()
        scheduleDailyTrendingRefresh()
    }

    private fun observeRecentlyPlayed() {
        userActionsRepository.getRecentlyPlayed()
            .catch { emit(emptyList()) }
            .onEach { recent -> _uiState.update { it.copy(recentlyPlayed = recent) } }
            .launchIn(viewModelScope)
    }

    private fun observeFavorites() {
        userActionsRepository.getFavorites()
            .catch { emit(emptyList()) }
            .onEach { fav -> _uiState.update { it.copy(favorites = fav) } }
            .launchIn(viewModelScope)
    }

    private fun loadPlaylists() {
        playlistRepository.getUserPlaylists()
            .catch { /* silently ignore */ }
            .onEach { _playlists.value = it }
            .launchIn(viewModelScope)
    }

    fun onAddToPlaylistClick(song: Song) {
        _selectedSong.value = song
        _showAddToPlaylistSheet.value = true
    }

    fun onDismissAddToPlaylist() {
        _showAddToPlaylistSheet.value = false
        _selectedSong.value = null
    }

    fun onPlaylistSelected(playlistId: String) {
        val song = _selectedSong.value ?: return
        viewModelScope.launch {
            val result = playlistRepository.addSongToPlaylist(playlistId, song)
            _addToPlaylistMessage.value = if (result.isSuccess) {
                "Added to playlist"
            } else {
                result.exceptionOrNull()?.message ?: "Failed to add song"
            }
            _showAddToPlaylistSheet.value = false
            _selectedSong.value = null
        }
    }

    fun clearAddToPlaylistMessage() {
        _addToPlaylistMessage.value = null
    }

    private fun warmUpBackend() {
        viewModelScope.launch {
            runCatching { watermelonRepository.ping() }
        }
    }

    /**
     * Schedules a refresh of the trending section every day at 5:30 AM IST.
     * IST = UTC+5:30, so 5:30 IST = 00:00 UTC.
     */
    private fun scheduleDailyTrendingRefresh() {
        viewModelScope.launch {
            val ist = TimeZone.getTimeZone("Asia/Kolkata")
            while (true) {
                val now = Calendar.getInstance(ist)
                val next = Calendar.getInstance(ist).apply {
                    set(Calendar.HOUR_OF_DAY, 5)
                    set(Calendar.MINUTE, 30)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                    // If 5:30 AM today has passed, target tomorrow
                    if (before(now)) add(Calendar.DAY_OF_YEAR, 1)
                }
                val delayMs = next.timeInMillis - now.timeInMillis
                delay(delayMs)
                loadHomeData()
                // Wait 24 h before next iteration (loop repeats)
                delay(24 * 60 * 60 * 1000L)
            }
        }
    }

    private fun loadHomeData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val trendingDeferred = async { runCatching { musicCatalogRepository.getTrendingMusic().first() }.getOrDefault(emptyList()) }

            val bollywoodDeferred = async { runCatching { musicCatalogRepository.getSongsByGenre("bollywood").first() }.getOrDefault(emptyList()) }
            val hollywoodDeferred = async { runCatching { musicCatalogRepository.getSongsByGenre("hollywood").first() }.getOrDefault(emptyList()) }
            val popDeferred = async { runCatching { musicCatalogRepository.getSongsByGenre("pop").first() }.getOrDefault(emptyList()) }
            val rockDeferred = async { runCatching { musicCatalogRepository.getSongsByGenre("rock").first() }.getOrDefault(emptyList()) }
            val jazzDeferred = async { runCatching { musicCatalogRepository.getSongsByGenre("jazz").first() }.getOrDefault(emptyList()) }
            val classicalDeferred = async { runCatching { musicCatalogRepository.getSongsByGenre("classical").first() }.getOrDefault(emptyList()) }
            val hiphopDeferred = async { runCatching { musicCatalogRepository.getSongsByGenre("hiphop").first() }.getOrDefault(emptyList()) }
            val electronicDeferred = async { runCatching { musicCatalogRepository.getSongsByGenre("electronic").first() }.getOrDefault(emptyList()) }

            val trending = trendingDeferred.await()

            _uiState.update { state ->
                state.copy(
                    isLoading = false,
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
