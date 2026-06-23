package com.watermelon.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.watermelon.data.remote.watermelon.WatermelonRepository
import com.watermelon.domain.model.Artist
import com.watermelon.domain.model.CommunityPlaylist
import com.watermelon.domain.model.CuratedPlaylist
import com.watermelon.domain.model.User
import com.watermelon.domain.repository.AuthRepository
import com.watermelon.domain.model.Playlist
import com.watermelon.domain.model.Song
import com.watermelon.domain.repository.ArtistRepository
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
import timber.log.Timber
import java.util.Calendar
import java.util.TimeZone
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val musicCatalogRepository: MusicCatalogRepository,
    private val userActionsRepository: UserActionsRepository,
    private val playlistRepository: PlaylistRepository,
    private val artistRepository: ArtistRepository,
    private val watermelonRepository: WatermelonRepository,
    private val authRepository: AuthRepository
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

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    init {
        warmUpBackend()
        observeRecentlyPlayed()
        observeFavorites()
        loadHomeData()
        loadUser()
        loadPlaylists()
        loadCommunityPlaylists()
        loadCuratedPlaylists()
        loadTrendingArtists()
        loadLanguagePlaylists()
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
        _uiState.update { it.copy(isLoading = true) }

        musicCatalogRepository.getTrendingMusic()
            .catch { emit(emptyList()) }
            .onEach { songs ->
                _uiState.update { it.copy(trendingMusic = songs, isLoading = false) }
            }
            .launchIn(viewModelScope)

        val genres = listOf(
            "bollywood" to { songs: List<Song> -> _uiState.update { it.copy(bollywood = songs.take(7)) } },
            "hollywood" to { songs: List<Song> -> _uiState.update { it.copy(hollywood = songs.take(7)) } },
            "pop" to { songs: List<Song> -> _uiState.update { it.copy(pop = songs.take(7)) } },
            "rock" to { songs: List<Song> -> _uiState.update { it.copy(rock = songs.take(7)) } },
            "jazz" to { songs: List<Song> -> _uiState.update { it.copy(jazz = songs.take(7)) } },
            "classical" to { songs: List<Song> -> _uiState.update { it.copy(classical = songs.take(7)) } },
            "hiphop" to { songs: List<Song> -> _uiState.update { it.copy(hiphop = songs.take(7)) } },
            "electronic" to { songs: List<Song> -> _uiState.update { it.copy(electronic = songs.take(7)) } }
        )

        for ((genre, updateState) in genres) {
            musicCatalogRepository.getSongsByGenre(genre)
                .catch { emit(emptyList()) }
                .onEach { songs ->
                    updateState(songs)
                }
                .launchIn(viewModelScope)
        }
    }

    /** Manually refresh all home data (trending + genres + favorites + recently played). */
    fun refresh() {
        loadHomeData()
    }

    private fun loadUser() {
        authRepository.getCurrentUser()
            .onEach { _user.value = it }
            .catch { _user.value = null }
            .launchIn(viewModelScope)
    }

    private fun loadCommunityPlaylists() {
        viewModelScope.launch {
            playlistRepository.getCommunityPlaylists()
                .onSuccess { playlists ->
                    _uiState.update { it.copy(communityPlaylists = playlists) }
                }
                .onFailure { Timber.e(it, "loadCommunityPlaylists failed") }
        }
    }

    private fun loadCuratedPlaylists() {
        viewModelScope.launch {
            val curated = listOf(
                CuratedPlaylist(
                    id = "curated_chill",
                    title = "Chill Vibes",
                    subtitle = "Relax and unwind",
                    coverUrl = null,
                    gradientColors = listOf("#667eea", "#764ba2"),
                    songs = emptyList(),
                    tag = "chill"
                ),
                CuratedPlaylist(
                    id = "curated_workout",
                    title = "Workout Mode",
                    subtitle = "Pump up your session",
                    coverUrl = null,
                    gradientColors = listOf("#f093fb", "#f5576c"),
                    songs = emptyList(),
                    tag = "workout"
                ),
                CuratedPlaylist(
                    id = "curated_party",
                    title = "Party Hits",
                    subtitle = "Get the party started",
                    coverUrl = null,
                    gradientColors = listOf("#4facfe", "#00f2fe"),
                    songs = emptyList(),
                    tag = "party"
                ),
                CuratedPlaylist(
                    id = "curated_focus",
                    title = "Deep Focus",
                    subtitle = "Concentrate better",
                    coverUrl = null,
                    gradientColors = listOf("#43e97b", "#38f9d7"),
                    songs = emptyList(),
                    tag = "focus"
                )
            )
            _uiState.update { it.copy(curatedPlaylists = curated) }
        }
    }

    private fun loadTrendingArtists() {
        viewModelScope.launch {
            artistRepository.searchArtists("music")
                .onSuccess { artists ->
                    _uiState.update { it.copy(trendingArtists = artists.take(10)) }
                }
                .onFailure { Timber.e(it, "loadTrendingArtists failed") }
        }
    }

    private fun loadLanguagePlaylists() {
        viewModelScope.launch {
            val queries = listOf(
                "Hindi songs playlist",
                "English hits playlist",
                "Telugu songs playlist",
                "Tamil songs playlist",
                "Spanish music playlist",
                "Korean music playlist"
            )
            val languagePlaylists = mutableListOf<CommunityPlaylist>()
            for (query in queries) {
                playlistRepository.searchPlaylists(query)
                    .onSuccess { results ->
                        results.firstOrNull()?.let { languagePlaylists.add(it) }
                    }
            }
            _uiState.update { it.copy(languagePlaylists = languagePlaylists) }
        }
    }

    fun likeCommunityPlaylist(playlistId: String) {
        viewModelScope.launch {
            playlistRepository.likeCommunityPlaylist(playlistId)
                .onSuccess {
                    loadCommunityPlaylists()
                }
                .onFailure { Timber.e(it, "likeCommunityPlaylist failed") }
        }
    }

    fun saveCommunityPlaylist(playlist: CommunityPlaylist) {
        viewModelScope.launch {
            playlistRepository.saveCommunityPlaylist(playlist)
                .onSuccess {
                    loadCommunityPlaylists()
                }
                .onFailure { Timber.e(it, "saveCommunityPlaylist failed") }
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
    val communityPlaylists: List<CommunityPlaylist> = emptyList(),
    val curatedPlaylists: List<CuratedPlaylist> = emptyList(),
    val trendingArtists: List<Artist> = emptyList(),
    val languagePlaylists: List<CommunityPlaylist> = emptyList(),
    val isLoading: Boolean = false
)

