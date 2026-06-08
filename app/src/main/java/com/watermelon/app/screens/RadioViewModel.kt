package com.watermelon.app.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.watermelon.data.remote.radio.RadioBrowserApi
import com.watermelon.data.remote.radio.RadioStationDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class RadioViewModel @Inject constructor(
    private val api: RadioBrowserApi
) : ViewModel() {

    private val _categories = MutableStateFlow<List<String>>(emptyList())
    val categories: StateFlow<List<String>> = _categories.asStateFlow()

    private val _countries = MutableStateFlow<List<String>>(emptyList())
    val countries: StateFlow<List<String>> = _countries.asStateFlow()

    private val _languages = MutableStateFlow<List<String>>(emptyList())
    val languages: StateFlow<List<String>> = _languages.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String>("")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _selectedCountry = MutableStateFlow<String>("")
    val selectedCountry: StateFlow<String> = _selectedCountry.asStateFlow()

    private val _selectedLanguage = MutableStateFlow<String>("")
    val selectedLanguage: StateFlow<String> = _selectedLanguage.asStateFlow()

    private val _stations = MutableStateFlow<List<RadioStationDto>>(emptyList())
    val stations: StateFlow<List<RadioStationDto>> = _stations.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadFilters()
    }

    private fun loadFilters() {
        viewModelScope.launch {
            _isLoading.value = true
            val (tags, countriesResult, languagesResult) = try {
                val tagsDeferred = async {
                    api.getTags()
                        .filter { it.stationcount > 30 }
                        .sortedByDescending { it.stationcount }
                        .take(30)
                        .map { it.name }
                }
                val countriesDeferred = async {
                    api.getCountries()
                        .filter { it.stationcount > 20 }
                        .sortedByDescending { it.stationcount }
                        .take(30)
                        .map { it.name }
                }
                val languagesDeferred = async {
                    api.getLanguages()
                        .filter { it.stationcount > 20 }
                        .sortedByDescending { it.stationcount }
                        .take(30)
                        .map { it.name }
                }
                Triple(tagsDeferred.await(), countriesDeferred.await(), languagesDeferred.await())
            } catch (e: Exception) {
                Timber.e(e, "Failed to load radio filters")
                Triple(
                    listOf("pop", "rock", "jazz", "classical", "bollywood", "electronic", "hiphop"),
                    listOf("India", "United States of America", "United Kingdom", "Germany", "France", "Canada", "Australia", "Brazil", "Russia", "Japan"),
                    listOf("english", "hindi", "spanish", "french", "german", "tamil", "telugu", "kannada", "malayalam", "marathi")
                )
            }

            _categories.value = tags
            _countries.value = countriesResult
            _languages.value = languagesResult

            // Auto-select first category and load stations
            if (_selectedCategory.value.isBlank() && tags.isNotEmpty()) {
                _selectedCategory.value = tags.first()
            }
            searchStations()
            _isLoading.value = false
        }
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
        searchStations()
    }

    fun selectCountry(country: String) {
        _selectedCountry.value = country
        searchStations()
    }

    fun selectLanguage(language: String) {
        _selectedLanguage.value = language
        searchStations()
    }

    fun clearFilters() {
        _selectedCountry.value = ""
        _selectedLanguage.value = ""
        searchStations()
    }

    private fun searchStations() {
        val tag = _selectedCategory.value
        if (tag.isBlank()) return

        viewModelScope.launch {
            _isLoading.value = true
            val country = _selectedCountry.value
            val language = _selectedLanguage.value
            try {
                val result = api.searchStations(
                    tag = tag,
                    country = country.takeIf { it.isNotBlank() },
                    language = language.takeIf { it.isNotBlank() },
                    limit = 50
                )
                _stations.value = result.takeIf { it.isNotEmpty() } ?: fallbackStations(tag, country, language)
            } catch (e: Exception) {
                Timber.e(e, "Radio search failed for tag=$tag country=$country lang=$language")
                _stations.value = fallbackStations(tag, country, language)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun fallbackStations(tag: String, country: String, language: String): List<RadioStationDto> {
        val raw = when (tag.lowercase()) {
            "pop" -> listOf(
                RadioStationDto("BBC Radio 1", "http://stream.live.vc.bbcmedia.co.uk/bbc_radio_one", country = "United Kingdom", bitrate = 128, tags = "pop"),
                RadioStationDto("Capital FM", "https://media-ssl.musicradio.com/Capital", country = "United Kingdom", bitrate = 128, tags = "pop"),
                RadioStationDto("Radio Disney", "https://live.wostreaming.net/direct/disney-disneydlmp3-ibc2", country = "United States of America", bitrate = 128, tags = "pop"),
                RadioStationDto("Europe 1", "https://stream.europe1.fr/europe1.mp3", country = "France", bitrate = 128, tags = "pop"),
                RadioStationDto("Radio Mirchi", "https://mirchitap.akamaized.net/mirchitap_mp3", country = "India", bitrate = 64, tags = "pop,bollywood"),
                RadioStationDto("Hit Radio FFH", "https://mp3.ffh.de/radioffh/hqlivestream.mp3", country = "Germany", bitrate = 128, tags = "pop")
            )
            "rock" -> listOf(
                RadioStationDto("Classic Rock Florida", "https://listen.181fm.com/181-classicrock_128k.mp3", country = "United States of America", bitrate = 128, tags = "rock"),
                RadioStationDto("Absolute Classic Rock", "https://edge-bauerall-01-gos2.sharp-stream.com/absoluteclassicrock.mp3", country = "United Kingdom", bitrate = 128, tags = "rock"),
                RadioStationDto("Radio BOB", "https://streams.radiobob.de/bob-national/mp3-192/streams.radiobob.de/", country = "Germany", bitrate = 192, tags = "rock"),
                RadioStationDto("Rock Antenne", "https://stream.rockantenne.de/rockantenne/stream/mp3", country = "Germany", bitrate = 128, tags = "rock")
            )
            "jazz" -> listOf(
                RadioStationDto("Jazz24", "https://live.wostreaming.net/direct/ppm-jazz24mp3-ibc1", country = "United States of America", bitrate = 128, tags = "jazz"),
                RadioStationDto("Smooth Jazz", "https://stream.revma.ihrhls.com/zc6280/hls.m3u8", country = "United States of America", bitrate = 128, tags = "jazz"),
                RadioStationDto("Radio Swiss Jazz", "http://stream.srg-ssr.ch/m/rsj/mp3_128", country = "Switzerland", bitrate = 128, tags = "jazz")
            )
            "classical" -> listOf(
                RadioStationDto("Classic FM", "https://media-ssl.musicradio.com/ClassicFM", country = "United Kingdom", bitrate = 128, tags = "classical"),
                RadioStationDto("Radio Swiss Classic", "http://stream.srg-ssr.ch/m/rsc_de/mp3_128", country = "Switzerland", bitrate = 128, tags = "classical"),
                RadioStationDto("ABC Classic", "http://live-radio01.mediahubaustralia.com/CLASIC/mp3/", country = "Australia", bitrate = 128, tags = "classical")
            )
            "bollywood" -> listOf(
                RadioStationDto("Radio Mirchi Bollywood", "https://mirchitap.akamaized.net/mirchitap_mp3", country = "India", bitrate = 64, tags = "bollywood,hindi"),
                RadioStationDto("Bollywood Radio", "https://stream.zeno.fm/rm4i9pdpxrquv", country = "India", bitrate = 128, tags = "bollywood"),
                RadioStationDto("Desi Music Mix", "https://icecast2.radiomast.io/d3bf2191-5010-4710-8231-1f51a9fd64b6", country = "India", bitrate = 128, tags = "bollywood")
            )
            "electronic" -> listOf(
                RadioStationDto("DI.FM Vocal Trance", "https://prem4.di.fm/vocaltrance_hi?", country = "United States of America", bitrate = 256, tags = "electronic"),
                RadioStationDto("Techno Club Radio", "https://stream.laut.fm/techno-club-radio", country = "Germany", bitrate = 128, tags = "electronic"),
                RadioStationDto("Minimal Mix Radio", "https://minimalmix.de/listen.m3u", country = "Germany", bitrate = 128, tags = "electronic")
            )
            "hiphop" -> listOf(
                RadioStationDto("Hot 97", "https://playerservices.streamtheworld.com/api/livestream-redirect/WQHTFM.mp3", country = "United States of America", bitrate = 128, tags = "hiphop"),
                RadioStationDto("Power 106", "https://playerservices.streamtheworld.com/api/livestream-redirect/KPWRAAC.aac", country = "United States of America", bitrate = 128, tags = "hiphop"),
                RadioStationDto("BBC 1Xtra", "http://stream.live.vc.bbcmedia.co.uk/bbc_1xtra", country = "United Kingdom", bitrate = 128, tags = "hiphop")
            )
            else -> listOf(
                RadioStationDto("BBC World Service", "http://stream.live.vc.bbcmedia.co.uk/bbc_world_service", country = "United Kingdom", bitrate = 128, tags = "news"),
                RadioStationDto("NPR News", "https://npr-ice.streamguys1.com/live.mp3", country = "United States of America", bitrate = 128, tags = "news")
            )
        }
        // Apply country/language filter to fallback
        return raw.filter { station ->
            (country.isBlank() || station.country.equals(country, ignoreCase = true)) &&
            (language.isBlank() || station.language.equals(language, ignoreCase = true) || station.tags?.contains(language, ignoreCase = true) == true)
        }
    }
}
