package com.watermelon.app.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.watermelon.data.remote.radio.RadioBrowserApi
import com.watermelon.data.remote.radio.toDomain
import com.watermelon.domain.model.RadioCountry
import com.watermelon.domain.model.RadioLanguage
import com.watermelon.domain.model.RadioStation
import com.watermelon.domain.repository.RadioStationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

enum class RadioTab(val label: String) {
    BROWSE("Browse"),
    LANGUAGES("Languages"),
    SEARCH("Search"),
    FAVORITES("Favorites"),
    RECENT("Recent")
}

data class RadioUiState(
    val selectedTab: RadioTab = RadioTab.BROWSE,
    val isLoading: Boolean = false,
    val error: String? = null,

    // Browse
    val countries: List<RadioCountry> = emptyList(),
    val selectedCountry: RadioCountry? = null,
    val countryStations: List<RadioStation> = emptyList(),

    // Languages
    val languages: List<RadioLanguage> = emptyList(),
    val selectedLanguage: String? = null,
    val languageStations: List<RadioStation> = emptyList(),

    // Search
    val searchQuery: String = "",
    val searchResults: List<RadioStation> = emptyList(),
    val isSearching: Boolean = false,

    // Local
    val favoriteStations: List<RadioStation> = emptyList(),
    val recentStations: List<RadioStation> = emptyList()
)

@HiltViewModel
class RadioViewModel @Inject constructor(
    private val api: RadioBrowserApi,
    private val radioStationRepository: RadioStationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RadioUiState())
    val uiState: StateFlow<RadioUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadCountries()
        loadLanguages()
        observeLocalStations()
    }

    private fun observeLocalStations() {
        viewModelScope.launch {
            radioStationRepository.getFavoriteStations().collect { favorites ->
                _uiState.update { it.copy(favoriteStations = favorites) }
            }
        }
        viewModelScope.launch {
            radioStationRepository.getRecentStations().collect { recent ->
                _uiState.update { it.copy(recentStations = recent) }
            }
        }
    }

    fun selectTab(tab: RadioTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    private fun loadCountries() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val countries = api.getCountries()
                    .filter { it.name.isNotBlank() }
                    .sortedByDescending { it.stationcount }
                    .map { it.toDomain() }
                val india = countries.find { it.name.equals("India", ignoreCase = true) }
                _uiState.update { it.copy(countries = countries, isLoading = false) }
                if (india != null) {
                    selectCountry(india)
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load countries")
                _uiState.update { it.copy(isLoading = false, error = "Failed to load countries") }
            }
        }
    }

    fun selectCountry(country: RadioCountry) {
        _uiState.update { it.copy(selectedCountry = country, isLoading = true, countryStations = emptyList()) }
        viewModelScope.launch {
            try {
                val stations = api.searchStations(country = country.name, limit = 100)
                    .filter { !it.url.isNullOrBlank() }
                    .sortedWith(compareByDescending<com.watermelon.data.remote.radio.RadioStationDto> { it.votes }.thenByDescending { it.bitrate })
                    .map { it.toDomain() }
                _uiState.update { it.copy(countryStations = stations, isLoading = false) }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load stations for ${country.name}")
                _uiState.update { it.copy(isLoading = false, error = "Failed to load stations") }
            }
        }
    }

    fun clearCountry() {
        _uiState.update { it.copy(selectedCountry = null, countryStations = emptyList()) }
    }

    private fun loadLanguages() {
        viewModelScope.launch {
            try {
                val languages = api.getLanguages()
                    .filter { it.name.isNotBlank() }
                    .sortedByDescending { it.stationcount }
                    .map { it.toDomain() }
                _uiState.update { it.copy(languages = languages) }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load languages")
            }
        }
    }

    fun selectLanguage(language: String) {
        _uiState.update { it.copy(selectedLanguage = language, isLoading = true, languageStations = emptyList()) }
        viewModelScope.launch {
            try {
                val stations = api.searchStations(language = language, limit = 100)
                    .filter { !it.url.isNullOrBlank() }
                    .sortedWith(compareByDescending<com.watermelon.data.remote.radio.RadioStationDto> { it.votes }.thenByDescending { it.bitrate })
                    .map { it.toDomain() }
                _uiState.update { it.copy(languageStations = stations, isLoading = false) }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load stations for language $language")
                _uiState.update { it.copy(isLoading = false, error = "Failed to load stations") }
            }
        }
    }

    fun clearLanguage() {
        _uiState.update { it.copy(selectedLanguage = null, languageStations = emptyList()) }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        if (query.isBlank()) {
            _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(400)
            _uiState.update { it.copy(isSearching = true) }
            try {
                val results = api.searchStations(name = query, limit = 50)
                    .filter { !it.url.isNullOrBlank() }
                    .sortedWith(compareByDescending<com.watermelon.data.remote.radio.RadioStationDto> { it.votes }.thenByDescending { it.bitrate })
                    .map { it.toDomain() }
                _uiState.update { it.copy(searchResults = results, isSearching = false) }
            } catch (e: Exception) {
                Timber.e(e, "Search failed")
                _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
            }
        }
    }

    fun toggleFavorite(station: RadioStation) {
        viewModelScope.launch {
            val uuid = station.stationuuid ?: "${station.name}_${station.url}"
            val isFav = _uiState.value.favoriteStations.any {
                (it.stationuuid ?: "${it.name}_${it.url}") == uuid
            }
            if (isFav) {
                radioStationRepository.removeFavorite(uuid)
            } else {
                radioStationRepository.addFavorite(station)
            }
        }
    }

    fun isFavorite(station: RadioStation): Boolean {
        val uuid = station.stationuuid ?: "${station.name}_${station.url}"
        return _uiState.value.favoriteStations.any {
            (it.stationuuid ?: "${it.name}_${it.url}") == uuid
        }
    }

    fun recordRecentlyPlayed(station: RadioStation) {
        viewModelScope.launch {
            radioStationRepository.recordRecentlyPlayed(station)
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
