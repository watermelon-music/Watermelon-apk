package com.watermelon.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.watermelon.domain.model.Playlist
import com.watermelon.domain.model.CommunityPlaylist
import com.watermelon.domain.model.Song
import com.watermelon.domain.model.Artist
import com.watermelon.domain.repository.ArtistRepository
import com.watermelon.domain.repository.MusicCatalogRepository
import com.watermelon.domain.repository.PlaylistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val musicCatalogRepository: MusicCatalogRepository,
    private val playlistRepository: PlaylistRepository,
    private val artistRepository: ArtistRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _loadingCount = MutableStateFlow(0)
    val isLoading: StateFlow<Boolean> = _loadingCount
        .map { it > 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val results: StateFlow<List<Song>> = _query
        .debounce(300)
        .flatMapLatest { q ->
            flow {
                _loadingCount.value += 1
                try {
                    emitAll(musicCatalogRepository.search(q))
                } finally {
                    _loadingCount.value = (_loadingCount.value - 1).coerceAtLeast(0)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()

    private val _showAddToPlaylistSheet = MutableStateFlow(false)
    val showAddToPlaylistSheet: StateFlow<Boolean> = _showAddToPlaylistSheet.asStateFlow()

    private val _selectedSong = MutableStateFlow<Song?>(null)
    val selectedSong: StateFlow<Song?> = _selectedSong.asStateFlow()

    private val _addToPlaylistMessage = MutableStateFlow<String?>(null)
    val addToPlaylistMessage: StateFlow<String?> = _addToPlaylistMessage.asStateFlow()

    private val _selectedCategory = MutableStateFlow(SearchCategory.ALL)
    val selectedCategory: StateFlow<SearchCategory> = _selectedCategory.asStateFlow()

    private val _playlistResults = MutableStateFlow<List<CommunityPlaylist>>(emptyList())
    val playlistResults: StateFlow<List<CommunityPlaylist>> = _playlistResults.asStateFlow()

    private val _artistResults = MutableStateFlow<List<Artist>>(emptyList())
    val artistResults: StateFlow<List<Artist>> = _artistResults.asStateFlow()

    init {
        loadPlaylists()
        observeQueryChanges()
    }

    private fun observeQueryChanges() {
        viewModelScope.launch {
            _query.collect { query ->
                if (query.length >= 2) {
                    when (_selectedCategory.value) {
                        SearchCategory.ALL, SearchCategory.PLAYLISTS -> searchPlaylists(query)
                        SearchCategory.SONGS -> { /* Song search handled by flow */ }
                        SearchCategory.ARTISTS -> searchArtists(query)
                    }
                    if (_selectedCategory.value == SearchCategory.ALL) {
                        searchArtists(query)
                    }
                } else {
                    _playlistResults.value = emptyList()
                    _artistResults.value = emptyList()
                }
            }
        }
    }

    private suspend fun searchPlaylists(query: String) {
        playlistRepository.searchPlaylists(query)
            .onSuccess { results -> _playlistResults.value = results }
            .onFailure { _playlistResults.value = emptyList() }
    }

    private fun searchArtists(query: String) {
        viewModelScope.launch {
            artistRepository.searchArtists(query)
                .onSuccess { _artistResults.value = it }
                .onFailure { _artistResults.value = emptyList() }
        }
    }

    private fun loadPlaylists() {
        playlistRepository.getUserPlaylists()
            .catch { /* silently ignore */ }
            .onEach { _playlists.value = it }
            .launchIn(viewModelScope)
    }

    fun onQueryChange(newQuery: String) {
        _query.update { newQuery }
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
                "Failed to add song"
            }
            _showAddToPlaylistSheet.value = false
            _selectedSong.value = null
        }
    }

    fun clearAddToPlaylistMessage() {
        _addToPlaylistMessage.value = null
    }

    fun onCategorySelected(category: SearchCategory) {
        _selectedCategory.value = category
        if (category == SearchCategory.PLAYLISTS && _query.value.length >= 2) {
            viewModelScope.launch {
                searchPlaylists(_query.value)
            }
        }
        if (category == SearchCategory.ARTISTS && _query.value.length >= 2) {
            searchArtists(_query.value)
        }
        if (category == SearchCategory.ALL && _query.value.length >= 2) {
            searchArtists(_query.value)
        }
    }

    fun onPlaylistClick(playlist: CommunityPlaylist) {
        viewModelScope.launch {
            _addToPlaylistMessage.value = "Loading playlist..."
            val url = playlist.id.removePrefix("ytpl_")
            playlistRepository.fetchPlaylistSongs(url)
                .onSuccess { songs ->
                    _addToPlaylistMessage.value = null
                    if (songs.isNotEmpty()) {
                        _playPlaylistEvent.value = songs
                    } else {
                        _addToPlaylistMessage.value = "Playlist is empty"
                    }
                }
                .onFailure {
                    _addToPlaylistMessage.value = "Failed to load playlist"
                }
        }
    }

    private val _playPlaylistEvent = kotlinx.coroutines.flow.MutableStateFlow<List<Song>?>(null)
    val playPlaylistEvent: kotlinx.coroutines.flow.StateFlow<List<Song>?> = _playPlaylistEvent.asStateFlow()

    fun clearPlayPlaylistEvent() {
        _playPlaylistEvent.value = null
    }

    fun onSavePlaylist(playlist: CommunityPlaylist) {
        viewModelScope.launch {
            playlistRepository.saveCommunityPlaylist(playlist)
                .onSuccess { _addToPlaylistMessage.value = "Saved to your library" }
                .onFailure { _addToPlaylistMessage.value = "Failed to save" }
        }
    }
}

enum class SearchCategory {
    ALL, SONGS, PLAYLISTS, ARTISTS
}
